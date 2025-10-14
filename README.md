# Java ERE Service

一个使用 JavaParser 分析 Java 项目源码，进行 ERE (Entity-Relationship-Extraction) 关系提取的项目，是构建基于 Java 源码的知识图谱的原始数据（实体、关系）来源。

## 功能特性

- ✅ **单文件分析**：支持分析单个 Java 源文件
- ✅ **批量分析**：支持目录递归扫描和批量分析
- ✅ **灵活过滤**：基于 Glob 模式的强大过滤规则
- ✅ **符号解析**：准确解析类型引用和方法调用
- ✅ **第三方库处理**：智能处理第三方库依赖，保持图谱简洁
- ✅ **Spring Boot 优化**：针对 Maven Spring Boot 项目优化

## 提取的实体类型

| 实体类型 | 描述 | 示例ID |
|---------|------|--------|
| ClassOrInterface | 类或接口 | `class_UserService` |
| Method | 方法 | `method_UserService_getUser` |
| Parameter | 方法参数 | `param_UserService_getUser_id` |
| Return | 返回值 | `return_UserService_getUser` |
| Field | 字段 | `field_UserService_userDao` |

## 提取的关系类型

- **has_parameter**: Method → Parameter
- **returns**: Method → Return
- **calls**: Method → Method（方法调用关系）

## 快速开始

### 1. 准备依赖（可选）

如果需要完整的符号解析，建议先将项目依赖复制到本地：

```bash
cd /path/to/your/project
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
```

### 2. 单文件分析

```java
CodeParser parser = new CodeParser();
parser.init("/path/to/project");
Map<String, Entity> result = parser.parseFile(new File("UserService.java"));
```

### 3. 项目级批量分析

```java
// 创建配置
AnalysisConfig config = new AnalysisConfig();
config.setProjectRoot("/path/to/project");
config.setProjectPackages(Arrays.asList("com.example"));

// 配置过滤规则
FilterConfig filter = new FilterConfig();
filter.setIncludePatterns(Arrays.asList("src/main/java/**/*.java"));
filter.setExcludePatterns(Arrays.asList("**/test/**", "**/*Test.java"));
config.setFilterConfig(filter);

// 执行分析
ProjectAnalyzer analyzer = new ProjectAnalyzer();
Map<String, Entity> result = analyzer.analyze(config);
```

## 运行示例

### 单文件分析示例

```bash
mvn exec:java -Dexec.mainClass="com.java.ere.entry.DebugMain"
```

### 项目分析示例

```bash
mvn exec:java -Dexec.mainClass="com.java.ere.entry.ConfigFileMain"
```

## 配置说明

### 过滤规则（Glob 模式）

```java
FilterConfig filter = new FilterConfig();

// 包含规则
filter.setIncludePatterns(Arrays.asList(
    "src/main/java/**/*.java",      // 所有主代码
    "**/service/**",                 // 所有 service 包
    "**/*Controller.java"            // 所有 Controller 类
));

// 排除规则（优先级更高）
filter.setExcludePatterns(Arrays.asList(
    "**/test/**",                    // 排除测试代码
    "**/generated/**",               // 排除生成代码
    "**/*Test.java",                 // 排除测试文件
    "**/*Mapper.java"                // 排除 Mapper 文件
));
```

### 符号解析器配置

```java
ResolverConfig resolver = config.getResolverConfig();

// 依赖模式
resolver.setDependencyMode("essential");  // none / essential / all

// 核心库模式会自动加载：
// - spring-context, spring-beans, spring-core
// - lombok
// - mybatis
// - slf4j-api
```

### 第三方库调用处理

```java
ExtractionConfig extraction = config.getExtractionConfig();

// 处理策略
extraction.setThirdPartyCallStrategy("mark");  
// - ignore: 完全忽略
// - mark: 标记为外部依赖（推荐）
// - full: 完整记录（不推荐）
```

## 核心设计原理

### 两阶段处理

1. **符号解析阶段**：加载所有源码（包括被过滤的文件），确保类型引用能正确解析
2. **实体提取阶段**：只处理过滤后的目标文件，控制输出内容

### 第三方库处理

- **加载但不提取**：第三方库用于符号解析，但不创建实体
- **标记外部调用**：记录第三方库调用信息，但不展开
- **保持图谱简洁**：只包含业务代码，易于分析和可视化

## 输出格式

```json
{
  "class_UserService": {
    "id": "class_UserService",
    "type": "ClassOrInterface",
    "properties": {
      "name": "UserService",
      "isInterface": "false",
      "purpose": "用户服务类"
    },
    "relations": {}
  },
  "method_UserService_getUser": {
    "id": "method_UserService_getUser",
    "type": "Method",
    "properties": {
      "name": "getUser",
      "owner": "UserService",
      "business_role": "根据ID获取用户",
      "external_dependencies": "org.slf4j.Logger.info"
    },
    "relations": {
      "has_parameter": ["param_UserService_getUser_id"],
      "returns": ["return_UserService_getUser"],
      "calls": ["method_UserDao_findById"]
    }
  }
}
```

## 技术栈

- **JavaParser 3.25.8**: Java 源码解析
- **JavaParser Symbol Solver**: 符号解析和类型推导
- **Spark Framework**: Web API（可选）
- **Gson**: JSON 序列化

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
