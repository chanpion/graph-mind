package com.chenpp.graph.core.exception;

/**
 * @author April.Chen
 * @date 2025/4/7 16:20
 */
public enum ErrorCode {
    // 已存在的错误码
    CONNECTION_FAILED(1001, "连接失败"),
    QUERY_EXECUTION_FAILED(1002, "查询执行失败"),
    SCHEMA_VALIDATION_FAILED(1003, "Schema校验失败"),
    TRANSACTION_FAILED(1004, "事务执行失败"),
    UNSUPPORTED_OPERATION(1005, "不支持的操作"),
    INVALID_CONFIGURATION(1006, "无效的配置"),
    BATCH_OPERATION_FAILED(1007, "批量操作失败"),
    VERSION_CONFLICT(1008, "版本冲突"),

    // 新增的通用错误码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    REQUEST_TIMEOUT(408, "请求超时"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    NOT_IMPLEMENTED(501, "功能未实现"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    // 业务相关错误码 (2000-2999)
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    ROLE_NOT_FOUND(2003, "角色不存在"),
    PERMISSION_DENIED(2004, "权限不足"),
    INVALID_CREDENTIALS(2005, "用户名或密码错误"),
    ACCOUNT_DISABLED(2006, "账户已被禁用"),
    GRAPH_NOT_FOUND(2007, "图不存在"),
    GRAPH_ALREADY_EXISTS(2008, "图已存在"),
    INVALID_SCHEMA_DEFINITION(2009, "无效的Schema定义"),
    GRAPH_CONNECTION_FAILED(2010, "图数据库连接失败"),
    GRAPH_QUERY_FAILED(2011, "图查询失败"),
    GRAPH_IMPORT_FAILED(2012, "图数据导入失败");

    private final int code;
    private final String message;

    ErrorCode(int code) {
        this.code = code;
        this.message = "";
    }

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}