package com.chenpp.graph.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 路径查询参数
 * 用于封装图数据库中的路径查询条件
 *
 * @author April.Chen
 * @date 2026/6/23
 */

@Data
public class PathQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Condition startProperty;
    private Condition endProperty;
    private int maxDepth = 5;
    private int limit = 10;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition implements Serializable {
        private static final long serialVersionUID = 1L;

        private String label;
        private String property;
        private String value;
    }
}
