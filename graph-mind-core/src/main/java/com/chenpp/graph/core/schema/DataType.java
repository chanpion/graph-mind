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
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        
        String normalizedType = type.trim();
        
        // 先尝试直接匹配枚举名称
        for (DataType value : values()) {
            if (value.name().equalsIgnoreCase(normalizedType)) {
                return value;
            }
        }
        
        // 支持中文类型名称映射
        return switch (normalizedType) {
            case "整型", "整数", "int64", "int32" -> Integer;
            case "短整型", "short" -> Short;
            case "长整型", "long" -> Long;
            case "字符串", "string", "text", "varchar" -> String;
            case "浮点数", "float" -> Float;
            case "双精度", "double" -> Double;
            case "布尔", "boolean", "bool" -> Boolean;
            case "日期", "date", "datetime", "timestamp" -> Date;
            case "数组", "array" -> Array;
            default -> null;
        };
    }
}
