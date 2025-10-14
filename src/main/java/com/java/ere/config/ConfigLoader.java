package com.java.ere.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 配置文件加载器
 * 支持从 YAML 文件加载配置
 */
public class ConfigLoader {

    /**
     * 从 YAML 文件加载配置
     */
    public static AnalysisConfig loadFromYaml(String yamlFilePath) throws IOException {
        Yaml yaml = new Yaml();
        
        try (InputStream inputStream = new FileInputStream(yamlFilePath)) {
            Map<String, Object> data = yaml.load(inputStream);
            return parseConfig(data);
        }
    }

    /**
     * 从 classpath 资源加载配置
     */
    public static AnalysisConfig loadFromResource(String resourcePath) throws IOException {
        Yaml yaml = new Yaml();
        
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("配置文件不存在: " + resourcePath);
            }
            Map<String, Object> data = yaml.load(inputStream);
            return parseConfig(data);
        }
    }

    /**
     * 解析配置数据
     */
    @SuppressWarnings("unchecked")
    private static AnalysisConfig parseConfig(Map<String, Object> data) {
        AnalysisConfig config = new AnalysisConfig();

        // 项目基本信息
        if (data.containsKey("project")) {
            Map<String, Object> project = (Map<String, Object>) data.get("project");
            
            if (project.containsKey("name")) {
                config.setProjectName((String) project.get("name"));
            }
            if (project.containsKey("root")) {
                config.setProjectRoot((String) project.get("root"));
            }
            if (project.containsKey("packages")) {
                config.setProjectPackages(toStringList(project.get("packages")));
            }
        }

        // 源码路径
        if (data.containsKey("sources")) {
            config.setSourcePaths(toStringList(data.get("sources")));
        }

        // 分析配置
        if (data.containsKey("analysis")) {
            Map<String, Object> analysis = (Map<String, Object>) data.get("analysis");
            FilterConfig filterConfig = parseFilterConfig(analysis);
            config.setFilterConfig(filterConfig);
        }

        // 符号解析器配置
        if (data.containsKey("symbolResolver")) {
            Map<String, Object> resolver = (Map<String, Object>) data.get("symbolResolver");
            ResolverConfig resolverConfig = parseResolverConfig(resolver);
            config.setResolverConfig(resolverConfig);
        }

        // 实体提取配置
        if (data.containsKey("extraction")) {
            Map<String, Object> extraction = (Map<String, Object>) data.get("extraction");
            ExtractionConfig extractionConfig = parseExtractionConfig(extraction);
            config.setExtractionConfig(extractionConfig);
        }

        return config;
    }

    /**
     * 解析过滤配置
     */
    @SuppressWarnings("unchecked")
    private static FilterConfig parseFilterConfig(Map<String, Object> analysis) {
        FilterConfig filterConfig = new FilterConfig();

        if (analysis.containsKey("includes")) {
            filterConfig.setIncludePatterns(toStringList(analysis.get("includes")));
        }
        if (analysis.containsKey("excludes")) {
            filterConfig.setExcludePatterns(toStringList(analysis.get("excludes")));
        }

        return filterConfig;
    }

    /**
     * 解析符号解析器配置
     */
    @SuppressWarnings("unchecked")
    private static ResolverConfig parseResolverConfig(Map<String, Object> resolver) {
        ResolverConfig resolverConfig = new ResolverConfig();

        if (resolver.containsKey("includeJdk")) {
            resolverConfig.setIncludeJdk((Boolean) resolver.get("includeJdk"));
        }

        if (resolver.containsKey("dependencies")) {
            Map<String, Object> dependencies = (Map<String, Object>) resolver.get("dependencies");
            
            if (dependencies.containsKey("mode")) {
                resolverConfig.setDependencyMode((String) dependencies.get("mode"));
            }
            if (dependencies.containsKey("essentialPatterns")) {
                resolverConfig.setEssentialPatterns(toStringList(dependencies.get("essentialPatterns")));
            }
            if (dependencies.containsKey("localDir")) {
                resolverConfig.setLocalDependencyDir((String) dependencies.get("localDir"));
            }
        }

        return resolverConfig;
    }

    /**
     * 解析实体提取配置
     */
    @SuppressWarnings("unchecked")
    private static ExtractionConfig parseExtractionConfig(Map<String, Object> extraction) {
        ExtractionConfig extractionConfig = new ExtractionConfig();

        // 基础配置
        if (extraction.containsKey("thirdPartyCallStrategy")) {
            extractionConfig.setThirdPartyCallStrategy((String) extraction.get("thirdPartyCallStrategy"));
        }
        if (extraction.containsKey("includeAnnotations")) {
            extractionConfig.setIncludeAnnotations((Boolean) extraction.get("includeAnnotations"));
        }
        if (extraction.containsKey("includeJavadoc")) {
            extractionConfig.setIncludeJavadoc((Boolean) extraction.get("includeJavadoc"));
        }
        
        // 高级选项（在场景之前设置，因为autoCompleteEntities会影响场景应用）
        if (extraction.containsKey("onResolutionFailure")) {
            extractionConfig.setOnResolutionFailure((String) extraction.get("onResolutionFailure"));
        }
        if (extraction.containsKey("enablePerformanceStats")) {
            extractionConfig.setEnablePerformanceStats((Boolean) extraction.get("enablePerformanceStats"));
        }
        if (extraction.containsKey("autoCompleteEntities")) {
            extractionConfig.setAutoCompleteEntities((Boolean) extraction.get("autoCompleteEntities"));
        }
        
        // 场景配置（会覆盖entities和relations，除非是custom场景）
        if (extraction.containsKey("scenario")) {
            String scenario = (String) extraction.get("scenario");
            
            // 如果是custom场景，先加载用户配置
            if ("custom".equals(scenario)) {
                // 加载实体配置
                if (extraction.containsKey("entities")) {
                    Map<String, Boolean> entities = (Map<String, Boolean>) extraction.get("entities");
                    extractionConfig.setEntities(entities);
                }
                
                // 加载关系配置
                if (extraction.containsKey("relations")) {
                    Map<String, Boolean> relations = (Map<String, Boolean>) extraction.get("relations");
                    extractionConfig.setRelations(relations);
                }
            }
            
            // 应用场景（custom场景不会覆盖）
            extractionConfig.setScenario(scenario);
        } else {
            // 没有指定场景，使用默认场景（已在构造函数中初始化）
        }

        return extractionConfig;
    }

    /**
     * 转换为字符串列表
     */
    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        if (obj instanceof String) {
            return Arrays.asList((String) obj);
        }
        return new ArrayList<>();
    }
}
