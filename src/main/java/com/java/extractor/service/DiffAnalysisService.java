package com.java.extractor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.extractor.diff.DiffHunk;
import com.java.extractor.diff.GitDiffParser;
import com.java.extractor.diff.JavaChangeExtractor;
import com.java.extractor.filter.CompositeChangeFilter;
import com.java.extractor.filter.FilterConfig;
import com.java.extractor.filter.FilterConfigLoader;
import com.java.extractor.model.ChangeInfo;
import com.java.extractor.util.CodeLineFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Git Diff分析服务
 * 一站式服务：git_diff.txt → generated_input.json
 */
public class DiffAnalysisService {

    private final GitDiffParser diffParser;
    private final JavaChangeExtractor changeExtractor;
    private final Gson gson;

    public DiffAnalysisService() {
        this.diffParser = new GitDiffParser();

        // 加载过滤器配置（包含代码行过滤配置）
        FilterConfig filterConfig = FilterConfigLoader.loadConfig();
        CodeLineFilter codeLineFilter = new CodeLineFilter(
            filterConfig.getCodeLineFilter()
        );

        // 使用代码行过滤器创建变更提取器
        this.changeExtractor = new JavaChangeExtractor(codeLineFilter);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * 分析Git Diff并生成输入JSON
     *
     * @param diffFilePath git diff文件路径
     * @param projectRoot 项目根目录
     * @param outputJsonPath 输出JSON路径
     * @param neo4jConfig Neo4j配置
     */
    public void analyzeDiffToJson(
        String diffFilePath,
        String projectRoot,
        String outputJsonPath,
        Map<String, String> neo4jConfig
    ) {
        System.out.println("===============================================");
        System.out.println("Git Diff分析服务");
        System.out.println("===============================================");
        System.out.println("Diff文件: " + diffFilePath);
        System.out.println("项目根目录: " + projectRoot);
        System.out.println();

        try {
            // 1. 解析git diff
            System.out.println("[1/3] 解析git diff...");
            List<DiffHunk> hunks = diffParser.parse(diffFilePath);
            System.out.println("  发现 " + hunks.size() + " 个diff块");
            System.out.println();

            // 2. 提取Java变更
            System.out.println("[2/3] 提取Java变更...");
            List<ChangeInfo> allChanges = new ArrayList<>();

            for (DiffHunk hunk : hunks) {
                System.out.println("  处理文件: " + hunk.getFilePath());
                System.out.println("    类名: " + hunk.getClassName());
                System.out.println(
                    "    新增行: " +
                        hunk.getAddedLines().size() +
                        ", 删除行: " +
                        hunk.getRemovedLines().size()
                );

                List<ChangeInfo> changes = changeExtractor.extractChanges(
                    hunk,
                    projectRoot
                );
                allChanges.addAll(changes);
            }

            // 后处理：合并跨 hunk 的 ClassOrInterface 记录
            allChanges = postProcessChanges(allChanges);

            System.out.println();
            System.out.println("  共提取 " + allChanges.size() + " 个变更");

            // 应用过滤器
            allChanges = applyFilters(allChanges);

            System.out.println();

            // 3. 生成输入JSON
            System.out.println("[3/3] 生成输入JSON...");
            generateInputJson(
                allChanges,
                projectRoot,
                neo4jConfig,
                outputJsonPath
            );

            System.out.println();
            System.out.println(
                "==============================================="
            );
            System.out.println("✓ 分析完成");
            System.out.println(
                "==============================================="
            );
            System.out.println("输出文件: " + outputJsonPath);
            System.out.println("变更数量: " + allChanges.size());
            System.out.println();
        } catch (IOException e) {
            System.err.println("❌ 分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用过滤器
     */
    private List<ChangeInfo> applyFilters(List<ChangeInfo> changes) {
        try {
            // 加载过滤器配置
            FilterConfig filterConfig = FilterConfigLoader.loadConfig();

            // 创建组合过滤器
            CompositeChangeFilter filter = new CompositeChangeFilter(
                filterConfig
            );

            // 应用过滤
            return filter.filter(changes);
        } catch (Exception e) {
            System.err.println("[过滤器] 应用过滤失败: " + e.getMessage());
            System.out.println("[过滤器] 跳过过滤，返回原始数据");
            return changes;
        }
    }

    /**
     * 后处理变更记录：
     * 合并同一个类的多个 ClassOrInterface 记录（跨 hunk）
     */
    private List<ChangeInfo> postProcessChanges(List<ChangeInfo> changes) {
        // 分离 ClassOrInterface 和其他类型的记录
        Map<String, ChangeInfo> classChanges = new LinkedHashMap<>();
        List<ChangeInfo> otherChanges = new ArrayList<>();

        for (ChangeInfo change : changes) {
            if ("ClassOrInterface".equals(change.getEntity_type())) {
                String entityId = change.toEntityId();
                if (classChanges.containsKey(entityId)) {
                    // 合并到已有记录
                    ChangeInfo existing = classChanges.get(entityId);
                    mergeClassChange(existing, change);
                } else {
                    classChanges.put(entityId, change);
                }
            } else {
                otherChanges.add(change);
            }
        }

        // 合并结果
        List<ChangeInfo> result = new ArrayList<>(otherChanges);
        result.addAll(classChanges.values());

        return result;
    }

    /**
     * 合并 ClassOrInterface 记录
     */
    private void mergeClassChange(ChangeInfo existing, ChangeInfo newChange) {
        // 合并 addedLines
        if (
            newChange.getAddedLines() != null &&
            !newChange.getAddedLines().isEmpty()
        ) {
            if (existing.getAddedLines() == null) {
                existing.setAddedLines(new ArrayList<>());
            }
            existing.getAddedLines().addAll(newChange.getAddedLines());
        }

        // 合并 removedLines
        if (
            newChange.getRemovedLines() != null &&
            !newChange.getRemovedLines().isEmpty()
        ) {
            if (existing.getRemovedLines() == null) {
                existing.setRemovedLines(new ArrayList<>());
            }
            existing.getRemovedLines().addAll(newChange.getRemovedLines());
        }
    }

    /**
     * 生成输入JSON文件
     */
    private void generateInputJson(
        List<ChangeInfo> changes,
        String projectRoot,
        Map<String, String> neo4jConfig,
        String outputPath
    ) throws IOException {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("projectRoot", projectRoot);
        inputData.put("neo4jConfig", neo4jConfig);

        // 转换ChangeInfo为字典格式
        List<Map<String, Object>> changesList = new ArrayList<>();
        for (ChangeInfo change : changes) {
            changesList.add(changeToMap(change));
        }
        inputData.put("changes", changesList);

        // 查询配置
        Map<String, Object> queryConfig = new HashMap<>();
        queryConfig.put("depth", 1);
        queryConfig.put("includeUpstream", true);
        queryConfig.put("includeDownstream", true);
        queryConfig.put("includeSourceCode", true);
        inputData.put("queryConfig", queryConfig);

        // 写入文件
        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(inputData, writer);
        }
    }

    /**
     * 将ChangeInfo转换为Map
     */
    private Map<String, Object> changeToMap(ChangeInfo change) {
        Map<String, Object> map = new HashMap<>();
        map.put("entity_type", change.getEntity_type());
        map.put("className", change.getClassName());
        map.put("changeType", change.getChangeType());
        map.put("filePath", change.getFilePath());

        if (change.getMethodName() != null) {
            map.put("methodName", change.getMethodName());
            map.put("methodSignature", change.getMethodSignature());
        }

        if (change.getFieldName() != null) {
            map.put("fieldName", change.getFieldName());
        }

        // 添加代码段（根据 entity_type 和 changeType 决定）
        String entityType = change.getEntity_type();
        String changeType = change.getChangeType();

        if ("Field".equals(entityType)) {
            // Field: addedLines 仅在 ADD 时出现，removedLines 仅在 DELETE 时出现
            if ("ADD".equals(changeType) && change.getAddedLines() != null) {
                map.put("addedLines", change.getAddedLines());
            }
            if (
                "DELETE".equals(changeType) && change.getRemovedLines() != null
            ) {
                map.put("removedLines", change.getRemovedLines());
            }
            // 添加 scope 字段
            if (change.getScope() != null) {
                map.put("scope", change.getScope());
            }
        } else if ("Method".equals(entityType)) {
            // Method: addedLines 和 removedLines 始终存在
            map.put(
                "addedLines",
                change.getAddedLines() != null
                    ? change.getAddedLines()
                    : new ArrayList<>()
            );
            map.put(
                "removedLines",
                change.getRemovedLines() != null
                    ? change.getRemovedLines()
                    : new ArrayList<>()
            );
            // 添加 signatureChange 字段
            if (change.getSignatureChange() != null) {
                map.put("signatureChange", change.getSignatureChange());
            }
        } else if ("ClassOrInterface".equals(entityType)) {
            // ClassOrInterface: addedLines 和 removedLines 始终存在
            map.put(
                "addedLines",
                change.getAddedLines() != null
                    ? change.getAddedLines()
                    : new ArrayList<>()
            );
            map.put(
                "removedLines",
                change.getRemovedLines() != null
                    ? change.getRemovedLines()
                    : new ArrayList<>()
            );
        }

        return map;
    }
}
