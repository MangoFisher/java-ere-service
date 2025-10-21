package com.java.extractor.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Java方法解析器
 * 从方法声明字符串中提取方法签名
 * 返回Map格式: {name: "方法名", signature: "参数签名", returnType: "返回类型"}
 */
public class JavaMethodParser {
    
    // 方法声明正则表达式
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(?:public|private|protected|static|final|synchronized|native|abstract|\\s)+" +
        "\\s+([\\w<>,\\[\\]\\s]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)"
    );
    
    /**
     * 解析方法签名（优先使用JavaParser，失败则降级到正则）
     * @return Map with keys: name, signature, returnType
     */
    public static Map<String, String> parseMethodSignature(String methodLine) {
        // 尝试使用JavaParser
        Map<String, String> sig = parseWithJavaParser(methodLine);
        if (sig != null) {
            return sig;
        }
        
        // 降级到正则表达式
        return parseWithRegex(methodLine);
    }
    
    /**
     * 使用JavaParser解析（精确）
     */
    private static Map<String, String> parseWithJavaParser(String methodLine) {
        try {
            // 将单行包装成完整的类
            String code = "class Temp { " + methodLine + " {} }";
            CompilationUnit cu = StaticJavaParser.parse(code);
            
            Optional<MethodDeclaration> method = cu.findFirst(MethodDeclaration.class);
            if (method.isPresent()) {
                MethodDeclaration md = method.get();
                String name = md.getNameAsString();
                String returnType = md.getType().asString();
                String signature = md.getParameters().stream()
                    .map(p -> p.getType().asString())
                    .collect(Collectors.joining(","));
                
                Map<String, String> result = new HashMap<>();
                result.put("name", name);
                result.put("signature", signature);
                result.put("returnType", returnType);
                return result;
            }
        } catch (Exception e) {
            // 解析失败，返回null让正则接管
        }
        return null;
    }
    
    /**
     * 使用正则表达式解析（容错）
     */
    private static Map<String, String> parseWithRegex(String methodLine) {
        Matcher m = METHOD_PATTERN.matcher(methodLine);
        if (m.find()) {
            String returnType = m.group(1).trim();
            String methodName = m.group(2);
            String params = m.group(3);
            
            List<String> paramTypes = parseParameters(params);
            String signature = String.join(",", paramTypes);
            
            Map<String, String> result = new HashMap<>();
            result.put("name", methodName);
            result.put("signature", signature);
            result.put("returnType", returnType);
            return result;
        }
        return null;
    }
    
    /**
     * 解析参数列表
     * 支持泛型、数组、varargs等
     */
    private static List<String> parseParameters(String params) {
        List<String> paramTypes = new ArrayList<>();
        
        if (params == null || params.trim().isEmpty()) {
            return paramTypes;
        }
        
        // 按逗号分割，但需要考虑泛型中的逗号
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        
        for (char c : (params + ",").toCharArray()) {
            if (c == '<') {
                bracketDepth++;
                current.append(c);
            } else if (c == '>') {
                bracketDepth--;
                current.append(c);
            } else if (c == ',' && bracketDepth == 0) {
                // 真正的参数分隔符
                String param = current.toString().trim();
                if (!param.isEmpty()) {
                    String paramType = extractParamType(param);
                    if (paramType != null && !paramType.isEmpty()) {
                        paramTypes.add(paramType);
                    }
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        return paramTypes;
    }
    
    /**
     * 从参数声明中提取类型
     * 例如: "@Nullable String name" -> "String"
     *       "List<String> items" -> "List<String>"
     *       "String... args" -> "String..."
     */
    private static String extractParamType(String param) {
        if (param == null || param.isEmpty()) {
            return null;
        }
        
        // 移除注解 @Xxx
        param = param.replaceAll("@\\w+\\s+", "");
        
        // 移除final修饰符
        param = param.replaceAll("\\bfinal\\s+", "");
        
        // 匹配类型部分（支持泛型、数组、varargs）
        // 格式: Type<Generic> variableName 或 Type[] variableName 或 Type... variableName
        Pattern typePattern = Pattern.compile("^([\\w<>,\\s\\[\\]\\.]+?)\\s+\\w+$");
        Matcher m = typePattern.matcher(param);
        
        if (m.find()) {
            String type = m.group(1).trim();
            // 移除类型中的多余空格
            type = type.replaceAll("\\s+", "");
            return type;
        }
        
        // 如果只有类型没有变量名（不常见）
        param = param.replaceAll("\\s+", "");
        return param.isEmpty() ? null : param;
    }
    
    /**
     * 判断是否为方法声明
     */
    public static boolean isMethodDeclaration(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // 跳过注释
        String trimmed = line.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
            return false;
        }
        
        // 排除类、接口、枚举声明
        if (trimmed.matches(".*\\b(class|interface|enum)\\s+\\w+.*")) {
            return false;
        }
        
        // 必须包含括号（方法特征）
        if (!trimmed.contains("(") || !trimmed.contains(")")) {
            return false;
        }
        
        // 尝试匹配方法模式
        return METHOD_PATTERN.matcher(trimmed).find();
    }
}
