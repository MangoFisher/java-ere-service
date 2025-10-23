package com.java.extractor.filter;

import com.java.extractor.model.ChangeInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClassOrInterface 专用过滤器
 * 处理 ClassOrInterface 类型特有的过滤规则
 */
public class ClassChangeFilter {

    private final FilterConfig.ClassFilterConfig config;

    public ClassChangeFilter(FilterConfig.ClassFilterConfig config) {
        this.config = config;
    }

    /**
     * 应用 ClassOrInterface 专用过滤规则
     */
    public List<ChangeInfo> filter(List<ChangeInfo> changes) {
        if (changes == null || changes.isEmpty()) {
            return changes;
        }

        return changes.stream()
            .filter(c -> "ClassOrInterface".equals(c.getEntity_type()))
            .filter(this::matchesChangedLines)
            .filter(this::matchesClassName)
            .collect(Collectors.toList());
    }

    /**
     * 检查变更行数是否满足最小要求
     */
    private boolean matchesChangedLines(ChangeInfo change) {
        if (config.getMinChangedLines() == null || config.getMinChangedLines() <= 0) {
            return true;
        }

        int addedCount = change.getAddedLines() != null ? change.getAddedLines().size() : 0;
        int removedCount = change.getRemovedLines() != null ? change.getRemovedLines().size() : 0;
        int totalChanged = addedCount + removedCount;

        return totalChanged >= config.getMinChangedLines();
    }

    /**
     * 检查类名是否匹配
     * 先检查 excludeClassNames（排除），再检查 includeClassNames（包含）
     */
    private boolean matchesClassName(ChangeInfo change) {
        String className = change.getClassName();
        if (className == null) {
            return false;
        }

        // 提取简单类名（不包含包名）
        String simpleClassName = extractSimpleClassName(className);

        // 检查排除列表
        if (config.getExcludeClassNames() != null && !config.getExcludeClassNames().isEmpty()) {
            for (String excludePattern : config.getExcludeClassNames()) {
                if (matchesPattern(simpleClassName, excludePattern)) {
                    return false;
                }
            }
        }

        // 检查包含列表（如果有配置）
        if (config.getIncludeClassNames() != null && !config.getIncludeClassNames().isEmpty()) {
            for (String includePattern : config.getIncludeClassNames()) {
                if (matchesPattern(simpleClassName, includePattern)) {
                    return true;
                }
            }
            return false; // 有 include 配置但不匹配，则排除
        }

        return true; // 没有 include 配置，则通过
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
}
