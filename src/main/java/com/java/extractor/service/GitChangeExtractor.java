package com.java.extractor.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.java.extractor.model.ContextOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Git变更提取器
 * 从generated_input.json中提取特定类的变更信息
 */
public class GitChangeExtractor {
    
    public GitChangeExtractor() {
    }
    
    /**
     * 从generated_input.json中提取指定类的变更信息
     * 
     * @param generatedInputPath generated_input.json文件路径
     * @param targetClassName 目标类名
     * @return 该类的变更信息映射
     */
    public ContextOutput.ClassContext extractClassChanges(String generatedInputPath, String targetClassName) {
        ContextOutput.ClassContext classContext = new ContextOutput.ClassContext();
        
        // 初始化 ClassOrInterface 上下文（字段、方法为列表，构造函数已初始化）
        classContext.setClassOrInterface(new ContextOutput.EntityContext());
        
        try {
            // 读取整个JSON文件
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(generatedInputPath)));
            JsonObject rootObject = JsonParser.parseString(content).getAsJsonObject();
            
            // 获取changes数组
            if (rootObject.has("changes") && rootObject.get("changes").isJsonArray()) {
                JsonArray changesArray = rootObject.get("changes").getAsJsonArray();
                
                for (JsonElement changeElement : changesArray) {
                    if (changeElement.isJsonObject()) {
                        JsonObject changeObject = changeElement.getAsJsonObject();
                        
                        // 检查是否是目标类的变更
                        if (changeObject.has("className") && targetClassName.equals(changeObject.get("className").getAsString())) {
                            String entityType = changeObject.get("entity_type").getAsString();

                            if ("Field".equals(entityType)) {
                                ContextOutput.EntityContext item = new ContextOutput.EntityContext();
                                extractChangesFromNode(changeObject, item);
                                classContext.getField().add(item);
                            } else if ("Method".equals(entityType)) {
                                ContextOutput.EntityContext item = new ContextOutput.EntityContext();
                                extractChangesFromNode(changeObject, item);
                                classContext.getMethod().add(item);
                            } else if ("ClassOrInterface".equals(entityType)) {
                                // 聚合到单个 ClassOrInterface 上下文
                                appendChangesToClass(changeObject, classContext);
                                // 同时记录类的 filePath（如果存在）
                                if (changeObject.has("filePath")) {
                                    classContext.setFilePath(changeObject.get("filePath").getAsString());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("读取generated_input.json失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return classContext;
    }
    
    /**
     * 从JSON节点中提取变更信息
     */
    private void extractChangesFromNode(JsonObject node, ContextOutput.EntityContext entityContext) {
        ContextOutput.Changes changes = new ContextOutput.Changes();
        
        // 提取实体名称
        String entityType = node.get("entity_type").getAsString();
        if ("Field".equals(entityType) && node.has("fieldName")) {
            entityContext.setFieldName(node.get("fieldName").getAsString());
        } else if ("Method".equals(entityType) && node.has("methodName")) {
            entityContext.setMethodName(node.get("methodName").getAsString());
        }
        
        // 提取addedLines
        List<String> addedLines = new ArrayList<>();
        if (node.has("addedLines") && node.get("addedLines").isJsonArray()) {
            JsonArray addedArray = node.get("addedLines").getAsJsonArray();
            for (JsonElement element : addedArray) {
                addedLines.add(element.getAsString());
            }
        }
        changes.setAddedLines(addedLines);
        
        // 提取removedLines
        List<String> removedLines = new ArrayList<>();
        if (node.has("removedLines") && node.get("removedLines").isJsonArray()) {
            JsonArray removedArray = node.get("removedLines").getAsJsonArray();
            for (JsonElement element : removedArray) {
                removedLines.add(element.getAsString());
            }
        }
        changes.setRemovedLines(removedLines);
        
        entityContext.setChanges(changes);
        
        // 初始化上下游列表（后续由Neo4j查询填充）
        entityContext.setUpstream(new ArrayList<>());
        entityContext.setDownstream(new ArrayList<>());
    }

    /**
     * 将类级别变更附加到 ClassOrInterface 实体（累加 added/removed）
     */
    private void appendChangesToClass(JsonObject node, ContextOutput.ClassContext classContext) {
        ContextOutput.EntityContext ctx = classContext.getClassOrInterface();
        if (ctx == null) {
            ctx = new ContextOutput.EntityContext();
            classContext.setClassOrInterface(ctx);
        }

        // 确保变更对象与列表初始化
        if (ctx.getChanges() == null) {
            ctx.setChanges(new ContextOutput.Changes());
        }
        if (ctx.getChanges().getAddedLines() == null) {
            ctx.getChanges().setAddedLines(new ArrayList<String>());
        }
        if (ctx.getChanges().getRemovedLines() == null) {
            ctx.getChanges().setRemovedLines(new ArrayList<String>());
        }

        // 追加新增行
        if (node.has("addedLines") && node.get("addedLines").isJsonArray()) {
            JsonArray addedArray = node.get("addedLines").getAsJsonArray();
            for (JsonElement element : addedArray) {
                ctx.getChanges().getAddedLines().add(element.getAsString());
            }
        }

        // 追加删除行
        if (node.has("removedLines") && node.get("removedLines").isJsonArray()) {
            JsonArray removedArray = node.get("removedLines").getAsJsonArray();
            for (JsonElement element : removedArray) {
                ctx.getChanges().getRemovedLines().add(element.getAsString());
            }
        }
    }
    
    /**
     * 获取generated_input.json中所有涉及的类名
     * 
     * @param generatedInputPath generated_input.json文件路径
     * @return 类名列表
     */
    public List<String> getAllClassNames(String generatedInputPath) {
        List<String> classNames = new ArrayList<>();
        
        try {
            // 读取整个JSON文件
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(generatedInputPath)));
            JsonObject rootObject = JsonParser.parseString(content).getAsJsonObject();
            
            // 获取changes数组
            if (rootObject.has("changes") && rootObject.get("changes").isJsonArray()) {
                JsonArray changesArray = rootObject.get("changes").getAsJsonArray();
                
                for (JsonElement changeElement : changesArray) {
                    if (changeElement.isJsonObject()) {
                        JsonObject changeObject = changeElement.getAsJsonObject();
                        
                        if (changeObject.has("className")) {
                            String className = changeObject.get("className").getAsString();
                            if (!classNames.contains(className)) {
                                classNames.add(className);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("读取generated_input.json失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return classNames;
    }
}
