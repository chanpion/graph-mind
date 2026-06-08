package com.chenpp.graph.admin.model;

import lombok.Data;

import java.util.List;

/**
 * Schema导出响应DTO
 *
 * @author April.Chen
 */
@Data
public class SchemaExportDTO {

    private String exportedAt;
    private Long graphId;
    private String graphCode;
    private List<GraphVertexDef> nodes;
    private List<GraphEdgeDef> edges;
}