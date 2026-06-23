package com.chenpp.graph.nebula.util;

import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.nebula.NebulaConf;
import com.chenpp.graph.nebula.schema.NebulaDataType;
import com.chenpp.graph.nebula.schema.NebulaIndex;
import com.chenpp.graph.nebula.schema.NebulaProperty;
import com.chenpp.graph.nebula.schema.SchemaType;
import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.Relationship;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author April.Chen
 * @date 2025/4/9 15:24
 */
@Slf4j
public class NebulaUtil {

    // ========== Space 操作 ==========

    public static String buildCreateSpace(NebulaConf nebulaConf) {
        return "CREATE SPACE IF NOT EXISTS " + nebulaConf.getGraphCode() + " (PARTITION_NUM = " + nebulaConf.getPartitionNum() + ", REPLICA_FACTOR = " + nebulaConf.getReplicaFactor() + ", VID_TYPE = FIXED_STRING(" + nebulaConf.getVidFixedStrLength() + "))";
    }

    public static String buildDropSpace(String spaceName) {
        return "DROP SPACE IF EXISTS " + spaceName;
    }

    public static String buildUseSpace(String spaceName) {
        return "USE " + spaceName;
    }

    public static String buildShowSpaces() {
        return "SHOW SPACES";
    }

    // ========== Tag 操作 ==========

    public static String buildCreateTag(GraphEntity entity) {
        StringBuilder tagBuilder = new StringBuilder();
        String properties = entity.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        tagBuilder.append("CREATE TAG IF NOT EXISTS ").append(entity.getLabel()).append(" (").append(properties).append(")");
        return tagBuilder.toString();
    }

    public static String buildDescribeTag(String tagName) {
        return "DESCRIBE TAG " + tagName;
    }

    public static String buildShowTags() {
        return "SHOW TAGS";
    }

    public static String buildDropTag(String tagName) {
        return "DROP TAG IF EXISTS " + tagName;
    }

    /**
     * 构建 ALTER TAG ADD 语句，为已有标签添加新属性
     */
    public static String buildAlterTagAdd(GraphEntity entity) {
        String properties = entity.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        return "ALTER TAG " + entity.getLabel() + " ADD (" + properties + ")";
    }

    // ========== Edge 操作 ==========

    public static String buildCreateEdge(GraphRelation relation) {
        StringBuilder edgeBuilder = new StringBuilder();
        String properties = relation.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        edgeBuilder.append("CREATE EDGE IF NOT EXISTS ").append(relation.getLabel()).append("(").append(properties).append(")");
        return edgeBuilder.toString();
    }

    public static String buildDescribeEdge(String edgeTypeName) {
        return "DESCRIBE EDGE " + edgeTypeName;
    }

    public static String buildShowEdges() {
        return "SHOW EDGES";
    }

    public static String buildDropEdge(String edgeTypeName) {
        return "DROP EDGE IF EXISTS " + edgeTypeName;
    }

    public static String buildAlterEdgeAdd(GraphRelation relation) {
        String properties = relation.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        return "ALTER EDGE " + relation.getLabel() + " ADD (" + properties + ")";
    }

    // ========== Index 操作 ==========

    public static String buildCreateIndex(NebulaIndex index) {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE ").append(index.getIndexType()).append(" INDEX IF NOT EXISTS ").append(index.getIndexName())
                .append(" ON ").append(index.getTypeName()).append(" (");
        if (index.getPropNameList() != null && !index.getPropNameList().isEmpty()) {
            builder.append(String.join(", ", index.getPropNameList().stream().map(p -> p + "(64)").collect(Collectors.toList())));
        }
        builder.append(")");
        return builder.toString();
    }

    public static String buildShowIndexes(SchemaType schemaType) {
        return "SHOW " + schemaType + " INDEXES";
    }

    public static String buildRebuildIndex(SchemaType schemaType, String indexName) {
        return "REBUILD " + schemaType + " INDEX " + indexName;
    }

    // ========== Vertex 数据操作 ==========

    public static String buildInsertVertex(GraphVertex vertex) {
        String uid = vertex.getUid();
        String keys = String.join(",", vertex.getProperties().keySet());
        String propValues = buildPropertyValuesClause(vertex.getProperties());
        return String.format("INSERT VERTEX %s(%s) VALUES \"%s\":(%s)", vertex.getLabel(), keys, uid, propValues);
    }

    public static String buildInsertVertexBatch(String label, String keys, String valuesClause) {
        return String.format("INSERT VERTEX %s(%s) VALUES %s", label, keys, valuesClause);
    }

    public static String buildUpdateVertex(String label, String vid, String setClause) {
        return String.format("UPDATE VERTEX ON %s \"%s\" SET %s", label, vid, setClause);
    }

    public static String buildDeleteVertex(String uid) {
        return String.format("DELETE VERTEX \"%s\" WITH EDGE;", uid);
    }

    // ========== Edge 数据操作 ==========

    public static String buildInsertEdge(String label, String keys, String startUid, String endUid, String propValues) {
        return String.format("INSERT EDGE %s (%s) VALUES \"%s\" -> \"%s\":(%s);", label, keys, startUid, endUid, propValues);
    }

    public static String buildInsertEdgeBatch(String label, String propKeys, String valuesStr) {
        return String.format("INSERT EDGE %s (%s) VALUES %s", label, propKeys, valuesStr);
    }

    public static String buildUpdateEdge(String label, String startUid, String endUid, String setClause) {
        return String.format("UPDATE EDGE ON %s \"%s\" -> \"%s\" SET %s;", label, startUid, endUid, setClause);
    }

    public static String buildDeleteEdge(String label, String startUid, String endUid) {
        return String.format("DELETE EDGE %s \"%s\" -> \"%s\";", label, startUid, endUid);
    }

    // ========== 查询操作 ==========

    public static String buildMatchVertices(String idListStr) {
        return String.format("MATCH (v) WHERE id(v) IN [%s] RETURN v LIMIT 1000", idListStr);
    }

    public static String buildExpandPath(String vertexId, int depth) {
        return String.format("MATCH p=(v)-[r*1..%d]-(v2) WHERE id(v) == \"%s\" RETURN p", depth, vertexId);
    }

    public static String buildShortestPath(String startVertexId, String endVertexId, int maxDepth) {
        return String.format("FIND SHORTEST PATH FROM \"%s\" TO \"%s\" OVER * UPTO %d STEPS", startVertexId, endVertexId, maxDepth);
    }

    public static String buildCountVertex(String label) {
        return String.format("MATCH (v:%s) RETURN count(v) AS count;", label);
    }

    public static String buildCountEdge(String label) {
        return String.format("MATCH ()-[e:%s]->() RETURN count(e) AS count;", label);
    }

    /**
     * 根据标签、属性名和属性值构建查找顶点的NGQL语句
     */
    public static String buildFindVertexByProperty(String label, String property, String value) {
        String escapedValue = value.replace("'", "\\'");
        return String.format("MATCH (n:`%s`) WHERE n.`%s`.`%s` == '%s' RETURN n", label, label, property, escapedValue);
    }

    // ========== 统计作业 ==========

    public static String buildSubmitJobStats() {
        return "SUBMIT JOB STATS";
    }

    public static String buildShowJob(long jobId) {
        return "SHOW JOB " + jobId;
    }

    public static String buildShowStats() {
        return "SHOW STATS";
    }

    // ========== 工具方法 ==========

    public static String buildPropertyValuesClause(Map<String, Object> properties) {
        return properties.values().stream()
                .map(NebulaUtil::formatValue)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * 格式化属性值，用于构建NGQL语句
     */
    public static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if ("null".equalsIgnoreCase(str) || "NULL".equals(str)) {
                return "NULL";
            }
            // 检查日期格式 (yyyy-MM-dd)
            if (str.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return "DATE(\"" + str + "\")";
            }
            // 检查日期时间格式 (yyyy-MM-ddTHH:mm:ss)
            if (str.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                return "DATETIME(\"" + str + "\")";
            }
            try {
                if (str.contains(".")) {
                    return String.valueOf(Double.parseDouble(str));
                } else {
                    return String.valueOf(Long.parseLong(str));
                }
            } catch (NumberFormatException ignored) {
            }
            if ("true".equalsIgnoreCase(str)) {
                return "TRUE";
            }
            if ("false".equalsIgnoreCase(str)) {
                return "FALSE";
            }
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString().toUpperCase();
        }
        return "\"" + value.toString().replace("\"", "\\\"") + "\"";
    }

    /**
     * 将通用数据类型转换为Nebula数据类型
     */
    private static String convertToNebulaDataType(DataType dataType) {
        if (dataType == null) {
            return "STRING";
        }
        return switch (dataType) {
            case Integer, Int, Long -> "INT64";
            case Float, Double -> "DOUBLE";
            case Boolean -> "BOOL";
            case String -> "STRING";
            case Date -> "DATE";
            case Datetime -> "DATETIME";
            default -> {
                log.warn("Unsupported data type: {}, using STRING as default", dataType);
                yield "STRING";
            }
        };
    }

    public static DataType convertToDataType(NebulaDataType type) {
        return switch (type) {
            case INT64 -> DataType.Long;
            case DOUBLE -> DataType.Double;
            case BOOL -> DataType.Boolean;
            case STRING -> DataType.String;
            case DATE -> DataType.Date;
            case DATETIME, TIMESTAMP -> DataType.Datetime;
            default -> DataType.String;
        };
    }

    public static GraphProperty toGraphProperty(NebulaProperty nebulaProp) {
        GraphProperty property = new GraphProperty();
        property.setCode(nebulaProp.getName());
        property.setName(nebulaProp.getName());
        property.setDataType(NebulaUtil.convertToDataType(nebulaProp.getDataType()));
        return property;
    }

    public static GraphVertex parseVertex(Node node) {
        GraphVertex graphVertex = new GraphVertex();
        try {
            String vid = node.getId().asString();
            graphVertex.setUid(vid);
            if (!node.tagNames().isEmpty()) {
                String label = node.tagNames().get(0);
                graphVertex.setLabel(label);
                Map<String, Object> properties = new HashMap<>();
                Map<String, ValueWrapper> nodeProps = node.properties(label);
                if (nodeProps != null) {
                    nodeProps.forEach((key, value) -> properties.put(key, parseValueWrapper(value)));
                }
                graphVertex.setProperties(properties);
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to parse vertex: {}", e.getMessage());
        }
        return graphVertex;
    }

    public static GraphEdge parseEdge(Relationship relationship) {
        GraphEdge graphEdge = new GraphEdge();
        try {
            String edgeName = relationship.edgeName();
            graphEdge.setLabel(edgeName);
            String srcId = relationship.srcId().asString();
            String dstId = relationship.dstId().asString();
            graphEdge.setStartUid(srcId);
            graphEdge.setEndUid(dstId);
            Map<String, Object> properties = new HashMap<>();
            Map<String, ValueWrapper> edgeProps = relationship.properties();
            if (edgeProps != null) {
                edgeProps.forEach((key, value) -> properties.put(key, parseValueWrapper(value)));
            }
            graphEdge.setProperties(properties);
            if (properties.get(GraphConstants.UID) != null) {
                graphEdge.setUid(properties.get(GraphConstants.UID).toString());
            } else {
                graphEdge.setUid(srcId + "->" + dstId + "@" + edgeName);
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to parse edge: {}", e.getMessage());
        }
        return graphEdge;
    }

    private static Object parseValueWrapper(ValueWrapper value) {
        try {
            if (value.isNull()) {
                return null;
            }
            if (value.isString()) {
                String str = value.asString();
                if ("NULL".equals(str)) {
                    return null;
                }
                return str;
            } else if (value.isLong()) {
                return value.asLong();
            } else if (value.isDouble()) {
                return value.asDouble();
            } else if (value.isBoolean()) {
                return value.asBoolean();
            } else if (value.isList()) {
                return value.asList().stream().map(NebulaUtil::parseValueWrapper).collect(Collectors.toList());
            } else if (value.isMap()) {
                Map<String, Object> map = new HashMap<>();
                value.asMap().forEach((k, v) -> map.put(k, parseValueWrapper(v)));
                return map;
            }
            return value.toString();
        } catch (Exception e) {
            log.warn("Failed to parse value: {}", e.getMessage());
            return value.toString();
        }
    }
}