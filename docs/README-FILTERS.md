# Git Diff 变更提取与过滤系统文档

本目录包含 Git Diff 变更提取和过滤系统的详细文档。

---

## 📚 文档列表

### 1. [过滤顺序详解](filter-order-explanation.md)
**内容：** 完整的过滤流程和执行顺序  
**适合：** 理解整个系统的过滤机制

**主要内容：**
- 四层过滤架构（代码行过滤 → 通用过滤 → 专用过滤）
- 每层过滤的执行时机和作用对象
- 配置示例和调试技巧

---

### 2. [Field Scope 判断逻辑](field-scope-detection.md)
**内容：** 如何判断 Field 属于类成员还是局部变量  
**适合：** 理解 Field 的 scope 字段是如何确定的

**主要内容：**
- JavaParser AST 分析原理
- 类成员变量 vs 局部变量的判断逻辑
- 边界情况处理（已删除字段、同名变量等）

---

## 🎯 快速参考

### 过滤层级总览

| 层级 | 名称 | 时机 | 作用对象 | 配置节点 |
|-----|------|------|---------|----------|
| **第1层** | 代码行过滤 | 提取阶段 | 代码行 | `code_line_filter` |
| **第2层** | 通用过滤 | 过滤阶段 | 所有实体 | `common` |
| **第3层** | 专用过滤 | 过滤阶段 | 特定实体 | `field/method/class_or_interface` |

---

### Field Scope 判断

```
extractFieldChanges()
  ├─ extractClassFields()
  │  └─ cu.findAll(FieldDeclaration.class)
  │     → scope = "ClassOrInterface"
  │
  └─ extractLocalVariables()
     └─ method.findAll(VariableDeclarationExpr.class)
        → scope = "Method"
```

---

## 🔍 常见问题

### Q1: 日志过滤在哪一层生效？
**A:** 在**第1层（代码行过滤）**，这是最早执行的过滤。

配置：
```yaml
git_diff_extraction:
  code_line_filter:
    filter_logging_statements: true
    logging_patterns:
      - "logger\\."
      - "System\\.out\\."
```

---

### Q2: 如何区分类成员变量和局部变量？
**A:** 通过 JavaParser 的 AST 分析：
- 类成员变量：`cu.findAll(FieldDeclaration.class)` → scope = "ClassOrInterface"
- 局部变量：`method.findAll(VariableDeclarationExpr.class)` → scope = "Method"

详见：[Field Scope 判断逻辑](field-scope-detection.md)

---

### Q3: 过滤规则的执行顺序是什么？
**A:** 从早到晚依次为：
1. **代码行过滤**（提取阶段）
2. **后处理合并**（合并跨 hunks 的类变更）
3. **通用过滤**（所有实体）
4. **专用过滤**（按实体类型）

详见：[过滤顺序详解](filter-order-explanation.md)

---

### Q4: getter/setter 方法如何过滤？
**A:** 在**第3层（Method 专用过滤）**：

```yaml
git_diff_extraction:
  method:
    exclude_method_names:
      - "get*"
      - "set*"
```

---

### Q5: 如何只保留签名变更的方法？
**A:** 配置 Method 专用过滤：

```yaml
git_diff_extraction:
  method:
    signature_changed_only: true
```

---

## 🛠️ 配置模板

### 场景1：只关注业务代码变更

```yaml
git_diff_extraction:
  code_line_filter:
    filter_logging_statements: true
    filter_comments: true
    
  common:
    exclude_paths:
      - "src/test/*"
    exclude_class_names:
      - "*Test"
      - "*Mock"
      
  method:
    exclude_method_names:
      - "get*"
      - "set*"
      - "toString"
```

---

### 场景2：只关注核心包的重大变更

```yaml
git_diff_extraction:
  common:
    include_packages:
      - "com.example.core"
      - "com.example.service"
      
  method:
    min_changed_lines: 5
    signature_changed_only: true
    
  class_or_interface:
    min_changed_lines: 10
```

---

### 场景3：完全禁用过滤

```yaml
git_diff_extraction:
  code_line_filter:
    filter_empty_lines: false
    filter_comments: false
    filter_logging_statements: false
```

---

## 📊 调试技巧

### 查看过滤统计

运行时会输出：
```
[过滤] 总记录: 1000 -> 500
[过滤]   - Field: 100 -> 80
[过滤]   - Method: 800 -> 400
[过滤]   - ClassOrInterface: 100 -> 20
```

### 测试代码行过滤

运行演示程序：
```bash
mvn exec:java -Dexec.mainClass="com.java.extractor.util.CodeLineFilterDemo"
```

---

## 🔗 相关文件

### 核心类

- `CodeLineFilter.java` - 代码行过滤器
- `CodeLineFilterConfig.java` - 代码行过滤配置
- `CompositeChangeFilter.java` - 组合过滤器
- `CommonChangeFilter.java` - 通用过滤器
- `FieldChangeFilter.java` - Field 专用过滤器
- `MethodChangeFilter.java` - Method 专用过滤器
- `ClassChangeFilter.java` - ClassOrInterface 专用过滤器
- `JavaChangeExtractor.java` - 变更提取器
- `DiffAnalysisService.java` - Diff 分析服务

### 配置文件

- `analysis-config.yml` - 主配置文件

---

## 💡 最佳实践

1. **先用宽松配置测试**：确保变更能正确提取
2. **逐步启用过滤**：先启用代码行过滤，再启用实体过滤
3. **关注过滤统计**：通过日志判断过滤效果
4. **针对场景调优**：不同场景使用不同的过滤策略

---

## 📝 更新日志

- **2024-01-XX**: 新增代码行过滤功能，支持日志语句过滤
- **2024-01-XX**: 完善 Field scope 判断逻辑
- **2024-01-XX**: 实现四层过滤架构

---

## 🤝 贡献

如有问题或建议，请参考主项目 README。

