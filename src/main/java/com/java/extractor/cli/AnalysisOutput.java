package com.java.extractor.cli;

import com.java.extractor.model.ChangeAnalysis;

import java.util.List;

/**
 * 分析输出 (简化版，仅用于CLI)
 */
public class AnalysisOutput {
    private List<ChangeAnalysis> changes;
    
    public AnalysisOutput() {
    }
    
    public AnalysisOutput(List<ChangeAnalysis> changes) {
        this.changes = changes;
    }
    
    public List<ChangeAnalysis> getChanges() {
        return changes;
    }
    
    public void setChanges(List<ChangeAnalysis> changes) {
        this.changes = changes;
    }
}
