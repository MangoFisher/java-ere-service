package com.java.extractor.model;

/**
 * 实体信息（从Neo4j查询得到）
 */
public class EntityInfo {
    private String id;                // 实体ID
    private String entity_type;       // 实体类型
    private String name;              // 实体名称
    private String owner;             // 所属类（仅Method/Field有）
    private String filePath;          // 文件路径
    private String relationshipType;  // 关系类型（如果是上下游查询结果）
    private String sourceCode;        // 源码（后续填充）

    public EntityInfo() {}

    public EntityInfo(String id, String entity_type, String name) {
        this.id = id;
        this.entity_type = entity_type;
        this.name = name;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEntity_type() { return entity_type; }
    public void setEntity_type(String entity_type) { this.entity_type = entity_type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    @Override
    public String toString() {
        return "EntityInfo{" +
                "id='" + id + '\'' +
                ", entity_type='" + entity_type + '\'' +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", filePath='" + filePath + '\'' +
                ", relationshipType='" + relationshipType + '\'' +
                '}';
    }
}
