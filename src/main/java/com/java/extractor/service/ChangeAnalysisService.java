package com.java.extractor.service;

import com.java.extractor.model.*;
import com.java.extractor.query.Neo4jQueryService;
import com.java.extractor.source.SourceExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * 变更分析服务
 * 整合Neo4j查询和源码提取
 */
public class ChangeAnalysisService {

    private final Neo4jQueryService neo4jService;
    private final SourceExtractor sourceExtractor;
    private final QueryConfig queryConfig;

    public ChangeAnalysisService(Neo4jQueryService neo4jService,
                                  SourceExtractor sourceExtractor,
                                  QueryConfig queryConfig) {
        this.neo4jService = neo4jService;
        this.sourceExtractor = sourceExtractor;
        this.queryConfig = queryConfig;
    }

    /**
     * 分析单个变更
     */
    public ChangeAnalysis analyzeChange(ChangeInfo change) {
        System.out.println("分析变更: " + change);

        ChangeAnalysis analysis = new ChangeAnalysis();
        analysis.setChange(change);

        // 1. 获取实体ID
        String entityId = change.toEntityId();
        if (entityId == null) {
            System.err.println("无法生成实体ID: " + change);
            return analysis;
        }

        // 2. 查询变更实体本身
        EntityInfo changeEntity = neo4jService.queryEntity(entityId);
        if (changeEntity == null) {
            System.err.println("未在Neo4j中找到实体: " + entityId);
            // 即使未找到，也可以尝试直接从源码提取
            changeEntity = createFallbackEntity(change);
        }

        // 3. 提取变更实体的源码
        if (queryConfig.isIncludeSourceCode() && changeEntity != null) {
            String sourceCode = sourceExtractor.extractSourceCode(changeEntity);
            changeEntity.setSourceCode(sourceCode);
        }

        analysis.setChangeEntity(changeEntity);

        // 4. 查询上游
        if (queryConfig.isIncludeUpstream()) {
            List<EntityInfo> upstream = neo4jService.queryUpstream(entityId, queryConfig.getDepth());

            // 提取上游源码
            if (queryConfig.isIncludeSourceCode()) {
                for (EntityInfo entity : upstream) {
                    // 如果没有filePath，尝试查询获取
                    if (entity.getFilePath() == null || entity.getFilePath().isEmpty()) {
                        EntityInfo fullEntity = neo4jService.queryEntity(entity.getId());
                        if (fullEntity != null && fullEntity.getFilePath() != null) {
                            entity.setFilePath(fullEntity.getFilePath());
                        }
                    }

                    String sourceCode = sourceExtractor.extractSourceCode(entity);
                    entity.setSourceCode(sourceCode);
                }
            }

            analysis.setUpstream(upstream);
            System.out.println("  上游数量: " + upstream.size());
        }

        // 5. 查询下游
        if (queryConfig.isIncludeDownstream()) {
            List<EntityInfo> downstream = neo4jService.queryDownstream(entityId, queryConfig.getDepth());

            // 提取下游源码
            if (queryConfig.isIncludeSourceCode()) {
                for (EntityInfo entity : downstream) {
                    // 如果没有filePath，尝试查询获取
                    if (entity.getFilePath() == null || entity.getFilePath().isEmpty()) {
                        EntityInfo fullEntity = neo4jService.queryEntity(entity.getId());
                        if (fullEntity != null && fullEntity.getFilePath() != null) {
                            entity.setFilePath(fullEntity.getFilePath());
                        }
                    }

                    String sourceCode = sourceExtractor.extractSourceCode(entity);
                    entity.setSourceCode(sourceCode);
                }
            }

            analysis.setDownstream(downstream);
            System.out.println("  下游数量: " + downstream.size());
        }

        return analysis;
    }

    /**
     * 批量分析变更
     */
    public List<ChangeAnalysis> analyzeChanges(List<ChangeInfo> changes) {
        List<ChangeAnalysis> analyses = new ArrayList<>();

        for (ChangeInfo change : changes) {
            ChangeAnalysis analysis = analyzeChange(change);
            analyses.add(analysis);
        }

        return analyses;
    }

    /**
     * 创建降级实体（当Neo4j中找不到时）
     */
    private EntityInfo createFallbackEntity(ChangeInfo change) {
        EntityInfo entity = new EntityInfo();
        entity.setId(change.toEntityId());
        entity.setEntity_type(change.getEntity_type());
        entity.setFilePath(change.getFilePath());

        String entityType = change.getEntity_type();
        if (entityType != null) {
            switch (entityType) {
                case "Method":
                case "method":
                    entity.setName(change.getMethodName());
                    entity.setOwner(change.getClassName());
                    break;
                case "Field":
                case "field":
                    entity.setName(change.getFieldName());
                    entity.setOwner(change.getClassName());
                    break;
                case "ClassOrInterface":
                case "class":
                    entity.setName(change.getClassName());
                    break;
            }
        }

        return entity;
    }
}
