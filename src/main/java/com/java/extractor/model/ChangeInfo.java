package com.java.extractor.model;

/**
 * 变更信息
 */
public class ChangeInfo {
    private String type;              // "method", "class", "field"
    private String className;         // 类名
    private String methodName;        // 方法名（仅method类型）
    private String methodSignature;   // 方法签名（仅method类型）
    private String fieldName;         // 字段名（仅field类型）
    private String changeType;        // "ADD", "MODIFY", "DELETE"
    private String filePath;          // 文件路径
    
    public ChangeInfo() {}
    
    public ChangeInfo(String type, String className) {
        this.type = type;
        this.className = className;
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public String getMethodSignature() { return methodSignature; }
    public void setMethodSignature(String methodSignature) { this.methodSignature = methodSignature; }
    
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    /**
     * 生成Neo4j实体ID
     */
    public String toEntityId() {
        switch (type) {
            case "method":
                return "method_" + className + "_" + methodName + "(" + (methodSignature != null ? methodSignature : "") + ")";
            case "field":
                return "field_" + className + "_" + fieldName;
            case "class":
                return "class_" + className;
            default:
                return null;
        }
    }
    
    @Override
    public String toString() {
        return "ChangeInfo{" +
                "type='" + type + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodSignature='" + methodSignature + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", changeType='" + changeType + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
