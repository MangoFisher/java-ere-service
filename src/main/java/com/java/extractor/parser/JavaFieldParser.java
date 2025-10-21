package com.java.extractor.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java字段解析器
 * 从字段声明字符串中提取字段信息
 * 返回Map格式: {name: "字段名", type: "类型", isConstant: boolean, isEnumMember: boolean}
 */
public class JavaFieldParser {
    
    // 枚举常量模式: ENUM_CONSTANT(args...),
    private static final Pattern ENUM_CONSTANT_PATTERN = 
        Pattern.compile("^([A-Z][A-Z0-9_]*)\\s*\\([^)]*\\)\\s*[,;]");
    
    // 常规字段模式: [modifiers] Type fieldName [= value];
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "(?:public|private|protected|static|final|volatile|transient)?\\s+" +
        "([\\w<>,\\s\\[\\]]+?)\\s+(\\w+)\\s*[=;]"
    );
    
    /**
     * 解析字段声明
     * @return Map with keys: name, type, isConstant, isEnumMember
     */
    public static Map<String, Object> parseFieldDeclaration(String fieldLine) {
        if (fieldLine == null || fieldLine.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = fieldLine.trim();
        
        // 跳过注释
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
            return null;
        }
        
        // 排除类、接口、枚举声明
        if (trimmed.matches(".*\\b(class|interface|enum)\\s+\\w+.*")) {
            return null;
        }
        
        // 排除方法声明（有括号且不是以;结尾的）
        if (trimmed.contains("(") && trimmed.contains(")") && !trimmed.endsWith(";")) {
            // 但枚举常量可能有括号
            if (!ENUM_CONSTANT_PATTERN.matcher(trimmed).find()) {
                return null;
            }
        }
        
        // 模式1: 枚举常量
        Matcher enumMatcher = ENUM_CONSTANT_PATTERN.matcher(trimmed);
        if (enumMatcher.find()) {
            Map<String, Object> field = new HashMap<>();
            field.put("name", enumMatcher.group(1));
            field.put("type", "EnumConstant");
            field.put("isConstant", true);
            field.put("isEnumMember", true);
            return field;
        }
        
        // 模式2: 常规字段
        Matcher fieldMatcher = FIELD_PATTERN.matcher(trimmed);
        if (fieldMatcher.find()) {
            String fieldType = fieldMatcher.group(1).trim();
            String fieldName = fieldMatcher.group(2);
            
            // 检查是否是常量
            boolean isConstant = (trimmed.contains("static") && trimmed.contains("final"))
                              || (fieldName.matches("[A-Z][A-Z0-9_]*"));
            
            Map<String, Object> field = new HashMap<>();
            field.put("name", fieldName);
            field.put("type", fieldType);
            field.put("isConstant", isConstant);
            field.put("isEnumMember", false);
            return field;
        }
        
        return null;
    }
    
    /**
     * 判断是否为字段声明
     */
    public static boolean isFieldDeclaration(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = line.trim();
        
        // 跳过注释
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
            return false;
        }
        
        // 排除类、接口、枚举声明
        if (trimmed.matches(".*\\b(class|interface|enum)\\s+\\w+.*")) {
            return false;
        }
        
        // 枚举常量或常规字段
        return ENUM_CONSTANT_PATTERN.matcher(trimmed).find() 
            || FIELD_PATTERN.matcher(trimmed).find();
    }
}
