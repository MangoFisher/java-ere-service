package com.java.ere.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体提取配置（6实体+8关系方案）
 */
public class ExtractionConfig {
    // 基础配置
    private String thirdPartyCallStrategy = "mark";  // ignore / mark / full
    private boolean includeAnnotations = true;
    private boolean includeJavadoc = true;
    
    // 场景配置
    private String scenario = "impact_analysis";  // call_chain / impact_analysis / full / custom
    
    // 6种实体类型配置
    private Map<String, Boolean> entities = new HashMap<>();
    
    // 8种关系类型配置
    private Map<String, Boolean> relations = new HashMap<>();
    
    // 高级选项
    private String onResolutionFailure = "warn";  // ignore / warn / error
    private boolean enablePerformanceStats = true;
    private boolean autoCompleteEntities = true;

    public ExtractionConfig() {
        // 初始化默认场景
        applyScenario(scenario);
    }

    /**
     * 应用预设场景配置
     */
    public void applyScenario(String scenarioName) {
        this.scenario = scenarioName;
        
        switch (scenarioName) {
            case "call_chain":
                applyCallChainScenario();
                break;
            case "impact_analysis":
                applyImpactAnalysisScenario();
                break;
            case "full":
                applyFullScenario();
                break;
            case "custom":
                // custom模式不覆盖，使用用户配置
                break;
            default:
                System.err.println("未知场景: " + scenarioName + "，使用默认场景 impact_analysis");
                applyImpactAnalysisScenario();
        }
        
        // 自动补全依赖的实体
        if (autoCompleteEntities) {
            autoCompleteEntityDependencies();
        }
    }
    
    /**
     * 场景1: 调用链分析（实体2个 + 关系1个）
     */
    private void applyCallChainScenario() {
        entities.clear();
        entities.put("ClassOrInterface", true);
        entities.put("Method", true);
        entities.put("Field", false);
        entities.put("Parameter", false);
        entities.put("Return", false);
        entities.put("Exception", false);
        
        relations.clear();
        relations.put("calls", true);
        relations.put("overrides", false);
        relations.put("has_parameter", false);
        relations.put("returns", false);
        relations.put("accesses", false);
        relations.put("throws", false);
        relations.put("has_annotation", false);
        relations.put("implements", false);
    }
    
    /**
     * 场景2: 影响范围分析（实体5个 + 关系3个）
     */
    private void applyImpactAnalysisScenario() {
        entities.clear();
        entities.put("ClassOrInterface", true);
        entities.put("Method", true);
        entities.put("Field", true);
        entities.put("Parameter", true);
        entities.put("Return", true);
        entities.put("Exception", false);
        
        relations.clear();
        relations.put("calls", true);
        relations.put("overrides", false);  // 暂不开启，耗时极高
        relations.put("has_parameter", true);
        relations.put("returns", true);
        relations.put("accesses", false);  
        relations.put("throws", false);    
        relations.put("has_annotation", false);  
        relations.put("implements", false);      
    }
    
    /**
     * 场景3: 完整分析（实体6个 + 关系8个）
     */
    private void applyFullScenario() {
        entities.clear();
        entities.put("ClassOrInterface", true);
        entities.put("Method", true);
        entities.put("Field", true);
        entities.put("Parameter", true);
        entities.put("Return", true);
        entities.put("Exception", true);  
        
        relations.clear();
        relations.put("calls", true);
        relations.put("overrides", true);        // ⚠️ 耗时极高
        relations.put("has_parameter", true);
        relations.put("returns", true);
        relations.put("accesses", true);         
        relations.put("throws", true);           
        relations.put("has_annotation", true);   
        relations.put("implements", true);       
    }
    
    /**
     * 自动补全依赖的实体
     */
    private void autoCompleteEntityDependencies() {
        // has_parameter 依赖 Parameter
        if (isRelationEnabled("has_parameter") && !isEntityEnabled("Parameter")) {
            entities.put("Parameter", true);
            System.out.println("[配置] 自动开启 Parameter 实体（被 has_parameter 关系依赖）");
        }
        
        // returns 依赖 Return
        if (isRelationEnabled("returns") && !isEntityEnabled("Return")) {
            entities.put("Return", true);
            System.out.println("[配置] 自动开启 Return 实体（被 returns 关系依赖）");
        }
        
        // throws 依赖 Exception
        if (isRelationEnabled("throws") && !isEntityEnabled("Exception")) {
            entities.put("Exception", true);
            System.out.println("[配置] 自动开启 Exception 实体（被 throws 关系依赖）");
        }
        
        // accesses 依赖 Field
        if (isRelationEnabled("accesses") && !isEntityEnabled("Field")) {
            entities.put("Field", true);
            System.out.println("[配置] 自动开启 Field 实体（被 accesses 关系依赖）");
        }
    }
    
    /**
     * 判断实体是否启用
     */
    public boolean isEntityEnabled(String entityType) {
        return entities.getOrDefault(entityType, false);
    }
    
    /**
     * 判断关系是否启用
     */
    public boolean isRelationEnabled(String relationType) {
        return relations.getOrDefault(relationType, false);
    }
    
    /**
     * 打印当前配置摘要
     */
    public void printSummary() {
        System.out.println("\n==================== 提取配置摘要 ====================");
        System.out.println("场景: " + scenario);
        
        System.out.println("\n启用的实体:");
        entities.forEach((type, enabled) -> {
            if (enabled) {
                System.out.println("  ✅ " + type);
            }
        });
        
        System.out.println("\n启用的关系:");
        relations.forEach((type, enabled) -> {
            if (enabled) {
                System.out.println("  ✅ " + type);
            }
        });
        
        System.out.println("\n高级选项:");
        System.out.println("  - 解析失败策略: " + onResolutionFailure);
        System.out.println("  - 性能统计: " + (enablePerformanceStats ? "开启" : "关闭"));
        System.out.println("  - 自动补全实体: " + (autoCompleteEntities ? "开启" : "关闭"));
        System.out.println("====================================================\n");
    }

    // Getters and Setters
    public String getThirdPartyCallStrategy() {
        return thirdPartyCallStrategy;
    }

    public void setThirdPartyCallStrategy(String thirdPartyCallStrategy) {
        this.thirdPartyCallStrategy = thirdPartyCallStrategy;
    }

    public boolean isIncludeAnnotations() {
        return includeAnnotations;
    }

    public void setIncludeAnnotations(boolean includeAnnotations) {
        this.includeAnnotations = includeAnnotations;
    }

    public boolean isIncludeJavadoc() {
        return includeJavadoc;
    }

    public void setIncludeJavadoc(boolean includeJavadoc) {
        this.includeJavadoc = includeJavadoc;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        applyScenario(scenario);
    }

    public Map<String, Boolean> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Boolean> entities) {
        this.entities = entities;
    }

    public Map<String, Boolean> getRelations() {
        return relations;
    }

    public void setRelations(Map<String, Boolean> relations) {
        this.relations = relations;
    }

    public String getOnResolutionFailure() {
        return onResolutionFailure;
    }

    public void setOnResolutionFailure(String onResolutionFailure) {
        this.onResolutionFailure = onResolutionFailure;
    }

    public boolean isEnablePerformanceStats() {
        return enablePerformanceStats;
    }

    public void setEnablePerformanceStats(boolean enablePerformanceStats) {
        this.enablePerformanceStats = enablePerformanceStats;
    }

    public boolean isAutoCompleteEntities() {
        return autoCompleteEntities;
    }

    public void setAutoCompleteEntities(boolean autoCompleteEntities) {
        this.autoCompleteEntities = autoCompleteEntities;
    }
}
