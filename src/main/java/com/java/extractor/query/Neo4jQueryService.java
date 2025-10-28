package com.java.extractor.query;

import com.java.extractor.model.EntityInfo;
import org.neo4j.driver.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j查询服务
 */
public class Neo4jQueryService implements AutoCloseable {

    private final Driver driver;

    public Neo4jQueryService(String uri, String user, String password) {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    /**
     * 查询上游（谁调用/访问/实现了我）
     * 深度1：直接上游
     */
    public List<EntityInfo> queryUpstream(String entityId, int depth) {
        String cypher = buildUpstreamQuery(depth);
        return executeQuery(cypher, entityId);
    }

    /**
     * 查询下游（我调用/访问/实现了谁）
     * 深度1：直接下游
     */
    public List<EntityInfo> queryDownstream(String entityId, int depth) {
        String cypher = buildDownstreamQuery(depth);
        return executeQuery(cypher, entityId);
    }

    /**
     * 查询实体本身的信息
     */
    public EntityInfo queryEntity(String entityId) {
        String cypher = "MATCH (n) WHERE n.id = $entityId RETURN n";

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                java.util.HashMap<String, Object> params = new java.util.HashMap<String, Object>();
                params.put("entityId", entityId);
                Result result = tx.run(cypher, params);

                if (result.hasNext()) {
                    org.neo4j.driver.Record record = result.next();
                    return recordToEntity(record.get("n").asNode(), null);
                }
                return null;
            });
        } catch (Exception e) {
            System.err.println("查询实体失败: " + entityId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 构建上游查询Cypher（深度1）
     */
    private String buildUpstreamQuery(int depth) {
        if (depth == 1) {
            // 查询所有指向我的关系，只查询代码实体（Method/Field/Class）
            return "MATCH (upstream)-[r]->(target) " +
                   "WHERE target.id = $entityId " +
                   "AND (upstream.id STARTS WITH 'method_' OR upstream.id STARTS WITH 'field_' OR upstream.id STARTS WITH 'class_' OR upstream.id STARTS WITH 'iface_') " +
                   "RETURN upstream, type(r) as relType";
        } else {
            // 暂不支持多层深度
            throw new UnsupportedOperationException("暂不支持深度 > 1 的查询");
        }
    }

    /**
     * 构建下游查询Cypher（深度1）
     */
    private String buildDownstreamQuery(int depth) {
        if (depth == 1) {
            // 查询我指向的所有关系，只查询代码实体（Method/Field/Class）
            return "MATCH (source)-[r]->(downstream) " +
                   "WHERE source.id = $entityId " +
                   "AND (downstream.id STARTS WITH 'method_' OR downstream.id STARTS WITH 'field_' OR downstream.id STARTS WITH 'class_' OR downstream.id STARTS WITH 'iface_') " +
                   "RETURN downstream, type(r) as relType";
        } else {
            // 暂不支持多层深度
            throw new UnsupportedOperationException("暂不支持深度 > 1 的查询");
        }
    }

    /**
     * 执行查询
     */
    private List<EntityInfo> executeQuery(String cypher, String entityId) {
        List<EntityInfo> results = new ArrayList<>();

        try (Session session = driver.session()) {
            session.executeRead(tx -> {
                java.util.HashMap<String, Object> params = new java.util.HashMap<String, Object>();
                params.put("entityId", entityId);
                Result result = tx.run(cypher, params);

                while (result.hasNext()) {
                    org.neo4j.driver.Record record = result.next();

                    // 获取节点（upstream或downstream）
                    Value nodeValue = record.get(0);
                    if (!nodeValue.isNull() && nodeValue.type().equals(org.neo4j.driver.internal.types.InternalTypeSystem.TYPE_SYSTEM.NODE())) {
                        org.neo4j.driver.types.Node node = nodeValue.asNode();

                        // 获取关系类型
                        String relType = record.get("relType").asString();

                        EntityInfo entity = recordToEntity(node, relType);
                        if (entity != null) {
                            results.add(entity);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            System.err.println("查询失败: " + entityId + " - " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * 将Neo4j节点转换为EntityInfo
     */
    private EntityInfo recordToEntity(org.neo4j.driver.types.Node node, String relationshipType) {
        try {
            EntityInfo entity = new EntityInfo();

            // 获取基本属性
            String id = null;
            if (node.containsKey("id")) {
                id = node.get("id").asString();
                entity.setId(id);
            }

            // 获取类型：优先从ID前缀判断，其次从Label
            String entityType = inferTypeFromId(id);
            if (entityType == null && node.labels().iterator().hasNext()) {
                entityType = node.labels().iterator().next();
            }
            entity.setEntity_type(entityType);

            if (node.containsKey("name")) {
                entity.setName(node.get("name").asString());
            }

            if (node.containsKey("owner")) {
                entity.setOwner(node.get("owner").asString());
            }

            if (node.containsKey("filePath")) {
                entity.setFilePath(node.get("filePath").asString());
            }

            entity.setRelationshipType(relationshipType);

            return entity;
        } catch (Exception e) {
            System.err.println("转换实体失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从ID推断实体类型
     * ID格式: method_ClassName_methodName(...) 或 field_ClassName_fieldName 或 class_ClassName
     */
    private String inferTypeFromId(String id) {
        if (id == null) {
            return null;
        }

        if (id.startsWith("method_")) {
            return "Method";
        } else if (id.startsWith("field_")) {
            return "Field";
        } else if (id.startsWith("class_") || id.startsWith("iface_")) {
            return "ClassOrInterface";
        } else if (id.startsWith("param_")) {
            return "Parameter";
        }

        return null;
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.close();
        }
    }

    /**
     * 测试连接
     */
    public boolean testConnection() {
        try (Session session = driver.session()) {
            session.executeRead(tx -> {
                tx.run("RETURN 1");
                return null;
            });
            return true;
        } catch (Exception e) {
            System.err.println("Neo4j连接失败: " + e.getMessage());
            return false;
        }
    }
}
