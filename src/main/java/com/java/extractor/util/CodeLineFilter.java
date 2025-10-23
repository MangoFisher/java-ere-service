package com.java.extractor.util;

import com.java.extractor.filter.CodeLineFilterConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 代码行过滤器
 * 统一处理空行、注释、日志调用等非核心业务代码的过滤
 */
public class CodeLineFilter {

    private final CodeLineFilterConfig config;

    // 预编译的正则表达式模式（性能优化）
    private final List<Pattern> loggingPatterns;
    private final List<Pattern> customExcludePatterns;

    // 多行注释状态跟踪
    private boolean inMultiLineComment = false;

    public CodeLineFilter(CodeLineFilterConfig config) {
        this.config = config != null ? config : CodeLineFilterConfig.createDefault();
        this.loggingPatterns = compilePatterns(this.config.getLoggingPatterns());
        this.customExcludePatterns = compilePatterns(this.config.getCustomExcludePatterns());
    }

    /**
     * 判断一行代码是否应该被过滤（非业务代码）
     *
     * @param line 代码行
     * @return true 表示应该被过滤（非业务代码），false 表示保留（业务代码）
     */
    public boolean shouldFilter(String line) {
        if (line == null) {
            return true;
        }

        String trimmed = line.trim();

        // 1. 空行
        if (config.isFilterEmptyLines() && trimmed.isEmpty()) {
            return true;
        }

        // 2. 注释
        if (config.isFilterComments() && isComment(trimmed)) {
            return true;
        }

        // 3. 日志调用
        if (config.isFilterLoggingStatements() && isLoggingStatement(trimmed)) {
            return true;
        }

        // 4. 导入和包声明
        if (config.isFilterImportsAndPackages()) {
            if (trimmed.startsWith("import ") || trimmed.startsWith("package ")) {
                return true;
            }
        }

        // 5. 自定义正则模式
        for (Pattern pattern : customExcludePatterns) {
            if (pattern.matcher(trimmed).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 过滤代码行列表
     *
     * @param lines 原始代码行列表
     * @return 过滤后的代码行列表（只包含业务代码）
     */
    public List<String> filter(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return lines != null ? lines : new ArrayList<>();
        }

        // 重置多行注释状态
        resetState();

        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (!shouldFilter(line)) {
                result.add(line);
            }
        }

        return result;
    }

    /**
     * 判断是否为注释行
     */
    private boolean isComment(String trimmed) {
        if (trimmed.isEmpty()) {
            return false;
        }

        // 单行注释
        if (trimmed.startsWith("//")) {
            return true;
        }

        // 多行注释开始
        if (trimmed.startsWith("/*")) {
            inMultiLineComment = true;
            // 检查是否在同一行结束
            if (trimmed.contains("*/")) {
                inMultiLineComment = false;
            }
            return true;
        }

        // 在多行注释中
        if (inMultiLineComment) {
            // 检查是否结束
            if (trimmed.contains("*/")) {
                inMultiLineComment = false;
            }
            return true;
        }

        // Javadoc 或多行注释的中间行
        if (trimmed.startsWith("*")) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为日志语句
     */
    private boolean isLoggingStatement(String trimmed) {
        if (trimmed.isEmpty()) {
            return false;
        }

        // 使用预编译的模式进行匹配
        for (Pattern pattern : loggingPatterns) {
            if (pattern.matcher(trimmed).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 编译正则表达式模式列表
     */
    private List<Pattern> compilePatterns(List<String> patterns) {
        List<Pattern> compiled = new ArrayList<>();
        if (patterns == null || patterns.isEmpty()) {
            return compiled;
        }

        for (String pattern : patterns) {
            try {
                compiled.add(Pattern.compile(pattern));
            } catch (Exception e) {
                System.err.println("[CodeLineFilter] 无效的正则表达式模式: " + pattern + " - " + e.getMessage());
            }
        }

        return compiled;
    }

    /**
     * 重置内部状态（用于处理多行注释）
     */
    private void resetState() {
        this.inMultiLineComment = false;
    }

    /**
     * 获取过滤统计信息
     */
    public FilterStatistics getStatistics(List<String> original, List<String> filtered) {
        FilterStatistics stats = new FilterStatistics();
        stats.originalCount = original != null ? original.size() : 0;
        stats.filteredCount = filtered != null ? filtered.size() : 0;
        stats.removedCount = stats.originalCount - stats.filteredCount;
        return stats;
    }

    /**
     * 过滤统计信息
     */
    public static class FilterStatistics {
        public int originalCount;
        public int filteredCount;
        public int removedCount;

        @Override
        public String toString() {
            return String.format(
                "FilterStatistics{original: %d, filtered: %d, removed: %d}",
                originalCount, filteredCount, removedCount
            );
        }
    }

    /**
     * 获取配置
     */
    public CodeLineFilterConfig getConfig() {
        return config;
    }
}
