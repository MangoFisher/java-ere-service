package com.java.extractor.model;

/**
 * Diff变更模型
 * 表示从git diff中提取的原始变更信息
 */
public class DiffChange {
    private String changeType;    // ADD, MODIFY, DELETE
    private String content;        // 变更内容（代码行）
    private CodeRange codeRange;   // 代码范围
    
    public DiffChange() {
    }
    
    public DiffChange(String changeType, String content) {
        this.changeType = changeType;
        this.content = content;
    }
    
    public DiffChange(String changeType, String content, CodeRange codeRange) {
        this.changeType = changeType;
        this.content = content;
        this.codeRange = codeRange;
    }
    
    // Getters and Setters
    
    public String getChangeType() {
        return changeType;
    }
    
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public CodeRange getCodeRange() {
        return codeRange;
    }
    
    public void setCodeRange(CodeRange codeRange) {
        this.codeRange = codeRange;
    }
    
    @Override
    public String toString() {
        return "DiffChange{" +
                "changeType='" + changeType + '\'' +
                ", content='" + content + '\'' +
                ", codeRange=" + codeRange +
                '}';
    }
}
