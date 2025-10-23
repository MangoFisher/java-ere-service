package com.java.extractor.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Git Diff 提取过滤器配置
 * 从 analysis-config.yml 的 git_diff_extraction 节点读取
 */
public class FilterConfig {

    private CodeLineFilterConfig codeLineFilter;
    private CommonFilterConfig common;
    private FieldFilterConfig field;
    private MethodFilterConfig method;
    private ClassFilterConfig classOrInterface;

    public FilterConfig() {
        this.codeLineFilter = new CodeLineFilterConfig();
        this.common = new CommonFilterConfig();
        this.field = new FieldFilterConfig();
        this.method = new MethodFilterConfig();
        this.classOrInterface = new ClassFilterConfig();
    }

    public CodeLineFilterConfig getCodeLineFilter() {
        return codeLineFilter;
    }

    public void setCodeLineFilter(CodeLineFilterConfig codeLineFilter) {
        this.codeLineFilter = codeLineFilter;
    }

    public CommonFilterConfig getCommon() {
        return common;
    }

    public void setCommon(CommonFilterConfig common) {
        this.common = common;
    }

    public FieldFilterConfig getField() {
        return field;
    }

    public void setField(FieldFilterConfig field) {
        this.field = field;
    }

    public MethodFilterConfig getMethod() {
        return method;
    }

    public void setMethod(MethodFilterConfig method) {
        this.method = method;
    }

    public ClassFilterConfig getClassOrInterface() {
        return classOrInterface;
    }

    public void setClassOrInterface(ClassFilterConfig classOrInterface) {
        this.classOrInterface = classOrInterface;
    }

    /**
     * 通用过滤配置（适用于所有实体类型）
     */
    public static class CommonFilterConfig {

        private List<String> excludePaths;
        private List<String> includePackages;
        private Set<String> changeTypes;
        private List<String> excludeClassNames;

        public CommonFilterConfig() {
            this.excludePaths = new ArrayList<>();
            this.includePackages = new ArrayList<>();
            this.changeTypes = new HashSet<>();
            this.excludeClassNames = new ArrayList<>();
        }

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths;
        }

        public List<String> getIncludePackages() {
            return includePackages;
        }

        public void setIncludePackages(List<String> includePackages) {
            this.includePackages = includePackages;
        }

        public Set<String> getChangeTypes() {
            return changeTypes;
        }

        public void setChangeTypes(Set<String> changeTypes) {
            this.changeTypes = changeTypes;
        }

        public List<String> getExcludeClassNames() {
            return excludeClassNames;
        }

        public void setExcludeClassNames(List<String> excludeClassNames) {
            this.excludeClassNames = excludeClassNames;
        }
    }

    /**
     * Field 专用过滤配置
     */
    public static class FieldFilterConfig {

        private Set<String> scopes;
        private Boolean constantsOnly;
        private List<String> excludeFieldNames;
        private List<String> includeFieldNames;

        public FieldFilterConfig() {
            this.scopes = new HashSet<>();
            this.constantsOnly = false;
            this.excludeFieldNames = new ArrayList<>();
            this.includeFieldNames = new ArrayList<>();
        }

        public Set<String> getScopes() {
            return scopes;
        }

        public void setScopes(Set<String> scopes) {
            this.scopes = scopes;
        }

        public Boolean getConstantsOnly() {
            return constantsOnly;
        }

        public void setConstantsOnly(Boolean constantsOnly) {
            this.constantsOnly = constantsOnly;
        }

        public List<String> getExcludeFieldNames() {
            return excludeFieldNames;
        }

        public void setExcludeFieldNames(List<String> excludeFieldNames) {
            this.excludeFieldNames = excludeFieldNames;
        }

        public List<String> getIncludeFieldNames() {
            return includeFieldNames;
        }

        public void setIncludeFieldNames(List<String> includeFieldNames) {
            this.includeFieldNames = includeFieldNames;
        }
    }

    /**
     * Method 专用过滤配置
     */
    public static class MethodFilterConfig {

        private Boolean signatureChangedOnly;
        private Integer minChangedLines;
        private List<String> excludeMethodNames;
        private List<String> includeMethodNames;

        public MethodFilterConfig() {
            this.signatureChangedOnly = false;
            this.minChangedLines = 0;
            this.excludeMethodNames = new ArrayList<>();
            this.includeMethodNames = new ArrayList<>();
        }

        public Boolean getSignatureChangedOnly() {
            return signatureChangedOnly;
        }

        public void setSignatureChangedOnly(Boolean signatureChangedOnly) {
            this.signatureChangedOnly = signatureChangedOnly;
        }

        public Integer getMinChangedLines() {
            return minChangedLines;
        }

        public void setMinChangedLines(Integer minChangedLines) {
            this.minChangedLines = minChangedLines;
        }

        public List<String> getExcludeMethodNames() {
            return excludeMethodNames;
        }

        public void setExcludeMethodNames(List<String> excludeMethodNames) {
            this.excludeMethodNames = excludeMethodNames;
        }

        public List<String> getIncludeMethodNames() {
            return includeMethodNames;
        }

        public void setIncludeMethodNames(List<String> includeMethodNames) {
            this.includeMethodNames = includeMethodNames;
        }
    }

    /**
     * ClassOrInterface 专用过滤配置
     */
    public static class ClassFilterConfig {

        private Integer minChangedLines;
        private List<String> excludeClassNames;
        private List<String> includeClassNames;

        public ClassFilterConfig() {
            this.minChangedLines = 0;
            this.excludeClassNames = new ArrayList<>();
            this.includeClassNames = new ArrayList<>();
        }

        public Integer getMinChangedLines() {
            return minChangedLines;
        }

        public void setMinChangedLines(Integer minChangedLines) {
            this.minChangedLines = minChangedLines;
        }

        public List<String> getExcludeClassNames() {
            return excludeClassNames;
        }

        public void setExcludeClassNames(List<String> excludeClassNames) {
            this.excludeClassNames = excludeClassNames;
        }

        public List<String> getIncludeClassNames() {
            return includeClassNames;
        }

        public void setIncludeClassNames(List<String> includeClassNames) {
            this.includeClassNames = includeClassNames;
        }
    }
}
