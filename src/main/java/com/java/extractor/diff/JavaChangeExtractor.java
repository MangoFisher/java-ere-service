package com.java.extractor.diff;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.java.extractor.filter.CodeLineFilterConfig;
import com.java.extractor.model.ChangeInfo;
import com.java.extractor.util.CodeLineFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java变更提取器（重构版）
 * 按照 Field -> Method -> ClassOrInterface 的顺序提取变更
 * 实现多层记录策略，确保不遗漏任何变更
 */
public class JavaChangeExtractor {

    private final CodeLineFilter codeLineFilter;

    /**
     * 构造方法（使用自定义代码行过滤器）
     */
    public JavaChangeExtractor(CodeLineFilter codeLineFilter) {
        this.codeLineFilter = codeLineFilter != null
            ? codeLineFilter
            : new CodeLineFilter(CodeLineFilterConfig.createDefault());
    }

    /**
     * 构造方法（使用默认代码行过滤器）
     */
    public JavaChangeExtractor() {
        this(new CodeLineFilter(CodeLineFilterConfig.createLenient()));
    }

    /**
     * 从 DiffHunk 提取变更
     *
     * @param hunk Diff块
     * @param projectRoot 项目根目录
     * @return 变更列表
     */
    public List<ChangeInfo> extractChanges(DiffHunk hunk, String projectRoot) {
        List<ChangeInfo> changes = new ArrayList<>();

        String className = hunk.getClassName();
        String filePath = normalizeFilePath(hunk.getFilePath(), projectRoot);

        // 文件删除检测
        if (hunk.isFileDeleted()) {
            ChangeInfo change = new ChangeInfo();
            change.setEntity_type("ClassOrInterface");
            change.setClassName(className);
            change.setChangeType("DELETE");
            change.setFilePath(filePath);
            change.setAddedLines(new ArrayList<>());
            change.setRemovedLines(
                codeLineFilter.filter(hunk.getRemovedLines())
            );
            changes.add(change);

            System.out.println("    [DELETE] 文件删除 - 类: " + className);
            return changes;
        }

        // 阶段1: 提取 Field 变更
        changes.addAll(
            extractFieldChanges(hunk, className, filePath, projectRoot)
        );

        // 阶段2: 提取 Method 变更
        changes.addAll(
            extractMethodChanges(hunk, className, filePath, projectRoot)
        );

        // 阶段3: 提取 ClassOrInterface 变更
        changes.addAll(extractClassChanges(hunk, className, filePath));

        return changes;
    }

    /**
     * 阶段1: 提取 Field 变更（类成员变量 + 局部变量）
     */
    private List<ChangeInfo> extractFieldChanges(
        DiffHunk hunk,
        String className,
        String filePath,
        String projectRoot
    ) {
        List<ChangeInfo> changes = new ArrayList<>();

        try {
            CompilationUnit cu = parseSourceFile(
                projectRoot,
                hunk.getFilePath()
            );
            if (cu == null) {
                return changes;
            }

            // 提取类成员变量（不再提取局部变量）
            changes.addAll(extractClassFields(hunk, cu, className, filePath));
        } catch (Exception e) {
            System.err.println("    [警告] Field提取失败: " + e.getMessage());
        }

        return changes;
    }

    /**
     * 提取类成员变量
     */
    private List<ChangeInfo> extractClassFields(
        DiffHunk hunk,
        CompilationUnit cu,
        String className,
        String filePath
    ) {
        List<ChangeInfo> changes = new ArrayList<>();
        Map<String, List<String>> addedFieldLines = new HashMap<>();
        Map<String, List<String>> removedFieldLines = new HashMap<>();

        // 先过滤掉日志语句等非业务代码
        List<String> filteredAddedLines = codeLineFilter.filter(
            hunk.getAddedLines()
        );
        List<String> filteredRemovedLines = codeLineFilter.filter(
            hunk.getRemovedLines()
        );

        // 获取所有类成员字段
        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);

        for (FieldDeclaration field : fields) {
            if (!field.getRange().isPresent()) continue;

            int fieldLine = field.getRange().get().begin.line;

            for (VariableDeclarator var : field.getVariables()) {
                String fieldName = var.getNameAsString();

                // 检查是否在新增行中（使用过滤后的行）
                for (String line : filteredAddedLines) {
                    if (containsFieldDeclaration(line, fieldName)) {
                        addedFieldLines
                            .computeIfAbsent(fieldName, k -> new ArrayList<>())
                            .add(line);
                    }
                }

                // 检查是否在删除行中（使用过滤后的行）
                for (String line : filteredRemovedLines) {
                    if (containsFieldDeclaration(line, fieldName)) {
                        removedFieldLines
                            .computeIfAbsent(fieldName, k -> new ArrayList<>())
                            .add(line);
                    }
                }
            }
        }

        // 对于 removedLines，JavaParser 可能无法找到（因为字段已被删除）
        // 需要直接从 removedLines 中解析字段名（使用过滤后的行）
        for (String line : filteredRemovedLines) {
            String fieldName = parseFieldNameFromLine(line);
            if (
                fieldName != null && !removedFieldLines.containsKey(fieldName)
            ) {
                removedFieldLines
                    .computeIfAbsent(fieldName, k -> new ArrayList<>())
                    .add(line);
            }
        }

        // 对于 addedLines，也补充直接解析（以防 JavaParser 解析不完整）
        for (String line : hunk.getAddedLines()) {
            String fieldName = parseFieldNameFromLine(line);
            if (fieldName != null && !addedFieldLines.containsKey(fieldName)) {
                addedFieldLines
                    .computeIfAbsent(fieldName, k -> new ArrayList<>())
                    .add(line);
            }
        }

        // 生成 Field 变更记录
        Set<String> processedFields = new HashSet<>();

        // 处理新增的字段
        for (Map.Entry<
            String,
            List<String>
        > entry : addedFieldLines.entrySet()) {
            String fieldName = entry.getKey();
            if (processedFields.contains(fieldName)) continue;

            if (!removedFieldLines.containsKey(fieldName)) {
                // 纯新增
                ChangeInfo change = new ChangeInfo();
                change.setEntity_type("Field");
                change.setClassName(className);
                change.setFieldName(fieldName);
                change.setChangeType("ADD");
                change.setFilePath(filePath);
                change.setScope("ClassOrInterface");
                change.setAddedLines(entry.getValue());
                changes.add(change);

                System.out.println("    [ADD] 字段: " + fieldName);
                processedFields.add(fieldName);
            } else {
                // 字段修改：拆分成 DELETE + ADD
                ChangeInfo deleteChange = new ChangeInfo();
                deleteChange.setEntity_type("Field");
                deleteChange.setClassName(className);
                deleteChange.setFieldName(fieldName);
                deleteChange.setChangeType("DELETE");
                deleteChange.setFilePath(filePath);
                deleteChange.setScope("ClassOrInterface");
                deleteChange.setRemovedLines(removedFieldLines.get(fieldName));
                changes.add(deleteChange);

                ChangeInfo addChange = new ChangeInfo();
                addChange.setEntity_type("Field");
                addChange.setClassName(className);
                addChange.setFieldName(fieldName);
                addChange.setChangeType("ADD");
                addChange.setFilePath(filePath);
                addChange.setScope("ClassOrInterface");
                addChange.setAddedLines(entry.getValue());
                changes.add(addChange);

                System.out.println("    [DELETE+ADD] 字段修改: " + fieldName);
                processedFields.add(fieldName);
            }
        }

        // 处理纯删除的字段
        for (Map.Entry<
            String,
            List<String>
        > entry : removedFieldLines.entrySet()) {
            String fieldName = entry.getKey();
            if (processedFields.contains(fieldName)) continue;

            ChangeInfo change = new ChangeInfo();
            change.setEntity_type("Field");
            change.setClassName(className);
            change.setFieldName(fieldName);
            change.setChangeType("DELETE");
            change.setFilePath(filePath);
            change.setScope("ClassOrInterface");
            change.setRemovedLines(entry.getValue());
            changes.add(change);

            System.out.println("    [DELETE] 字段: " + fieldName);
        }

        return changes;
    }

    /**
     * 提取局部变量
     */
    private List<ChangeInfo> extractLocalVariables(
        DiffHunk hunk,
        CompilationUnit cu,
        String className,
        String filePath
    ) {
        List<ChangeInfo> changes = new ArrayList<>();
        Map<String, List<String>> addedVarLines = new HashMap<>();
        Map<String, List<String>> removedVarLines = new HashMap<>();
        Map<String, String> varToMethod = new HashMap<>();

        // 遍历所有方法
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

        for (MethodDeclaration method : methods) {
            String methodName = method.getNameAsString();

            // 查找方法内的变量声明
            List<VariableDeclarationExpr> varDecls = method.findAll(
                VariableDeclarationExpr.class
            );

            for (VariableDeclarationExpr varDecl : varDecls) {
                for (VariableDeclarator var : varDecl.getVariables()) {
                    String varName = var.getNameAsString();

                    // 检查是否在新增行中
                    for (String line : hunk.getAddedLines()) {
                        if (containsVariableDeclaration(line, varName)) {
                            addedVarLines
                                .computeIfAbsent(varName, k ->
                                    new ArrayList<>()
                                )
                                .add(line);
                            varToMethod.put(varName, methodName);
                        }
                    }

                    // 检查是否在删除行中
                    for (String line : hunk.getRemovedLines()) {
                        if (containsVariableDeclaration(line, varName)) {
                            removedVarLines
                                .computeIfAbsent(varName, k ->
                                    new ArrayList<>()
                                )
                                .add(line);
                            varToMethod.put(varName, methodName);
                        }
                    }
                }
            }
        }

        // 生成 Field 变更记录（局部变量也用 Field）
        Set<String> processedVars = new HashSet<>();

        // 处理新增的局部变量
        for (Map.Entry<String, List<String>> entry : addedVarLines.entrySet()) {
            String varName = entry.getKey();
            if (processedVars.contains(varName)) continue;

            if (!removedVarLines.containsKey(varName)) {
                // 纯新增
                ChangeInfo change = new ChangeInfo();
                change.setEntity_type("Field");
                change.setClassName(className);
                change.setFieldName(varName);
                change.setMethodName(varToMethod.get(varName));
                change.setChangeType("ADD");
                change.setFilePath(filePath);
                change.setScope("Method");
                change.setAddedLines(entry.getValue());
                changes.add(change);

                System.out.println(
                    "    [ADD] 局部变量: " +
                        varName +
                        " (方法: " +
                        varToMethod.get(varName) +
                        ")"
                );
                processedVars.add(varName);
            } else {
                // 变量修改：拆分成 DELETE + ADD
                ChangeInfo deleteChange = new ChangeInfo();
                deleteChange.setEntity_type("Field");
                deleteChange.setClassName(className);
                deleteChange.setFieldName(varName);
                deleteChange.setMethodName(varToMethod.get(varName));
                deleteChange.setChangeType("DELETE");
                deleteChange.setFilePath(filePath);
                deleteChange.setScope("Method");
                deleteChange.setRemovedLines(removedVarLines.get(varName));
                changes.add(deleteChange);

                ChangeInfo addChange = new ChangeInfo();
                addChange.setEntity_type("Field");
                addChange.setClassName(className);
                addChange.setFieldName(varName);
                addChange.setMethodName(varToMethod.get(varName));
                addChange.setChangeType("ADD");
                addChange.setFilePath(filePath);
                addChange.setScope("Method");
                addChange.setAddedLines(entry.getValue());
                changes.add(addChange);

                System.out.println(
                    "    [DELETE+ADD] 局部变量修改: " +
                        varName +
                        " (方法: " +
                        varToMethod.get(varName) +
                        ")"
                );
                processedVars.add(varName);
            }
        }

        // 处理纯删除的局部变量
        for (Map.Entry<
            String,
            List<String>
        > entry : removedVarLines.entrySet()) {
            String varName = entry.getKey();
            if (processedVars.contains(varName)) continue;

            ChangeInfo change = new ChangeInfo();
            change.setEntity_type("Field");
            change.setClassName(className);
            change.setFieldName(varName);
            change.setMethodName(varToMethod.get(varName));
            change.setChangeType("DELETE");
            change.setFilePath(filePath);
            change.setScope("Method");
            change.setRemovedLines(entry.getValue());
            changes.add(change);

            System.out.println(
                "    [DELETE] 局部变量: " +
                    varName +
                    " (方法: " +
                    varToMethod.get(varName) +
                    ")"
            );
        }

        return changes;
    }

    /**
     * 阶段2: 提取 Method 变更
     */
    private List<ChangeInfo> extractMethodChanges(
        DiffHunk hunk,
        String className,
        String filePath,
        String projectRoot
    ) {
        List<ChangeInfo> changes = new ArrayList<>();

        try {
            CompilationUnit cu = parseSourceFile(
                projectRoot,
                hunk.getFilePath()
            );
            if (cu == null) {
                return changes;
            }

            List<MethodDeclaration> methods = cu.findAll(
                MethodDeclaration.class
            );

            for (MethodDeclaration method : methods) {
                if (!method.getRange().isPresent()) continue;

                String methodName = method.getNameAsString();
                String signature = extractMethodSignature(method);
                int methodStartLine = method.getRange().get().begin.line;
                int methodEndLine = method.getRange().get().end.line;

                // 收集该方法范围内的变更行
                List<String> addedLines = new ArrayList<>();
                List<String> removedLines = new ArrayList<>();
                boolean signatureChanged = false;

                // 检查新增行（使用精确的行号匹配 + 过滤明显的类级别代码）
                for (ChangedLine changedLine : hunk.getAddedLinesWithNumbers()) {
                    String line = changedLine.getContent();
                    int lineNumber = changedLine.getLineNumber();

                    if (
                        !line.trim().isEmpty() &&
                        !isObviouslyNotInMethod(line) &&
                        lineNumber >= methodStartLine &&
                        lineNumber <= methodEndLine
                    ) {
                        addedLines.add(line);

                        // 判断是否是方法签名行
                        if (containsMethodSignature(line, methodName)) {
                            signatureChanged = true;
                        }
                    }
                }

                // 检查删除行（改进策略：只有当方法有新增行时才处理删除行）
                // 因为删除行的行号基于旧文件，无法精确匹配新文件的方法位置
                if (!addedLines.isEmpty()) {
                    for (ChangedLine changedLine : hunk.getRemovedLinesWithNumbers()) {
                        String line = changedLine.getContent();

                        if (
                            !line.trim().isEmpty() &&
                            !isObviouslyNotInMethod(line)
                        ) {
                            removedLines.add(line);

                            // 判断是否是方法签名行
                            if (containsMethodSignature(line, methodName)) {
                                signatureChanged = true;
                            }
                        }
                    }
                }

                // 如果有变更，生成 Method 记录
                if (!addedLines.isEmpty() || !removedLines.isEmpty()) {
                    String changeType = determineMethodChangeType(
                        addedLines,
                        removedLines
                    );

                    ChangeInfo change = new ChangeInfo();
                    change.setEntity_type("Method");
                    change.setClassName(className);
                    change.setMethodName(methodName);
                    change.setMethodSignature(signature);
                    change.setChangeType(changeType);
                    change.setSignatureChange(signatureChanged);
                    change.setFilePath(filePath);
                    change.setAddedLines(addedLines);
                    change.setRemovedLines(removedLines);
                    changes.add(change);

                    System.out.println(
                        "    [" +
                            changeType +
                            "] 方法: " +
                            methodName +
                            (signatureChanged ? " (签名变更)" : "")
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("    [警告] Method提取失败: " + e.getMessage());
        }

        return changes;
    }

    /**
     * 阶段3: 提取 ClassOrInterface 变更
     */
    private List<ChangeInfo> extractClassChanges(
        DiffHunk hunk,
        String className,
        String filePath
    ) {
        List<ChangeInfo> changes = new ArrayList<>();

        // 收集所有代码行的变更（过滤空行、注释、日志等非业务代码）
        List<String> addedLines = codeLineFilter.filter(hunk.getAddedLines());
        List<String> removedLines = codeLineFilter.filter(
            hunk.getRemovedLines()
        );

        // 判断变更类型
        String changeType = "MODIFY";
        if (hunk.isFileDeleted()) {
            changeType = "DELETE";
        } else if (isNewFile(hunk)) {
            changeType = "ADD";
        }

        ChangeInfo change = new ChangeInfo();
        change.setEntity_type("ClassOrInterface");
        change.setClassName(className);
        change.setChangeType(changeType);
        change.setFilePath(filePath);
        change.setAddedLines(addedLines);
        change.setRemovedLines(removedLines);
        changes.add(change);

        System.out.println("    [" + changeType + "] 类: " + className);

        return changes;
    }

    /**
     * 解析源文件
     */
    private CompilationUnit parseSourceFile(
        String projectRoot,
        String filePath
    ) {
        try {
            File sourceFile = new File(projectRoot, filePath);
            if (!sourceFile.exists()) {
                return null;
            }
            return StaticJavaParser.parse(sourceFile);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取方法签名（参数类型列表）
     */
    private String extractMethodSignature(MethodDeclaration method) {
        return method
            .getParameters()
            .stream()
            .map(p -> p.getType().asString())
            .collect(java.util.stream.Collectors.joining(","));
    }

    /**
     * 判断方法变更类型
     */
    private String determineMethodChangeType(
        List<String> added,
        List<String> removed
    ) {
        if (added.isEmpty() && !removed.isEmpty()) {
            return "DELETE";
        }
        if (!added.isEmpty() && removed.isEmpty()) {
            return "ADD";
        }
        return "MODIFY";
    }

    /**
     * 判断删除行是否明显不在方法内（启发式规则）
     * 只识别明显的类级别代码特征，减少误判
     */
    private boolean isObviouslyNotInMethod(String line) {
        String trimmed = line.trim();

        // 空行，跳过
        if (trimmed.isEmpty()) {
            return true;
        }

        // 明显不在方法内的代码特征：

        // 1. 枚举常量：大写字母开头，后跟括号
        if (trimmed.matches("^[A-Z_][A-Z0-9_]*\\s*\\(.*")) {
            return true;
        }

        // 2. 枚举常量的续行：以大量空格开头，包含参数和结束符
        if (line.startsWith("            ") || line.startsWith("\t\t\t")) {
            if (
                trimmed.endsWith("),") ||
                trimmed.endsWith("\");") ||
                (trimmed.matches(".*\".*\"\\s*\\)[,;].*") &&
                    !trimmed.contains("("))
            ) {
                return true;
            }
        }

        // 3. 字段声明（不在方法内）
        if (
            trimmed.matches(
                "^(private|public|protected)\\s+(static\\s+)?(final\\s+)?\\w+.*\\s+\\w+\\s*[;=].*"
            )
        ) {
            if (!trimmed.contains("(") || trimmed.endsWith(";")) {
                return true;
            }
        }

        // 4. 类级别的注解
        if (
            trimmed.startsWith("@") &&
            (trimmed.contains("class") ||
                trimmed.contains("interface") ||
                trimmed.contains("enum"))
        ) {
            return true;
        }

        // 5. 注释块
        if (
            trimmed.equals("/**") ||
            trimmed.equals("*/") ||
            trimmed.startsWith("/*")
        ) {
            return true;
        }

        // 6. 包声明、import语句
        if (trimmed.startsWith("package ") || trimmed.startsWith("import ")) {
            return true;
        }

        // 7. 类、接口、枚举声明行
        if (
            trimmed.matches(
                "^(public|private|protected)?\\s*(static\\s+)?(final\\s+)?(class|interface|enum)\\s+\\w+.*"
            )
        ) {
            return true;
        }

        // 其他情况：不能确定，认为可能在方法内
        return false;
    }

    /**
     * 判断行是否包含字段声明
     */
    private boolean containsFieldDeclaration(String line, String fieldName) {
        String trimmed = line.trim();
        // 简单匹配：包含字段名且有类型声明
        return (
            trimmed.contains(fieldName) &&
            (trimmed.contains("static") ||
                trimmed.contains("private") ||
                trimmed.contains("public") ||
                trimmed.contains("protected"))
        );
    }

    /**
     * 从代码行中解析字段名
     * 支持格式：public static final String FIELD_NAME = "value";
     */
    private String parseFieldNameFromLine(String line) {
        String trimmed = line.trim();

        // 跳过注释和空行
        if (
            trimmed.isEmpty() ||
            trimmed.startsWith("//") ||
            trimmed.startsWith("/*") ||
            trimmed.startsWith("*")
        ) {
            return null;
        }

        // 跳过类声明、方法声明等
        if (
            trimmed.contains("class ") ||
            trimmed.contains("interface ") ||
            trimmed.contains("enum ") ||
            trimmed.matches(".*\\s+\\w+\\s*\\(.*\\).*")
        ) {
            return null;
        }

        // 匹配字段声明模式：[modifiers] Type fieldName [= value];
        // 例如：public static final String CONFIG_NAME = "value";
        if (
            trimmed.contains("static") ||
            trimmed.contains("private") ||
            trimmed.contains("public") ||
            trimmed.contains("protected")
        ) {
            // 简单的字段名提取：找到类型后的第一个标识符
            String[] parts = trimmed.split("\\s+");
            boolean foundType = false;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];

                // 跳过修饰符
                if (
                    part.equals("public") ||
                    part.equals("private") ||
                    part.equals("protected") ||
                    part.equals("static") ||
                    part.equals("final") ||
                    part.equals("volatile") ||
                    part.equals("transient")
                ) {
                    continue;
                }

                // 找到类型后的下一个词就是字段名
                if (foundType && i < parts.length) {
                    // 移除可能的 = 或 ; 符号
                    String fieldName = part.split("[=;]")[0].trim();
                    if (!fieldName.isEmpty() && fieldName.matches("\\w+")) {
                        return fieldName;
                    }
                    break;
                }

                // 类型可能是简单类型或泛型
                if (part.matches("\\w+(<.*>)?")) {
                    foundType = true;
                }
            }
        }

        return null;
    }

    /**
     * 判断行是否包含变量声明
     */
    private boolean containsVariableDeclaration(String line, String varName) {
        String trimmed = line.trim();
        // 简单匹配：包含变量名且有类型声明或赋值
        return (
            trimmed.contains(varName) &&
            (trimmed.contains("=") ||
                trimmed.matches(".*\\s+" + varName + "\\s*[;,)].*"))
        );
    }

    /**
     * 判断行是否包含方法签名
     */
    private boolean containsMethodSignature(String line, String methodName) {
        String trimmed = line.trim();
        return (
            trimmed.contains(methodName + "(") ||
            (trimmed.contains(methodName) && trimmed.contains("("))
        );
    }

    /**
     * 判断是否为新文件
     */
    private boolean isNewFile(DiffHunk hunk) {
        return (
            hunk.getRemovedLines().isEmpty() && !hunk.getAddedLines().isEmpty()
        );
    }

    /**
     * 过滤空行（已废弃，使用 CodeLineFilter 替代）
     * @deprecated 使用 codeLineFilter.filter() 替代
     */
    @Deprecated
    private List<String> filterEmptyLines(List<String> lines) {
        // 为了向后兼容，保留此方法但使用 CodeLineFilter
        return codeLineFilter.filter(lines);
    }

    /**
     * 规范化文件路径
     */
    private String normalizeFilePath(String filePath, String projectRoot) {
        if (filePath.startsWith(projectRoot)) {
            return filePath.substring(projectRoot.length());
        }
        return filePath;
    }
}
