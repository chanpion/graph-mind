package com.chenpp.graph.core.schema;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * @author April.Chen
 * @date 2024/5/14 19:12
 */
public enum IndexType {


    /**
     * 精确查找（对应 janus 的组合索引）
     */
    COMPOSITE("composite"),

    /**
     * 模糊查找（对应 janus 的混合索引，nebula 不支持）
     */
    MIX("mix"),

    /**
     * 关系的点中心性索引（对应 janus 的点中心性索引，nebula 不支持）
     */
    VERTEX_CENTRIC("vertexCentric");

    private static final Map<String, IndexType> CONTAINER;

    static {
        ImmutableMap.Builder<String, IndexType> builder = new ImmutableMap.Builder<>();
        for (IndexType indexType : IndexType.values()) {
            builder.put(indexType.code, indexType);
        }
        CONTAINER = builder.build();
    }

    private final String code;

    IndexType(String code) {
        this.code = code;
    }

    public static IndexType get(String code) {
        if (code == null) {
            return null;
        }
        return CONTAINER.get(code);
    }
}
