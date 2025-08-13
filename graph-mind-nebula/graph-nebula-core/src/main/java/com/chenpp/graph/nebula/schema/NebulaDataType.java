package com.chenpp.graph.nebula.schema;

/**
 * @author hongbin.duan
 * @create 2022-01-27
 */
public enum NebulaDataType {

    /**
     * 64 位带符号整数 [-9223372036854775808, 9223372036854775807]
     */
    INT64,

    /**
     * 双精度浮点数
     */
    DOUBLE,

    /**
     * 布尔类型
     */
    BOOL,

    /**
     * 定长字符串
     */
    FIXED_STRING,

    /**
     * 变长字符串
     */
    STRING,

    /**
     * 包含日期，不含时间
     */
    DATE,

    /**
     * 包含时间，不含日期
     */
    TIME,

    /**
     * 包含日期和时间
     */
    DATETIME,

    /**
     * 包含日期和时间
     */
    TIMESTAMP

}
