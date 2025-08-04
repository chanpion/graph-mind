package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 *
 * @author April.Chen
 * @date 2025/8/4 10:45
 */
@Data
@TableName("sys_operation_log")
public class OperationLog {
    /**
     * 日志ID
     */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 操作描述
     */
    @TableField("operation")
    private String operation;

    /**
     * 请求方法
     */
    @TableField("method")
    private String method;

    /**
     * 请求URI
     */
    @TableField("uri")
    private String uri;

    /**
     * 请求参数
     */
    @TableField("params")
    private String params;

    /**
     * 响应结果
     */
    @TableField("result")
    private String result;

    /**
     * 请求IP
     */
    @TableField("ip")
    private String ip;

    /**
     * 请求耗时(毫秒)
     */
    @TableField("time")
    private Long time;

    /**
     * 状态码
     */
    @TableField("status_code")
    private Integer statusCode;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}