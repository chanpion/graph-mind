package com.chenpp.graph.janus.util;

import com.chenpp.graph.core.constant.GraphConstants;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.core.schema.DataType;
import com.chenpp.graph.core.util.DataTypeConverter;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphEdge;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/8/18 16:14
 */
@Slf4j
public class JanusUtil {

    /**
     * 产品定义的属性数据类型 -> janus 数据类型
     */
    public static final Map<DataType, Class<?>> DATA_TYPE_MAP = new ImmutableMap.Builder<DataType, Class<?>>()
            .put(DataType.Short, Short.class)
            .put(DataType.Boolean, Boolean.class)
            .put(DataType.String, String.class)
            .put(DataType.Integer, Integer.class)
            .put(DataType.Int, Integer.class)
            .put(DataType.Float, Float.class)
            .put(DataType.Double, Double.class)
            .put(DataType.Long, Long.class)
            .put(DataType.Date, Date.class)
            .put(DataType.Datetime, Date.class)
            .build();

    /**
     * janus 数据类型 -> 产品定义的属性数据类型
     */
    public static final Map<Class<?>, DataType> CLASS_TYPE_MAP = new ImmutableMap.Builder<Class<?>, DataType>()
            .put(Short.class, DataType.Short)
            .put(Boolean.class, DataType.Boolean)
            .put(String.class, DataType.String)
            .put(Integer.class, DataType.Integer)
            .put(Float.class, DataType.Float)
            .put(Double.class, DataType.Double)
            .put(Long.class, DataType.Long)
            .put(Date.class, DataType.Datetime)
            .build();


    public static Class<?> getJanusDataType(DataType dataType) {
        return DATA_TYPE_MAP.get(dataType);
    }

    public static DataType getDataType(Class<?> clazz) {
        return CLASS_TYPE_MAP.get(clazz);
    }


    /**
     * 解析 JanusGraphVertex 为 GraphVertex
     */
    public static GraphVertex parseVertex(JanusGraphVertex vertex) {
        GraphVertex graphVertex = new GraphVertex();

        if (vertex.property(GraphConstants.UID).isPresent()) {
            graphVertex.setUid(vertex.property(GraphConstants.UID).value().toString());
        } else {
            graphVertex.setUid(vertex.id().toString());
        }
        graphVertex.setId(vertex.id().toString());
        graphVertex.setLabel(vertex.label());

        Map<String, Object> properties = new HashMap<>();
        vertex.keys().forEach(key -> {
            if (vertex.property(key).isPresent() && !GraphConstants.UID.equals(key)) {
                properties.put(key, vertex.property(key).value());
            }
        });
        graphVertex.setProperties(properties);

        return graphVertex;
    }

    /**
     * 解析 JanusGraphEdge 为 GraphEdge
     */
    public static GraphEdge parseEdge(JanusGraphEdge edge) {
        GraphEdge graphEdge = new GraphEdge();

        if (edge.property(GraphConstants.UID).isPresent()) {
            graphEdge.setUid((String) edge.property(GraphConstants.UID).value());
        } else {
            graphEdge.setUid(edge.id().toString());
        }

        graphEdge.setLabel(edge.label());

        JanusGraphVertex outVertex = edge.outVertex();
        JanusGraphVertex inVertex = edge.inVertex();

        if (outVertex.property(GraphConstants.UID).isPresent()) {
            graphEdge.setStartUid((String) outVertex.property(GraphConstants.UID).value());
        }

        if (inVertex.property(GraphConstants.UID).isPresent()) {
            graphEdge.setEndUid((String) inVertex.property(GraphConstants.UID).value());
        }

        Map<String, Object> properties = new HashMap<>();
        edge.keys().forEach(key -> {
            if (edge.property(key).isPresent() && !GraphConstants.UID.equals(key)) {
                properties.put(key, edge.property(key).value());
            }
        });
        graphEdge.setProperties(properties);

        return graphEdge;
    }


    /**
     * 从 TinkerPop 顶点获取 uid 属性值
     */
    public static String getVertexUid(Vertex vertex, JanusGraph graph) {
        try {
            if (vertex.property(GraphConstants.UID).isPresent()) {
                Object uid = vertex.property(GraphConstants.UID).value();
                if (StringUtils.isNotBlank(uid.toString())) {
                    return uid.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get uid directly, will try to refresh vertex: {}", e.getMessage());
        }

        try {
            Iterator<Vertex> refreshed = graph.vertices(vertex.id());
            if (refreshed.hasNext()) {
                Vertex freshVertex = refreshed.next();
                if (freshVertex.property(GraphConstants.UID).isPresent() && !freshVertex.property(GraphConstants.UID).value().toString().isEmpty()) {
                    return freshVertex.property(GraphConstants.UID).value().toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to refresh vertex for uid: {}", e.getMessage());
        }

        return vertex.id().toString();
    }


    /**
     * 获取属性的类型信息
     */
    public static Class<?> getPropertyType(String propertyName, JanusGraph graph) {
        try {
            org.janusgraph.core.PropertyKey propertyKey = graph.getPropertyKey(propertyName);
            if (propertyKey != null) {
                return propertyKey.dataType();
            }
        } catch (Exception e) {
            log.debug("Failed to get property type for {}: {}", propertyName, e.getMessage());
        }
        return null;
    }

    /**
     * 根据属性类型转换值
     */
    public static Object convertPropertyValue(String key, Object value, JanusGraph graph) {
        if (value == null) {
            return null;
        }

        Class<?> propertyType = getPropertyType(key, graph);
        if (propertyType == null || propertyType.isInstance(value)) {
            return value;
        }

        if (!(value instanceof String)) {
            return value;
        }

        String strValue = (String) value;
        try {
            if (propertyType == Date.class) {
                return DataTypeConverter.parseDate(strValue);
            }
            if (propertyType == Integer.class || propertyType == int.class) {
                return DataTypeConverter.toLong(strValue).intValue();
            }
            if (propertyType == Long.class || propertyType == long.class) {
                return DataTypeConverter.toLong(strValue);
            }
            if (propertyType == Double.class || propertyType == double.class) {
                return DataTypeConverter.toDouble(strValue);
            }
            if (propertyType == Float.class || propertyType == float.class) {
                return DataTypeConverter.toDouble(strValue).floatValue();
            }
            if (propertyType == Boolean.class || propertyType == boolean.class) {
                return DataTypeConverter.toBoolean(strValue);
            }
        } catch (Exception e) {
            log.warn("Failed to convert value '{}' to type {} for property {}: {}",
                    strValue, propertyType.getSimpleName(), key, e.getMessage());
        }
        return value;
    }


    public static void safeRollback(JanusGraphManagement management) {
        if (management != null) {
            try {
                management.rollback();
            } catch (Exception e) {
                log.warn("Failed to rollback management transaction", e);
            }
        }
    }
}
