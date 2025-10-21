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
        
        // 提取字段变更
        changes.addAll(extractFieldChanges(hunk, className, filePath));
        
        // 提取方法变更
        changes.addAll(extractMethodChanges(hunk, className, filePath));
        
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
        
        for (String line : hunk.getAddedLines()) {
            if (JavaFieldParser.isFieldDeclaration(line)) {
                Map<String, Object> field = JavaFieldParser.parseFieldDeclaration(line);
                if (field != null) {
                    String fieldName = (String) field.get("name");
                    addedFields.put(fieldName, field);
                }
            }
        }
        
        for (String line : hunk.getRemovedLines()) {
            if (JavaFieldParser.isFieldDeclaration(line)) {
                Map<String, Object> field = JavaFieldParser.parseFieldDeclaration(line);
                if (field != null) {
                    String fieldName = (String) field.get("name");
                    removedFields.put(fieldName, field);
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
            change.setType("field");
            change.setClassName(className);
            change.setFieldName(fieldName);
            change.setChangeType(changeType);
            change.setFilePath(filePath);
            
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
        
        for (String line : hunk.getAddedLines()) {
            if (JavaMethodParser.isMethodDeclaration(line)) {
                Map<String, String> method = JavaMethodParser.parseMethodSignature(line);
                if (method != null) {
                    String name = method.get("name");
                    String sig = method.get("signature");
                    String key = name + "(" + (sig != null ? sig : "") + ")";  // methodName(signature)
                    addedMethods.put(key, method);
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
            change.setType("method");
            change.setClassName(className);
            change.setMethodName(method.get("name"));
            change.setMethodSignature(method.get("signature"));
            change.setChangeType(changeType);
            change.setFilePath(filePath);
            
            changes.add(change);
            
            // 输出日志
            System.out.println("    [" + changeType + "] 方法: " + signature);
            System.out.println("          实体ID: " + change.toEntityId());
        }
        
        return changes;
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
