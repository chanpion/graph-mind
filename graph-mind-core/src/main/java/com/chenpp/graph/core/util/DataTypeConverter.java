package com.chenpp.graph.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 统一的数据类型转换工具类，消除各模块间的重复转换逻辑
 *
 * @author April.Chen
 * @date 2026/6/26
 */
public final class DataTypeConverter {

    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd"
    };

    private DataTypeConverter() {
    }

    // ==================== 类型转换 ====================

    /**
     * 根据类型名称转换值
     *
     * @param value 原始值
     * @param type  类型名称（long, int64, integer, double, float, boolean, date, datetime, string 等）
     * @return 转换后的值，转换失败时返回原值
     */
    public static Object convertValue(Object value, String type) {
        if (value == null) {
            return null;
        }
        if (type == null) {
            return value;
        }

        String typeLower = type.toLowerCase();
        try {
            return switch (typeLower) {
                case "long", "int64", "integer", "int", "int32" -> toLong(value);
                case "double", "float" -> toDouble(value);
                case "boolean", "bool" -> toBoolean(value);
                case "date", "datetime", "timestamp" -> toDate(value);
                case "string", "text", "varchar" -> value.toString();
                default -> value;
            };
        } catch (Exception e) {
            return value;
        }
    }

    // ==================== 基础类型解析 ====================

    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String s = value.toString().trim();
        if (s.contains(".")) {
            return (long) Double.parseDouble(s);
        }
        return Long.parseLong(s);
    }

    public static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString().trim());
    }

    public static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String s = value.toString().trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) {
            return Boolean.TRUE;
        }
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Unable to parse boolean: " + value);
    }

    public static Date toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Long) {
            return new Date((Long) value);
        }
        return parseDate(value.toString());
    }

    // ==================== 日期解析 ====================

    /**
     * 解析日期字符串，支持多种常见格式（委托 commons-lang3 DateUtils）
     */
    public static Date parseDate(String strValue) {
        if (StringUtils.isBlank(strValue)) {
            return null;
        }
        try {
            return DateUtils.parseDate(strValue.trim(), DATE_PATTERNS);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse date: " + strValue, e);
        }
    }

    /**
     * 判断字符串是否为日期格式（不实际解析，仅格式匹配）
     */
    public static boolean isDateString(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        String s = value.trim();
        return s.matches("\\d{4}-\\d{2}-\\d{2}")
                || s.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")
                || s.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}");
    }

    // ==================== 属性提取 ====================

    /**
     * 从原始属性 Map 中排除指定 key，返回新的属性 Map
     */
    public static Map<String, Object> extractProperties(Map<String, ?> raw, Set<String> excludeKeys) {
        Map<String, Object> result = new HashMap<>();
        if (raw == null) {
            return result;
        }
        raw.forEach((key, value) -> {
            if (excludeKeys == null || !excludeKeys.contains(key)) {
                result.put(key, value);
            }
        });
        return result;
    }
}
