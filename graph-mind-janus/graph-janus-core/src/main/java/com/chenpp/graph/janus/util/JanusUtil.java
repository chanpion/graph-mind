package com.chenpp.graph.janus.util;

import com.chenpp.graph.core.schema.DataType;
import com.google.common.collect.ImmutableMap;

import java.util.Date;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/8/18 16:14
 */
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


    public static Class<?> getJanusDataType(DataType dataType) {
        return DATA_TYPE_MAP.get(dataType);
    }
}
