package com.java.extractor.filter;

import com.java.extractor.model.ChangeInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组合过滤器
 * 整合所有过滤器，按顺序应用过滤规则
 */
public class CompositeChangeFilter {

    private final CommonChangeFilter commonFilter;
    private final FieldChangeFilter fieldFilter;
    private final MethodChangeFilter methodFilter;
    private final ClassChangeFilter classFilter;
    private final boolean enabled;

    public CompositeChangeFilter(FilterConfig config) {
        this.commonFilter = new CommonChangeFilter(config.getCommon());
        this.fieldFilter = new FieldChangeFilter(config.getField());
        this.methodFilter = new MethodChangeFilter(config.getMethod());
        this.classFilter = new ClassChangeFilter(config.getClassOrInterface());
        this.enabled = true;
    }

    public CompositeChangeFilter(
        CommonChangeFilter commonFilter,
        FieldChangeFilter fieldFilter,
        MethodChangeFilter methodFilter,
        ClassChangeFilter classFilter
    ) {
        this.commonFilter = commonFilter;
        this.fieldFilter = fieldFilter;
        this.methodFilter = methodFilter;
        this.classFilter = classFilter;
        this.enabled = true;
    }

    /**
     * 应用所有过滤规则
     *
     * @param changes 原始变更列表
     * @return 过滤后的变更列表
     */
    public List<ChangeInfo> filter(List<ChangeInfo> changes) {
        if (!enabled || changes == null || changes.isEmpty()) {
            return changes;
        }

        int originalCount = changes.size();

        // 第一步：应用通用过滤
        List<ChangeInfo> afterCommon = commonFilter.filter(changes);

        if (afterCommon.isEmpty()) {
            System.out.println("[过滤] 通用过滤后无记录，跳过专用过滤");
            return afterCommon;
        }

        // 第二步：按实体类型分组
        Map<String, List<ChangeInfo>> byType = afterCommon.stream()
            .collect(Collectors.groupingBy(
                c -> c.getEntity_type() != null ? c.getEntity_type() : "Unknown"
            ));

        // 第三步：应用专用过滤
        List<ChangeInfo> result = new ArrayList<>();

        // Field 过滤
        List<ChangeInfo> fields = byType.getOrDefault("Field", Collections.emptyList());
        if (!fields.isEmpty()) {
            List<ChangeInfo> filteredFields = fieldFilter.filter(fields);
            result.addAll(filteredFields);
        }

        // Method 过滤
        List<ChangeInfo> methods = byType.getOrDefault("Method", Collections.emptyList());
        if (!methods.isEmpty()) {
            List<ChangeInfo> filteredMethods = methodFilter.filter(methods);
            result.addAll(filteredMethods);
        }

        // ClassOrInterface 过滤
        List<ChangeInfo> classes = byType.getOrDefault("ClassOrInterface", Collections.emptyList());
        if (!classes.isEmpty()) {
            List<ChangeInfo> filteredClasses = classFilter.filter(classes);
            result.addAll(filteredClasses);
        }

        // 输出过滤统计
        System.out.println("[过滤] 总记录: " + originalCount + " -> " + result.size());
        System.out.println("[过滤]   - Field: " + fields.size() + " -> " +
            result.stream().filter(c -> "Field".equals(c.getEntity_type())).count());
        System.out.println("[过滤]   - Method: " + methods.size() + " -> " +
            result.stream().filter(c -> "Method".equals(c.getEntity_type())).count());
        System.out.println("[过滤]   - ClassOrInterface: " + classes.size() + " -> " +
            result.stream().filter(c -> "ClassOrInterface".equals(c.getEntity_type())).count());

        return result;
    }

    /**
     * 获取过滤统计信息
     */
    public FilterStatistics getStatistics(List<ChangeInfo> original, List<ChangeInfo> filtered) {
        FilterStatistics stats = new FilterStatistics();
        stats.originalCount = original != null ? original.size() : 0;
        stats.filteredCount = filtered != null ? filtered.size() : 0;
        stats.removedCount = stats.originalCount - stats.filteredCount;

        if (original != null) {
            stats.originalFieldCount = (int) original.stream()
                .filter(c -> "Field".equals(c.getEntity_type())).count();
            stats.originalMethodCount = (int) original.stream()
                .filter(c -> "Method".equals(c.getEntity_type())).count();
            stats.originalClassCount = (int) original.stream()
                .filter(c -> "ClassOrInterface".equals(c.getEntity_type())).count();
        }

        if (filtered != null) {
            stats.filteredFieldCount = (int) filtered.stream()
                .filter(c -> "Field".equals(c.getEntity_type())).count();
            stats.filteredMethodCount = (int) filtered.stream()
                .filter(c -> "Method".equals(c.getEntity_type())).count();
            stats.filteredClassCount = (int) filtered.stream()
                .filter(c -> "ClassOrInterface".equals(c.getEntity_type())).count();
        }

        return stats;
    }

    /**
     * 过滤统计信息
     */
    public static class FilterStatistics {
        public int originalCount;
        public int filteredCount;
        public int removedCount;
        public int originalFieldCount;
        public int filteredFieldCount;
        public int originalMethodCount;
        public int filteredMethodCount;
        public int originalClassCount;
        public int filteredClassCount;

        @Override
        public String toString() {
            return String.format(
                "FilterStatistics{total: %d->%d, Field: %d->%d, Method: %d->%d, Class: %d->%d}",
                originalCount, filteredCount,
                originalFieldCount, filteredFieldCount,
                originalMethodCount, filteredMethodCount,
                originalClassCount, filteredClassCount
            );
        }
    }
}
