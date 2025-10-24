# Field Scope 判断逻辑详解

## 概述

在提取 Git Diff 变更时，系统需要判断每个 Field（字段/变量）属于：
- **ClassOrInterface**（类成员变量）
- **Method**（方法局部变量）

这个判断通过 `scope` 字段体现。

---

## 判断流程

### 总体策略

Field 提取分为**两个独立的步骤**：

```
extractFieldChanges()
  ├─ extractClassFields()        → scope = "ClassOrInterface"
  └─ extractLocalVariables()     → scope = "Method"
```

### 第1步：提取类成员变量（extractClassFields）

**判断逻辑：使用 JavaParser 的 AST 分析**

```java
// 1. 使用 JavaParser 解析整个源文件
CompilationUnit cu = parseSourceFile(projectRoot, filePath);

// 2. 查找所有类级别的字段声明
List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);

// 3. 遍历每个字段
for (FieldDeclaration field : fields) {
    for (VariableDeclarator var : field.getVariables()) {
        String fieldName = var.getNameAsString();
        
        // 4. 检查该字段是否在 diff 的新增/删除行中
        if (hunk.getAddedLines() 包含 fieldName) {
            // 创建 Field 变更记录
            change.setScope("ClassOrInterface");  // ← 设置为类成员
        }
    }
}
```

**关键特征：**
- ✅ 使用 JavaParser 的 AST（抽象语法树）精确定位
- ✅ `cu.findAll(FieldDeclaration.class)` 只返回类级别的字段
- ✅ 自动排除方法内的局部变量
- ✅ 支持识别 `private/public/protected/static/final` 等修饰符

**示例：**
```java
public class Example {
    private String userName;     // ← scope = "ClassOrInterface"
    private static int count;    // ← scope = "ClassOrInterface"
    
    public void process() {
        String temp = "test";    // ← 不在这里提取
    }
}
```

---

### 第2步：提取局部变量（extractLocalVariables）

**判断逻辑：在方法范围内查找变量声明**

```java
// 1. 使用 JavaParser 查找所有方法
List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

// 2. 遍历每个方法
for (MethodDeclaration method : methods) {
    String methodName = method.getNameAsString();
    
    // 3. 查找该方法内的所有变量声明
    List<VariableDeclarationExpr> varDecls = method.findAll(
        VariableDeclarationExpr.class
    );
    
    // 4. 遍历每个局部变量
    for (VariableDeclarationExpr varDecl : varDecls) {
        for (VariableDeclarator var : varDecl.getVariables()) {
            String varName = var.getNameAsString();
            
            // 5. 检查该变量是否在 diff 的新增/删除行中
            if (hunk.getAddedLines() 包含 varName) {
                // 创建 Field 变更记录
                change.setScope("Method");           // ← 设置为局部变量
                change.setMethodName(methodName);    // ← 记录所属方法
                varToMethod.put(varName, methodName); // ← 建立变量→方法映射
            }
        }
    }
}
```

**关键特征：**
- ✅ 使用 JavaParser 的 `method.findAll(VariableDeclarationExpr.class)`
- ✅ 只在方法体内查找变量声明
- ✅ 自动记录变量所属的方法名（`methodName`）
- ✅ 通过 `varToMethod` 映射维护变量-方法关系

**示例：**
```java
public class Example {
    public void process(String input) {
        String temp = input.trim();      // ← scope = "Method", methodName = "process"
        int count = 0;                   // ← scope = "Method", methodName = "process"
        
        for (int i = 0; i < 10; i++) {   // ← scope = "Method", methodName = "process"
            count++;
        }
    }
    
    public void calculate() {
        double result = 0.0;             // ← scope = "Method", methodName = "calculate"
    }
}
```

---

## 辅助判断方法

### containsFieldDeclaration()

**用于：判断一行代码是否包含类成员字段声明**

```java
private boolean containsFieldDeclaration(String line, String fieldName) {
    String trimmed = line.trim();
    return (
        trimmed.contains(fieldName) &&
        (trimmed.contains("static") ||
         trimmed.contains("private") ||
         trimmed.contains("public") ||
         trimmed.contains("protected"))
    );
}
```

**判断依据：**
- ✅ 包含字段名
- ✅ 包含访问修饰符（`private/public/protected`）或 `static`

**示例：**
```java
"private String userName;"           // ✅ 匹配
"public static final int MAX = 100;" // ✅ 匹配
"String temp = userName;"            // ❌ 不匹配（无修饰符）
```

---

### containsVariableDeclaration()

**用于：判断一行代码是否包含变量声明**

```java
private boolean containsVariableDeclaration(String line, String varName) {
    String trimmed = line.trim();
    return (
        trimmed.contains(varName) &&
        (trimmed.contains("=") ||
         trimmed.matches(".*\\s+" + varName + "\\s*[;,)].*"))
    );
}
```

**判断依据：**
- ✅ 包含变量名
- ✅ 包含赋值符号 `=` 或者匹配声明模式

**示例：**
```java
"String temp = input.trim();"        // ✅ 匹配（有 =）
"int count;"                         // ✅ 匹配（有 ;）
"for (int i = 0; i < 10; i++)"      // ✅ 匹配
"temp.trim()"                        // ❌ 不匹配（只是使用）
```

---

### parseFieldNameFromLine()

**用于：从代码行中解析字段名（用于处理已删除的字段）**

```java
private String parseFieldNameFromLine(String line) {
    // 1. 跳过注释和空行
    if (trimmed.isEmpty() || trimmed.startsWith("//") || ...) {
        return null;
    }
    
    // 2. 跳过类声明、方法声明
    if (trimmed.contains("class ") || trimmed.matches(".*\\(.*\\).*")) {
        return null;
    }
    
    // 3. 匹配字段声明模式：[modifiers] Type fieldName [= value];
    if (trimmed.contains("static") || trimmed.contains("private") || ...) {
        // 提取字段名（类型后的第一个标识符）
        return extractFieldName(parts);
    }
    
    return null;
}
```

**解析逻辑：**

```
输入: "private static final String CONFIG_NAME = \"value\";"

步骤:
1. 跳过修饰符: private, static, final
2. 识别类型: String
3. 提取字段名: CONFIG_NAME ← 返回这个
4. 忽略后续: = "value";
```

**示例：**
```java
"private String userName;"                    → "userName"
"public static final int MAX_SIZE = 100;"     → "MAX_SIZE"
"protected List<String> items = new ArrayList<>();" → "items"
"public void process() {"                     → null（方法声明）
"// This is a comment"                        → null（注释）
```

---

## 完整的判断流程图

```
Git Diff Hunk
    ↓
解析源文件（JavaParser）
    ↓
    ├─── extractClassFields()
    │    ├─ cu.findAll(FieldDeclaration.class)
    │    │  └─ 只查找类级别的字段
    │    ├─ 检查字段是否在 addedLines/removedLines
    │    └─ 设置 scope = "ClassOrInterface"
    │
    └─── extractLocalVariables()
         ├─ cu.findAll(MethodDeclaration.class)
         │  └─ 遍历每个方法
         ├─ method.findAll(VariableDeclarationExpr.class)
         │  └─ 只查找该方法内的变量
         ├─ 检查变量是否在 addedLines/removedLines
         ├─ 建立 varToMethod 映射（变量 → 方法名）
         └─ 设置 scope = "Method" + methodName
```

---

## 关键数据结构

### varToMethod 映射

```java
Map<String, String> varToMethod = new HashMap<>();

// 示例内容：
varToMethod.put("temp", "process");      // 变量 temp 属于 process 方法
varToMethod.put("count", "process");     // 变量 count 属于 process 方法
varToMethod.put("result", "calculate");  // 变量 result 属于 calculate 方法
```

这个映射用于：
1. 记录局部变量所属的方法
2. 在生成 ChangeInfo 时填充 `methodName` 字段

---

## 输出示例

### 类成员变量

```json
{
  "entity_type": "Field",
  "className": "com.example.User",
  "fieldName": "userName",
  "scope": "ClassOrInterface",
  "changeType": "ADD",
  "addedLines": ["private String userName;"]
}
```

### 局部变量

```json
{
  "entity_type": "Field",
  "className": "com.example.Service",
  "fieldName": "temp",
  "methodName": "processData",
  "scope": "Method",
  "changeType": "ADD",
  "addedLines": ["String temp = input.trim();"]
}
```

---

## 边界情况处理

### 1. 已删除的字段（JavaParser 无法解析）

**问题：** 字段已被删除，源文件中不存在，JavaParser 找不到

**解决：** 使用 `parseFieldNameFromLine()` 直接从 `removedLines` 解析

```java
// 对于 removedLines，JavaParser 可能无法找到（因为字段已被删除）
for (String line : hunk.getRemovedLines()) {
    String fieldName = parseFieldNameFromLine(line);
    if (fieldName != null && !removedFieldLines.containsKey(fieldName)) {
        removedFieldLines.put(fieldName, line);
    }
}
```

### 2. 同名变量在不同方法

**问题：** 不同方法可能有同名的局部变量

**解决：** 通过 `varToMethod` 映射区分，每个变量记录其所属方法

```java
public void method1() {
    String temp = "a";  // temp → method1
}

public void method2() {
    String temp = "b";  // temp → method2
}
```

生成两条独立的 Field 记录：
- Field: temp, methodName: method1, scope: Method
- Field: temp, methodName: method2, scope: Method

### 3. 方法参数

**当前行为：** 方法参数暂不作为 Field 提取

**原因：** 参数属于方法签名的一部分，应该在 Method 的 `signatureChange` 中体现

---

## 优缺点分析

### 优点

✅ **精确性高**：使用 JavaParser AST，避免正则表达式的误判  
✅ **层级清晰**：类成员和局部变量分开处理  
✅ **上下文完整**：局部变量记录了所属方法名  
✅ **容错性好**：对已删除的字段有兜底方案

### 缺点

❌ **依赖源文件**：需要能访问完整的源文件（不能只靠 diff）  
❌ **解析开销**：每个文件都需要完整的 AST 解析  
❌ **复杂场景**：嵌套类、匿名类的字段可能识别不完整

---

## 总结

**判断核心：通过 JavaParser 的 AST 区分字段的声明位置**

- **类级别** → `cu.findAll(FieldDeclaration.class)` → scope = "ClassOrInterface"
- **方法级别** → `method.findAll(VariableDeclarationExpr.class)` → scope = "Method"

这种方式利用了 Java 的语法结构，比纯正则匹配更准确和可靠。

