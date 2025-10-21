package com.java.extractor.model;

/**
 * 代码范围模型
 * 表示代码在文件中的行号范围
 */
public class CodeRange {
    private int startLine;    // 起始行号
    private int endLine;      // 结束行号
    private String filePath;  // 文件路径
    
    public CodeRange() {
    }
    
    public CodeRange(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }
    
    public CodeRange(int startLine, int endLine, String filePath) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.filePath = filePath;
    }
    
    /**
     * 获取行数
     */
    public int getLineCount() {
        return endLine - startLine + 1;
    }
    
    /**
     * 判断指定行是否在范围内
     */
    public boolean contains(int lineNumber) {
        return lineNumber >= startLine && lineNumber <= endLine;
    }
    
    // Getters and Setters
    
    public int getStartLine() {
        return startLine;
    }
    
    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }
    
    public int getEndLine() {
        return endLine;
    }
    
    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public String toString() {
        return "CodeRange{" +
                "startLine=" + startLine +
                ", endLine=" + endLine +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
