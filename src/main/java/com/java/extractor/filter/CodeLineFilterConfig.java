package com.java.extractor.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码行过滤器配置
 * 用于配置哪些类型的代码行应该被过滤（空行、注释、日志等）
 */
public class CodeLineFilterConfig {

    /**
     * 是否过滤空行
     */
    private boolean filterEmptyLines;

    /**
     * 是否过滤注释
     */
    private boolean filterComments;

    /**
     * 是否过滤日志语句
     */
    private boolean filterLoggingStatements;

    /**
     * 日志语句匹配模式（正则表达式）
     */
    private List<String> loggingPatterns;

    /**
     * 是否过滤导入和包声明
     */
    private boolean filterImportsAndPackages;

    /**
     * 自定义排除模式（正则表达式）
     */
    private List<String> customExcludePatterns;

    public CodeLineFilterConfig() {
        // 默认配置
        this.filterEmptyLines = true;
        this.filterComments = true;
        this.filterLoggingStatements = false;
        this.loggingPatterns = new ArrayList<>();
        this.filterImportsAndPackages = false;
        this.customExcludePatterns = new ArrayList<>();
    }

    /**
     * 创建默认配置（不过滤任何内容）
     */
    public static CodeLineFilterConfig createDefault() {
        CodeLineFilterConfig config = new CodeLineFilterConfig();
        config.setFilterEmptyLines(false);
        config.setFilterComments(false);
        config.setFilterLoggingStatements(false);
        config.setFilterImportsAndPackages(false);
        return config;
    }

    /**
     * 创建宽松配置（只过滤空行和注释）
     */
    public static CodeLineFilterConfig createLenient() {
        CodeLineFilterConfig config = new CodeLineFilterConfig();
        config.setFilterEmptyLines(true);
        config.setFilterComments(true);
        config.setFilterLoggingStatements(false);
        config.setFilterImportsAndPackages(false);
        return config;
    }

    /**
     * 创建严格配置（过滤所有非业务代码）
     */
    public static CodeLineFilterConfig createStrict() {
        CodeLineFilterConfig config = new CodeLineFilterConfig();
        config.setFilterEmptyLines(true);
        config.setFilterComments(true);
        config.setFilterLoggingStatements(true);
        config.setFilterImportsAndPackages(true);

        // 设置默认的日志模式
        List<String> patterns = new ArrayList<>();
        patterns.add("logger\\.");
        patterns.add("log\\.");
        patterns.add("LOGGER\\.");
        patterns.add("LOG\\.");
        patterns.add("System\\.out\\.");
        patterns.add("System\\.err\\.");
        config.setLoggingPatterns(patterns);

        // 设置默认的自定义排除模式
        List<String> customPatterns = new ArrayList<>();
        customPatterns.add("printStackTrace\\(\\)");
        customPatterns.add("\\.printStackTrace\\(\\)");
        config.setCustomExcludePatterns(customPatterns);

        return config;
    }

    // Getters and Setters

    public boolean isFilterEmptyLines() {
        return filterEmptyLines;
    }

    public void setFilterEmptyLines(boolean filterEmptyLines) {
        this.filterEmptyLines = filterEmptyLines;
    }

    public boolean isFilterComments() {
        return filterComments;
    }

    public void setFilterComments(boolean filterComments) {
        this.filterComments = filterComments;
    }

    public boolean isFilterLoggingStatements() {
        return filterLoggingStatements;
    }

    public void setFilterLoggingStatements(boolean filterLoggingStatements) {
        this.filterLoggingStatements = filterLoggingStatements;
    }

    public List<String> getLoggingPatterns() {
        return loggingPatterns;
    }

    public void setLoggingPatterns(List<String> loggingPatterns) {
        this.loggingPatterns = loggingPatterns != null ? loggingPatterns : new ArrayList<>();
    }

    public boolean isFilterImportsAndPackages() {
        return filterImportsAndPackages;
    }

    public void setFilterImportsAndPackages(boolean filterImportsAndPackages) {
        this.filterImportsAndPackages = filterImportsAndPackages;
    }

    public List<String> getCustomExcludePatterns() {
        return customExcludePatterns;
    }

    public void setCustomExcludePatterns(List<String> customExcludePatterns) {
        this.customExcludePatterns = customExcludePatterns != null ? customExcludePatterns : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "CodeLineFilterConfig{" +
                "filterEmptyLines=" + filterEmptyLines +
                ", filterComments=" + filterComments +
                ", filterLoggingStatements=" + filterLoggingStatements +
                ", loggingPatterns=" + loggingPatterns +
                ", filterImportsAndPackages=" + filterImportsAndPackages +
                ", customExcludePatterns=" + customExcludePatterns +
                '}';
    }
}
