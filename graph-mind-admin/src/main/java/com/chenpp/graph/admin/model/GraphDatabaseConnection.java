package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图数据库连接配置信息
 *
 * @author April.Chen
 * @date 2025/8/1 15:57
 */
@TableName("graph_database_connection")
@Data
public class GraphDatabaseConnection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String type;
    private String host;
    private Integer port;
    @TableField("`database`")
    private String database;
    private String username;
    private String password;
    /**
     * 0: disconnected, 1: connected, 2: connecting
     */
    private Integer status;
    private Integer poolSize;
    private Integer timeout;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}