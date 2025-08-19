package com.chenpp.graph.core.schema;

/**
 * 属性的数据类型
 *
 * @author April.Chen
 * @date 2023/10/11 4:44 下午
 **/
public enum DataType {

    /**
     * 短整型
     */
    Short,

    /**
     * 布尔值
     */
    Boolean,

    /**
     * 字符串
     */
    String,

    /**
     * 整型
     */
    Integer,
    /**
     * 整型
     */
    Int,

    /**
     * 短浮点数
     */
    Float,

    /**
     * 长浮点数
     */
    Double,

    /**
     * 长整型
     */
    Long,

    /**
     * 时间（日期+时间）
     */
    Date,
    /**
     * 时间（日期+时间）
     */
    Datetime,

    /**
     * 数组
     */
    Array;

    public static DataType instanceOf(String type) {
        for (DataType value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }
}
