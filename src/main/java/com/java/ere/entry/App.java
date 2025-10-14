package com.java.ere.entry;

import com.google.gson.Gson;
import com.java.ere.CodeParser;
import com.java.ere.Entity;
import spark.Spark;

import java.io.File;
import java.util.Map;

/**
 * Java ERE 项目分析服务，提供REST API接口
 */
public class App {
    private static final CodeParser parser = new CodeParser();
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        Spark.port(8080); // 服务端口

        // API接口：解析Java文件
        Spark.post("/parse", (request, response) -> {
            response.type("application/json");
            try {
                String filePath = gson.fromJson(request.body(), Map.class).get("filePath").toString();
                File javaFile = new File(filePath);
                if (!javaFile.exists()) {
                    return gson.toJson(Map.of("error", "文件不存在"));
                }

                parser.init(javaFile.getParentFile().getAbsolutePath());
                Map<String, Entity> result = parser.parseFile(javaFile);
                return gson.toJson(result);
            } catch (Exception e) {
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        });

        System.out.println("服务启动成功，访问地址：http://localhost:8080");
    }
}