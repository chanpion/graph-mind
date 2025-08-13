package com.chenpp.graph.admin.model;

import com.chenpp.graph.admin.constant.Constants;
import lombok.Data;

import java.io.Serializable;

/**
 * @author April.Chen
 * @date 2025/8/1 17:47
 */
@Data
public class LoginResponse implements Serializable {
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 令牌类型
     */
    private String tokenType = Constants.BEARER_PREFIX.trim();

    /**
     * 过期时间（毫秒）
     */
    private Long expiresIn;

    private String username;

    public LoginResponse(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}