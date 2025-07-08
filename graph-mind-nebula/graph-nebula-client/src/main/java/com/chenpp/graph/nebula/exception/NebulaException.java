package com.chenpp.graph.nebula.exception;

/**
 * @author April.Chen
 * @date 2025/7/8 15:38
 */
public class NebulaException extends Exception {

    public NebulaException(String msg) {
        super(msg);
    }

    public NebulaException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public NebulaException(Throwable cause) {
        super(cause);
    }

}
