package com.chenpp.graph.nebula.schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author April.Chen
 * @date 2025/7/8 15:57
 */
@Builder
@Data
public class NebulaIndex {
    /**
     * 索引类型
     */
    private SchemaType indexType;

    /**
     * 索引名
     */
    private String indexName;

    /**
     * 索引目标类型名 标签名 或者 边类型名
     */
    private String typeName;

    /**
     * 索引目标类型属性名列表，如果是变长字符串类型，需要处理成 propname(len) 格式
     */
    private List<String> propNameList;
}
