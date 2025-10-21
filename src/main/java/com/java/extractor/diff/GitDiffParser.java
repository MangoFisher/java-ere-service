package com.java.extractor.diff;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Git Diff解析器
 * 解析git diff格式的文本文件
 */
public class GitDiffParser {
    
    /**
     * 解析git diff文件
     * 
     * @param diffFilePath diff文件路径
     * @return Diff块列表
     */
    public List<DiffHunk> parse(String diffFilePath) throws IOException {
        List<DiffHunk> hunks = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(diffFilePath))) {
            String line;
            DiffHunk currentHunk = null;
            String currentFilePath = null;
            
            while ((line = reader.readLine()) != null) {
                // diff --git a/path/to/file b/path/to/file
                if (line.startsWith("diff --git")) {
                    // 开始新文件的diff
                    if (currentHunk != null && 
                        (!currentHunk.getAddedLines().isEmpty() || 
                         !currentHunk.getRemovedLines().isEmpty())) {
                        hunks.add(currentHunk);
                    }
                    currentHunk = null;
                }
                // +++ b/path/to/file
                else if (line.startsWith("+++")) {
                    // 提取文件路径
                    currentFilePath = extractFilePath(line);
                    
                    // 只处理Java文件
                    if (currentFilePath != null && currentFilePath.endsWith(".java")) {
                        currentHunk = new DiffHunk(currentFilePath);
                    }
                }
                // @@ -old_line,old_count +new_line,new_count @@ optional context
                else if (line.startsWith("@@")) {
                    // 新的变更块（hunk header）
                    if (currentHunk != null) {
                        // 如果当前hunk已有内容，保存它
                        if (!currentHunk.getAddedLines().isEmpty() || 
                            !currentHunk.getRemovedLines().isEmpty()) {
                            hunks.add(currentHunk);
                            // 创建新hunk但保持相同的文件
                            currentHunk = new DiffHunk(currentFilePath);
                        }
                    }
                }
                // + added line
                else if (line.startsWith("+") && !line.startsWith("+++")) {
                    if (currentHunk != null) {
                        // 移除开头的+号和空格
                        String content = line.substring(1);
                        currentHunk.addAddedLine(content);
                    }
                }
                // - removed line
                else if (line.startsWith("-") && !line.startsWith("---")) {
                    if (currentHunk != null) {
                        // 移除开头的-号和空格
                        String content = line.substring(1);
                        currentHunk.addRemovedLine(content);
                    }
                }
                // context line (starts with space)
                else {
                    // 上下文行，暂时忽略
                }
            }
            
            // 添加最后一个hunk
            if (currentHunk != null && (!currentHunk.getAddedLines().isEmpty() || 
                !currentHunk.getRemovedLines().isEmpty())) {
                hunks.add(currentHunk);
            }
        }
        
        return hunks;
    }
    
    /**
     * 从diff行中提取文件路径
     * 例如: "+++ b/src/main/java/Example.java" -> "src/main/java/Example.java"
     */
    private String extractFilePath(String line) {
        // +++ b/path 或 +++ /dev/null
        if (line.contains("/dev/null")) {
            return null;  // 文件被删除
        }
        
        // 提取 b/ 之后的路径
        int bIndex = line.indexOf("b/");
        if (bIndex >= 0) {
            return line.substring(bIndex + 2).trim();
        }
        
        // 降级：去掉+++ 和空格
        return line.replaceFirst("^\\+\\+\\+\\s+", "").trim();
    }
}
