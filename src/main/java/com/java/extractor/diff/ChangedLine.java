package com.java.extractor.diff;

/**
 * 变更行模型
 * 表示一个变更行及其行号信息
 */
public class ChangedLine {
    private String content;     // 行内容
    private int lineNumber;     // 行号（在新文件中的行号，对于删除的行则是在旧文件中的行号）
    private boolean isAdded;    // 是否是新增行

    public ChangedLine() {
    }

    public ChangedLine(String content, int lineNumber, boolean isAdded) {
        this.content = content;
        this.lineNumber = lineNumber;
        this.isAdded = isAdded;
    }

    /**
     * 创建一个新增行
     */
    public static ChangedLine added(String content, int lineNumber) {
        return new ChangedLine(content, lineNumber, true);
    }

    /**
     * 创建一个删除行
     */
    public static ChangedLine removed(String content, int lineNumber) {
        return new ChangedLine(content, lineNumber, false);
    }

    // Getters and Setters

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isAdded() {
        return isAdded;
    }

    public void setAdded(boolean added) {
        isAdded = added;
    }

    @Override
    public String toString() {
        return "ChangedLine{" +
                "content='" + content + '\'' +
                ", lineNumber=" + lineNumber +
                ", isAdded=" + isAdded +
                '}';
    }
}
