package com.chenpp.graph.core.exception;

/**
 * @author April.Chen
 * @date 2025/4/7 16:20
 */
public enum ErrorCode {
    CONNECTION_FAILED(1001),
    QUERY_EXECUTION_FAILED(1002),
    SCHEMA_VALIDATION_FAILED(1003),
    TRANSACTION_FAILED(1004),
    UNSUPPORTED_OPERATION(1005),
    INVALID_CONFIGURATION(1006),
    BATCH_OPERATION_FAILED(1007),
    VERSION_CONFLICT(1008);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
