package com.java.extractor.source;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.java.extractor.model.EntityInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 源码提取服务
 */
public class SourceExtractor {

    private final String projectRoot;

    public SourceExtractor(String projectRoot) {
        this.projectRoot = projectRoot;
    }
    
    public String getProjectRoot() {
        return projectRoot;
    }

    /**
     * 为实体提取源码
     */
    public String extractSourceCode(EntityInfo entity) {
        if (entity == null || entity.getFilePath() == null) {
            return null;
        }

        String entityType = entity.getEntity_type();
        if (entityType == null) {
            return null;
        }

        try {
            switch (entityType) {
                case "Method":
                    return extractMethodSource(entity);
                case "ClassOrInterface":
                    return extractClassSource(entity);
                case "Field":
                    return extractFieldSource(entity);
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("提取源码失败: " + entity.getId() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 提取方法源码
     */
    private String extractMethodSource(EntityInfo entity) throws Exception {
        File file = resolveFile(entity.getFilePath());
        if (!file.exists()) {
            System.err.println("文件不存在: " + file.getAbsolutePath());
            return null;
        }

        CompilationUnit cu = StaticJavaParser.parse(file);
        String owner = entity.getOwner();
        String methodName = entity.getName();

        // 查找方法
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            // 检查方法名匹配
            if (!method.getNameAsString().equals(methodName)) {
                continue;
            }

            // 检查所属类匹配
            if (owner != null) {
                String actualOwner = method.findAncestor(ClassOrInterfaceDeclaration.class)
                    .map(ClassOrInterfaceDeclaration::getNameAsString)
                    .orElse(null);

                if (!owner.equals(actualOwner)) {
                    continue;
                }
            }

            // 提取完整方法体
            return method.toString();
        }

        System.err.println("未找到方法: " + entity.getId());
        return null;
    }

    /**
     * 提取类源码
     */
    private String extractClassSource(EntityInfo entity) throws Exception {
        File file = resolveFile(entity.getFilePath());
        if (!file.exists()) {
            System.err.println("文件不存在: " + file.getAbsolutePath());
            return null;
        }

        // 对于类，返回整个文件内容
        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * 提取字段源码
     */
    private String extractFieldSource(EntityInfo entity) throws Exception {
        File file = resolveFile(entity.getFilePath());
        if (!file.exists()) {
            System.err.println("文件不存在: " + file.getAbsolutePath());
            return null;
        }

        CompilationUnit cu = StaticJavaParser.parse(file);
        String owner = entity.getOwner();
        String fieldName = entity.getName();

        // 查找字段
        for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
            // 检查字段名匹配
            boolean nameMatches = field.getVariables().stream()
                .anyMatch(v -> v.getNameAsString().equals(fieldName));

            if (!nameMatches) {
                continue;
            }

            // 检查所属类匹配
            if (owner != null) {
                String actualOwner = field.findAncestor(ClassOrInterfaceDeclaration.class)
                    .map(ClassOrInterfaceDeclaration::getNameAsString)
                    .orElse(null);

                if (!owner.equals(actualOwner)) {
                    continue;
                }
            }

            // 提取字段声明
            return field.toString();
        }

        System.err.println("未找到字段: " + entity.getId());
        return null;
    }

    /**
     * 提取方法所在类的上下文（类声明+字段）
     */
    public String extractClassContext(EntityInfo methodEntity) {
        if (methodEntity == null || !methodEntity.getEntity_type().equals("Method")) {
            return null;
        }

        try {
            File file = resolveFile(methodEntity.getFilePath());
            if (!file.exists()) {
                return null;
            }

            CompilationUnit cu = StaticJavaParser.parse(file);
            String owner = methodEntity.getOwner();

            // 查找类
            for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (classDecl.getNameAsString().equals(owner)) {
                    // 返回类声明和字段（不包括方法体）
                    StringBuilder context = new StringBuilder();
                    context.append("// 类: ").append(owner).append("\n");
                    context.append(classDecl.getNameAsString()).append(" {\n");

                    // 添加字段
                    for (FieldDeclaration field : classDecl.getFields()) {
                        context.append("  ").append(field.toString()).append("\n");
                    }

                    context.append("  // ... 其他方法省略 ...\n");
                    context.append("}\n");

                    return context.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("提取类上下文失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 解析文件路径：优先支持绝对路径；若为相对路径，则基于projectRoot解析
     */
    private File resolveFile(String path) {
        if (path == null || path.isEmpty()) {
            return new File("");
        }
        File direct = new File(path);
        if (direct.isAbsolute()) {
            return direct;
        }
        // 相对路径：拼接projectRoot
        if (projectRoot != null && !projectRoot.isEmpty()) {
            File joined = Paths.get(projectRoot, path).toFile();
            return joined;
        }
        return direct;
    }
}
