package com.chenpp.graph.admin.model;

import lombok.Data;

import java.util.List;

/**
 * Schema导入请求DTO
 *
 * @author April.Chen
 */
@Data
public class SchemaImportDTO {

    /**
     * 导入模式: merge(合并/追加) | replace(替换/覆盖)
     */
    private String mode = "merge";

    private List<GraphVertexDef> vertices;

    private List<GraphEdgeDef> edges;
}