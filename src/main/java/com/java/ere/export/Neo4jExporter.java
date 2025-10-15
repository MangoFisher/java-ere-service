package com.java.ere.export;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.java.ere.Entity;
import com.java.ere.EntityJsonAdapter;

/**
 * Neo4j 导出器
 * 将实体和关系导出为 Cypher 脚本，可直接在 Neo4j 中执行
 */
public class Neo4jExporter {
    
    /**
     * 将JSON文件转换为Cypher脚本
     */
    public static void exportToCypher(String jsonFilePath, String cypherFilePath) throws IOException {
        System.out.println("==================== Neo4j 导出 ====================");
        System.out.println("读取文件: " + jsonFilePath);
        
        // 读取JSON文件（使用支持count的适配器）
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Entity.class, new EntityJsonAdapter())
            .create();
        Type type = new TypeToken<Map<String, Entity>>(){}.getType();
        Map<String, Entity> entities;
        
        try (FileReader reader = new FileReader(jsonFilePath)) {
            entities = gson.fromJson(reader, type);
        }
        
        System.out.println("实体数量: " + entities.size());
        
        // 生成Cypher脚本
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cypherFilePath))) {
            writeCypherScript(writer, entities);
        }
        
        System.out.println("Cypher脚本已生成: " + cypherFilePath);
        System.out.println("\n执行方式:");
        System.out.println("1. 打开 Neo4j Browser: http://localhost:7474");
        System.out.println("2. 复制脚本内容并执行");
        System.out.println("3. 或者使用命令: cat " + cypherFilePath + " | cypher-shell -u neo4j -p password");
        System.out.println("====================================================\n");
    }
    
    /**
     * 生成Cypher脚本内容
     */
    private static void writeCypherScript(BufferedWriter writer, Map<String, Entity> entities) throws IOException {
        // 清空数据库（可选，谨慎使用）
        writer.write("// ==================== 清空现有数据 ====================\n");
        writer.write("// 注意：这会删除数据库中所有节点和关系！\n");
        writer.write("// 如果不想清空，请注释掉下面这行\n");
        writer.write("MATCH (n) DETACH DELETE n;\n\n");
        
        // 创建索引（提升性能）
        writer.write("// ==================== 创建索引 ====================\n");
        writer.write("CREATE INDEX entity_id IF NOT EXISTS FOR (n:Entity) ON (n.id);\n");
        writer.write("CREATE INDEX method_name IF NOT EXISTS FOR (n:Method) ON (n.name);\n");
        writer.write("CREATE INDEX class_name IF NOT EXISTS FOR (n:ClassOrInterface) ON (n.name);\n\n");
        
        // 统计各类型实体数量
        int totalEntities = entities.size();
        long classCount = entities.values().stream().filter(e -> "ClassOrInterface".equals(e.getType())).count();
        long methodCount = entities.values().stream().filter(e -> "Method".equals(e.getType())).count();
        long fieldCount = entities.values().stream().filter(e -> "Field".equals(e.getType())).count();
        long paramCount = entities.values().stream().filter(e -> "Parameter".equals(e.getType())).count();
        long returnCount = entities.values().stream().filter(e -> "Return".equals(e.getType())).count();
        long exceptionCount = entities.values().stream().filter(e -> "Exception".equals(e.getType())).count();
        long annotationCount = entities.values().stream().filter(e -> "Annotation".equals(e.getType())).count();
        
        writer.write("// ==================== 统计信息 ====================\n");
        writer.write(String.format("// 总实体数: %d\n", totalEntities));
        writer.write(String.format("// - ClassOrInterface: %d\n", classCount));
        writer.write(String.format("// - Method: %d\n", methodCount));
        writer.write(String.format("// - Field: %d\n", fieldCount));
        writer.write(String.format("// - Parameter: %d\n", paramCount));
        writer.write(String.format("// - Return: %d\n", returnCount));
        writer.write(String.format("// - Exception: %d\n", exceptionCount));
        writer.write(String.format("// - Annotation: %d\n", annotationCount));
        writer.write("// ==================================================\n\n");
        
        // 创建节点
        writer.write("// ==================== 创建节点 ====================\n");
        int nodeCount = 0;
        for (Entity entity : entities.values()) {
            writeCypherNode(writer, entity);
            nodeCount++;
            
            // 每100个节点输出进度
            if (nodeCount % 100 == 0) {
                writer.write(String.format("// 进度: %d/%d 节点已创建\n", nodeCount, totalEntities));
            }
        }
        writer.write(String.format("// 所有 %d 个节点创建完成\n\n", nodeCount));
        
        // 创建关系
        writer.write("// ==================== 创建关系 ====================\n");
        int relationCount = 0;
        for (Entity entity : entities.values()) {
            int count = writeCypherRelations(writer, entity);
            relationCount += count;
        }
        writer.write(String.format("// 所有 %d 个关系创建完成\n\n", relationCount));
        
        // 验证查询
        writer.write("// ==================== 验证查询 ====================\n");
        writer.write("// 查看所有节点类型统计\n");
        writer.write("MATCH (n) RETURN labels(n) AS type, count(*) AS count ORDER BY count DESC;\n\n");
        
        writer.write("// 查看所有关系类型统计\n");
        writer.write("MATCH ()-[r]->() RETURN type(r) AS relType, count(*) AS count ORDER BY count DESC;\n\n");
        
        writer.write("// 查看示例：某个方法的所有关系\n");
        writer.write("MATCH (m:Method)-[r]-(related) WHERE m.name = 'register' RETURN m, r, related LIMIT 20;\n\n");
    }
    
    /**
     * 生成创建节点的Cypher语句
     */
    private static void writeCypherNode(BufferedWriter writer, Entity entity) throws IOException {
        // 转义ID和属性中的特殊字符
        String escapedId = escapeCypher(entity.getId());
        String entityType = entity.getType();
        
        // 构建CREATE语句
        // 标签顺序：Entity在前，具体类型在后，确保具体类型的样式优先级更高
        StringBuilder cypher = new StringBuilder();
        cypher.append("CREATE (n:Entity:").append(entityType).append(" {");
        cypher.append("id: '").append(escapedId).append("'");
        cypher.append(", type: '").append(entityType).append("'");
        
        // 添加属性
        if (entity.getProperties() != null && !entity.getProperties().isEmpty()) {
            for (Map.Entry<String, String> prop : entity.getProperties().entrySet()) {
                String key = prop.getKey();
                String value = prop.getValue();
                if (value != null) {
                    cypher.append(", ").append(key).append(": '");
                    cypher.append(escapeCypher(value)).append("'");
                }
            }
        }
        
        cypher.append("});\n");
        writer.write(cypher.toString());
    }
    
    /**
     * 生成创建关系的Cypher语句（带count属性）
     */
    private static int writeCypherRelations(BufferedWriter writer, Entity entity) throws IOException {
        if (entity.getRelations() == null || entity.getRelations().isEmpty()) {
            return 0;
        }
        
        int count = 0;
        String escapedSourceId = escapeCypher(entity.getId());
        
        for (Map.Entry<String, java.util.List<String>> relation : entity.getRelations().entrySet()) {
            String relationType = relation.getKey();
            String relationTypeUpper = relationType.toUpperCase();  // Neo4j关系类型通常大写
            
            // 获取该类型的所有关系及其计数
            Map<String, Integer> relationCounts = entity.getRelationsByType(relationType);
            
            for (String targetId : relation.getValue()) {
                String escapedTargetId = escapeCypher(targetId);
                int relationCount = relationCounts.getOrDefault(targetId, 1);
                
                // 构建MATCH + CREATE关系语句（带count属性）
                writer.write(String.format(
                    "MATCH (a:Entity {id: '%s'}), (b:Entity {id: '%s'}) CREATE (a)-[:%s {count: %d}]->(b);\n",
                    escapedSourceId, escapedTargetId, relationTypeUpper, relationCount
                ));
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 转义Cypher中的特殊字符
     */
    private static String escapeCypher(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")  // 反斜杠
            .replace("'", "\\'")     // 单引号
            .replace("\"", "\\\"")   // 双引号
            .replace("\n", "\\n")    // 换行
            .replace("\r", "\\r")    // 回车
            .replace("\t", "\\t");   // 制表符
    }
    
    /**
     * 主方法：命令行使用
     */
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("用法: java Neo4jExporter <json文件路径> [cypher输出路径]");
                System.err.println("示例: java Neo4jExporter analysis-result.json kg-import.cypher");
                System.exit(1);
            }
            
            String jsonFilePath = args[0];
            String cypherFilePath = args.length > 1 ? args[1] : 
                jsonFilePath.replace(".json", "-neo4j.cypher");
            
            exportToCypher(jsonFilePath, cypherFilePath);
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
