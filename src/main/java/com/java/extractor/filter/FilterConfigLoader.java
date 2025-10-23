package com.java.extractor.filter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

/**
 * 过滤器配置加载器
 * 从 analysis-config.yml 的 git_diff_extraction 节点加载配置
 */
public class FilterConfigLoader {

    private static final String DEFAULT_CONFIG_PATH = "analysis-config.yml";

    /**
     * 从默认路径加载配置
     */
    public static FilterConfig loadConfig() {
        return loadConfig(DEFAULT_CONFIG_PATH);
    }

    /**
     * 从指定路径加载配置
     */
    public static FilterConfig loadConfig(String configPath) {
        try (InputStream input = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            if (data == null || !data.containsKey("git_diff_extraction")) {
                System.out.println(
                    "[过滤器] 未找到 git_diff_extraction 配置，使用默认配置（无过滤）"
                );
                return createDefaultConfig();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> extractionConfig = (Map<
                String,
                Object
            >) data.get("git_diff_extraction");

            return parseFilterConfig(extractionConfig);
        } catch (Exception e) {
            System.err.println("[过滤器] 加载配置失败: " + e.getMessage());
            System.out.println("[过滤器] 使用默认配置（无过滤）");
            return createDefaultConfig();
        }
    }

    /**
     * 解析过滤器配置
     */
    private static FilterConfig parseFilterConfig(
        Map<String, Object> extractionConfig
    ) {
        FilterConfig config = new FilterConfig();

        // 解析代码行过滤配置
        if (extractionConfig.containsKey("code_line_filter")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> codeLineFilterMap = (Map<
                String,
                Object
            >) extractionConfig.get("code_line_filter");
            config.setCodeLineFilter(
                parseCodeLineFilterConfig(codeLineFilterMap)
            );
        }

        // 解析通用配置
        if (extractionConfig.containsKey("common")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> commonMap = (Map<
                String,
                Object
            >) extractionConfig.get("common");
            config.setCommon(parseCommonConfig(commonMap));
        }

        // 解析 Field 配置
        if (extractionConfig.containsKey("field")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldMap = (Map<
                String,
                Object
            >) extractionConfig.get("field");
            config.setField(parseFieldConfig(fieldMap));
        }

        // 解析 Method 配置
        if (extractionConfig.containsKey("method")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> methodMap = (Map<
                String,
                Object
            >) extractionConfig.get("method");
            config.setMethod(parseMethodConfig(methodMap));
        }

        // 解析 ClassOrInterface 配置
        if (extractionConfig.containsKey("class_or_interface")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> classMap = (Map<
                String,
                Object
            >) extractionConfig.get("class_or_interface");
            config.setClassOrInterface(parseClassConfig(classMap));
        }

        return config;
    }

    /**
     * 解析代码行过滤配置
     */
    private static CodeLineFilterConfig parseCodeLineFilterConfig(
        Map<String, Object> map
    ) {
        CodeLineFilterConfig config = new CodeLineFilterConfig();

        if (map.containsKey("filter_empty_lines")) {
            config.setFilterEmptyLines(
                parseBoolean(map.get("filter_empty_lines"), true)
            );
        }

        if (map.containsKey("filter_comments")) {
            config.setFilterComments(
                parseBoolean(map.get("filter_comments"), true)
            );
        }

        if (map.containsKey("filter_logging_statements")) {
            config.setFilterLoggingStatements(
                parseBoolean(map.get("filter_logging_statements"), false)
            );
        }

        if (map.containsKey("logging_patterns")) {
            config.setLoggingPatterns(
                parseStringList(map.get("logging_patterns"))
            );
        }

        if (map.containsKey("filter_imports_and_packages")) {
            config.setFilterImportsAndPackages(
                parseBoolean(map.get("filter_imports_and_packages"), false)
            );
        }

        if (map.containsKey("custom_exclude_patterns")) {
            config.setCustomExcludePatterns(
                parseStringList(map.get("custom_exclude_patterns"))
            );
        }

        return config;
    }

    /**
     * 解析通用配置
     */
    private static FilterConfig.CommonFilterConfig parseCommonConfig(
        Map<String, Object> map
    ) {
        FilterConfig.CommonFilterConfig config =
            new FilterConfig.CommonFilterConfig();

        if (map.containsKey("exclude_paths")) {
            config.setExcludePaths(parseStringList(map.get("exclude_paths")));
        }

        if (map.containsKey("include_packages")) {
            config.setIncludePackages(
                parseStringList(map.get("include_packages"))
            );
        }

        if (map.containsKey("change_types")) {
            List<String> types = parseStringList(map.get("change_types"));
            config.setChangeTypes(new HashSet<>(types));
        }

        if (map.containsKey("exclude_class_names")) {
            config.setExcludeClassNames(
                parseStringList(map.get("exclude_class_names"))
            );
        }

        return config;
    }

    /**
     * 解析 Field 配置
     */
    private static FilterConfig.FieldFilterConfig parseFieldConfig(
        Map<String, Object> map
    ) {
        FilterConfig.FieldFilterConfig config =
            new FilterConfig.FieldFilterConfig();

        if (map.containsKey("scopes")) {
            List<String> scopes = parseStringList(map.get("scopes"));
            config.setScopes(new HashSet<>(scopes));
        }

        if (map.containsKey("constants_only")) {
            config.setConstantsOnly((Boolean) map.get("constants_only"));
        }

        if (map.containsKey("exclude_field_names")) {
            config.setExcludeFieldNames(
                parseStringList(map.get("exclude_field_names"))
            );
        }

        if (map.containsKey("include_field_names")) {
            config.setIncludeFieldNames(
                parseStringList(map.get("include_field_names"))
            );
        }

        return config;
    }

    /**
     * 解析 Method 配置
     */
    private static FilterConfig.MethodFilterConfig parseMethodConfig(
        Map<String, Object> map
    ) {
        FilterConfig.MethodFilterConfig config =
            new FilterConfig.MethodFilterConfig();

        if (map.containsKey("signature_changed_only")) {
            config.setSignatureChangedOnly(
                (Boolean) map.get("signature_changed_only")
            );
        }

        if (map.containsKey("min_changed_lines")) {
            config.setMinChangedLines((Integer) map.get("min_changed_lines"));
        }

        if (map.containsKey("exclude_method_names")) {
            config.setExcludeMethodNames(
                parseStringList(map.get("exclude_method_names"))
            );
        }

        if (map.containsKey("include_method_names")) {
            config.setIncludeMethodNames(
                parseStringList(map.get("include_method_names"))
            );
        }

        return config;
    }

    /**
     * 解析 ClassOrInterface 配置
     */
    private static FilterConfig.ClassFilterConfig parseClassConfig(
        Map<String, Object> map
    ) {
        FilterConfig.ClassFilterConfig config =
            new FilterConfig.ClassFilterConfig();

        if (map.containsKey("min_changed_lines")) {
            config.setMinChangedLines((Integer) map.get("min_changed_lines"));
        }

        if (map.containsKey("exclude_class_names")) {
            config.setExcludeClassNames(
                parseStringList(map.get("exclude_class_names"))
            );
        }

        if (map.containsKey("include_class_names")) {
            config.setIncludeClassNames(
                parseStringList(map.get("include_class_names"))
            );
        }

        return config;
    }

    /**
     * 解析布尔值
     */
    private static boolean parseBoolean(Object obj, boolean defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        }
        return defaultValue;
    }

    /**
     * 解析字符串列表（过滤掉 null 和空字符串）
     */
    private static List<String> parseStringList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }

        if (!(obj instanceof List)) {
            return new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) obj;

        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                String str = item.toString().trim();
                if (!str.isEmpty()) {
                    result.add(str);
                }
            }
        }

        return result;
    }

    /**
     * 创建默认配置（无过滤）
     */
    private static FilterConfig createDefaultConfig() {
        return new FilterConfig();
    }

    /**
     * 打印配置信息（用于调试）
     */
    public static void printConfig(FilterConfig config) {
        System.out.println("========== 过滤器配置 ==========");

        // Code Line Filter
        CodeLineFilterConfig codeLineFilter = config.getCodeLineFilter();
        System.out.println("代码行过滤配置:");
        System.out.println(
            "  filterEmptyLines: " + codeLineFilter.isFilterEmptyLines()
        );
        System.out.println(
            "  filterComments: " + codeLineFilter.isFilterComments()
        );
        System.out.println(
            "  filterLoggingStatements: " +
                codeLineFilter.isFilterLoggingStatements()
        );
        System.out.println(
            "  loggingPatterns: " + codeLineFilter.getLoggingPatterns()
        );
        System.out.println(
            "  filterImportsAndPackages: " +
                codeLineFilter.isFilterImportsAndPackages()
        );
        System.out.println(
            "  customExcludePatterns: " +
                codeLineFilter.getCustomExcludePatterns()
        );

        // Common
        FilterConfig.CommonFilterConfig common = config.getCommon();
        System.out.println("通用配置:");
        System.out.println("  excludePaths: " + common.getExcludePaths());
        System.out.println("  includePackages: " + common.getIncludePackages());
        System.out.println("  changeTypes: " + common.getChangeTypes());
        System.out.println(
            "  excludeClassNames: " + common.getExcludeClassNames()
        );

        // Field
        FilterConfig.FieldFilterConfig field = config.getField();
        System.out.println("Field 配置:");
        System.out.println("  scopes: " + field.getScopes());
        System.out.println("  constantsOnly: " + field.getConstantsOnly());
        System.out.println(
            "  excludeFieldNames: " + field.getExcludeFieldNames()
        );
        System.out.println(
            "  includeFieldNames: " + field.getIncludeFieldNames()
        );

        // Method
        FilterConfig.MethodFilterConfig method = config.getMethod();
        System.out.println("Method 配置:");
        System.out.println(
            "  signatureChangedOnly: " + method.getSignatureChangedOnly()
        );
        System.out.println("  minChangedLines: " + method.getMinChangedLines());
        System.out.println(
            "  excludeMethodNames: " + method.getExcludeMethodNames()
        );
        System.out.println(
            "  includeMethodNames: " + method.getIncludeMethodNames()
        );

        // ClassOrInterface
        FilterConfig.ClassFilterConfig clazz = config.getClassOrInterface();
        System.out.println("ClassOrInterface 配置:");
        System.out.println("  minChangedLines: " + clazz.getMinChangedLines());
        System.out.println(
            "  excludeClassNames: " + clazz.getExcludeClassNames()
        );
        System.out.println(
            "  includeClassNames: " + clazz.getIncludeClassNames()
        );

        System.out.println("================================");
    }
}
