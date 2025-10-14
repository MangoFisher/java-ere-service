package com.java.ere.entry;

import com.java.ere.export.Neo4jExporter;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 导出到Neo4j的入口程序
 */
public class ExportToNeo4jMain {
    public static void main(String[] args) {
        try {
            System.out.println("===============================================");
            System.out.println("Java ERE - Neo4j 导出工具");
            System.out.println("===============================================\n");
            
            String jsonFile;
            String cypherFile;
            
            // 如果用户提供了参数
            if (args.length >= 1) {
                jsonFile = args[0];
                // 如果提供的路径不包含目录，自动加上extract_out/
                if (!jsonFile.contains("/") && !jsonFile.contains("\\")) {
                    jsonFile = "extract_out/" + jsonFile;
                }
            } else {
                // 默认查找extract_out目录中最新的JSON文件
                jsonFile = findLatestJsonFile();
            }
            
            if (args.length >= 2) {
                cypherFile = args[2];
            } else {
                // Cypher文件也输出到extract_out目录
                cypherFile = "extract_out/neo4j-import.cypher";
            }
            
            // 执行导出
            Neo4jExporter.exportToCypher(jsonFile, cypherFile);
            
            // 使用说明
            System.out.println("\n📋 下一步操作:");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("1️⃣  打开 Neo4j Browser");
            System.out.println("   访问: http://localhost:7474");
            System.out.println("   用户名: neo4j");
            System.out.println("   密码: (你设置的密码)");
            System.out.println();
            System.out.println("2️⃣  复制粘贴脚本内容到查询框");
            System.out.println("   文件: " + cypherFile);
            System.out.println("   (或者拖拽文件到浏览器窗口)");
            System.out.println();
            System.out.println("3️⃣  点击运行按钮 ▶️");
            System.out.println("   等待导入完成（约10-30秒）");
            System.out.println();
            System.out.println("4️⃣  验证导入结果");
            System.out.println("   输入: MATCH (n) RETURN n LIMIT 25");
            System.out.println("   应该能看到节点的图形化展示");
            System.out.println();
            System.out.println("5️⃣  尝试查询示例");
            System.out.println("   // 查看某个方法的调用关系");
            System.out.println("   MATCH (m:Method {name: 'register'})-[r]-(other)");
            System.out.println("   RETURN m, r, other LIMIT 20");
            System.out.println();
            System.out.println("   // 查看类的实现关系");
            System.out.println("   MATCH (c:ClassOrInterface)-[:IMPLEMENTS]->(i:ClassOrInterface)");
            System.out.println("   RETURN c, i");
            System.out.println();
            System.out.println("   // 查看方法访问的字段");
            System.out.println("   MATCH (m:Method)-[:ACCESSES]->(f:Field)");
            System.out.println("   RETURN m, f LIMIT 20");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
        } catch (Exception e) {
            System.err.println("❌ 导出失败: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\n使用方法:");
            System.err.println("  mvn exec:java -Dexec.mainClass=\"com.java.ere.entry.ExportToNeo4jMain\"");
            System.err.println("  mvn exec:java -Dexec.mainClass=\"com.java.ere.entry.ExportToNeo4jMain\" -Dexec.args=\"result.json\"");
        }
    }
    
    /**
     * 查找extract_out目录中最新的JSON文件
     */
    private static String findLatestJsonFile() {
        File outputDir = new File("extract_out");
        
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            throw new RuntimeException("extract_out 目录不存在，请先运行分析生成JSON文件");
        }
        
        File[] jsonFiles = outputDir.listFiles((dir, name) -> 
            name.startsWith("analysis-result") && name.endsWith(".json"));
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            throw new RuntimeException("extract_out 目录中没有找到分析结果文件");
        }
        
        // 按修改时间排序，返回最新的
        Arrays.sort(jsonFiles, Comparator.comparingLong(File::lastModified).reversed());
        
        String latestFile = jsonFiles[0].getPath();
        System.out.println("📄 自动选择最新的JSON文件: " + latestFile);
        
        return latestFile;
    }
}
