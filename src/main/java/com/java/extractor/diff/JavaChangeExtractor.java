package com.java.extractor.diff;

import com.java.extractor.model.ChangeInfo;
import com.java.extractor.parser.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java变更提取器
 * 从Diff块中提取Java代码变更
 */
public class JavaChangeExtractor {

    private final ChangeDetector changeDetector;

    public JavaChangeExtractor() {
        this.changeDetector = new ChangeDetector();
    }

    /**
     * 从Diff块中提取变更
     *
     * @param hunk Diff块
     * @param projectRoot 项目根目录（用于生成相对路径）
     * @return 变更列表
     */
    public List<ChangeInfo> extractChanges(DiffHunk hunk, String projectRoot) {
        List<ChangeInfo> changes = new ArrayList<>();

        String className = hunk.getClassName();
        String filePath = normalizeFilePath(hunk.getFilePath(), projectRoot);

        // 提取类/接口变更
        changes.addAll(extractClassChanges(hunk, className, filePath));

        // 提取字段变更（包括枚举常量）
        changes.addAll(extractFieldChanges(hunk, className, filePath));

        // 提取方法变更
        changes.addAll(extractMethodChanges(hunk, className, filePath));

        // 提取枚举值变更
        changes.addAll(extractEnumChanges(hunk, className, filePath));

        return changes;
    }

    /**
     * 提取字段变更
     */
    private List<ChangeInfo> extractFieldChanges(DiffHunk hunk, String className, String filePath) {
        List<ChangeInfo> changes = new ArrayList<>();

        // 解析新增和删除的字段
        Map<String, Map<String, Object>> addedFields = new HashMap<>();
        Map<String, Map<String, Object>> removedFields = new HashMap<>();
        Map<String, List<String>> addedFieldLines = new HashMap<>();
        Map<String, List<String>> removedFieldLines = new HashMap<>();

        for (String line : hunk.getAddedLines()) {
            if (JavaFieldParser.isFieldDeclaration(line)) {
                Map<String, Object> field = JavaFieldParser.parseFieldDeclaration(line);
                if (field != null) {
                    String fieldName = (String) field.get("name");
                    addedFields.put(fieldName, field);
                    addedFieldLines.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(line);
                }
            }
        }

        for (String line : hunk.getRemovedLines()) {
            if (JavaFieldParser.isFieldDeclaration(line)) {
                Map<String, Object> field = JavaFieldParser.parseFieldDeclaration(line);
                if (field != null) {
                    String fieldName = (String) field.get("name");
                    removedFields.put(fieldName, field);
                    removedFieldLines.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(line);
                }
            }
        }

        // 检测变更类型
        Map<String, String> fieldChanges = changeDetector.detectFieldChanges(addedFields, removedFields);

        // 生成ChangeInfo
        for (Map.Entry<String, String> entry : fieldChanges.entrySet()) {
            String fieldName = entry.getKey();
            String changeType = entry.getValue();

            ChangeInfo change = new ChangeInfo();
            change.setEntity_type("Field");
            change.setClassName(className);
            change.setFieldName(fieldName);
            change.setChangeType(changeType);
            change.setFilePath(filePath);

            // 设置代码段
            if (addedFieldLines.containsKey(fieldName)) {
                change.setAddedLines(new ArrayList<>(addedFieldLines.get(fieldName)));
            }
            if (removedFieldLines.containsKey(fieldName)) {
                change.setRemovedLines(new ArrayList<>(removedFieldLines.get(fieldName)));
            }

            changes.add(change);

            // 输出日志
            Boolean isConstant = addedFields.containsKey(fieldName)
                ? (Boolean) addedFields.get(fieldName).get("isConstant") : false;
            String qualifier = (isConstant != null && isConstant) ? "常量" : "字段";
            System.out.println("    [" + changeType + "] " + qualifier + ": " + fieldName);
            System.out.println("          实体ID: " + change.toEntityId());
        }

        return changes;
    }

    /**
     * 提取方法变更
     */
    private List<ChangeInfo> extractMethodChanges(DiffHunk hunk, String className, String filePath) {
        List<ChangeInfo> changes = new ArrayList<>();

        // 解析新增和删除的方法
        Map<String, Map<String, String>> addedMethods = new HashMap<>();
        Map<String, Map<String, String>> removedMethods = new HashMap<>();
        Map<String, List<String>> addedMethodLines = new HashMap<>();
        Map<String, List<String>> removedMethodLines = new HashMap<>();

        for (String line : hunk.getAddedLines()) {
            if (JavaMethodParser.isMethodDeclaration(line)) {
                Map<String, String> method = JavaMethodParser.parseMethodSignature(line);
                if (method != null) {
                    String name = method.get("name");
                    String sig = method.get("signature");
                    String key = name + "(" + (sig != null ? sig : "") + ")";  // methodName(signature)
                    addedMethods.put(key, method);
                    addedMethodLines.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
                }
            }
        }

        for (String line : hunk.getRemovedLines()) {
            if (JavaMethodParser.isMethodDeclaration(line)) {
                Map<String, String> method = JavaMethodParser.parseMethodSignature(line);
                if (method != null) {
                    String name = method.get("name");
                    String sig = method.get("signature");
                    String key = name + "(" + (sig != null ? sig : "") + ")";  // methodName(signature)
                    removedMethods.put(key, method);
                    removedMethodLines.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
                }
            }
        }

        // 检测变更类型
        Map<String, String> methodChanges = changeDetector.detectMethodChanges(addedMethods, removedMethods);

        // 生成ChangeInfo
        for (Map.Entry<String, String> entry : methodChanges.entrySet()) {
            String signature = entry.getKey();
            String changeType = entry.getValue();

            // 从signature中分离方法名和参数
            Map<String, String> method = addedMethods.getOrDefault(signature, removedMethods.get(signature));

            ChangeInfo change = new ChangeInfo();
            change.setEntity_type("Method");
            change.setClassName(className);
            change.setMethodName(method.get("name"));
            change.setMethodSignature(method.get("signature"));
            change.setChangeType(changeType);
            change.setFilePath(filePath);

            // 设置代码段
            if (addedMethodLines.containsKey(signature)) {
                change.setAddedLines(new ArrayList<>(addedMethodLines.get(signature)));
            }
            if (removedMethodLines.containsKey(signature)) {
                change.setRemovedLines(new ArrayList<>(removedMethodLines.get(signature)));
            }

            changes.add(change);

            // 输出日志
            System.out.println("    [" + changeType + "] 方法: " + signature);
            System.out.println("          实体ID: " + change.toEntityId());
        }

        return changes;
    }

    /**
     * 提取枚举变更（枚举值的修改）
     */
    private List<ChangeInfo> extractEnumChanges(DiffHunk hunk, String className, String filePath) {
        List<ChangeInfo> changes = new ArrayList<>();

        // 检测枚举值的变更（通常是枚举常量的参数变化）
        List<String> addedEnumLines = new ArrayList<>();
        List<String> removedEnumLines = new ArrayList<>();

        for (String line : hunk.getAddedLines()) {
            String trimmed = line.trim();
            // 枚举常量模式：大写字母开头，包含括号
            if (trimmed.matches("^[A-Z][A-Z0-9_]*\\s*\\([^)]*\\).*")) {
                addedEnumLines.add(line);
            }
        }

        for (String line : hunk.getRemovedLines()) {
            String trimmed = line.trim();
            if (trimmed.matches("^[A-Z][A-Z0-9_]*\\s*\\([^)]*\\).*")) {
                removedEnumLines.add(line);
            }
        }

        // 如果有枚举值的变更，创建一个汇总的变更记录
        if (!addedEnumLines.isEmpty() || !removedEnumLines.isEmpty()) {
            ChangeInfo change = new ChangeInfo();
            change.setEntity_type("Field");  // 枚举常量本质上是 Field
            change.setClassName(className);
            change.setFieldName("ENUM_VALUES");  // 使用特殊名称表示枚举值变更
            change.setChangeType("MODIFY");
            change.setFilePath(filePath);
            change.setAddedLines(addedEnumLines);
            change.setRemovedLines(removedEnumLines);

            changes.add(change);

            System.out.println("    [MODIFY] 枚举值: " + addedEnumLines.size() + " 个新增, " +
                             removedEnumLines.size() + " 个删除");
        }

        return changes;
    }

    /**
     * 提取类/接口变更
     */
    private List<ChangeInfo> extractClassChanges(DiffHunk hunk, String className, String filePath) {
        List<ChangeInfo> changes = new ArrayList<>();

        List<String> addedClassLines = new ArrayList<>();
        List<String> removedClassLines = new ArrayList<>();

        // 检测类/接口/枚举声明的变更
        for (String line : hunk.getAddedLines()) {
            String trimmed = line.trim();
            if (isClassDeclaration(trimmed)) {
                addedClassLines.add(line);
            }
        }

        for (String line : hunk.getRemovedLines()) {
            String trimmed = line.trim();
            if (isClassDeclaration(trimmed)) {
                removedClassLines.add(line);
            }
        }

        // 如果有类声明的变更
        if (!addedClassLines.isEmpty() || !removedClassLines.isEmpty()) {
            String changeType = "MODIFY";
            if (addedClassLines.isEmpty()) {
                changeType = "DELETE";
            } else if (removedClassLines.isEmpty()) {
                changeType = "ADD";
            }

            ChangeInfo change = new ChangeInfo();
            change.setEntity_type("ClassOrInterface");
            change.setClassName(className);
            change.setChangeType(changeType);
            change.setFilePath(filePath);
            change.setAddedLines(addedClassLines);
            change.setRemovedLines(removedClassLines);

            changes.add(change);

            System.out.println("    [" + changeType + "] 类/接口: " + className);
            System.out.println("          实体ID: " + change.toEntityId());
        }

        return changes;
    }

    /**
     * 判断是否为类/接口/枚举声明
     */
    private boolean isClassDeclaration(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        // 跳过注释
        if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
            return false;
        }

        // 匹配类、接口、枚举声明
        // public class ClassName
        // public interface InterfaceName
        // public enum EnumName
        // class ClassName implements/extends
        return line.matches(".*\\b(class|interface|enum)\\s+\\w+.*\\{?");
    }

    /**
     * 规范化文件路径（转换为相对路径）
     */
    private String normalizeFilePath(String filePath, String projectRoot) {
        if (filePath == null) {
            return null;
        }

        // 如果已经是相对路径，直接返回
        if (!filePath.startsWith("/") && !filePath.contains(":\\")) {
            return filePath;
        }

        // 如果是绝对路径，尝试转换为相对路径
        if (projectRoot != null) {
            String normalizedRoot = projectRoot.replace("\\", "/");
            String normalizedPath = filePath.replace("\\", "/");

            if (!normalizedRoot.endsWith("/")) {
                normalizedRoot += "/";
            }

            if (normalizedPath.startsWith(normalizedRoot)) {
                return normalizedPath.substring(normalizedRoot.length());
            }
        }

        return filePath;
    }
}
