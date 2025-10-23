package com.java.extractor.filter;

import com.java.extractor.model.ChangeInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用变更过滤器
 * 适用于所有实体类型的通用过滤规则
 */
public class CommonChangeFilter {

    private final FilterConfig.CommonFilterConfig config;

    public CommonChangeFilter(FilterConfig.CommonFilterConfig config) {
        this.config = config;
    }

    /**
     * 应用通用过滤规则
     */
    public List<ChangeInfo> filter(List<ChangeInfo> changes) {
        if (changes == null || changes.isEmpty()) {
            return changes;
        }

        return changes.stream()
            .filter(this::matchesPath)
            .filter(this::matchesPackage)
            .filter(this::matchesChangeType)
            .filter(this::matchesClassName)
            .collect(Collectors.toList());
    }

    /**
     * 检查文件路径是否匹配
     * 如果在 excludePaths 中，则排除
     */
    private boolean matchesPath(ChangeInfo change) {
        if (config.getExcludePaths() == null || config.getExcludePaths().isEmpty()) {
            return true;
        }

        String filePath = change.getFilePath();
        if (filePath == null) {
            return true;
        }

        for (String excludePattern : config.getExcludePaths()) {
            if (matchesPattern(filePath, excludePattern)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查包名是否匹配
     * 如果有 includePackages 配置，必须匹配其中之一
     */
    private boolean matchesPackage(ChangeInfo change) {
        if (config.getIncludePackages() == null || config.getIncludePackages().isEmpty()) {
            return true;
        }

        String className = change.getClassName();
        if (className == null) {
            return false;
        }

        // 提取包名
        String packageName = extractPackageName(className);

        for (String includePattern : config.getIncludePackages()) {
            if (matchesPattern(packageName, includePattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查变更类型是否匹配
     * 如果有 changeTypes 配置，必须匹配其中之一
     */
    private boolean matchesChangeType(ChangeInfo change) {
        if (config.getChangeTypes() == null || config.getChangeTypes().isEmpty()) {
            return true;
        }

        String changeType = change.getChangeType();
        return config.getChangeTypes().contains(changeType);
    }

    /**
     * 检查类名是否匹配
     * 如果在 excludeClassNames 中，则排除
     */
    private boolean matchesClassName(ChangeInfo change) {
        if (config.getExcludeClassNames() == null || config.getExcludeClassNames().isEmpty()) {
            return true;
        }

        String className = change.getClassName();
        if (className == null) {
            return true;
        }

        // 只取简单类名（不包含包名）
        String simpleClassName = extractSimpleClassName(className);

        for (String excludePattern : config.getExcludeClassNames()) {
            if (matchesPattern(simpleClassName, excludePattern)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 模式匹配（支持通配符 *）
     */
    private boolean matchesPattern(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }

        // 将通配符模式转换为正则表达式
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");

        return text.matches(regex);
    }

    /**
     * 从完整类名提取包名
     * 例如：com.example.MyClass -> com.example
     */
    private String extractPackageName(String fullClassName) {
        if (fullClassName == null) {
            return "";
        }

        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot > 0) {
            return fullClassName.substring(0, lastDot);
        }

        return "";
    }

    /**
     * 从完整类名提取简单类名
     * 例如：com.example.MyClass -> MyClass
     */
    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "";
        }

        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullClassName.substring(lastDot + 1);
        }

        return fullClassName;
    }
}
