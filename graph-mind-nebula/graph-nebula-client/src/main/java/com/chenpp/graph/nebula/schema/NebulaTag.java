package com.chenpp.graph.nebula.schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/7/7 15:15
 */
@Builder
@Data
public class NebulaTag {

    /**
     * 标签名
     */
    private String name;

    /**
     * 属性 Time-To-Live 时间，单位：秒，非负整数，0 表示永不过期
     */
    private Long ttlDuration;

    /**
     * 设置了 TTL 时间的属性，必须是 int64 类型或者 timestamp
     */
    private String ttlCol;

    /**
     * 标签描述
     */
    private String comment;

    /**
     * 标签属性列表
     */
    private List<NebulaProperty> properties;
}
