package com.java.extractor.filter;

import com.java.extractor.model.ChangeInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Field 专用过滤器
 * 处理 Field 类型特有的过滤规则
 */
public class FieldChangeFilter {

    private final FilterConfig.FieldFilterConfig config;

    public FieldChangeFilter(FilterConfig.FieldFilterConfig config) {
        this.config = config;
    }

    /**
     * 应用 Field 专用过滤规则
     */
    public List<ChangeInfo> filter(List<ChangeInfo> changes) {
        if (changes == null || changes.isEmpty()) {
            return changes;
        }

        return changes.stream()
            .filter(c -> "Field".equals(c.getEntity_type()))
            .filter(this::matchesScope)
            .filter(this::matchesConstant)
            .filter(this::matchesFieldName)
            .collect(Collectors.toList());
    }

    /**
     * 检查作用域是否匹配
     * 如果有 scopes 配置，必须匹配其中之一
     */
    private boolean matchesScope(ChangeInfo change) {
        if (config.getScopes() == null || config.getScopes().isEmpty()) {
            return true;
        }

        String scope = change.getScope();
        if (scope == null) {
            return false;
        }

        return config.getScopes().contains(scope);
    }

    /**
     * 检查是否为常量
     * 如果 constantsOnly = true，则只保留常量字段
     */
    private boolean matchesConstant(ChangeInfo change) {
        if (!Boolean.TRUE.equals(config.getConstantsOnly())) {
            return true;
        }

        // 判断是否为常量：字段名全大写且包含下划线，或者以大写字母开头
        String fieldName = change.getFieldName();
        if (fieldName == null) {
            return false;
        }

        // 常量判断规则：
        // 1. 全大写 (CONFIG_VALUE)
        // 2. 全大写且包含下划线
        return fieldName.matches("^[A-Z][A-Z0-9_]*$");
    }

    /**
     * 检查字段名是否匹配
     * 先检查 excludeFieldNames（排除），再检查 includeFieldNames（包含）
     */
    private boolean matchesFieldName(ChangeInfo change) {
        String fieldName = change.getFieldName();
        if (fieldName == null) {
            return false;
        }

        // 检查排除列表
        if (config.getExcludeFieldNames() != null && !config.getExcludeFieldNames().isEmpty()) {
            for (String excludePattern : config.getExcludeFieldNames()) {
                if (matchesPattern(fieldName, excludePattern)) {
                    return false;
                }
            }
        }

        // 检查包含列表（如果有配置）
        if (config.getIncludeFieldNames() != null && !config.getIncludeFieldNames().isEmpty()) {
            for (String includePattern : config.getIncludeFieldNames()) {
                if (matchesPattern(fieldName, includePattern)) {
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
