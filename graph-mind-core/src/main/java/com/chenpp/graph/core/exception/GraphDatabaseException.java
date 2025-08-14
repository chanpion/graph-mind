package com.chenpp.graph.core.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/4/7 16:21
 */
public class GraphDatabaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    public GraphDatabaseException(ErrorCode errorCode, String message) {
        this(errorCode, message, Collections.emptyMap());
    }

    public GraphDatabaseException(ErrorCode errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = Map.copyOf(context);
    }

    public GraphDatabaseException(String message) {
        this(ErrorCode.CONNECTION_FAILED, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return context;
    }
}