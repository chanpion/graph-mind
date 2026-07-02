package com.chenpp.graph.nebula.util;

import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphProperty;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.core.util.DataTypeConverter;
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


    public static String buildCreateSpace(NebulaConf nebulaConf) {
        return "CREATE SPACE IF NOT EXISTS " + quoteIdentifier(nebulaConf.getGraphCode()) + " (PARTITION_NUM = " + nebulaConf.getPartitionNum() + ", REPLICA_FACTOR = " + nebulaConf.getReplicaFactor() + ", VID_TYPE = FIXED_STRING(" + nebulaConf.getVidFixedStrLength() + "))";
    }

    public static String buildDropSpace(String spaceName) {
        return "DROP SPACE IF EXISTS " + quoteIdentifier(spaceName);
    }

    public static String buildUseSpace(String spaceName) {
        return "USE " + quoteIdentifier(spaceName);
    }

    public static String buildShowSpaces() {
        return "SHOW SPACES";
    }


    public static String buildCreateTag(GraphEntity entity) {
        StringBuilder tagBuilder = new StringBuilder();
        String properties = entity.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        tagBuilder.append("CREATE TAG IF NOT EXISTS ").append(quoteIdentifier(entity.getLabel())).append(" (").append(properties).append(")");
        return tagBuilder.toString();
    }

    public static String buildDescribeTag(String tagName) {
        return "DESCRIBE TAG " + quoteIdentifier(tagName);
    }

    public static String buildShowTags() {
        return "SHOW TAGS";
    }

    public static String buildDropTag(String tagName) {
        return "DROP TAG IF EXISTS " + quoteIdentifier(tagName);
    }

    public static String buildAlterTagAdd(GraphEntity entity) {
        String properties = entity.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        return "ALTER TAG " + quoteIdentifier(entity.getLabel()) + " ADD (" + properties + ")";
    }


    public static String buildCreateEdge(GraphRelation relation) {
        StringBuilder edgeBuilder = new StringBuilder();
        String properties = relation.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        edgeBuilder.append("CREATE EDGE IF NOT EXISTS ").append(quoteIdentifier(relation.getLabel())).append("(").append(properties).append(")");
        return edgeBuilder.toString();
    }

    public static String buildDescribeEdge(String edgeTypeName) {
        return "DESCRIBE EDGE " + quoteIdentifier(edgeTypeName);
    }

    public static String buildShowEdges() {
        return "SHOW EDGES";
    }

    public static String buildDropEdge(String edgeTypeName) {
        return "DROP EDGE IF EXISTS " + quoteIdentifier(edgeTypeName);
    }

    public static String buildAlterEdgeAdd(GraphRelation relation) {
        String properties = relation.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        return "ALTER EDGE " + quoteIdentifier(relation.getLabel()) + " ADD (" + properties + ")";
    }


    public static String buildCreateIndex(NebulaIndex index) {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE ").append(index.getIndexType()).append(" INDEX IF NOT EXISTS ").append(quoteIdentifier(index.getIndexName()))
                .append(" ON ").append(quoteIdentifier(index.getTypeName())).append(" (");
        if (index.getPropNameList() != null && !index.getPropNameList().isEmpty()) {
            Map<String, String> propTypeMap = index.getPropTypeMap();
            builder.append(index.getPropNameList().stream().map(p -> {
                if (propTypeMap != null) {
                    String propType = propTypeMap.get(p);
                    if ("STRING".equalsIgnoreCase(propType) || "FIXED_STRING".equalsIgnoreCase(propType)) {
                        return p + "(64)";
                    }
                }
                return p;
            }).collect(Collectors.joining(", ")));
        }
        builder.append(")");
        return builder.toString();
    }

    public static String buildShowIndexes(SchemaType schemaType) {
        return "SHOW " + schemaType + " INDEXES";
    }

    public static String buildRebuildIndex(SchemaType schemaType, String indexName) {
        return "REBUILD " + schemaType + " INDEX " + quoteIdentifier(indexName);
    }

    public static String buildDropIndex(SchemaType schemaType, String indexName) {
        return "DROP " + schemaType + " INDEX IF EXISTS " + quoteIdentifier(indexName);
    }


    public static String buildInsertVertex(GraphVertex vertex) {
        return buildInsertVertex(vertex, null);
    }

    public static String buildInsertVertex(GraphVertex vertex, Map<String, DataType> propertyTypes) {
        String uid = vertex.getUid();
        String keys = String.join(",", vertex.getProperties().keySet());
        String propValues = buildPropertyValuesClause(vertex.getProperties(), propertyTypes);
        return String.format("INSERT VERTEX %s(%s) VALUES \"%s\":(%s)", quoteIdentifier(vertex.getLabel()), keys, uid, propValues);
    }

    public static String buildInsertVertexBatch(String label, String keys, String valuesClause) {
        return String.format("INSERT VERTEX %s(%s) VALUES %s", quoteIdentifier(label), keys, valuesClause);
    }

    public static String buildInsertVertexValuesClause(GraphVertex vertex, Map<String, DataType> propertyTypes) {
        String propValues = buildPropertyValuesClause(vertex.getProperties(), propertyTypes);
        return String.format("\"%s\":(%s)", vertex.getUid(), propValues);
    }

    public static String buildUpdateVertex(String label, String vid, String setClause) {
        return String.format("UPDATE VERTEX ON %s \"%s\" SET %s", quoteIdentifier(label), vid, setClause);
    }

    public static String buildDeleteVertex(String uid) {
        return String.format("DELETE VERTEX \"%s\" WITH EDGE;", uid);
    }


    public static String buildInsertEdge(String label, String keys, String startUid, String endUid, String propValues) {
        return String.format("INSERT EDGE %s (%s) VALUES \"%s\" -> \"%s\":(%s);", quoteIdentifier(label), keys, startUid, endUid, propValues);
    }

    public static String buildInsertEdgeBatch(String label, String propKeys, String valuesStr) {
        return String.format("INSERT EDGE %s (%s) VALUES %s", quoteIdentifier(label), propKeys, valuesStr);
    }

    public static String buildUpdateEdge(String label, String startUid, String endUid, String setClause) {
        return String.format("UPDATE EDGE ON %s \"%s\" -> \"%s\" SET %s;", quoteIdentifier(label), startUid, endUid, setClause);
    }

    public static String buildDeleteEdge(String label, String startUid, String endUid) {
        return String.format("DELETE EDGE %s \"%s\" -> \"%s\";", quoteIdentifier(label), startUid, endUid);
    }


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
        return String.format("MATCH (v:%s) RETURN count(v) AS count;", quoteIdentifier(label));
    }

    public static String buildCountEdge(String label) {
        return String.format("MATCH ()-[e:%s]->() RETURN count(e) AS count;", quoteIdentifier(label));
    }

    public static String buildFindVertexByProperty(String label, String property, String value) {
        String escapedValue = value.replace("'", "\\'");
        return String.format("MATCH (n:`%s`) WHERE n.`%s`.`%s` == '%s' RETURN n", label, label, property, escapedValue);
    }


    public static String buildSubmitJobStats() {
        return "SUBMIT JOB STATS";
    }

    public static String buildShowJob(long jobId) {
        return "SHOW JOB " + jobId;
    }

    public static String buildShowStats() {
        return "SHOW STATS";
    }


    /**
     * 为 NGQL 标识符（标签名、边类型名、属性名等）添加反引号转义
     */
    private static String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
    }

    public static String buildPropertyValuesClause(Map<String, Object> properties) {
        return properties.values().stream()
                .map(p -> formatValue(p, null))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public static String buildPropertyValuesClause(Map<String, Object> properties, Map<String, DataType> propertyTypes) {
        return properties.entrySet().stream()
                .map(entry -> formatValue(entry.getValue(), propertyTypes != null ? propertyTypes.get(entry.getKey()) : null))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public static String formatValue(Object value, DataType dataType) {
        if (value == null) {
            return "NULL";
        }

        if (dataType != null && value instanceof String) {
            String strValue = ((String) value).trim();
            if ("null".equalsIgnoreCase(strValue) || "NULL".equals(strValue)) {
                return "NULL";
            }

            return switch (dataType) {
                case Short, Integer, Int, Long -> {
                    try {
                        if (strValue.contains(".")) {
                            yield String.valueOf((long) Double.parseDouble(strValue));
                        } else {
                            yield String.valueOf(Long.parseLong(strValue));
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse '{}' as number, treating as string", strValue);
                        yield "\"" + strValue.replace("\"", "\\\"") + "\"";
                    }
                }
                case Float, Double -> {
                    try {
                        yield String.valueOf(Double.parseDouble(strValue));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse '{}' as double, treating as string", strValue);
                        yield "\"" + strValue.replace("\"", "\\\"") + "\"";
                    }
                }
                case Boolean -> {
                    try {
                        yield DataTypeConverter.toBoolean(strValue) ? "TRUE" : "FALSE";
                    } catch (IllegalArgumentException e) {
                        log.warn("Failed to parse '{}' as boolean, treating as string", strValue);
                        yield "\"" + strValue.replace("\"", "\\\"") + "\"";
                    }
                }
                case Date, Datetime -> {
                    if (DataTypeConverter.isDateString(strValue)) {
                        if (strValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            yield "DATETIME('" + strValue + " 00:00:00')";
                        } else {
                            yield "DATETIME('" + strValue + "')";
                        }
                    } else {
                        log.warn("Failed to parse '{}' as datetime, treating as string", strValue);
                        yield "\"" + strValue.replace("\"", "\\\"") + "\"";
                    }
                }
                case String, Array ->
                        "\"" + strValue.replace("\"", "\\\"") + "\"";
            };
        }

        if (value instanceof String) {
            String str = ((String) value).trim();
            if ("null".equalsIgnoreCase(str) || "NULL".equals(str)) {
                return "NULL";
            }
            if (DataTypeConverter.isDateString(str)) {
                if (str.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return "DATETIME('" + str + " 00:00:00')";
                }
                return "DATETIME('" + str + "')";
            }
            try {
                return DataTypeConverter.toBoolean(str) ? "TRUE" : "FALSE";
            } catch (IllegalArgumentException ignored) {
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
    public static String convertToNebulaDataType(DataType dataType) {
        if (dataType == null) {
            return "STRING";
        }
        return switch (dataType) {
            case Short -> "INT32";
            case Integer, Int, Long -> "INT64";
            case Float, Double -> "DOUBLE";
            case Boolean -> "BOOL";
            case String -> "STRING";
            case Date, Datetime -> "DATETIME";
            case Array -> "STRING";
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
            } else if (value.isDateTime()) {
                String datetimeStr = value.toString();
                int startIdx = datetimeStr.indexOf("datetime: ");
                if (startIdx >= 0) {
                    startIdx += "datetime: ".length();
                    int endIdx = datetimeStr.indexOf(",", startIdx);
                    if (endIdx >= 0) {
                        datetimeStr = datetimeStr.substring(startIdx, endIdx).trim();
                    } else {
                        datetimeStr = datetimeStr.substring(startIdx).trim();
                    }
                    // 移除微秒部分（保留到秒级）
                    int dotIdx = datetimeStr.indexOf(".");
                    if (dotIdx >= 0) {
                        datetimeStr = datetimeStr.substring(0, dotIdx);
                    }
                    // 如果是标准ISO格式，转换为本地格式
                    if (datetimeStr.contains("T")) {
                        datetimeStr = datetimeStr.replace("T", " ");
                    }
                }
                return datetimeStr;
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