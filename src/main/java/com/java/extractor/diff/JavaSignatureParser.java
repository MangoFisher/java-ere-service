package com.java.extractor.diff;

import com.java.extractor.parser.JavaMethodParser;
import com.java.extractor.parser.JavaFieldParser;

import java.util.Map;

/**
 * Java签名解析器（独立实现）
 * 整合方法和字段解析，提供统一接口
 */
public class JavaSignatureParser {
    
    /**
     * 解析方法签名
     * @return Map with keys: name, signature, returnType
     */
    public static Map<String, String> parseMethod(String line) {
        return JavaMethodParser.parseMethodSignature(line);
    }
    
    /**
     * 解析字段声明
     * @return Map with keys: name, type, isConstant, isEnumMember
     */
    public static Map<String, Object> parseField(String line) {
        return JavaFieldParser.parseFieldDeclaration(line);
    }
    
    /**
     * 判断是否为方法声明
     */
    public static boolean isMethod(String line) {
        return JavaMethodParser.isMethodDeclaration(line);
    }
    
    /**
     * 判断是否为字段声明
     */
    public static boolean isField(String line) {
        return JavaFieldParser.isFieldDeclaration(line);
    }
    
    /**
     * 判断是否为类声明
     */
    public static boolean isClass(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = line.trim();
        
        // 跳过注释
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
            return false;
        }
        
        // 匹配类、接口、枚举声明
        return trimmed.matches(".*\\b(class|interface|enum)\\s+\\w+.*");
    }
    
    /**
     * 从类声明中提取类名
     * 例如: "public class MyClass extends Base {" -> "MyClass"
     */
    public static String extractClassName(String line) {
        if (!isClass(line)) {
            return null;
        }
        
        String trimmed = line.trim();
        
        // 匹配 class/interface/enum 关键字后的类名
        String[] keywords = {"class", "interface", "enum"};
        for (String keyword : keywords) {
            int keywordIndex = trimmed.indexOf(keyword);
            if (keywordIndex >= 0) {
                String afterKeyword = trimmed.substring(keywordIndex + keyword.length()).trim();
                // 提取第一个单词（类名）
                int spaceIndex = afterKeyword.indexOf(' ');
                int braceIndex = afterKeyword.indexOf('{');
                int ltIndex = afterKeyword.indexOf('<');
                
                int endIndex = afterKeyword.length();
                if (spaceIndex > 0 && spaceIndex < endIndex) endIndex = spaceIndex;
                if (braceIndex > 0 && braceIndex < endIndex) endIndex = braceIndex;
                if (ltIndex > 0 && ltIndex < endIndex) endIndex = ltIndex;
                
                return afterKeyword.substring(0, endIndex).trim();
            }
        }
        
        return null;
    }
}
