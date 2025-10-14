# Java ERE 使用指南

## 目录

1. [快速开始](#快速开始)
2. [使用场景](#使用场景)
3. [配置详解](#配置详解)
4. [最佳实践](#最佳实践)
5. [常见问题](#常见问题)

---

## 快速开始

### 场景1：分析单个文件（向后兼容）

```java
CodeParser parser = new CodeParser();
parser.init("/path/to/project");
Map<String, Entity> result = parser.parseFile(new File("UserService.java"));
```

### 场景2：分析整个项目

```java
// 最简配置
AnalysisConfig config = new AnalysisConfig();
config.setProjectRoot("/path/to/project");
config.setProjectPackages(Arrays.asList("com.example"));

ProjectAnalyzer analyzer = new ProjectAnalyzer();
Map<String, Entity> result = analyzer.analyze(config);
```

### 场景3：只分析特定目录

```java
AnalysisConfig config = new AnalysisConfig();
config.setProjectRoot("/path/to/spring-boot-project");
config.setProjectPackages(Arrays.asList("com.example"));

// 只分析 service 和 controller 层
FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList(
    "**/service/**",
    "**/controller/**"
));
filter.setExcludePatterns(Arrays.asList("**/test/**"));
config.setFilterConfig(filter);

ProjectAnalyzer analyzer = new ProjectAnalyzer();
Map<String, Entity> result = analyzer.analyze(config);
```

---

## 使用场景

### 场景A：Spring Boot 项目完整分析

**目标**：分析整个 Spring Boot 项目，排除测试代码和配置类

```java
AnalysisConfig config = new AnalysisConfig();
config.setProjectRoot("/path/to/spring-boot-project");
config.setProjectPackages(Arrays.asList("com.yourcompany.project"));

// 过滤规则
FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList("src/main/java/**/*.java"));
filter.setExcludePatterns(Arrays.asList(
    "**/test/**",
    "**/config/**",
    "**/*Config.java",
    "**/*Configuration.java"
));
config.setFilterConfig(filter);

// 加载核心库（推荐）
config.getResolverConfig().setDependencyMode("essential");

ProjectAnalyzer analyzer = new ProjectAnalyzer();
Map<String, Entity> result = analyzer.analyze(config);
```

### 场景B：只分析核心业务层

**目标**：只关注 service 层的业务逻辑

```java
AnalysisConfig config = new AnalysisConfig();
config.setProjectRoot("/path/to/project");
config.setProjectPackages(Arrays.asList("com.example"));

FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList("**/service/**"));
filter.setExcludePatterns(Arrays.asList(
    "**/*Test.java",
    "**/*Mock*.java"
));
config.setFilterConfig(filter);

ProjectAnalyzer analyzer = new ProjectAnalyzer();
Map<String, Entity> result = analyzer.analyze(config);
```

### 场景C：分析特定模块

**目标**：多模块项目中只分析某个子模块

```java
AnalysisConfig config = new AnalysisConfig();
config.setProjectRoot("/path/to/multi-module-project");
config.setProjectPackages(Arrays.asList("com.example"));

// 只分析 user-service 模块
FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList("user-service/src/main/java/**/*.java"));
config.setFilterConfig(filter);

ProjectAnalyzer analyzer = new ProjectAnalyzer();
Map<String, Entity> result = analyzer.analyze(config);
```

### 场景D：排除自动生成的代码

**目标**：排除 MyBatis Generator、Lombok 等生成的代码

```java
FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList("src/main/java/**/*.java"));
filter.setExcludePatterns(Arrays.asList(
    "**/generated/**",
    "**/*Mapper.java",
    "**/*MapperExt.java",
    "**/*DO.java",
    "**/*DTO.java",
    "**/*VO.java",
    "**/*Example.java"
));
config.setFilterConfig(filter);
```

---

## 配置详解

### 1. AnalysisConfig（主配置）

```java
AnalysisConfig config = new AnalysisConfig();

// 项目根路径（必需）
config.setProjectRoot("/path/to/project");

// 项目包名（必需）- 用于识别哪些是项目代码
config.setProjectPackages(Arrays.asList("com.example", "cn.mycompany"));

// 源码路径（可选，默认 src/main/java 和 src/test/java）
config.setSourcePaths(Arrays.asList("src/main/java"));
```

### 2. FilterConfig（过滤配置）

```java
FilterConfig filter = new FilterConfig();

// Include 规则：指定要分析的文件
filter.setIncludePatterns(Arrays.asList(
    "src/main/java/**/*.java",     // 所有主代码
    "**/service/**",                // 所有 service 包
    "**/*Controller.java"           // 所有 Controller 类
));

// Exclude 规则：排除不需要的文件（优先级更高）
filter.setExcludePatterns(Arrays.asList(
    "**/test/**",                   // 排除测试目录
    "**/*Test.java",                // 排除测试文件
    "**/generated/**"               // 排除生成代码
));

config.setFilterConfig(filter);
```

**Glob 模式语法**：
- `*` - 匹配单层路径中的任意字符
- `**` - 匹配任意层级的目录
- `?` - 匹配单个字符
- `[abc]` - 匹配括号中的任意字符

### 3. ResolverConfig（符号解析器配置）

```java
ResolverConfig resolver = config.getResolverConfig();

// 依赖模式
resolver.setDependencyMode("essential");  
// - "none": 不加载第三方库（最快）
// - "essential": 只加载核心库（推荐）
// - "all": 加载所有依赖（最慢）

// 自定义核心库模式（可选）
resolver.setEssentialPatterns(Arrays.asList(
    "spring-context",
    "spring-beans",
    "lombok",
    "mybatis"
));

// 依赖目录（可选）
resolver.setLocalDependencyDir("target/dependency");
```

**准备依赖（essential 或 all 模式需要）**：
```bash
cd /path/to/your/project
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
```

### 4. ExtractionConfig（提取配置）

```java
ExtractionConfig extraction = config.getExtractionConfig();

// 第三方库调用处理策略
extraction.setThirdPartyCallStrategy("mark");
// - "ignore": 完全忽略第三方库调用
// - "mark": 标记为外部依赖（推荐）
// - "full": 完整记录（不推荐）

// 是否提取注解（可选）
extraction.setIncludeAnnotations(true);

// 是否提取 Javadoc（可选）
extraction.setIncludeJavadoc(true);
```

---

## 最佳实践

### 1. 性能优化

**小型项目（<100个文件）**
```java
config.getResolverConfig().setDependencyMode("essential");
```

**大型项目（>1000个文件）**
```java
// 第一次运行：不加载依赖，快速了解项目结构
config.getResolverConfig().setDependencyMode("none");

// 第二次运行：加载核心库，获取准确的调用关系
config.getResolverConfig().setDependencyMode("essential");
```

### 2. 分层分析策略

**步骤1：先分析核心业务层**
```java
FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList("**/service/**"));
```

**步骤2：再分析接口层**
```java
filter.setIncludePatterns(Arrays.asList("**/controller/**", "**/api/**"));
```

**步骤3：最后分析数据层**
```java
filter.setIncludePatterns(Arrays.asList("**/dao/**", "**/repository/**"));
```

### 3. 多项目包名配置

```java
// 如果项目有多个包名空间
config.setProjectPackages(Arrays.asList(
    "com.yourcompany.project",      // 主项目包
    "com.yourcompany.common",       // 公共包
    "com.yourcompany.framework"     // 框架包
));
```

### 4. 输出结果保存

```java
Map<String, Entity> result = analyzer.analyze(config);

// 保存为 JSON
Gson gson = new GsonBuilder().setPrettyPrinting().create();
String json = gson.toJson(result);
Files.write(Paths.get("analysis-result.json"), json.getBytes());

// 生成统计信息
long classCount = result.values().stream()
    .filter(e -> "ClassOrInterface".equals(e.getType()))
    .count();
long methodCount = result.values().stream()
    .filter(e -> "Method".equals(e.getType()))
    .count();
System.out.println("类: " + classCount + ", 方法: " + methodCount);
```

---

## 常见问题

### Q1: 为什么提取的实体数量很少？

**可能原因**：
1. 过滤规则太严格，排除了大部分文件
2. projectPackages 配置不正确

**解决方法**：
```java
// 检查实际过滤的文件数量
System.out.println("过滤后文件数: " + targetFiles.size());

// 暂时移除过滤规则测试
FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList("src/main/java/**/*.java"));
// filter.setExcludePatterns(...);  // 先注释掉
```

### Q2: 符号解析失败，无法提取方法调用关系

**可能原因**：
1. 没有加载第三方库
2. 依赖目录不存在

**解决方法**：
```bash
# 准备依赖
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

# 检查依赖目录
ls target/dependency
```

```java
// 至少使用 essential 模式
config.getResolverConfig().setDependencyMode("essential");
```

### Q3: 分析速度很慢

**可能原因**：
1. 加载了太多第三方库
2. 分析的文件太多

**解决方法**：
```java
// 使用 none 或 essential 模式
config.getResolverConfig().setDependencyMode("essential");

// 缩小分析范围
filter.setIncludePatterns(Arrays.asList("**/service/**"));
```

### Q4: 如何查看中间过程？

所有日志会输出到控制台：
```
[符号解析器] 加载源码路径: src/main/java
[符号解析器] 加载核心库: spring-context-5.3.20.jar
[实体提取] 开始解析 50 个文件...
[实体提取] 完成: 成功 48 个, 失败 2 个
```

### Q5: 可以分析多个项目吗？

可以，但需要分别配置和执行：
```java
String[] projects = {"/path/to/project1", "/path/to/project2"};
Map<String, Map<String, Entity>> allResults = new HashMap<>();

for (String projectPath : projects) {
    AnalysisConfig config = new AnalysisConfig();
    config.setProjectRoot(projectPath);
    // ... 其他配置
    
    ProjectAnalyzer analyzer = new ProjectAnalyzer();
    Map<String, Entity> result = analyzer.analyze(config);
    allResults.put(projectPath, result);
}
```

### Q6: 如何只提取类的继承关系？

当前版本主要提取方法调用关系。如需提取继承关系，可以扩展 `CodeParser`：

```java
// 在 extractClassesAndInterfaces 方法中添加
classDecl.getExtendedTypes().forEach(extendedType -> {
    String superClass = extendedType.getNameAsString();
    entity.addRelation("extends", "class_" + superClass);
});
```

---

## 运行示例

### 测试当前项目
```bash
mvn exec:java -Dexec.mainClass="com.java.ere.QuickTest"
```

### 分析外部项目
```bash
# 修改 ProjectAnalysisMain 中的路径
# 然后运行
mvn exec:java -Dexec.mainClass="com.java.ere.ProjectAnalysisMain"
```

### 作为 Web 服务运行
```bash
mvn exec:java -Dexec.mainClass="com.java.ere.App"
# 访问 http://localhost:4567/parse?file=/path/to/file.java
```

---

## 下一步

1. **集成到 CI/CD**：定期分析代码，追踪架构演进
2. **导入知识图谱数据库**：如 Neo4j，进行更复杂的查询
3. **可视化**：使用 D3.js 或 Cytoscape 可视化调用关系
4. **增量分析**：只分析变更的文件，提升效率

---

有问题？查看 [README.md](README.md) 或提交 Issue！
