package com.chenpp.graph.admin.model;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;

/**
 * 图查询请求参数
 *
 * @author April.Chen
 * @date 2026/6/22
 */
@Data
public class GraphQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    Long graphId;
    Long connectionId;
    String graphCode;

    /**
     * 查询语句
     */
    private String query;
}