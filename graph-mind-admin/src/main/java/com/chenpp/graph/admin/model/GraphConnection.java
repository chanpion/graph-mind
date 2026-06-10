package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chenpp.graph.admin.enums.GraphTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图数据库连接配置信息
 *
 * @author April.Chen
 * @date 2025/8/1 15:57
 */
@TableName("graph_connection")
@Data
public class GraphConnection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    @TableField(value = "graph_type")
    private GraphTypeEnum graphType;
    private String hosts;
    private Integer port;
    private String username;
    private String password;
    /**
     * 0: 未检测, 1: 通过, 2: 失败
     */
    private Integer status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /**
     * json参数
     */
    private String params;
}