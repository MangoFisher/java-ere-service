package com.java.ere; // 包路径更新

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Entity {
    private String id;
    private String type;
    private Map<String, String> properties = new HashMap<>();
    private Map<String, List<String>> relations = new HashMap<>();

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
    public Map<String, List<String>> getRelations() { return relations; }
    public void setRelations(Map<String, List<String>> relations) { this.relations = relations; }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public void addRelation(String relationType, String targetId) {
        relations.computeIfAbsent(relationType, k -> new ArrayList<>()).add(targetId);
    }
}