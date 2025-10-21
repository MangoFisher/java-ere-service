package com.java.extractor.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff块模型
 * 表示一个文件中的变更块
 */
public class DiffHunk {
    private String filePath;           // 文件路径
    private String className;          // 类名（从文件路径提取）
    private List<String> addedLines;   // 新增的行
    private List<String> removedLines; // 删除的行
    private int startLine;             // 开始行号
    private int lineCount;             // 行数
    
    public DiffHunk() {
        this.addedLines = new ArrayList<>();
        this.removedLines = new ArrayList<>();
    }
    
    public DiffHunk(String filePath) {
        this();
        this.filePath = filePath;
        this.className = extractClassName(filePath);
    }
    
    /**
     * 从文件路径提取类名
     * 例如: src/main/java/com/example/Test.java -> Test
     */
    private String extractClassName(String filePath) {
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
    
    public void addAddedLine(String line) {
        this.addedLines.add(line);
    }
    
    public void addRemovedLine(String line) {
        this.removedLines.add(line);
    }
    
    // Getters and Setters
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.className = extractClassName(filePath);
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public List<String> getAddedLines() {
        return addedLines;
    }
    
    public void setAddedLines(List<String> addedLines) {
        this.addedLines = addedLines;
    }
    
    public List<String> getRemovedLines() {
        return removedLines;
    }
    
    public void setRemovedLines(List<String> removedLines) {
        this.removedLines = removedLines;
    }
    
    public int getStartLine() {
        return startLine;
    }
    
    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }
    
    public int getLineCount() {
        return lineCount;
    }
    
    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
    
    @Override
    public String toString() {
        return "DiffHunk{" +
                "filePath='" + filePath + '\'' +
                ", className='" + className + '\'' +
                ", added=" + addedLines.size() +
                ", removed=" + removedLines.size() +
                '}';
    }
}
