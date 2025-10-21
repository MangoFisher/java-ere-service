package com.java.extractor;

import com.github.javaparser.Range;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Optional;

/**
 * 代码提取结果
 */
public class ExtractResult {
    private final boolean success;
    private final String code;
    private final String errorMessage;
    private final Integer startLine;
    private final Integer endLine;
    private final CodeLocation location;
    
    private ExtractResult(boolean success, String code, String errorMessage,
                         Integer startLine, Integer endLine, CodeLocation location) {
        this.success = success;
        this.code = code;
        this.errorMessage = errorMessage;
        this.startLine = startLine;
        this.endLine = endLine;
        this.location = location;
    }
    
    /**
     * 成功提取
     */
    public static ExtractResult success(String code, Range range, CodeLocation location) {
        Integer startLine = null;
        Integer endLine = null;
        if (range != null) {
            startLine = range.begin.line;
            endLine = range.end.line;
        }
        return new ExtractResult(true, code, null, startLine, endLine, location);
    }
    
    /**
     * 未找到方法
     */
    public static ExtractResult notFound(CodeLocation location) {
        return new ExtractResult(false, null, 
            "Method not found: " + location.toString(), null, null, location);
    }
    
    /**
     * 提取失败
     */
    public static ExtractResult failure(String errorMessage, CodeLocation location) {
        return new ExtractResult(false, null, errorMessage, null, null, location);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Integer getStartLine() {
        return startLine;
    }
    
    public Integer getEndLine() {
        return endLine;
    }
    
    public CodeLocation getLocation() {
        return location;
    }
    
    /**
     * 转换为 JSON（供 Python 调用）
     */
    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("ExtractResult[success, lines=%d-%d, code=%d chars]",
                startLine, endLine, code != null ? code.length() : 0);
        } else {
            return String.format("ExtractResult[failed, error=%s]", errorMessage);
        }
    }
}
