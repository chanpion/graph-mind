package com.chenpp.graph.core.exception;

/**
 * @author April.Chen
 * @date 2025/4/7 17:22
 */
public class GraphException extends RuntimeException{

    public GraphException(String message) {
        super(message);
    }

    public GraphException(String message, Throwable cause) {
        super(message, cause);
    }

    public GraphException(Throwable cause) {
        super(cause);
    }
}
