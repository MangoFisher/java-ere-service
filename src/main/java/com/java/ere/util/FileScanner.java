package com.java.ere.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件扫描工具
 */
public class FileScanner {

    /**
     * 递归扫描目录，查找所有Java文件
     */
    public static List<File> scanJavaFiles(String rootPath) {
        List<File> javaFiles = new ArrayList<>();
        
        try {
            javaFiles = Files.walk(Paths.get(rootPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .map(Path::toFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("扫描目录失败: " + rootPath + " - " + e.getMessage());
        }
        
        return javaFiles;
    }

    /**
     * 扫描指定的多个源码目录
     */
    public static List<File> scanSourcePaths(String projectRoot, List<String> sourcePaths) {
        List<File> allFiles = new ArrayList<>();
        
        for (String sourcePath : sourcePaths) {
            File sourceDir = new File(projectRoot, sourcePath);
            if (sourceDir.exists() && sourceDir.isDirectory()) {
                List<File> files = scanJavaFiles(sourceDir.getAbsolutePath());
                allFiles.addAll(files);
            }
        }
        
        return allFiles;
    }

    /**
     * 检查路径是否存在
     */
    public static boolean exists(String path) {
        return new File(path).exists();
    }

    /**
     * 检查是否为Java文件
     */
    public static boolean isJavaFile(File file) {
        return file.isFile() && file.getName().endsWith(".java");
    }
}
