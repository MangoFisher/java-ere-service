package com.java.ere.config;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件过滤配置
 */
public class FilterConfig {
    private List<String> includePatterns = new ArrayList<>();
    private List<String> excludePatterns = new ArrayList<>();

    public FilterConfig() {
    }

    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    /**
     * 判断文件是否应该被包含
     */
    public boolean shouldInclude(File file, String projectRoot) {
        String relativePath = getRelativePath(file, projectRoot);
        
        // 1. 检查exclude规则（优先级高）
        for (String pattern : excludePatterns) {
            if (matchesGlob(relativePath, pattern)) {
                return false;
            }
        }
        
        // 2. 检查include规则
        if (includePatterns.isEmpty()) {
            return true;  // 没有include规则，默认包含
        }
        
        for (String pattern : includePatterns) {
            if (matchesGlob(relativePath, pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取相对路径
     */
    private String getRelativePath(File file, String projectRoot) {
        Path filePath = file.toPath().toAbsolutePath();
        Path rootPath = Paths.get(projectRoot).toAbsolutePath();
        
        try {
            Path relativePath = rootPath.relativize(filePath);
            return relativePath.toString().replace(File.separator, "/");
        } catch (IllegalArgumentException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * Glob模式匹配
     */
    private boolean matchesGlob(String path, String pattern) {
        try {
            PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + pattern);
            return matcher.matches(Paths.get(path));
        } catch (Exception e) {
            return false;
        }
    }
}
