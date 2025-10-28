package com.java.extractor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.extractor.model.ContextOutput;
import com.java.extractor.model.EntityInfo;
import com.java.extractor.parser.EntityIdGenerator;
import com.java.extractor.query.Neo4jQueryService;
import com.java.extractor.source.SourceExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 上下文分析服务
 * 整合Git变更信息和Neo4j上下游关系，生成完整的上下文分析报告
 */
public class ContextAnalysisService {
    
    private final GitChangeExtractor gitChangeExtractor;
    private final Neo4jQueryService neo4jQueryService;
    private final SourceExtractor sourceExtractor;
    private final Gson gson;
    
    public ContextAnalysisService(Neo4jQueryService neo4jQueryService, String projectRoot) {
        this.gitChangeExtractor = new GitChangeExtractor();
        this.neo4jQueryService = neo4jQueryService;
        this.sourceExtractor = new SourceExtractor(projectRoot);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * 分析指定类的上下文信息
     * 
     * @param generatedInputPath generated_input.json文件路径
     * @param targetClassName 目标类名
     * @return 上下文分析结果
     */
    public ContextOutput analyzeClassContext(String generatedInputPath, String targetClassName) {
        System.out.println("开始分析类: " + targetClassName);
        
        // 1. 从generated_input.json提取变更信息
        ContextOutput.ClassContext classContext = gitChangeExtractor.extractClassChanges(generatedInputPath, targetClassName);
        
        // 2. 查询Neo4j获取上下游关系并提取源码
        enrichWithNeo4jData(targetClassName, classContext);
        
        // 3. 构建输出结果
        ContextOutput result = new ContextOutput();
        result.put(targetClassName, classContext);
        
        System.out.println("完成分析类: " + targetClassName);
        return result;
    }
    
    /**
     * 使用Neo4j数据丰富上下文信息
     */
    private void enrichWithNeo4jData(String className, ContextOutput.ClassContext classContext) {
        // 处理Field实体（列表）
        if (classContext.getField() != null) {
            enrichFieldContexts(className, classContext.getField());
        }
        
        // 处理Method实体（列表）
        if (classContext.getMethod() != null) {
            enrichMethodContexts(className, classContext.getMethod());
        }
        
        // 处理ClassOrInterface实体
        if (classContext.getClassOrInterface() != null) {
            enrichClassContext(className, classContext.getClassOrInterface());
        }
    }
    /**
     * 丰富Field上下文（Field只有上游，没有下游）
     */
    private void enrichFieldContexts(String className, List<ContextOutput.EntityContext> fieldContexts) {
        for (ContextOutput.EntityContext fieldContext : fieldContexts) {
            try {
                String fieldName = fieldContext.getFieldName();
                if (fieldName == null || fieldName.isEmpty()) {
                    System.out.println("跳过Field上下文查询，fieldName为空: " + className);
                    continue;
                }

                String fieldId = EntityIdGenerator.generateFieldId(className, fieldName);
                List<EntityInfo> upstreamEntities = neo4jQueryService.queryUpstream(fieldId, 1);
                List<String> upstreamSources = extractSourceCodes(upstreamEntities);
                fieldContext.setUpstream(upstreamSources);

                System.out.println("Field " + className + "." + fieldName + " 上游数量: " + upstreamSources.size());
            } catch (Exception e) {
                System.err.println("丰富Field上下文失败: " + className + " - " + e.getMessage());
            }
        }
    }
    /**
     * 丰富Method上下文（Method有上游和下游）
     */
    private void enrichMethodContexts(String className, List<ContextOutput.EntityContext> methodContexts) {
        for (ContextOutput.EntityContext methodContext : methodContexts) {
            try {
                String methodName = methodContext.getMethodName();
                if (methodName == null || methodName.isEmpty()) {
                    System.out.println("跳过Method上下文查询，methodName为空: " + className);
                    continue;
                }

                String methodId = EntityIdGenerator.generateMethodId(className, methodName, ""); // 签名暂时为空

                List<EntityInfo> upstreamEntities = neo4jQueryService.queryUpstream(methodId, 1);
                List<String> upstreamSources = extractSourceCodes(upstreamEntities);
                methodContext.setUpstream(upstreamSources);

                List<EntityInfo> downstreamEntities = neo4jQueryService.queryDownstream(methodId, 1);
                List<String> downstreamSources = extractSourceCodes(downstreamEntities);
                methodContext.setDownstream(downstreamSources);

                System.out.println("Method " + className + "." + methodName + " 上游数量: " + upstreamSources.size() + ", 下游数量: " + downstreamSources.size());
            } catch (Exception e) {
                System.err.println("丰富Method上下文失败: " + className + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * 丰富ClassOrInterface上下文（Class有上游和下游）
     */
    private void enrichClassContext(String className, ContextOutput.EntityContext classContext) {
        try {
            // 构建Class的实体ID
            String classId = EntityIdGenerator.generateClassId(className);
            
            // 查询上游（谁依赖了这个类）
            List<EntityInfo> upstreamEntities = neo4jQueryService.queryUpstream(classId, 1);
            List<String> upstreamSources = extractSourceCodes(upstreamEntities);
            classContext.setUpstream(upstreamSources);
            
            // 查询下游（这个类依赖了谁）
            List<EntityInfo> downstreamEntities = neo4jQueryService.queryDownstream(classId, 1);
            List<String> downstreamSources = extractSourceCodes(downstreamEntities);
            classContext.setDownstream(downstreamSources);
            
        } catch (Exception e) {
            System.err.println("丰富Class上下文失败: " + className + " - " + e.getMessage());
        }
    }
    
    /**
     * 从实体列表中提取源码
     */
    private List<String> extractSourceCodes(List<EntityInfo> entities) {
        List<String> sourceCodes = new ArrayList<>();
        
        for (EntityInfo entity : entities) {
            try {
                String sourceCode = sourceExtractor.extractSourceCode(entity);
                if (sourceCode != null && !sourceCode.trim().isEmpty()) {
                    sourceCodes.add(sourceCode);
                }
            } catch (Exception e) {
                System.err.println("提取源码失败: " + entity.getId() + " - " + e.getMessage());
            }
        }
        
        return sourceCodes;
    }
    
    /**
     * 将分析结果保存到JSON文件
     */
    public void saveToFile(ContextOutput contextOutput, String outputPath) {
        try (java.io.FileWriter writer = new java.io.FileWriter(outputPath)) {
            gson.toJson(contextOutput, writer);
            System.out.println("上下文分析结果已保存到: " + outputPath);
            
            // 显示文件统计信息
            java.io.File file = new java.io.File(outputPath);
            System.out.println("文件大小: " + String.format("%.2f KB", file.length() / 1024.0));
            
            // 统计分析的类数量
            int classCount = contextOutput.size();
            System.out.println("分析的类数量: " + classCount);
            
        } catch (IOException e) {
            System.err.println("保存文件失败: " + outputPath + " - " + e.getMessage());
        }
    }
    
    /**
     * 批量分析所有类
     */
    public ContextOutput analyzeAllClasses(String generatedInputPath) {
        List<String> allClassNames = gitChangeExtractor.getAllClassNames(generatedInputPath);
        System.out.println("发现 " + allClassNames.size() + " 个类需要分析");
        
        ContextOutput result = new ContextOutput();
        
        for (String className : allClassNames) {
            try {
                ContextOutput singleClassResult = analyzeClassContext(generatedInputPath, className);
                result.putAll(singleClassResult);
            } catch (Exception e) {
                System.err.println("分析类失败: " + className + " - " + e.getMessage());
            }
        }
        
        return result;
    }
}
