package com.java.extractor.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.extractor.CodeLocation;
import com.java.extractor.ExtractResult;
import com.java.extractor.MethodExtractor;
import com.java.extractor.model.ChangeAnalysis;
import com.java.extractor.query.Neo4jQueryService;
import com.java.extractor.service.ChangeAnalysisService;
import com.java.extractor.service.DiffAnalysisService;
import com.java.extractor.source.SourceExtractor;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码提取器命令行接口
 * 
 * 用法:
 *   java -cp ... com.java.extractor.cli.ExtractorCLI extract-method <file> <class> <method>
 *   java -cp ... com.java.extractor.cli.ExtractorCLI analyze-changes --input <input.json> --output <output.json>
 * 
 * 示例:
 *   java -cp ... com.java.extractor.cli.ExtractorCLI extract-method \
 *     src/main/java/com/java/ere/CodeParser.java \
 *     CodeParser \
 *     initSymbolResolver
 */
public class ExtractorCLI {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        
        switch (command) {
            case "parse-diff":
                handleParseDiff(args);
                break;
                
            case "extract-method":
                handleExtractMethod(args);
                break;
                
            case "analyze-changes":
                handleAnalyzeChanges(args);
                break;
                
            case "help":
            case "--help":
            case "-h":
                printUsage();
                break;
                
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }
    
    /**
     * 处理 parse-diff 命令
     */
    private static void handleParseDiff(String[] args) {
        String diffFile = null;
        String projectRoot = null;
        String outputFile = "generated_input.json";  // 默认输出
        String neo4jUri = "bolt://localhost:7687";
        String neo4jUser = "neo4j";
        String neo4jPassword = "password";
        
        // 解析参数
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--diff") && i + 1 < args.length) {
                diffFile = args[i + 1];
                i++;
            } else if (args[i].equals("--project") && i + 1 < args.length) {
                projectRoot = args[i + 1];
                i++;
            } else if (args[i].equals("--output") && i + 1 < args.length) {
                outputFile = args[i + 1];
                i++;
            } else if (args[i].equals("--neo4j-uri") && i + 1 < args.length) {
                neo4jUri = args[i + 1];
                i++;
            } else if (args[i].equals("--neo4j-user") && i + 1 < args.length) {
                neo4jUser = args[i + 1];
                i++;
            } else if (args[i].equals("--neo4j-password") && i + 1 < args.length) {
                neo4jPassword = args[i + 1];
                i++;
            }
        }
        
        // 验证必需参数
        if (diffFile == null) {
            System.err.println("Error: --diff is required");
            System.err.println("Usage: parse-diff --diff <file> --project <path> [--output <file>]");
            System.exit(1);
        }
        
        if (projectRoot == null) {
            System.err.println("Error: --project is required");
            System.err.println("Usage: parse-diff --diff <file> --project <path> [--output <file>]");
            System.exit(1);
        }
        
        // 创建Neo4j配置
        Map<String, String> neo4jConfig = new HashMap<>();
        neo4jConfig.put("uri", neo4jUri);
        neo4jConfig.put("user", neo4jUser);
        neo4jConfig.put("password", neo4jPassword);
        
        // 执行分析
        DiffAnalysisService service = new DiffAnalysisService();
        service.analyzeDiffToJson(diffFile, projectRoot, outputFile, neo4jConfig);
    }
    
    /**
     * 处理 extract-method 命令
     */
    private static void handleExtractMethod(String[] args) {
        if (args.length < 4) {
            System.err.println("Error: extract-method requires 3 arguments");
            System.err.println("Usage: extract-method <file> <class> <method>");
            System.exit(1);
        }
        
        String filePath = args[1];
        String className = args[2];
        String methodName = args[3];
        String signature = args.length > 4 ? args[4] : null;
        
        // 创建提取器
        MethodExtractor extractor = new MethodExtractor();
        CodeLocation location = new CodeLocation(className, methodName, signature);
        
        // 执行提取
        ExtractResult result = extractor.extractMethod(filePath, location);
        
        // 输出 JSON（供 Python 解析）
        System.out.println(result.toJson());
        
        // 设置退出码
        System.exit(result.isSuccess() ? 0 : 1);
    }
    
    /**
     * 处理 analyze-changes 命令
     */
    private static void handleAnalyzeChanges(String[] args) {
        String inputFile = null;
        String outputFile = null;
        
        // 解析参数
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--input") && i + 1 < args.length) {
                inputFile = args[i + 1];
                i++;
            } else if (args[i].equals("--output") && i + 1 < args.length) {
                outputFile = args[i + 1];
                i++;
            }
        }
        
        if (inputFile == null) {
            System.err.println("Error: --input is required");
            System.exit(1);
        }
        
        if (outputFile == null) {
            System.err.println("Error: --output is required");
            System.exit(1);
        }
        
        try {
            // 读取输入JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            AnalysisInput request = gson.fromJson(new FileReader(inputFile), AnalysisInput.class);
            
            System.out.println("===============================================");
            System.out.println("变更分析工具");
            System.out.println("===============================================");
            System.out.println("输入文件: " + inputFile);
            System.out.println("输出文件: " + outputFile);
            System.out.println("项目路径: " + request.getProjectRoot());
            System.out.println("变更数量: " + request.getChanges().size());
            System.out.println();
            
            // 测试Neo4j连接
            System.out.println("[1/3] 连接Neo4j...");
            Neo4jQueryService neo4jService = new Neo4jQueryService(
                request.getNeo4jConfig().getUri(),
                request.getNeo4jConfig().getUser(),
                request.getNeo4jConfig().getPassword()
            );
            
            if (!neo4jService.testConnection()) {
                System.err.println("Neo4j连接失败！");
                System.exit(1);
            }
            System.out.println("✓ Neo4j连接成功");
            System.out.println();
            
            // 创建服务
            System.out.println("[2/3] 初始化服务...");
            SourceExtractor sourceExtractor = new SourceExtractor(request.getProjectRoot());
            ChangeAnalysisService analysisService = new ChangeAnalysisService(
                neo4jService, 
                sourceExtractor, 
                request.getQueryConfig()
            );
            System.out.println("✓ 服务初始化完成");
            System.out.println();
            
            // 执行分析
            System.out.println("[3/3] 分析变更...");
            java.util.List<ChangeAnalysis> analyses = analysisService.analyzeChanges(request.getChanges());
            System.out.println("✓ 分析完成");
            System.out.println();
            
            // 输出结果统计
            System.out.println("===============================================");
            System.out.println("分析结果统计");
            System.out.println("===============================================");
            for (ChangeAnalysis analysis : analyses) {
                System.out.println("变更: " + analysis.getChange().toEntityId());
                System.out.println("  上游: " + (analysis.getUpstream() != null ? analysis.getUpstream().size() : 0));
                System.out.println("  下游: " + (analysis.getDownstream() != null ? analysis.getDownstream().size() : 0));
            }
            System.out.println();
            
            // 保存结果
            System.out.println("保存结果到: " + outputFile);
            AnalysisOutput output = new AnalysisOutput(analyses);
            try (FileWriter writer = new FileWriter(outputFile)) {
                gson.toJson(output, writer);
            }
            System.out.println("✓ 结果已保存");
            System.out.println("===============================================");
            
            // 关闭连接
            neo4jService.close();
            
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("Code Extractor CLI - 代码提取工具");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  1. parse-diff --diff <file> --project <path> [options]");
        System.out.println("     解析git diff文件，生成变更分析输入JSON");
        System.out.println("     Options:");
        System.out.println("       --output <file>          输出文件 (默认: generated_input.json)");
        System.out.println("       --neo4j-uri <uri>        Neo4j URI (默认: bolt://localhost:7687)");
        System.out.println("       --neo4j-user <user>      Neo4j用户名 (默认: neo4j)");
        System.out.println("       --neo4j-password <pass>  Neo4j密码 (默认: password)");
        System.out.println();
        System.out.println("  2. analyze-changes --input <input.json> --output <output.json>");
        System.out.println("     分析代码变更的上下游影响");
        System.out.println();
        System.out.println("  3. extract-method <file> <class> <method> [signature]");
        System.out.println("     从文件中提取指定方法的代码");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # 解析git diff");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"com.java.extractor.cli.ExtractorCLI\" \\");
        System.out.println("    -Dexec.args=\"parse-diff --diff git_diff.txt --project /path/to/project\"");
        System.out.println();
        System.out.println("  # 分析变更");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"com.java.extractor.cli.ExtractorCLI\" \\");
        System.out.println("    -Dexec.args=\"analyze-changes --input generated_input.json --output output.json\"");
        System.out.println();
        System.out.println("  # 提取方法");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"com.java.extractor.cli.ExtractorCLI\" \\");
        System.out.println("    -Dexec.args=\"extract-method src/main/java/Test.java Test method\"");
    }
}
