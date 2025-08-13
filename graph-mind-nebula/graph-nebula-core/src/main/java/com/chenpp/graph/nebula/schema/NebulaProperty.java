package com.chenpp.graph.nebula.schema;

import lombok.Data;

/**
 * @author April.Chen
 * @date 2025/7/7 15:15
 */
@Data
public class NebulaProperty {
    public static final int FIXED_STRING_SIZE = 64;

    private static final boolean DEFAULT_NULLABLE = true;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 数据类型
     */
    private NebulaDataType dataType;

    /**
     * 是否可为 NULL, true - 可为 NULL; false - 不可为 NULL
     */
    private boolean nullable = DEFAULT_NULLABLE;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 默认值表达式，优先级高于 默认值
     */
    private String defaultValueExpr;

}
