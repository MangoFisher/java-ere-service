package com.java.extractor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diff工具类
 * 提供diff解析相关的工具方法
 */
public class DiffUtil {
    
    // diff块头部模式: @@ -old_line,old_count +new_line,new_count @@
    private static final Pattern HUNK_HEADER_PATTERN = 
        Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");
    
    /**
     * 从文件路径提取类名
     * 例如: src/main/java/com/example/Test.java -> Test
     */
    public static String extractClassNameFromPath(String filePath) {
        if (filePath == null) {
            return null;
        }
        
        // 移除.java后缀
        String name = filePath.replaceAll("\\.java$", "");
        
        // 提取最后一个路径分隔符后的部分
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        
        return name;
    }
    
    /**
     * 从文件路径提取包名
     * 例如: src/main/java/com/example/Test.java -> com.example
     */
    public static String extractPackageFromPath(String filePath) {
        if (filePath == null) {
            return null;
        }
        
        // 查找 src/main/java/ 或 src/test/java/ 之后的路径
        String[] patterns = {"src/main/java/", "src/test/java/"};
        
        for (String pattern : patterns) {
            int index = filePath.indexOf(pattern);
            if (index >= 0) {
                String afterSrc = filePath.substring(index + pattern.length());
                // 移除文件名
                int lastSlash = Math.max(afterSrc.lastIndexOf('/'), afterSrc.lastIndexOf('\\'));
                if (lastSlash > 0) {
                    String packagePath = afterSrc.substring(0, lastSlash);
                    return packagePath.replace('/', '.').replace('\\', '.');
                }
            }
        }
        
        return null;
    }
    
    /**
     * 解析hunk头部信息
     * @param hunkHeader hunk头部行，例如: @@ -10,5 +12,7 @@
     * @return [oldStartLine, oldLineCount, newStartLine, newLineCount]
     */
    public static int[] parseHunkHeader(String hunkHeader) {
        Matcher m = HUNK_HEADER_PATTERN.matcher(hunkHeader);
        if (m.find()) {
            int oldStart = Integer.parseInt(m.group(1));
            int oldCount = m.group(2) != null ? Integer.parseInt(m.group(2)) : 1;
            int newStart = Integer.parseInt(m.group(3));
            int newCount = m.group(4) != null ? Integer.parseInt(m.group(4)) : 1;
            
            return new int[]{oldStart, oldCount, newStart, newCount};
        }
        return null;
    }
    
    /**
     * 判断是否为diff文件路径行
     * 例如: +++ b/src/main/java/Test.java
     */
    public static boolean isDiffFilePath(String line) {
        return line != null && (line.startsWith("+++") || line.startsWith("---"));
    }
    
    /**
     * 判断是否为hunk头部
     * 例如: @@ -10,5 +12,7 @@
     */
    public static boolean isHunkHeader(String line) {
        return line != null && line.startsWith("@@");
    }
    
    /**
     * 判断是否为新增行
     */
    public static boolean isAddedLine(String line) {
        return line != null && line.startsWith("+") && !line.startsWith("+++");
    }
    
    /**
     * 判断是否为删除行
     */
    public static boolean isRemovedLine(String line) {
        return line != null && line.startsWith("-") && !line.startsWith("---");
    }
    
    /**
     * 判断是否为上下文行（未修改的行）
     */
    public static boolean isContextLine(String line) {
        return line != null && !isAddedLine(line) && !isRemovedLine(line) 
            && !isDiffFilePath(line) && !isHunkHeader(line) && !line.startsWith("diff ");
    }
    
    /**
     * 移除行首的diff标记（+或-）
     */
    public static String stripDiffMarker(String line) {
        if (line == null) {
            return null;
        }
        
        if (isAddedLine(line) || isRemovedLine(line)) {
            return line.substring(1);
        }
        
        return line;
    }
    
    /**
     * 判断是否为Java文件
     */
    public static boolean isJavaFile(String filePath) {
        return filePath != null && filePath.endsWith(".java");
    }
    
    /**
     * 合并连续的代码行
     */
    public static List<String> mergeLines(List<String> lines) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // 如果是注释或空行，直接添加
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                if (current.length() > 0) {
                    merged.add(current.toString());
                    current = new StringBuilder();
                }
                merged.add(line);
                continue;
            }
            
            // 累积代码行
            if (current.length() > 0) {
                current.append(" ");
            }
            current.append(trimmed);
            
            // 如果是完整语句（以;或{或}结尾），添加到列表
            if (trimmed.endsWith(";") || trimmed.endsWith("{") || trimmed.endsWith("}")) {
                merged.add(current.toString());
                current = new StringBuilder();
            }
        }
        
        // 添加剩余内容
        if (current.length() > 0) {
            merged.add(current.toString());
        }
        
        return merged;
    }
}
