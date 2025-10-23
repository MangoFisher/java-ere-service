package com.java.extractor.util;

import com.java.extractor.filter.CodeLineFilterConfig;

import java.util.Arrays;
import java.util.List;

/**
 * 代码行过滤器演示类
 * 展示如何使用 CodeLineFilter 过滤各种非业务代码
 */
public class CodeLineFilterDemo {

    public static void main(String[] args) {
        System.out.println("==================== 代码行过滤器演示 ====================\n");

        // 测试数据
        List<String> sampleCode = Arrays.asList(
            "",
            "// This is a single line comment",
            "package com.example;",
            "",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "",
            "/**",
            " * Javadoc comment",
            " */",
            "public class Example {",
            "    ",
            "    // Field declaration",
            "    private static final Logger logger = LoggerFactory.getLogger(Example.class);",
            "    private String name;",
            "    ",
            "    public void processData(String input) {",
            "        logger.info(\"Processing input: \" + input);",
            "        System.out.println(\"Debug: \" + input);",
            "        ",
            "        // Business logic",
            "        if (input == null) {",
            "            log.error(\"Input is null\");",
            "            return;",
            "        }",
            "        ",
            "        this.name = input.trim();",
            "        LOGGER.debug(\"Name set to: \" + this.name);",
            "        LOG.warn(\"Processing complete\");",
            "    }",
            "    ",
            "    /* Multi-line comment",
            "       continues here */",
            "    public String getName() {",
            "        return this.name;",
            "    }",
            "}"
        );

        System.out.println("原始代码行数: " + sampleCode.size());
        System.out.println("原始代码:");
        printLines(sampleCode);
        System.out.println("\n" + "=".repeat(60) + "\n");

        // 测试1: 默认配置（不过滤）
        testFilter("默认配置（不过滤）", CodeLineFilterConfig.createDefault(), sampleCode);

        // 测试2: 宽松配置（只过滤空行和注释）
        testFilter("宽松配置（空行 + 注释）", CodeLineFilterConfig.createLenient(), sampleCode);

        // 测试3: 严格配置（过滤所有非业务代码）
        testFilter("严格配置（空行 + 注释 + 日志 + 导入）", CodeLineFilterConfig.createStrict(), sampleCode);

        // 测试4: 自定义配置（只过滤日志语句）
        CodeLineFilterConfig customConfig = new CodeLineFilterConfig();
        customConfig.setFilterEmptyLines(false);
        customConfig.setFilterComments(false);
        customConfig.setFilterLoggingStatements(true);
        customConfig.setLoggingPatterns(Arrays.asList(
            "logger\\.",
            "log\\.",
            "LOGGER\\.",
            "LOG\\.",
            "System\\.out\\.",
            "System\\.err\\."
        ));
        testFilter("自定义配置（只过滤日志）", customConfig, sampleCode);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("演示完成！");
        System.out.println("=".repeat(60));
    }

    /**
     * 测试过滤器
     */
    private static void testFilter(String testName, CodeLineFilterConfig config, List<String> lines) {
        System.out.println("【" + testName + "】");
        System.out.println("配置:");
        System.out.println("  - 过滤空行: " + config.isFilterEmptyLines());
        System.out.println("  - 过滤注释: " + config.isFilterComments());
        System.out.println("  - 过滤日志: " + config.isFilterLoggingStatements());
        System.out.println("  - 过滤导入: " + config.isFilterImportsAndPackages());

        CodeLineFilter filter = new CodeLineFilter(config);
        List<String> filtered = filter.filter(lines);

        System.out.println("\n过滤结果:");
        System.out.println("  原始行数: " + lines.size());
        System.out.println("  过滤后: " + filtered.size());
        System.out.println("  移除行数: " + (lines.size() - filtered.size()));

        System.out.println("\n过滤后的代码:");
        printLines(filtered);

        System.out.println("\n" + "=".repeat(60) + "\n");
    }

    /**
     * 打印代码行（带行号）
     */
    private static void printLines(List<String> lines) {
        if (lines.isEmpty()) {
            System.out.println("  (无内容)");
            return;
        }

        int maxLineNum = lines.size();
        int width = String.valueOf(maxLineNum).length();

        for (int i = 0; i < lines.size(); i++) {
            String lineNum = String.format("%" + width + "d", i + 1);
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                System.out.println("  " + lineNum + " | (空行)");
            } else {
                System.out.println("  " + lineNum + " | " + line);
            }
        }
    }
}
