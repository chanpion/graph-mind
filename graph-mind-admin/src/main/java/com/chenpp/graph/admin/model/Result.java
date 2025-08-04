package com.chenpp.graph.admin.model;

import lombok.Data;

/**
 * @author April.Chen
 * @date 2025/8/1 10:45
 */
@Data
public class Result<T> {
    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     *
     * @param message 响应消息
     * @return Result
     */
    public static<T> Result<T>  success(String message) {
        return new Result<>(200, message);
    }

    /**
     * 成功响应（有数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应（有数据和消息）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return Result
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应
     *
     * @param message 响应消息
     * @return Result
     */
    public static Result<Void> error(String message) {
        return new Result<>(500, message);
    }

    /**
     * 失败响应
     *
     * @param code    响应码
     * @param message 响应消息
     * @return Result
     */
    public static Result<Void> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    /**
     * 失败响应（带数据）
     *
     * @param code    响应码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return Result
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}