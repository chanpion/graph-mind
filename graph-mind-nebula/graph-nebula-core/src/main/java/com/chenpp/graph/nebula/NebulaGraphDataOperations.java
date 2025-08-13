package com.chenpp.graph.nebula;

import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.exception.GraphException;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/8/13 11:18
 */
@Slf4j
public class NebulaGraphDataOperations implements GraphDataOperations {
    private NebulaConf nebulaConf;
    private SessionPool sessionPool;

    public NebulaGraphDataOperations(NebulaConf nebulaConf) {
        this.nebulaConf = nebulaConf;
        this.sessionPool = NebulaClientFactory.getSessionPool(nebulaConf);
    }

    @Override
    public GraphVertex addVertex(GraphVertex vertex) throws GraphException {
        try {
            // 构建插入顶点的NGQL语句
            StringBuilder builder = new StringBuilder();
            builder.append("INSERT VERTEX ").append(vertex.getLabel()).append(" (");
            
            // 添加属性键
            if (vertex.getProperties() != null && !vertex.getProperties().isEmpty()) {
                String propKeys = String.join(", ", vertex.getProperties().keySet());
                builder.append(propKeys);
            }
            
            builder.append(") VALUES \"").append(vertex.getUid()).append("\":(");
            
            // 添加属性值
            if (vertex.getProperties() != null && !vertex.getProperties().isEmpty()) {
                String propValues = vertex.getProperties().values().stream()
                        .map(this::formatValue)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(propValues);
            }
            
            builder.append(");");
            
            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);
            
            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to add vertex, errorCode: " + resultSet.getErrorCode() 
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
            
            return vertex;
        } catch (IOErrorException e) {
            throw new GraphException("Failed to add vertex due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to add vertex", e);
        }
    }

    @Override
    public GraphVertex updateVertex(GraphVertex vertex) throws GraphException {
        // Nebula中更新顶点使用UPDATE语法
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE VERTEX ON ").append(vertex.getLabel()).append(" \"")
                   .append(vertex.getUid()).append("\" ");
            
            // 添加SET子句
            if (vertex.getProperties() != null && !vertex.getProperties().isEmpty()) {
                builder.append("SET ");
                String setClause = vertex.getProperties().entrySet().stream()
                        .map(entry -> entry.getKey() + " = " + formatValue(entry.getValue()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(setClause);
            }
            
            builder.append(";");
            
            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);
            
            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to update vertex, errorCode: " + resultSet.getErrorCode() 
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
            
            return vertex;
        } catch (IOErrorException e) {
            throw new GraphException("Failed to update vertex due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to update vertex", e);
        }
    }

    @Override
    public void addVertices(Collection<GraphVertex> vertices) throws GraphException {
        if (CollectionUtils.isEmpty(vertices)) {
            return;
        }
        
        // 批量插入顶点
        for (GraphVertex vertex : vertices) {
            addVertex(vertex);
        }
    }

    @Override
    public void deleteVertex(GraphVertex vertex) throws GraphException {
        try {
            // 删除顶点及其关联的边
            String nql = "DELETE VERTEX \"" + vertex.getUid() + "\" WITH EDGE;";
            log.info("Execute NGQL: {}", nql);
            
            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to delete vertex, errorCode: " + resultSet.getErrorCode() 
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
        } catch (IOErrorException e) {
            throw new GraphException("Failed to delete vertex due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to delete vertex", e);
        }
    }

    @Override
    public void addEdge(GraphEdge edge) throws GraphException {
        try {
            // 构建插入边的NGQL语句
            StringBuilder builder = new StringBuilder();
            builder.append("INSERT EDGE ").append(edge.getLabel()).append(" (");
            
            // 添加属性键
            if (edge.getProperties() != null && !edge.getProperties().isEmpty()) {
                String propKeys = String.join(", ", edge.getProperties().keySet());
                builder.append(propKeys);
            }
            
            builder.append(") VALUES \"").append(edge.getStartUid()).append("\" -> \"")
                   .append(edge.getEndUid()).append("\":(");
            
            // 添加属性值
            if (edge.getProperties() != null && !edge.getProperties().isEmpty()) {
                String propValues = edge.getProperties().values().stream()
                        .map(this::formatValue)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(propValues);
            }
            
            builder.append(");");
            
            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);
            
            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to add edge, errorCode: " + resultSet.getErrorCode() 
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
        } catch (IOErrorException e) {
            throw new GraphException("Failed to add edge due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to add edge", e);
        }
    }

    @Override
    public void addEdges(Collection<GraphEdge> edges) throws GraphException {
        if (CollectionUtils.isEmpty(edges)) {
            return;
        }
        
        // 批量插入边
        for (GraphEdge edge : edges) {
            addEdge(edge);
        }
    }

    @Override
    public int updateEdge(GraphEdge edge) throws GraphException {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE EDGE ON ").append(edge.getLabel()).append(" \"")
                   .append(edge.getStartUid()).append("\" -> \"").append(edge.getEndUid()).append("\" ");
            
            // 添加SET子句
            if (edge.getProperties() != null && !edge.getProperties().isEmpty()) {
                builder.append("SET ");
                String setClause = edge.getProperties().entrySet().stream()
                        .map(entry -> entry.getKey() + " = " + formatValue(entry.getValue()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                builder.append(setClause);
            }
            
            builder.append(";");
            
            String nql = builder.toString();
            log.info("Execute NGQL: {}", nql);
            
            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to update edge, errorCode: " + resultSet.getErrorCode() 
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
            
            return 1; // Nebula更新操作成功返回1
        } catch (IOErrorException e) {
            throw new GraphException("Failed to update edge due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to update edge", e);
        }
    }

    @Override
    public int deleteEdge(GraphEdge edge) throws GraphException {
        try {
            String nql = "DELETE EDGE " + edge.getLabel() + " \"" + edge.getStartUid() 
                    + "\" -> \"" + edge.getEndUid() + "\";";
            log.info("Execute NGQL: {}", nql);
            
            ResultSet resultSet = sessionPool.execute(nql);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to delete edge, errorCode: " + resultSet.getErrorCode() 
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
            
            return 1; // 成功删除
        } catch (IOErrorException e) {
            throw new GraphException("Failed to delete edge due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to delete edge", e);
        }
    }

    @Override
    public GraphData query(String cypher) throws GraphException {
        try {
            log.info("Execute NGQL: {}", cypher);
            
            ResultSet resultSet = sessionPool.execute(cypher);
            if (!resultSet.isSucceeded()) {
                throw new GraphException("Failed to execute query, errorCode: " + resultSet.getErrorCode() 
                        + ", errorMessage: " + resultSet.getErrorMessage());
            }
            
            GraphData graphData = new GraphData();
            
            // 解析结果集
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                // 这里需要根据实际查询结果进行解析
                // 由于Nebula的查询结果格式较为复杂，需要根据具体查询语句来解析
                // 此处仅为示例框架
                log.debug("Processing row: {}", i);
            }
            
            return graphData;
        } catch (IOErrorException e) {
            throw new GraphException("Failed to execute query due to IO error", e);
        } catch (Exception e) {
            throw new GraphException("Failed to execute query", e);
        }
    }
    
    /**
     * 格式化属性值，用于构建NGQL语句
     * 
     * @param value 属性值
     * @return 格式化后的字符串
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof String) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        
        if (value instanceof Boolean) {
            return value.toString().toUpperCase();
        }
        
        return value.toString();
    }
}