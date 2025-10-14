package com.java.ere;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Entity的自定义JSON序列化/反序列化适配器
 * 支持带count的关系格式
 */
public class EntityJsonAdapter implements JsonSerializer<Entity>, JsonDeserializer<Entity> {
    
    @Override
    public JsonElement serialize(Entity entity, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        
        // 序列化基本字段
        jsonObject.addProperty("id", entity.getId());
        jsonObject.addProperty("type", entity.getType());
        
        // 序列化properties
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : entity.getProperties().entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        jsonObject.add("properties", properties);
        
        // 序列化relations（带count）
        JsonObject relations = new JsonObject();
        for (Map.Entry<String, List<String>> entry : entity.getRelations().entrySet()) {
            String relationType = entry.getKey();
            List<String> targets = entry.getValue();
            
            JsonArray relationArray = new JsonArray();
            for (String target : targets) {
                int count = entity.getRelationCount(relationType, target);
                
                // 统一使用对象格式（包含count）
                JsonObject relationItem = new JsonObject();
                relationItem.addProperty("target", target);
                relationItem.addProperty("count", count);
                relationArray.add(relationItem);
            }
            
            relations.add(relationType, relationArray);
        }
        jsonObject.add("relations", relations);
        
        return jsonObject;
    }
    
    @Override
    public Entity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        // 反序列化基本字段
        String id = jsonObject.get("id").getAsString();
        String type = jsonObject.get("type").getAsString();
        Entity entity = new Entity(id, type);
        
        // 反序列化properties
        if (jsonObject.has("properties")) {
            JsonObject properties = jsonObject.getAsJsonObject("properties");
            for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
                entity.addProperty(entry.getKey(), entry.getValue().getAsString());
            }
        }
        
        // 反序列化relations（统一对象格式）
        if (jsonObject.has("relations")) {
            JsonObject relations = jsonObject.getAsJsonObject("relations");
            for (Map.Entry<String, JsonElement> entry : relations.entrySet()) {
                String relationType = entry.getKey();
                JsonArray relationArray = entry.getValue().getAsJsonArray();
                
                for (JsonElement relationElement : relationArray) {
                    JsonObject relationObj = relationElement.getAsJsonObject();
                    String target = relationObj.get("target").getAsString();
                    int count = relationObj.get("count").getAsInt();
                    
                    // 添加count次关系
                    for (int i = 0; i < count; i++) {
                        entity.addRelation(relationType, target);
                    }
                }
            }
        }
        
        return entity;
    }
}
