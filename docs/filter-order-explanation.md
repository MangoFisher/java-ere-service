# Git Diff 变更过滤流程详解

## 过滤规则的完整执行顺序

过滤分为 **两大阶段** 和 **四个层级**：

---

## 📋 完整流程图

```
Git Diff 文件
    ↓
[1] 解析 Git Diff → DiffHunks
    ↓
[2] 提取变更（JavaChangeExtractor）
    ├─ ⚡ 【第1层】代码行过滤（CodeLineFilter）
    │   ├─ 过滤空行
    │   ├─ 过滤注释
    │   ├─ 过滤日志语句（logger.*, System.out.*）
    │   └─ 过滤导入/包声明
    │   结果：addedLines 和 removedLines 已经是纯业务代码
    ↓
    生成 Field/Method/ClassOrInterface 变更记录
    ↓
[3] 后处理（postProcessChanges）
    └─ 合并跨 hunks 的同一个类的 ClassOrInterface 记录
    ↓
[4] 实体过滤（CompositeChangeFilter）
    ├─ 🔍 【第2层】通用过滤（CommonFilter）
    │   ├─ 文件路径过滤（excludePaths）
    │   ├─ 包名过滤（includePackages）
    │   ├─ 变更类型过滤（changeTypes: ADD/MODIFY/DELETE）
    │   └─ 类名过滤（excludeClassNames）
    │   结果：所有实体类型都经过通用规则筛选
    ↓
    按实体类型分组（Field/Method/ClassOrInterface）
    ↓
    ├─ 🎯 【第3层】Field 专用过滤（FieldChangeFilter）
    │   ├─ 作用域过滤（scopes: ClassOrInterface/Method）
    │   ├─ 常量过滤（constantsOnly）
    │   └─ 字段名过滤（excludeFieldNames/includeFieldNames）
    │
    ├─ 🎯 【第4层】Method 专用过滤（MethodChangeFilter）
    │   ├─ 签名变更过滤（signatureChangedOnly）
    │   ├─ 最小变更行数（minChangedLines）
    │   └─ 方法名过滤（excludeMethodNames/includeMethodNames）
    │
    └─ 🎯 【第4层】ClassOrInterface 专用过滤（ClassChangeFilter）
        ├─ 最小变更行数（minChangedLines）
        └─ 类名过滤（excludeClassNames/includeClassNames）
    ↓
合并结果 → 输出 JSON
```

---

## 🔢 详细说明

### 【阶段A】提取阶段

#### 第1层：代码行过滤（Code Line Filter）
- **执行时机**：在 `JavaChangeExtractor.extractChanges()` 中
- **作用对象**：每个 DiffHunk 的 `addedLines` 和 `removedLines`
- **过滤内容**：
  - ✅ 空行
  - ✅ 单行注释 `//`
  - ✅ 多行注释 `/* ... */`
  - ✅ Javadoc 注释 `/** ... */`
  - ✅ 日志调用语句：
    - `logger.info/debug/error/warn/trace(...)`
    - `log.info/debug/error/warn(...)`
    - `System.out.println/print/printf(...)`
    - `System.err.println/print/printf(...)`
  - ✅ 导入语句 `import ...`（可选）
  - ✅ 包声明 `package ...`（可选）
  - ✅ 自定义正则模式（如 `printStackTrace()`）

- **配置位置**：`analysis-config.yml` → `git_diff_extraction.code_line_filter`
- **关键特点**：
  - 这是**最早执行**的过滤
  - 过滤的是**代码行本身**，不是实体
  - 过滤后，`ChangeInfo` 的 `addedLines` 和 `removedLines` 中只包含业务代码行

**示例配置：**
```yaml
git_diff_extraction:
  code_line_filter:
    filter_empty_lines: true
    filter_comments: true
    filter_logging_statements: true
    logging_patterns:
      - "logger\\."
      - "System\\.out\\."
```

---

### 【阶段B】过滤阶段

#### 第2层：通用过滤（Common Filter）
- **执行时机**：在 `CompositeChangeFilter.filter()` 的第一步
- **作用对象**：所有实体类型（Field、Method、ClassOrInterface）
- **过滤条件**：
  1. **文件路径**：排除指定路径（如测试文件）
  2. **包名**：只保留指定包（可选）
  3. **变更类型**：只保留指定的变更类型（ADD/MODIFY/DELETE）
  4. **类名**：排除指定类名模式（如 `*Test`、`*Mock`）

- **配置位置**：`analysis-config.yml` → `git_diff_extraction.common`

**示例配置：**
```yaml
git_diff_extraction:
  common:
    exclude_paths:
      - "src/test/*"
      - "*/test/*"
    exclude_class_names:
      - "*Test"
      - "*Mock"
```

---

#### 第3-4层：专用过滤（Specific Filters）
通用过滤后，按实体类型分组，分别应用专用过滤规则。

##### 🔹 Field 专用过滤（FieldChangeFilter）
- **作用对象**：只过滤 `entity_type = "Field"` 的记录
- **过滤条件**：
  1. **作用域**：只保留指定作用域的字段
     - `ClassOrInterface`：类成员变量
     - `Method`：方法局部变量
  2. **常量标识**：只保留常量字段（`constantsOnly: true`）
  3. **字段名**：
     - 排除指定字段名（`excludeFieldNames`）
     - 只保留指定字段名（`includeFieldNames`）

**示例配置：**
```yaml
git_diff_extraction:
  field:
    scopes:
      - "ClassOrInterface"  # 只保留类成员变量
    exclude_field_names:
      - "temp*"
      - "logger"
```

---

##### 🔹 Method 专用过滤（MethodChangeFilter）
- **作用对象**：只过滤 `entity_type = "Method"` 的记录
- **过滤条件**：
  1. **签名变更**：只保留签名变更的方法（`signatureChangedOnly: true`）
  2. **最小变更行数**：过滤变更行数太少的方法（`minChangedLines`）
  3. **方法名**：
     - 排除指定方法名（`excludeMethodNames`）
     - 只保留指定方法名（`includeMethodNames`）

**示例配置：**
```yaml
git_diff_extraction:
  method:
    min_changed_lines: 3  # 过滤变更小于3行的方法
    exclude_method_names:
      - "get*"
      - "set*"
```

---

##### 🔹 ClassOrInterface 专用过滤（ClassChangeFilter）
- **作用对象**：只过滤 `entity_type = "ClassOrInterface"` 的记录
- **过滤条件**：
  1. **最小变更行数**：过滤变更行数太少的类（`minChangedLines`）
  2. **类名**：
     - 排除指定类名（`excludeClassNames`）
     - 只保留指定类名（`includeClassNames`）

**示例配置：**
```yaml
git_diff_extraction:
  class_or_interface:
    min_changed_lines: 5
    exclude_class_names:
      - "*Test"
```

---

## 🎯 关键要点

### 1. 代码行过滤 vs 实体过滤
- **代码行过滤**（第1层）：过滤的是具体的代码行内容
- **实体过滤**（第2-4层）：过滤的是变更记录（ChangeInfo）

### 2. 通用过滤 vs 专用过滤
- **通用过滤**（第2层）：对所有实体类型生效，先执行
- **专用过滤**（第3-4层）：只对特定实体类型生效，后执行

### 3. 过滤是递进式的
- 每一层过滤都会减少记录数量
- 后面的过滤只作用于前面过滤的结果
- 如果通用过滤后没有记录，则跳过专用过滤

### 4. 日志过滤在第1层
你提到的日志过滤（`logger.info`、`System.out.println` 等）是在**第1层（代码行过滤）**生效的，这是最早执行的过滤。

---

## 📊 过滤效果示例

假设原始 Git Diff 有以下内容：

```java
// 原始变更
+ // This is a comment
+ logger.info("Processing data");
+ private String userName;
+ public void setUserName(String name) { this.userName = name; }
+ System.out.println("Debug info");
```

**第1层：代码行过滤**
```java
// 过滤空行、注释、日志后
+ private String userName;
+ public void setUserName(String name) { this.userName = name; }
```

**第2层：通用过滤**
```
假设配置排除了 set* 方法，但这里还保留（因为通用过滤不看方法名）
- Field: userName (保留)
- Method: setUserName (保留)
```

**第3-4层：专用过滤**
```
假设 Method 配置了 exclude_method_names: ["set*"]
- Field: userName (保留)
- Method: setUserName (被过滤掉！)
```

**最终输出**：只有 `Field: userName` 这一条记录

---

## 🔧 如何调试过滤顺序

运行时会输出过滤统计信息：

```
[过滤] 总记录: 1000 -> 500
[过滤]   - Field: 100 -> 80
[过滤]   - Method: 800 -> 400
[过滤]   - ClassOrInterface: 100 -> 20
```

可以通过这些日志判断每一层过滤的效果。

---

## 📝 配置建议

### 场景1：只关注业务逻辑变更
```yaml
git_diff_extraction:
  code_line_filter:
    filter_logging_statements: true  # 过滤日志
  method:
    exclude_method_names:
      - "get*"
      - "set*"
      - "toString"
```

### 场景2：只关注核心类的变更
```yaml
git_diff_extraction:
  common:
    include_packages:
      - "com.example.core"
    exclude_paths:
      - "src/test/*"
```

### 场景3：只关注签名变更
```yaml
git_diff_extraction:
  method:
    signature_changed_only: true
```

---

## 💡 总结

过滤顺序：**代码行过滤（最早）→ 后处理合并 → 通用过滤 → 专用过滤（最后）**

日志过滤在**第1层（代码行过滤）**，是所有过滤中**最早执行**的！

