package com.chenpp.graph.core.schema;

import lombok.Data;

/**
 * 属性定义
 *
 * @author April.Chen
 * @date 2023/10/11 3:51 下午
 **/
@Data
public class GraphProperty {
    /**
     * 唯一标识
     */
    public String uid;
    /**
     * 标识
     */
    private String code;
    /**
     * 名称
     */
    private String name;

    /**
     * 发布状态：true=已发布 false=未发布
     */
    private Boolean deployed;

    /**
     * 数据类型
     */
    private DataType dataType;

    /**
     * 属性值的复合类型，包括：single、list、set
     */
    private ValueCardinality valueCardinality;

    private Boolean isWriteGraph;

    /**
     * 是否允许为空
     */
    private Boolean nullable;

}
