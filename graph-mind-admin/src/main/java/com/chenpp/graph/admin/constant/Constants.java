package com.chenpp.graph.admin.constant;

/**
 * @author April.Chen
 * @date 2025/8/11 10:30
 */
public class Constants {
    /**
     * Authorization相关常量
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = 7;

    /**
     * JWT相关常量
     */
    public static final long JWT_EXPIRATION = 24 * 60 * 60 * 1000; // 24小时(毫秒)

    /**
     * 系统相关常量
     */
    public static final String ANONYMOUS_USER = "anonymous";

    /**
     * 序列化相关常量
     */
    public static final long SERIAL_VERSION_UID = 1L;
}