package com.java.ere.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 分析配置主类
 */
public class AnalysisConfig {
    private String projectName;
    private String projectRoot;
    private List<String> projectPackages = new ArrayList<>();
    private List<String> sourcePaths = new ArrayList<>();
    private FilterConfig filterConfig = new FilterConfig();
    private ResolverConfig resolverConfig = new ResolverConfig();
    private ExtractionConfig extractionConfig = new ExtractionConfig();

    public AnalysisConfig() {
        // 默认源码路径（只加载主代码，测试代码通常不需要）
        sourcePaths.add("src/main/java");
    }

    public AnalysisConfig(String projectRoot, List<String> projectPackages) {
        this();
        this.projectRoot = projectRoot;
        this.projectPackages = projectPackages;
    }

    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(String projectRoot) {
        this.projectRoot = projectRoot;
    }

    public List<String> getProjectPackages() {
        return projectPackages;
    }

    public void setProjectPackages(List<String> projectPackages) {
        this.projectPackages = projectPackages;
    }

    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    public void setSourcePaths(List<String> sourcePaths) {
        this.sourcePaths = sourcePaths;
    }

    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public ResolverConfig getResolverConfig() {
        return resolverConfig;
    }

    public void setResolverConfig(ResolverConfig resolverConfig) {
        this.resolverConfig = resolverConfig;
    }

    public ExtractionConfig getExtractionConfig() {
        return extractionConfig;
    }

    public void setExtractionConfig(ExtractionConfig extractionConfig) {
        this.extractionConfig = extractionConfig;
    }

    /**
     * 从 YAML 配置文件加载
     */
    public static AnalysisConfig loadFromYaml(String yamlFilePath) throws IOException {
        return ConfigLoader.loadFromYaml(yamlFilePath);
    }

    /**
     * 从 classpath 资源加载
     */
    public static AnalysisConfig loadFromResource(String resourcePath) throws IOException {
        return ConfigLoader.loadFromResource(resourcePath);
    }
}
