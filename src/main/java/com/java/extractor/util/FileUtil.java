package com.java.extractor.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件工具类
 * 
 * 功能：根据类名在项目中查找对应的源文件
 */
public class FileUtil {
    
    private final String projectRoot;
    private final List<String> sourcePaths;
    
    public FileUtil(String projectRoot) {
        this(projectRoot, List.of("src/main/java", "src/test/java"));
    }
    
    public FileUtil(String projectRoot, List<String> sourcePaths) {
        this.projectRoot = projectRoot;
        this.sourcePaths = sourcePaths;
    }
    
    /**
     * 根据类名查找源文件
     * 
     * @param className 类名（简单类名或全限定名）
     * @return 文件路径，如果找不到返回 null
     */
    public String locateSourceFile(String className) {
        // 1. 如果是全限定名，转换为文件路径
        if (className.contains(".")) {
            String relativePath = className.replace('.', '/') + ".java";
            for (String sourcePath : sourcePaths) {
                String fullPath = Paths.get(projectRoot, sourcePath, relativePath).toString();
                if (new File(fullPath).exists()) {
                    return fullPath;
                }
            }
        }
        
        // 2. 如果是简单类名，搜索所有源码目录
        String simpleClassName = className.contains(".") 
            ? className.substring(className.lastIndexOf('.') + 1)
            : className;
        
        String fileName = simpleClassName + ".java";
        
        for (String sourcePath : sourcePaths) {
            String sourceDir = Paths.get(projectRoot, sourcePath).toString();
            String found = searchFile(sourceDir, fileName);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * 在目录中递归搜索文件
     */
    private String searchFile(String directory, String fileName) {
        try {
            Path startPath = Paths.get(directory);
            if (!Files.exists(startPath)) {
                return null;
            }
            
            try (Stream<Path> paths = Files.walk(startPath)) {
                List<Path> results = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .collect(Collectors.toList());
                
                if (!results.isEmpty()) {
                    return results.get(0).toString();
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回 null
        }
        
        return null;
    }
    
    /**
     * 列出项目中所有的 Java 源文件
     */
    public List<String> listAllJavaFiles() {
        List<String> allFiles = new ArrayList<>();
        
        for (String sourcePath : sourcePaths) {
            String sourceDir = Paths.get(projectRoot, sourcePath).toString();
            try {
                Path startPath = Paths.get(sourceDir);
                if (!Files.exists(startPath)) {
                    continue;
                }
                
                try (Stream<Path> paths = Files.walk(startPath)) {
                    List<String> files = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .map(Path::toString)
                        .collect(Collectors.toList());
                    
                    allFiles.addAll(files);
                }
            } catch (Exception e) {
                // 继续处理其他路径
            }
        }
        
        return allFiles;
    }
}
