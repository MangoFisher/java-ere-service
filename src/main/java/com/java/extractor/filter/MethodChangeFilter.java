package com.java.extractor.filter;

import com.java.extractor.model.ChangeInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Method 专用过滤器
 * 处理 Method 类型特有的过滤规则
 */
public class MethodChangeFilter {

    private final FilterConfig.MethodFilterConfig config;

    public MethodChangeFilter(FilterConfig.MethodFilterConfig config) {
        this.config = config;
    }

    /**
     * 应用 Method 专用过滤规则
     */
    public List<ChangeInfo> filter(List<ChangeInfo> changes) {
        if (changes == null || changes.isEmpty()) {
            return changes;
        }

        return changes.stream()
            .filter(c -> "Method".equals(c.getEntity_type()))
            .filter(this::matchesSignatureChange)
            .filter(this::matchesChangedLines)
            .filter(this::matchesMethodName)
            .collect(Collectors.toList());
    }

    /**
     * 检查签名变更是否匹配
     * 如果 signatureChangedOnly = true，则只保留签名变更的方法
     */
    private boolean matchesSignatureChange(ChangeInfo change) {
        if (!Boolean.TRUE.equals(config.getSignatureChangedOnly())) {
            return true;
        }

        Boolean signatureChange = change.getSignatureChange();
        return Boolean.TRUE.equals(signatureChange);
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
     * 检查方法名是否匹配
     * 先检查 excludeMethodNames（排除），再检查 includeMethodNames（包含）
     */
    private boolean matchesMethodName(ChangeInfo change) {
        String methodName = change.getMethodName();
        if (methodName == null) {
            return false;
        }

        // 检查排除列表
        if (config.getExcludeMethodNames() != null && !config.getExcludeMethodNames().isEmpty()) {
            for (String excludePattern : config.getExcludeMethodNames()) {
                if (matchesPattern(methodName, excludePattern)) {
                    return false;
                }
            }
        }

        // 检查包含列表（如果有配置）
        if (config.getIncludeMethodNames() != null && !config.getIncludeMethodNames().isEmpty()) {
            for (String includePattern : config.getIncludeMethodNames()) {
                if (matchesPattern(methodName, includePattern)) {
                    return true;
                }
            }
            return false; // 有 include 配置但不匹配，则排除
        }

        return true; // 没有 include 配置，则通过
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
