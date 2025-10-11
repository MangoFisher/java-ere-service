package com.java.ere;

import com.google.gson.Gson;
import java.io.File;
import java.util.Map;

// 本地调试入口
public class DebugMain {
    public static void main(String[] args) {
        try {
            // 1. 指定要解析的Java文件（替换为你的本地Java文件绝对路径）
            String javaFilePath = "/Users/zhangxiaoguo/Downloads/spring-boot-admin-3.5.5/spring-boot-admin-client/src/main/java/de/codecentric/boot/admin/client/registration/ApplicationRegistrator.java";
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

            // 4. 打印解析结果（格式化JSON，便于查看）
            Gson gson = new Gson();
            System.out.println("解析结果：");
            System.out.println(gson.toJson(result));

        } catch (Exception e) {
            System.out.println("解析出错：" + e.getMessage());
            e.printStackTrace();
        }
    }
}