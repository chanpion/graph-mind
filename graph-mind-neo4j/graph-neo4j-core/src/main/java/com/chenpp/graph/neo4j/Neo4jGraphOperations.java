package com.chenpp.graph.neo4j;

import com.chenpp.graph.core.GraphOperations;
import com.chenpp.graph.core.exception.ErrorCode;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.Graph;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.schema.GraphSchema;
import com.chenpp.graph.neo4j.util.Neo4jUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author April.Chen
 * @date 2025/7/14 09:46
 */
@Slf4j
public class Neo4jGraphOperations implements GraphOperations {
    private Neo4jConf neo4jConf;
    private Driver driver;

    public Neo4jGraphOperations(Neo4jConf neo4jConf, Driver driver) {
        this.neo4jConf = neo4jConf;
        this.driver = driver;
    }

    @Override
    public void createGraph(GraphConf graphConf) throws GraphException {
        if (!isEnterpriseEdition()) {
            throw new GraphException(ErrorCode.UNSUPPORTED_OPERATION, "Neo4j 社区版不支持多数据库操作");
        }

        String database = graphConf.getGraphCode();
        try (Session session = driver.session()) {
            session.run("CREATE DATABASE $" + "name", Map.of("name", database)).consume();
            log.info("Created database in Neo4j: {}", database);
        } catch (Exception e) {
            log.error("Failed to create database in Neo4j: {}", database, e);
            throw new GraphException("Failed to create database in Neo4j: " + database, e);
        }
    }

    @Override
    public void dropGraph(GraphConf graphConf) throws GraphException {
        log.info("Dropping graph in Neo4j: {}", graphConf.getGraphCode());

        if (!isEnterpriseEdition()) {
            throw new GraphException(ErrorCode.UNSUPPORTED_OPERATION, "Neo4j 社区版不支持多数据库操作");
        }

        String database = graphConf.getGraphCode();
        try (Session session = driver.session()) {
            session.run("DROP DATABASE $" + "name", Map.of("name", database)).consume();
            log.info("Dropped database in Neo4j: {}", database);
        } catch (Exception e) {
            log.error("Failed to drop database in Neo4j: {}", database, e);
            throw new GraphException("Failed to drop database in Neo4j: " + database, e);
        }
    }

    /**
     * 是否企业版（企业版 vs 社区版）
     * 通过 dbms.components() 查询 Neo4j 版本类型
     */
    private boolean isEnterpriseEdition() {
        try (Session session = driver.session()) {
            Result result = session.run("CALL dbms.components() YIELD edition RETURN edition");
            if (result.hasNext()) {
                String edition = result.next().get(0).asString();
                return "enterprise".equalsIgnoreCase(edition);
            }
        } catch (Exception e) {
            log.error("Not enterprise edition: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public List<Graph> listGraphs(GraphConf graphConf) {
        log.info("Listing graphs in Neo4j");
        List<Graph> result = new ArrayList<>();

        try (Session session = driver.session()) {
            Result dbResult = session.run("SHOW DATABASES");
            while (dbResult.hasNext()) {
                Record record = dbResult.next();
                String dbName = record.get("name").asString();
                String status = record.get("currentStatus").asString();
                if ("system".equalsIgnoreCase(dbName) || !"online".equalsIgnoreCase(status)) {
                    continue;
                }

                Graph g = new Graph();
                g.setCode(dbName);
                g.setName(dbName);
                result.add(g);
            }
        }

        return result;
    }

    /**
     * 获取 Neo4j 默认数据库名称
     */
    private String getDefaultDatabaseName() {
        try (Session session = driver.session()) {
            Result dbResult = session.run("SHOW DEFAULT DATABASE");
            if (dbResult.hasNext()) {
                String name = dbResult.next().get("name").asString();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        }
        return "neo4j";
    }

    @Override
    public void applySchema(GraphConf graphConf, GraphSchema graphSchema) {
        log.info("Applying schema to graph: {}", graphConf.getGraphCode());
        if (CollectionUtils.isNotEmpty(graphSchema.getIndexes())) {
            graphSchema.getIndexes().forEach(this::createIndex);
        }
    }

    @Override
    public GraphSchema getPublishedSchema(GraphConf graphConf) throws GraphException {
        GraphSchema schema = new GraphSchema();
        String database = resolveDatabase(graphConf.getGraphCode());

        try {
            List<GraphEntity> entities = getNodeLabels(database);
            schema.setEntities(entities);

            List<GraphRelation> relations = getRelationshipTypes(database);
            schema.setRelations(relations);

            List<GraphIndex> indexes = getIndexes(database);
            schema.setIndexes(indexes);
        } catch (Exception e) {
            log.error("Failed to get published schema from Neo4j", e);
            throw new GraphException("Failed to get published schema from Neo4j", e);
        }

        return schema;
    }

    /**
     * 解析要使用的数据库名称
     * 企业版：优先使用 graphCode 指定的多数据库
     * 社区版：使用默认数据库
     */
    private String resolveDatabase(String graphCode) {
        if (graphCode != null && !graphCode.isEmpty()) {
            try (Session session = driver.session(SessionConfig.builder().withDatabase(graphCode).build())) {
                session.run("RETURN 1").consume();
                return graphCode;
            } catch (Exception e) {
                log.debug("Database '{}' not available, using default: {}", graphCode, e.getMessage());
            }
        }
        return getDefaultDatabaseName();
    }

    public List<GraphEntity> getNodeLabels(String database) throws GraphException {
        Map<String, GraphEntity> entityMap = new LinkedHashMap<>();

        try (Session session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            // 获取所有节点标签
            Result labelsResult = session.run("CALL db.labels()");
            while (labelsResult.hasNext()) {
                String label = labelsResult.next().get(0).asString();
                GraphEntity entity = new GraphEntity(label, new ArrayList<>());
                entityMap.put(label, entity);
            }

            Result propsResult = session.run("CALL db.schema.nodeTypeProperties()");
            while (propsResult.hasNext()) {
                Record record = propsResult.next();
                List<String> nodeLabels = safeGetList(record, "nodeLabels", Value::asString);
                if (nodeLabels == null || nodeLabels.isEmpty()) {
                    continue;
                }

                String label = nodeLabels.get(0);
                GraphEntity entity = entityMap.get(label);
                if (entity == null) {
                    continue;
                }

                String propertyName = safeGetString(record, "propertyName");
                if (StringUtils.isBlank(propertyName)) {
                    continue;
                }

                String typeStr = safeGetSingleFromList(record, "propertyTypes");
                DataType dataType = DataType.instanceOf(typeStr);
                if (dataType == null) {
                    dataType = DataType.String;
                }

                GraphProperty prop = new GraphProperty();
                prop.setCode(propertyName);
                prop.setName(propertyName);
                prop.setDataType(dataType);

                if (entity.getProperties() == null) {
                    entity.setProperties(new ArrayList<>());
                }
                entity.getProperties().add(prop);
            }
        } catch (Exception e) {
            log.error("Failed to get node labels from Neo4j", e);
            throw new GraphException("Failed to get node labels", e);
        }

        return new ArrayList<>(entityMap.values());
    }

    public List<GraphRelation> getRelationshipTypes(String database) throws GraphException {
        Map<String, GraphRelation> relationMap = new LinkedHashMap<>();

        try (Session session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            Result typesResult = session.run("CALL db.relationshipTypes()");
            while (typesResult.hasNext()) {
                String type = typesResult.next().get(0).asString();
                GraphRelation relation = new GraphRelation(type);
                relationMap.put(type, relation);
            }

            Result propsResult = session.run("CALL db.schema.relTypeProperties()");
            while (propsResult.hasNext()) {
                Record record = propsResult.next();
                String rawRelType = safeGetString(record, "relType");
                if (rawRelType == null) {
                    continue;
                }

                String relType = rawRelType.replaceAll("[:`]", "");
                GraphRelation relation = relationMap.get(relType);
                String propertyName = safeGetString(record, "propertyName");
                if (StringUtils.isBlank(propertyName)) {
                    continue;
                }

                String typeStr = safeGetSingleFromList(record, "propertyTypes");
                DataType dataType = DataType.instanceOf(typeStr);
                if (dataType == null) {
                    dataType = DataType.String;
                }

                GraphProperty prop = new GraphProperty();
                prop.setCode(propertyName);
                prop.setName(propertyName);
                prop.setDataType(dataType);

                relation.getProperties().add(prop);
            }

            inferEdgeEndpoints(session, relationMap);
        } catch (Exception e) {
            log.error("Failed to get relationship types from Neo4j", e);
            throw new GraphException("Failed to get relationship types", e);
        }

        return new ArrayList<>(relationMap.values());
    }

    /**
     * 从实际数据中采样推断边的起点/终点节点标签
     */
    private void inferEdgeEndpoints(Session session, Map<String, GraphRelation> relationMap) {
        if (relationMap.isEmpty()) return;

        for (String relType : relationMap.keySet()) {
            String sampleCypher = String.format(
                    "MATCH (a)-[r:`%s`]->(b) RETURN labels(a)[0] as startLabel, labels(b)[0] as endLabel LIMIT 1", relType);
            try {
                Result result = session.run(sampleCypher);
                if (result.hasNext()) {
                    Record record = result.next();
                    String startLabel = record.get("startLabel").asString();
                    String endLabel = record.get("endLabel").asString();
                    GraphRelation relation = relationMap.get(relType);
                    relation.setStartLabel(startLabel);
                    relation.setEndLabel(endLabel);
                }
            } catch (Exception e) {
                log.debug("Failed to infer endpoints for relationship type {}: {}", relType, e.getMessage());
            }
        }
    }

    public List<GraphIndex> getIndexes(String database) throws GraphException {
        String cypher = "SHOW INDEXES";
        List<GraphIndex> indexes = new ArrayList<>();

        try (Session session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                GraphIndex index = new GraphIndex();
                index.setName(result.next().get("name").asString());
                indexes.add(index);
            }
        } catch (Exception e) {
            log.error("Failed to get indexes from Neo4j", e);
            throw new GraphException("Failed to get indexes", e);
        }
        return indexes;
    }

    public void createIndex(GraphIndex graphIndex) {
        String cypher = Neo4jUtil.buildCreateSingleIndex(graphIndex.getName(), graphIndex.getLabel(), graphIndex.getProperty());
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            log.info("Created index: {}", graphIndex.getName());
            result.consume();
        } catch (Exception e) {
            log.error("Failed to create index: {}", graphIndex.getName(), e);
        }
    }


    public void dropIndex(String indexName) {
        String cypher = String.format("DROP INDEX %s", indexName);
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jConf.getGraphCode()).build())) {
            Result result = session.run(cypher);
            log.info("Dropped index: {}", indexName);
            result.consume();
        } catch (Exception e) {
            log.error("Failed to drop index: {}", indexName, e);
        }
    }

    private static String safeGetString(Record record, String key) {
        try {
            Value value = record.get(key);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asString();
        } catch (Exception e) {
            return null;
        }
    }

    private static <T> List<T> safeGetList(Record record, String key, Function<Value, T> converter) {
        try {
            Value value = record.get(key);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asList(converter);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeGetSingleFromList(Record record, String key) {
        List<String> list = safeGetList(record, key, Value::asString);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}