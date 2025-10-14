package com.java.ere; // 包路径更新

import java.util.*;

public class Entity {
    private String id;
    private String type;
    private Map<String, String> properties = new HashMap<>();
    
    // 内部存储：关系类型 -> 目标ID -> 计数
    private Map<String, Map<String, Integer>> relationCounts = new HashMap<>();

    public Entity(String id, String type) {
        this.id = id;
        this.type = type;
    }

    // Getter和Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * 添加关系，自动去重并计数
     */
    public void addRelation(String relationType, String targetId) {
        relationCounts
            .computeIfAbsent(relationType, k -> new HashMap<>())
            .merge(targetId, 1, Integer::sum);
    }
    
    /**
     * 获取关系计数（用于JSON序列化）
     * 返回格式：relationType -> List<{target: id, count: n}>
     */
    public Map<String, List<Map<String, Object>>> getRelationsWithCount() {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Integer>> entry : relationCounts.entrySet()) {
            String relationType = entry.getKey();
            List<Map<String, Object>> relationList = new ArrayList<>();
            
            for (Map.Entry<String, Integer> targetEntry : entry.getValue().entrySet()) {
                Map<String, Object> relationItem = new HashMap<>();
                relationItem.put("target", targetEntry.getKey());
                relationItem.put("count", targetEntry.getValue());
                relationList.add(relationItem);
            }
            
            result.put(relationType, relationList);
        }
        
        return result;
    }
    
    /**
     * 获取简化的关系列表（向后兼容，用于简单场景）
     * 只返回目标ID列表（去重）
     */
    public Map<String, List<String>> getRelations() {
        Map<String, List<String>> result = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Integer>> entry : relationCounts.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue().keySet()));
        }
        
        return result;
    }
    
    /**
     * 获取特定关系的计数
     */
    public int getRelationCount(String relationType, String targetId) {
        return relationCounts
            .getOrDefault(relationType, Collections.emptyMap())
            .getOrDefault(targetId, 0);
    }
    
    /**
     * 获取特定类型的所有关系（带计数）
     */
    public Map<String, Integer> getRelationsByType(String relationType) {
        return relationCounts.getOrDefault(relationType, Collections.emptyMap());
    }
}