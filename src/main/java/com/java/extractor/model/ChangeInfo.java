package com.java.extractor.model;

/**
 * 变更信息
 */
public class ChangeInfo {
    private String entity_type;       // "Method", "Field", "ClassOrInterface"
    private String className;         // 类名
    private String methodName;        // 方法名（仅Method类型）
    private String methodSignature;   // 方法签名（仅Method类型）
    private String fieldName;         // 字段名（仅Field类型）
    private String changeType;        // "ADD", "MODIFY", "DELETE"
    private String filePath;          // 文件路径

    // 代码段信息
    private java.util.List<String> addedLines;      // 新增的代码行
    private java.util.List<String> removedLines;    // 删除的代码行
    private java.util.List<String> contextLines;    // 上下文代码行

    public ChangeInfo() {}

    public ChangeInfo(String entity_type, String className) {
        this.entity_type = entity_type;
        this.className = className;
    }

    // Getters and Setters
    public String getEntity_type() { return entity_type; }
    public void setEntity_type(String entity_type) { this.entity_type = entity_type; }

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

    public java.util.List<String> getAddedLines() { return addedLines; }
    public void setAddedLines(java.util.List<String> addedLines) { this.addedLines = addedLines; }

    public java.util.List<String> getRemovedLines() { return removedLines; }
    public void setRemovedLines(java.util.List<String> removedLines) { this.removedLines = removedLines; }

    public java.util.List<String> getContextLines() { return contextLines; }
    public void setContextLines(java.util.List<String> contextLines) { this.contextLines = contextLines; }

    /**
     * 生成Neo4j实体ID
     */
    public String toEntityId() {
        if (entity_type == null) {
            return null;
        }
        switch (entity_type) {
            case "Method":
            case "method":
                return "method_" + className + "_" + methodName + "(" + (methodSignature != null ? methodSignature : "") + ")";
            case "Field":
            case "field":
                return "field_" + className + "_" + fieldName;
            case "ClassOrInterface":
            case "class":
                return "class_" + className;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "ChangeInfo{" +
                "entity_type='" + entity_type + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodSignature='" + methodSignature + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", changeType='" + changeType + '\'' +
                ", filePath='" + filePath + '\'' +
                ", addedLines=" + (addedLines != null ? addedLines.size() : 0) +
                ", removedLines=" + (removedLines != null ? removedLines.size() : 0) +
                '}';
    }
}
