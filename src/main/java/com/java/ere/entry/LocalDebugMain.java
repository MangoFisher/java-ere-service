package com.java.ere.entry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.ere.CodeParser;
import com.java.ere.Entity;
import com.java.ere.EntityJsonAdapter;

// 本地调试入口
public class LocalDebugMain {
    public static void main(String[] args) {
        try {
            // 1. 指定要解析的Java文件（替换为你的本地Java文件绝对路径）
            String javaFilePath = "/Users/zhangxiaoguo/Downloads/spring-boot-admin-3.5.5/spring-boot-admin-client/src/main/java/de/codecentric/boot/admin/client/registration/DefaultApplicationFactory.java";
            File javaFile = new File(javaFilePath);
            if (!javaFile.exists()) {
                System.out.println("错误：文件不存在！路径：" + javaFilePath);
                return;
            }

            // 2. 初始化解析器（项目路径为被解析文件所在的项目根目录）
            // 例如：若Java文件在 "C:\projects\my-java-project\src\..."，则项目路径为 "C:\projects\my-java-project"
            String projectPath = javaFile.getParentFile().getParentFile().getParentFile().getAbsolutePath();
            CodeParser parser = new CodeParser();
            parser.init(projectPath);

            // 3. 解析Java文件，获取实体和关系
            Map<String, Entity> result = parser.parseFile(javaFile);

            // 4. 输出解析结果到文件
            saveResultToFile(javaFile, result);

        } catch (Exception e) {
            System.err.println("解析出错：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 将解析结果保存到文件
     * @param javaFile 被解析的Java文件
     * @param result 解析结果
     */
    private static void saveResultToFile(File javaFile, Map<String, Entity> result) throws IOException {
        // 创建输出目录
        File outputDir = new File("extract_out");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            System.out.println("创建输出目录: " + outputDir.getAbsolutePath());
        }
        
        // 生成文件名：local-debug_<被解析文件名>_<时间戳>.json
        String fileName = javaFile.getName().replace(".java", "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputFileName = String.format("local-debug_%s_%s.json", fileName, timestamp);
        File outputFile = new File(outputDir, outputFileName);
        
        // 格式化JSON并写入文件（使用EntityJsonAdapter保持与项目分析一致的格式）
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Entity.class, new EntityJsonAdapter())
            .create();
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(result, writer);
        }
        
        System.out.println("✓ 解析完成！");
        System.out.println("共提取 " + result.size() + " 个实体");
        System.out.println("结果已保存至: " + outputFile.getAbsolutePath());
    }
}
