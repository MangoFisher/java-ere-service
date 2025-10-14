package com.java.ere.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 符号解析器配置
 */
public class ResolverConfig {
    private boolean includeJdk = true;
    private String dependencyMode = "essential";  // none / essential / all
    private List<String> essentialPatterns = new ArrayList<>();
    private String localDependencyDir = "target/dependency";

    public ResolverConfig() {
        // 默认的核心库模式
        essentialPatterns.addAll(Arrays.asList(
            "spring-context",
            "spring-beans",
            "spring-core",
            "lombok",
            "mybatis",
            "slf4j-api"
        ));
    }

    public boolean isIncludeJdk() {
        return includeJdk;
    }

    public void setIncludeJdk(boolean includeJdk) {
        this.includeJdk = includeJdk;
    }

    public String getDependencyMode() {
        return dependencyMode;
    }

    public void setDependencyMode(String dependencyMode) {
        this.dependencyMode = dependencyMode;
    }

    public List<String> getEssentialPatterns() {
        return essentialPatterns;
    }

    public void setEssentialPatterns(List<String> essentialPatterns) {
        this.essentialPatterns = essentialPatterns;
    }

    public String getLocalDependencyDir() {
        return localDependencyDir;
    }

    public void setLocalDependencyDir(String localDependencyDir) {
        this.localDependencyDir = localDependencyDir;
    }
}
