package com.java.extractor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.extractor.diff.DiffHunk;
import com.java.extractor.diff.GitDiffParser;
import com.java.extractor.diff.JavaChangeExtractor;
import com.java.extractor.model.ChangeInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
        this.changeExtractor = new JavaChangeExtractor();
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
    public void analyzeDiffToJson(String diffFilePath, String projectRoot,
                                   String outputJsonPath, Map<String, String> neo4jConfig) {
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
                System.out.println("    新增行: " + hunk.getAddedLines().size() +
                                 ", 删除行: " + hunk.getRemovedLines().size());

                List<ChangeInfo> changes = changeExtractor.extractChanges(hunk, projectRoot);
                allChanges.addAll(changes);
            }

            System.out.println();
            System.out.println("  共提取 " + allChanges.size() + " 个变更");
            System.out.println();

            // 3. 生成输入JSON
            System.out.println("[3/3] 生成输入JSON...");
            generateInputJson(allChanges, projectRoot, neo4jConfig, outputJsonPath);

            System.out.println();
            System.out.println("===============================================");
            System.out.println("✓ 分析完成");
            System.out.println("===============================================");
            System.out.println("输出文件: " + outputJsonPath);
            System.out.println("变更数量: " + allChanges.size());
            System.out.println();

        } catch (IOException e) {
            System.err.println("❌ 分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成输入JSON文件
     */
    private void generateInputJson(List<ChangeInfo> changes, String projectRoot,
                                    Map<String, String> neo4jConfig, String outputPath)
            throws IOException {

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

        // 添加代码段
        if (change.getAddedLines() != null && !change.getAddedLines().isEmpty()) {
            map.put("addedLines", change.getAddedLines());
        }

        if (change.getRemovedLines() != null && !change.getRemovedLines().isEmpty()) {
            map.put("removedLines", change.getRemovedLines());
        }

        return map;
    }
}
