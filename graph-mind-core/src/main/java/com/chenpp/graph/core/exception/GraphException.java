package com.chenpp.graph.core.exception;

/**
 * 图操作异常
 *
 * @author April.Chen
 * @date 2025/4/7 17:22
 */
public class GraphException extends RuntimeException {
    private final ErrorCode errorCode;

    public GraphException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    }

    public GraphException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    }

    public GraphException(Throwable cause) {
        super(cause);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    }

    public GraphException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GraphException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GraphException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public GraphException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}