package com.java.extractor.cli;

import com.java.extractor.model.ChangeInfo;
import com.java.extractor.model.QueryConfig;

import java.util.List;

/**
 * 分析输入 (简化版，仅用于CLI)
 */
public class AnalysisInput {
    private String projectRoot;
    private Neo4jConfig neo4jConfig;
    private QueryConfig queryConfig;
    private List<ChangeInfo> changes;
    
    public static class Neo4jConfig {
        private String uri;
        private String user;
        private String password;
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public String getProjectRoot() { return projectRoot; }
    public void setProjectRoot(String projectRoot) { this.projectRoot = projectRoot; }
    
    public Neo4jConfig getNeo4jConfig() { return neo4jConfig; }
    public void setNeo4jConfig(Neo4jConfig neo4jConfig) { this.neo4jConfig = neo4jConfig; }
    
    public QueryConfig getQueryConfig() { return queryConfig; }
    public void setQueryConfig(QueryConfig queryConfig) { this.queryConfig = queryConfig; }
    
    public List<ChangeInfo> getChanges() { return changes; }
    public void setChanges(List<ChangeInfo> changes) { this.changes = changes; }
}
