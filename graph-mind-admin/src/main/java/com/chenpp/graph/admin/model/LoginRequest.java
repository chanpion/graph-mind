package com.chenpp.graph.admin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author April.Chen
 * @date 2025/8/1 17:45
 */
@Data
public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}