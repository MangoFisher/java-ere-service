# Java ERE Service

一个使用 JavaParser 分析 Java 项目源码，进行 ERE (Entity-Relationship-Extraction) 关系提取的项目，是构建基于 Java 源码的知识图谱的原始数据（实体、关系）来源。

## 功能特性

- ✅ **单文件分析**：支持分析单个 Java 源文件
- ✅ **批量分析**：支持目录递归扫描和批量分析
- ✅ **灵活过滤**：基于 Glob 模式的强大过滤规则
- ✅ **符号解析**：准确解析类型引用和方法调用
- ✅ **第三方库处理**：智能处理第三方库依赖，保持图谱简洁
- ✅ **Spring Boot 优化**：针对 Maven Spring Boot 项目优化

## 目前支持的实体&关系类型
见 [entity_relationship_analyze.md](docs/entity_relationship_analyze.md)

## 如何开始分析单个文件

## 如何开始分析整个项目
### 1. 根据需求修改配置文件

### 2. 生成抽取实体和关系的json文件

```bash
mvn exec:java -Dexec.mainClass="com.java.ere.entry.ConfigFileMain" -Dexec.args="analysis-config.yml"
```

命令会读取`analysis-config.yml`（如需其他配置文件可替换路径），并在`extract_out/`目录下生成带时间戳的`analysis-result_*.json`文件。

### 3. 根据生成的json文件生成cyper脚本

```bash
mvn exec:java -Dexec.mainClass="com.java.ere.entry.ExportToNeo4jMain" \
  -Dexec.args="extract_out/analysis-result_demo_20250101_120000.json extract_out/neo4j-import.cypher"
```

不传参数时程序会自动选择`extract_out/`目录中最新的结果文件；如需指定输入/输出，可替换命令中的JSON路径与Cypher输出路径。

### 4. 将cyper脚本导入到Neo4j数据库
  复制cyper脚本中全部内容，然后在neo4j browser中执行


### 5. 根据你想要查询的实体和关系，使用Cypher查询语言进行查询, 如查询所有实体间的关系
![alt text](image.png)

