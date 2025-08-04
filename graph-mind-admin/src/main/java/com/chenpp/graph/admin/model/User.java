package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

/**
 * @author April.Chen
 * @date 2025/8/1 10:10
 */
@Data
@TableName("sys_user")
public class User {
    /**
     * 用户ID
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 密码（加密后存储）
     */
    @TableField
    private String password;

    /**
     * 原始密码（用于接收明文密码并加密）
     */
    @TableField(exist = false)
    private transient String rawPassword;

    /**
     * 手机号码
     */
    @TableField("phone_number")
    private String phoneNumber;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 性别（0:男 1:女 2:未知）
     */
    @TableField("sex")
    private Integer sex;

    /**
     * 状态（0:正常 1:停用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * Encodes the raw password using BCryptPasswordEncoder.
     */
    public void encodePassword() {
        if (rawPassword != null && !rawPassword.isEmpty()) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            this.password = encoder.encode(rawPassword);
        }
    }
}