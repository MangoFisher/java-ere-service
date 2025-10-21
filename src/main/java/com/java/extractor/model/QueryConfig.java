package com.java.extractor.model;

/**
 * 查询配置
 * 控制上下游查询的行为
 */
public class QueryConfig {
    private int depth = 1;                      // 查询深度
    private boolean includeUpstream = true;     // 包含上游
    private boolean includeDownstream = true;   // 包含下游
    private boolean includeSourceCode = true;   // 包含源码
    
    public QueryConfig() {
    }
    
    public QueryConfig(int depth, boolean includeUpstream, boolean includeDownstream, boolean includeSourceCode) {
        this.depth = depth;
        this.includeUpstream = includeUpstream;
        this.includeDownstream = includeDownstream;
        this.includeSourceCode = includeSourceCode;
    }
    
    // Getters and Setters
    
    public int getDepth() {
        return depth;
    }
    
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    public boolean isIncludeUpstream() {
        return includeUpstream;
    }
    
    public void setIncludeUpstream(boolean includeUpstream) {
        this.includeUpstream = includeUpstream;
    }
    
    public boolean isIncludeDownstream() {
        return includeDownstream;
    }
    
    public void setIncludeDownstream(boolean includeDownstream) {
        this.includeDownstream = includeDownstream;
    }
    
    public boolean isIncludeSourceCode() {
        return includeSourceCode;
    }
    
    public void setIncludeSourceCode(boolean includeSourceCode) {
        this.includeSourceCode = includeSourceCode;
    }
    
    @Override
    public String toString() {
        return "QueryConfig{" +
                "depth=" + depth +
                ", includeUpstream=" + includeUpstream +
                ", includeDownstream=" + includeDownstream +
                ", includeSourceCode=" + includeSourceCode +
                '}';
    }
}
