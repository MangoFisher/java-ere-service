package com.java.ere.entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.ere.Entity;
import com.java.ere.EntityJsonAdapter;
import com.java.ere.ProjectAnalyzer;
import com.java.ere.config.AnalysisConfig;
import com.java.ere.config.ConfigLoader;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 使用配置文件的示例程序
 */
public class ConfigFileMain {
    public static void main(String[] args) {
        try {
            String configFile = "analysis-config.yml";
            
            // 如果用户提供了配置文件路径参数
            if (args.length > 0) {
                configFile = args[0];
            }
            
            System.out.println("===============================================");
            System.out.println("Java ERE - 配置文件模式");
            System.out.println("===============================================");
            System.out.println("配置文件: " + configFile);
            System.out.println();
            
            // 从 YAML 配置文件加载
            AnalysisConfig config = ConfigLoader.loadFromYaml(configFile);
            
            System.out.println("配置加载成功！");
            System.out.println("项目名称: " + config.getProjectName());
            System.out.println("项目路径: " + config.getProjectRoot());
            System.out.println("项目包名: " + config.getProjectPackages());
            
            // 打印提取配置摘要
            config.getExtractionConfig().printSummary();
            
            // 执行分析
            long startTime = System.currentTimeMillis();
            ProjectAnalyzer analyzer = new ProjectAnalyzer();
            Map<String, Entity> result = analyzer.analyze(config);
            long endTime = System.currentTimeMillis();
            
            // 打印性能统计
            if (config.getExtractionConfig().isEnablePerformanceStats()) {
                System.out.println("\n==================== 性能统计 ====================");
                System.out.println("总耗时: " + (endTime - startTime) / 1000.0 + " 秒");
                System.out.println("提取实体数: " + result.size());
                System.out.println("==================================================\n");
            }
            
            // 输出结果到控制台（前5个实体）
            System.out.println("\n分析结果（前5个实体）:");
            System.out.println("===============================================");
            
            // 创建支持关系计数的Gson实例
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Entity.class, new EntityJsonAdapter())
                .create();
            result.entrySet().stream()
                .limit(5)
                .forEach(entry -> {
                    System.out.println(gson.toJson(entry.getValue()));
                    System.out.println("-----------------------------------------------");
                });
            
            System.out.println("\n完成！总共 " + result.size() + " 个实体");
            
            // 保存结果到文件
            saveResultToFile(result, config.getProjectName(), gson);
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\n使用方法:");
            System.err.println("  mvn exec:java -Dexec.mainClass=\"com.java.ere.entry.ConfigFileMain\"");
            System.err.println("  mvn exec:java -Dexec.mainClass=\"com.java.ere.entry.ConfigFileMain\" -Dexec.args=\"/path/to/config.yml\"");
        }
    }
    
    /**
     * 保存分析结果到文件
     */
    private static void saveResultToFile(Map<String, Entity> result, String projectName, Gson gson) {
        try {
            // 确保输出目录存在
            java.io.File outputDir = new java.io.File("extract_out");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 生成文件名（带时间戳）
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String sanitizedProjectName = projectName != null ? 
                projectName.replaceAll("[^a-zA-Z0-9\\-_]", "_") : "project";
            String fileName = "extract_out/analysis-result_" + sanitizedProjectName + "_" + timestamp + ".json";
            
            System.out.println("\n正在保存结果到文件: " + fileName);
            
            // 写入文件
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(gson.toJson(result));
            }
            
            System.out.println("✓ 结果已保存到: " + fileName);
            System.out.println("文件大小: " + new java.io.File(fileName).length() / 1024 + " KB");
            
        } catch (IOException e) {
            System.err.println("保存文件失败: " + e.getMessage());
        }
    }
}
