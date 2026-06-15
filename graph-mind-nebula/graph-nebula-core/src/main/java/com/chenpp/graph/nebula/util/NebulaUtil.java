package com.chenpp.graph.nebula.util;

import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.nebula.NebulaConf;
import com.chenpp.graph.nebula.schema.NebulaDataType;
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
        return "CREATE SPACE IF NOT EXISTS " + nebulaConf.getSpace() + " (PARTITION_NUM = " + nebulaConf.getPartitionNum() + ", REPLICA_FACTOR = " + nebulaConf.getReplicaFactor() + ", VID_TYPE = FIXED_STRING(" + nebulaConf.getVidFixedStrLength() + "))";
    }

    public static String buildDropSpace(String spaceName) {
        return "DROP SPACE IF EXISTS " + spaceName;
    }

    public static void createUniqueIndex(NebulaConf nebulaConf, GraphIndex index) {
        String vertexIndex = "CREATE UNIQUE INDEX <index_name> ON <tag_name> (<property_name> [, <property_name> ...]);";
        String edgeIndex = "CREATE UNIQUE INDEX <index_name> ON <edge_type> (<property_name> [, <property_name> ...]);";
        String idx = "CREATE INDEX person_name_age_index ON person (name, age);";
        String dropIndex = "DROP INDEX person_name_index;";
    }

    public static String buildCreateTag(GraphEntity entity) {
        StringBuilder tagBuilder = new StringBuilder();
        String properties = entity.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        tagBuilder.append("CREATE TAG IF NOT EXISTS ").append(entity.getLabel()).append(" (").append(properties).append(")");

        return tagBuilder.toString();
    }

    public static String buildCreateEdge(GraphRelation relation) {
        StringBuilder edgeBuilder = new StringBuilder();
        String properties = relation.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        edgeBuilder.append("CREATE EDGE IF NOT EXISTS ").append(relation.getLabel()).append("(").append(properties).append(")");
        return edgeBuilder.toString();
    }

    /**
     * 将通用数据类型转换为Nebula数据类型
     *
     * @param dataType 通用数据类型
     * @return Nebula数据类型字符串
     */
    private static String convertToNebulaDataType(DataType dataType) {
        if (dataType == null) {
            return "STRING";
        }

        return switch (dataType) {
            case Integer, Long -> "INT64";
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

    public static String buildDescribeTag(String tagName) {
        return "DESCRIBE TAG " + tagName;
    }

    public static String buildShowTags() {
        return "SHOW TAGS";
    }

    public static String buildDropTag(String tagName) {
        return "DROP TAG IF EXISTS " + tagName;
    }

    public static String buildDescribeEdge(String edgeTypeName) {
        return "DESCRIBE EDGE " + edgeTypeName;
    }

    public static String buildShowEdges() {
        return "SHOW EDGES";
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

    /**
     * 构建 ALTER EDGE ADD 语句，为已有边类型添加新属性
     */
    public static String buildAlterEdgeAdd(GraphRelation relation) {
        String properties = relation.getProperties().stream()
                .map(prop -> prop.getCode() + " " + convertToNebulaDataType(prop.getDataType()))
                .collect(Collectors.joining(", "));
        return "ALTER EDGE " + relation.getLabel() + " ADD (" + properties + ")";
    }

    public static String buildDropEdge(String edgeTypeName) {
        return "DROP EDGE IF EXISTS " + edgeTypeName;
    }

    public static String buildInsertVertex(GraphVertex vertex) {
        String uid = vertex.getUid();
        String keys = String.join(",", vertex.getProperties().keySet());
        String propValues = buildPropertyValuesClause(vertex.getProperties());
        return String.format("INSERT VERTEX %s(%s) VALUES \"%s\":(%s)", vertex.getLabel(), keys, uid, propValues);
    }

    public static String buildPropertyValuesClause(Map<String, Object> properties) {
        return properties.values().stream()
                .map(NebulaUtil::formatValue)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * 格式化属性值，用于构建NGQL语句
     * 根据值的类型正确格式化：
     * - 字符串：加双引号
     * - 数字：直接输出（不加引号）
     * - 布尔：转大写
     * - null/NULL：输出 NULL（不加引号）
     * - 数字字符串：转换为数字后输出
     *
     * @param value 属性值
     * @return 格式化后的字符串
     */
    public static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof String) {
            String str = ((String) value).trim();
            // 处理 null 字符串
            if ("null".equalsIgnoreCase(str) || "NULL".equals(str)) {
                return "NULL";
            }
            // 尝试解析为数字
            try {
                if (str.contains(".")) {
                    double d = Double.parseDouble(str);
                    return String.valueOf(d);
                } else {
                    long l = Long.parseLong(str);
                    return String.valueOf(l);
                }
            } catch (NumberFormatException e) {
                // 不是数字，作为字符串处理
            }
            // 尝试解析为布尔值
            if ("true".equalsIgnoreCase(str)) {
                return "TRUE";
            }
            if ("false".equalsIgnoreCase(str)) {
                return "FALSE";
            }
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }

        if (value instanceof Number) {
            // 数字类型：整数和浮点数都直接输出
            return value.toString();
        }

        if (value instanceof Boolean) {
            return value.toString().toUpperCase();
        }

        // 其他类型默认转为字符串（加引号）
        return "\"" + value.toString().replace("\"", "\\\"") + "\"";
    }

    /**
     * 解析Nebula Vertex为GraphVertex
     *
     * @param node Nebula Node对象
     * @return GraphVertex对象
     */
    public static GraphVertex parseVertex(Node node) {
        GraphVertex graphVertex = new GraphVertex();

        // 解析顶点ID
        try {
            String vid = node.getId().asString();
            graphVertex.setUid(vid);

            // 解析标签和属性
            if (!node.tagNames().isEmpty()) {
                // 使用第一个标签作为顶点标签
                String label = node.tagNames().get(0);
                graphVertex.setLabel(label);

                // 解析属性
                Map<String, Object> properties = new HashMap<>();
                Map<String, ValueWrapper> nodeProps = node.properties(label);
                if (nodeProps != null) {
                    nodeProps.forEach((key, value) -> {
                        Object propValue = parseValueWrapper(value);
                        properties.put(key, propValue);
                    });
                }
                graphVertex.setProperties(properties);
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to parse vertex due to encoding issue: {}", e.getMessage(), e);
        }

        return graphVertex;
    }

    /**
     * 解析Nebula Edge为GraphEdge
     *
     * @param relationship Nebula Relationship对象
     * @return GraphEdge对象
     */
    public static GraphEdge parseEdge(Relationship relationship) {
        GraphEdge graphEdge = new GraphEdge();

        try {
            // 解析边的名称
            String edgeName = relationship.edgeName();
            graphEdge.setLabel(edgeName);

            // 解析起点和终点ID
            String srcId = relationship.srcId().asString();
            String dstId = relationship.dstId().asString();
            graphEdge.setStartUid(srcId);
            graphEdge.setEndUid(dstId);

            // 解析属性
            Map<String, Object> properties = new HashMap<>();
            Map<String, ValueWrapper> edgeProps = relationship.properties();
            if (edgeProps != null) {
                edgeProps.forEach((key, value) -> {
                    Object propValue = parseValueWrapper(value);
                    properties.put(key, propValue);
                });
            }
            graphEdge.setProperties(properties);
            if (properties.get("uid") != null) {
                graphEdge.setUid(properties.get("uid").toString());
            } else {
                // 使用复合键作为 uid，避免路径查询中多条边因 uid 为 null 而相互覆盖
                graphEdge.setUid(srcId + "->" + dstId + "@" + edgeName);
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to parse edge due to encoding issue: {}", e.getMessage(), e);
        }

        return graphEdge;
    }

    /**
     * 解析ValueWrapper值为Java对象
     *
     * @param value ValueWrapper值
     * @return Java对象
     */
    private static Object parseValueWrapper(ValueWrapper value) {
        try {
            // 先检查是否为 null
            if (value.isNull()) {
                return null;
            }
            if (value.isString()) {
                String str = value.asString();
                // 如果是 "NULL" 字符串，转为 null
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
                // 列表值
                // 注意：这里需要递归处理列表中的元素
                return value.asList().stream()
                        .map(NebulaUtil::parseValueWrapper)
                        .collect(Collectors.toList());
            } else if (value.isMap()) {
                // 映射值
                Map<String, Object> map = new HashMap<>();
                Map<String, ValueWrapper> valueMap = value.asMap();
                valueMap.forEach((key, val) -> {
                    map.put(key, parseValueWrapper(val));
                });
                return map;
            } else {
                // 其他类型默认转换为字符串
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to parse value: {}", e.getMessage(), e);
            return value.toString();
        }
    }
}