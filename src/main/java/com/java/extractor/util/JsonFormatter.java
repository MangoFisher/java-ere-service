package com.java.extractor.util;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

/**
 * JSON格式化工具
 * 将压缩的JSON文件格式化为易读的格式
 */
public class JsonFormatter {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("用法: JsonFormatter <输入文件> [输出文件]");
            System.out.println("示例: JsonFormatter all_output.json all_output_formatted.json");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = args.length > 1 ? args[1] : inputFile.replace(".json", "_formatted.json");
        
        try {
            formatJsonFile(inputFile, outputFile);
            System.out.println("✓ JSON格式化完成！");
            
            // 显示文件大小对比
            File input = new File(inputFile);
            File output = new File(outputFile);
            System.out.println("输入文件: " + inputFile + " (" + String.format("%.2f KB", input.length() / 1024.0) + ")");
            System.out.println("输出文件: " + outputFile + " (" + String.format("%.2f KB", output.length() / 1024.0) + ")");
            
        } catch (Exception e) {
            System.err.println("格式化失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * 格式化JSON文件
     */
    public static void formatJsonFile(String inputPath, String outputPath) throws IOException {
        // 读取原始JSON内容
        String content = new String(Files.readAllBytes(Paths.get(inputPath)));
        
        // 解析并格式化
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()  // 避免转义特殊字符
                .create();
        
        JsonElement element = JsonParser.parseString(content);
        
        // 写入格式化后的内容
        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(element, writer);
        }
    }
}
