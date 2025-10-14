package com.java.ere;

import com.java.ere.config.AnalysisConfig;
import com.java.ere.util.FileScanner;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目分析器 - 主入口
 */
public class ProjectAnalyzer {
    private CodeParser codeParser;

    public ProjectAnalyzer() {
        this.codeParser = new CodeParser();
    }

    /**
     * 执行完整的项目分析
     */
    public Map<String, Entity> analyze(AnalysisConfig config) {
        System.out.println("===============================================");
        System.out.println("Java ERE 项目分析");
        System.out.println("===============================================");
        System.out.println("项目路径: " + config.getProjectRoot());
        System.out.println("项目包名: " + config.getProjectPackages());
        System.out.println();

        // 步骤1：扫描所有Java文件
        System.out.println("[1/4] 扫描Java文件...");
        List<File> allJavaFiles = scanAllJavaFiles(config);
        System.out.println("发现 " + allJavaFiles.size() + " 个Java文件");
        System.out.println();

        // 步骤2：初始化CodeParser（传入提取配置）
        codeParser = new CodeParser(
            config.getProjectPackages(),
            config.getExtractionConfig()
        );
        
        // 步骤3：初始化符号解析器（使用全部文件）
        System.out.println("[2/4] 初始化符号解析器...");
        codeParser.initSymbolResolver(
            config.getProjectRoot(),
            config.getSourcePaths(),
            config.getResolverConfig()
        );
        System.out.println();

        // 步骤3：应用过滤规则，获取目标文件
        System.out.println("[3/4] 应用过滤规则...");
        List<File> targetFiles = applyFilters(allJavaFiles, config);
        System.out.println("过滤后剩余 " + targetFiles.size() + " 个目标文件");
        System.out.println();

        // 步骤4：只对目标文件提取实体
        System.out.println("[4/4] 提取实体和关系...");
        Map<String, Entity> entities = codeParser.parseFiles(targetFiles);
        
        System.out.println();
        System.out.println("===============================================");
        System.out.println("分析完成！");
        System.out.println("总实体数: " + entities.size());
        System.out.println("===============================================");

        return entities;
    }

    /**
     * 扫描所有Java文件
     */
    private List<File> scanAllJavaFiles(AnalysisConfig config) {
        return FileScanner.scanSourcePaths(
            config.getProjectRoot(),
            config.getSourcePaths()
        );
    }

    /**
     * 应用过滤规则
     */
    private List<File> applyFilters(List<File> files, AnalysisConfig config) {
        String projectRoot = config.getProjectRoot();
        
        return files.stream()
            .filter(file -> config.getFilterConfig().shouldInclude(file, projectRoot))
            .collect(Collectors.toList());
    }
}
