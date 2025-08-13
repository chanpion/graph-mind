package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图实体类
 *
 * @author April.Chen
 * @date 2025/8/1 16:31
 */
@TableName("graph")
@Data
public class Graph {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String description;

    /**
     * 状态
     * 0: 未发布
     * 1: 已发布
     */
    private Integer status;

    /**
     * 关联的图数据库连接ID
     */
    private Long connectionId;

    /**
     * 创建人
     */
    private String creator;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}