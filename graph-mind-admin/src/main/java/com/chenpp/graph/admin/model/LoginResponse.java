package com.chenpp.graph.admin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author April.Chen
 * @date 2025/8/1 17:47
 */
@Data
public class LoginResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    /**
     * 过期时间（毫秒）
     */
    private Long expiresIn;

    public LoginResponse(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}