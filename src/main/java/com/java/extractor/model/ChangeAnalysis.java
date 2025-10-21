package com.java.extractor.model;

import java.util.List;

/**
 * 变更分析结果
 * 包含单个变更及其上下游实体
 */
public class ChangeAnalysis {
    private ChangeInfo change;              // 变更信息
    private EntityInfo changeEntity;        // 变更实体本身
    private List<EntityInfo> upstream;      // 上游实体
    private List<EntityInfo> downstream;    // 下游实体
    
    public ChangeAnalysis() {
    }
    
    public ChangeAnalysis(ChangeInfo change) {
        this.change = change;
    }
    
    // Getters and Setters
    
    public ChangeInfo getChange() {
        return change;
    }
    
    public void setChange(ChangeInfo change) {
        this.change = change;
    }
    
    public EntityInfo getChangeEntity() {
        return changeEntity;
    }
    
    public void setChangeEntity(EntityInfo changeEntity) {
        this.changeEntity = changeEntity;
    }
    
    public List<EntityInfo> getUpstream() {
        return upstream;
    }
    
    public void setUpstream(List<EntityInfo> upstream) {
        this.upstream = upstream;
    }
    
    public List<EntityInfo> getDownstream() {
        return downstream;
    }
    
    public void setDownstream(List<EntityInfo> downstream) {
        this.downstream = downstream;
    }
    
    @Override
    public String toString() {
        return "ChangeAnalysis{" +
                "change=" + change +
                ", changeEntity=" + changeEntity +
                ", upstreamCount=" + (upstream != null ? upstream.size() : 0) +
                ", downstreamCount=" + (downstream != null ? downstream.size() : 0) +
                '}';
    }
}
