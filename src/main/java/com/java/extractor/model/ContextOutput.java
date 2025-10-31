package com.java.extractor.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 上下文输出数据模型
 * 对应context_output.jsonc的数据结构
 * 直接以类名为key的Map结构
 */
public class ContextOutput extends HashMap<String, ContextOutput.ClassContext> {
    
    public ContextOutput() {
        super();
    }
    
    /**
     * 单个类的上下文信息
     */
    public static class ClassContext {
        private List<EntityContext> Field;   // 由单个改为列表
        private List<EntityContext> Method;  // 由单个改为列表
        private EntityContext ClassOrInterface;
        private String filePath; // 新增：类对应的文件路径
        private List<String> latestSourceCode; // 最新源码（整个类的源码）
        
        public ClassContext() {
            this.Field = new ArrayList<>();
            this.Method = new ArrayList<>();
            this.latestSourceCode = new ArrayList<>();
        }
        
        public List<EntityContext> getField() { return Field; }
        public void setField(List<EntityContext> field) { Field = field; }
        
        public List<EntityContext> getMethod() { return Method; }
        public void setMethod(List<EntityContext> method) { Method = method; }
        public EntityContext getClassOrInterface() { return ClassOrInterface; }
        public void setClassOrInterface(EntityContext classOrInterface) { ClassOrInterface = classOrInterface; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public List<String> getLatestSourceCode() { return latestSourceCode; }
        public void setLatestSourceCode(List<String> latestSourceCode) { this.latestSourceCode = latestSourceCode; }
    }
    
    /**
     * 单个实体的上下文信息
     */
    public static class EntityContext {
        private String fieldName;    // 字段名（仅Field类型使用）
        private String methodName;   // 方法名（仅Method类型使用）
        private Changes changes;
        private List<String> upstream;
        private List<String> downstream;
        
        public EntityContext() {
            this.changes = new Changes();
            this.upstream = new ArrayList<>();
            this.downstream = new ArrayList<>();
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }
        
        public Changes getChanges() { return changes; }
        public void setChanges(Changes changes) { this.changes = changes; }
        
        public List<String> getUpstream() { return upstream; }
        public void setUpstream(List<String> upstream) { this.upstream = upstream; }
        
        public List<String> getDownstream() { return downstream; }
        public void setDownstream(List<String> downstream) { this.downstream = downstream; }
    }
    
    /**
     * 变更信息
     */
    public static class Changes {
        private List<String> addedLines;
        private List<String> removedLines;
        
        public Changes() {}
        
        public List<String> getAddedLines() { return addedLines; }
        public void setAddedLines(List<String> addedLines) { this.addedLines = addedLines; }
        
        public List<String> getRemovedLines() { return removedLines; }
        public void setRemovedLines(List<String> removedLines) { this.removedLines = removedLines; }
    }
}
