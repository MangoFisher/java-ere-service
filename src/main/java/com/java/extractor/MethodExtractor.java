package com.java.extractor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 方法代码提取器
 * 
 * 功能：从 Java 源文件中精确提取指定方法的代码
 */
public class MethodExtractor {
    
    /**
     * 从文件中提取指定方法的代码
     * 
     * @param filePath 源文件路径
     * @param location 代码位置（类名 + 方法名）
     * @return 提取结果
     */
    public ExtractResult extractMethod(String filePath, CodeLocation location) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return ExtractResult.failure("File not found: " + filePath, location);
            }
            
            // 解析文件
            CompilationUnit cu = StaticJavaParser.parse(file);
            
            // 查找方法
            Optional<MethodDeclaration> methodOpt = findMethod(cu, location);
            
            if (methodOpt.isPresent()) {
                MethodDeclaration method = methodOpt.get();
                String code = method.toString();
                return ExtractResult.success(
                    code,
                    method.getRange().orElse(null),
                    location
                );
            } else {
                return ExtractResult.notFound(location);
            }
            
        } catch (IOException e) {
            return ExtractResult.failure(
                "Failed to parse file: " + e.getMessage(),
                location
            );
        } catch (Exception e) {
            return ExtractResult.failure(
                "Unexpected error: " + e.getMessage(),
                location
            );
        }
    }
    
    /**
     * 批量提取多个方法
     * 
     * @param filePath 源文件路径
     * @param locations 多个代码位置
     * @return 位置 -> 提取结果的映射
     */
    public Map<CodeLocation, ExtractResult> extractMethods(
            String filePath, 
            List<CodeLocation> locations) {
        
        Map<CodeLocation, ExtractResult> results = new HashMap<>();
        
        for (CodeLocation location : locations) {
            results.put(location, extractMethod(filePath, location));
        }
        
        return results;
    }
    
    /**
     * 在编译单元中查找指定的方法
     */
    private Optional<MethodDeclaration> findMethod(CompilationUnit cu, CodeLocation location) {
        String targetClassName = location.getClassName();
        String targetMethodName = location.getMethodName();
        String targetSignature = location.getSignature();
        
        // 查找所有方法
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        
        for (MethodDeclaration method : methods) {
            // 检查方法名
            if (!method.getNameAsString().equals(targetMethodName)) {
                continue;
            }
            
            // 检查是否属于目标类
            if (!belongsToClass(method, targetClassName)) {
                continue;
            }
            
            // 如果指定了签名，还要检查签名
            if (targetSignature != null) {
                String methodSignature = method.getSignature().toString();
                if (!methodSignature.contains(targetSignature)) {
                    continue;
                }
            }
            
            return Optional.of(method);
        }
        
        return Optional.empty();
    }
    
    /**
     * 判断方法是否属于指定的类
     */
    private boolean belongsToClass(MethodDeclaration method, String targetClassName) {
        Optional<Node> parent = method.getParentNode();
        
        while (parent.isPresent()) {
            Node node = parent.get();
            
            if (node instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) node;
                String className = classDecl.getNameAsString();
                
                // 简单类名匹配
                if (className.equals(targetClassName)) {
                    return true;
                }
                
                // 全限定类名匹配（如果目标类名包含包路径）
                if (targetClassName.endsWith("." + className)) {
                    return true;
                }
            }
            
            parent = node.getParentNode();
        }
        
        return false;
    }
}
