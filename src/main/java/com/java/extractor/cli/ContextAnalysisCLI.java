package com.java.extractor.cli;

import com.java.extractor.model.ContextOutput;
import com.java.extractor.query.Neo4jQueryService;
import com.java.extractor.service.ContextAnalysisService;

/**
 * 上下文分析命令行入口
 * 独立于现有的git diff分析逻辑
 */
public class ContextAnalysisCLI {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        
        try {
            switch (command) {
                case "analyze-class":
                    analyzeClass(args);
                    break;
                case "analyze-all":
                    analyzeAll(args);
                    break;
                default:
                    System.err.println("未知命令: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * 分析单个类
     * 用法: analyze-class <generated_input.json路径> <类名> [输出文件路径] [项目根路径] [neo4j配置]
     */
    private static void analyzeClass(String[] args) {
        if (args.length < 3) {
            System.err.println("analyze-class 命令参数不足");
            System.err.println("用法: analyze-class <generated_input.json路径> <类名> [输出文件路径] [项目根路径] [neo4j-uri] [neo4j-user] [neo4j-password]");
            System.exit(1);
        }
        
        String generatedInputPath = args[1];
        String className = args[2];
        String outputPath = args.length > 3 ? args[3] : "context_output_" + className + ".json";
        String projectRoot = args.length > 4 ? args[4] : System.getProperty("user.dir");
        String neo4jUri = args.length > 5 ? args[5] : "bolt://localhost:7687";
        String neo4jUser = args.length > 6 ? args[6] : "neo4j";
        String neo4jPassword = args.length > 7 ? args[7] : "password";
        
        System.out.println("开始分析类: " + className);
        System.out.println("输入文件: " + generatedInputPath);
        System.out.println("输出文件: " + outputPath);
        System.out.println("项目根路径: " + projectRoot);
        System.out.println("Neo4j连接: " + neo4jUri);
        
        try (Neo4jQueryService neo4jService = new Neo4jQueryService(neo4jUri, neo4jUser, neo4jPassword)) {
            // 测试Neo4j连接
            if (!neo4jService.testConnection()) {
                System.err.println("Neo4j连接失败，请检查连接配置");
                System.exit(1);
            }
            
            ContextAnalysisService analysisService = new ContextAnalysisService(neo4jService, projectRoot);
            ContextOutput result = analysisService.analyzeClassContext(generatedInputPath, className);
            analysisService.saveToFile(result, outputPath);
            
            System.out.println("分析完成！");
            
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * 分析所有类
     * 用法: analyze-all <generated_input.json路径> [输出文件路径] [项目根路径] [neo4j配置]
     */
    private static void analyzeAll(String[] args) {
        if (args.length < 2) {
            System.err.println("analyze-all 命令参数不足");
            System.err.println("用法: analyze-all <generated_input.json路径> [输出文件路径] [项目根路径] [neo4j-uri] [neo4j-user] [neo4j-password]");
            System.exit(1);
        }
        
        String generatedInputPath = args[1];
        String outputPath = args.length > 2 ? args[2] : "context_output_all.json";
        String projectRoot = args.length > 3 ? args[3] : System.getProperty("user.dir");
        String neo4jUri = args.length > 4 ? args[4] : "bolt://localhost:7687";
        String neo4jUser = args.length > 5 ? args[5] : "neo4j";
        String neo4jPassword = args.length > 6 ? args[6] : "password";
        
        System.out.println("开始分析所有类");
        System.out.println("输入文件: " + generatedInputPath);
        System.out.println("输出文件: " + outputPath);
        System.out.println("项目根路径: " + projectRoot);
        System.out.println("Neo4j连接: " + neo4jUri);
        
        try (Neo4jQueryService neo4jService = new Neo4jQueryService(neo4jUri, neo4jUser, neo4jPassword)) {
            // 测试Neo4j连接
            if (!neo4jService.testConnection()) {
                System.err.println("Neo4j连接失败，请检查连接配置");
                System.exit(1);
            }
            
            ContextAnalysisService analysisService = new ContextAnalysisService(neo4jService, projectRoot);
            ContextOutput result = analysisService.analyzeAllClasses(generatedInputPath);
            analysisService.saveToFile(result, outputPath);
            
            System.out.println("分析完成！");
            
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("上下文分析工具");
        System.out.println("用法:");
        System.out.println("  analyze-class <generated_input.json路径> <类名> [输出文件路径] [项目根路径] [neo4j-uri] [neo4j-user] [neo4j-password]");
        System.out.println("    - 分析指定类的上下文信息");
        System.out.println("  analyze-all <generated_input.json路径> [输出文件路径] [项目根路径] [neo4j-uri] [neo4j-user] [neo4j-password]");
        System.out.println("    - 分析所有类的上下文信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -cp target/classes com.java.extractor.cli.ContextAnalysisCLI analyze-class generated_input.json MyClass");
        System.out.println("  java -cp target/classes com.java.extractor.cli.ContextAnalysisCLI analyze-all generated_input.json");
    }
}
