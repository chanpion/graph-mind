package com.chenpp.graph.admin.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.service.GraphDataService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphNodeDefService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
import com.chenpp.graph.neo4j.Neo4jClient;
import com.chenpp.graph.neo4j.Neo4jConf;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图数据服务实现类
 * 专门处理图数据的导入、查询等操作
 *
 * @author April.Chen
 * @date 2025/8/11 15:35
 */
@Slf4j
@Service
public class GraphDataServiceImpl implements GraphDataService {

    @Autowired
    private GraphService graphService;

    @Autowired
    private GraphNodeDefService nodeDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphDatabaseConnectionService connectionService;

    @Override
    public ImportResult importNodeData(Long graphId, Long nodeTypeId, MultipartFile file, String headers, String mapping, String data) {
        ImportResult result = new ImportResult();
        result.setTotalCount(0);
        result.setSuccessCount(0);
        result.setFailureCount(0);
        List<String> errorMessages = new ArrayList<>();

        try {
            // 获取图信息
            Graph graph = graphService.getById(graphId);
            if (graph == null) {
                log.error("图不存在，graphId={}", graphId);
                errorMessages.add("图不存在，graphId=" + graphId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 获取图数据库连接信息
            GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graph.getConnectionId());
                errorMessages.add("图数据库连接不存在，connectionId=" + graph.getConnectionId());
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 检查是否为Neo4j数据库
            if (!"neo4j".equalsIgnoreCase(connection.getType())) {
                log.error("暂不支持的数据库类型: {}", connection.getType());
                errorMessages.add("暂不支持的数据库类型: " + connection.getType());
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 获取节点定义信息
            GraphNodeDef nodeDef = nodeDefService.getById(nodeTypeId);
            if (nodeDef == null) {
                log.error("节点类型不存在，nodeTypeId={}", nodeTypeId);
                errorMessages.add("节点类型不存在，nodeTypeId=" + nodeTypeId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 解析映射关系
            ObjectMapper objectMapper = new ObjectMapper();
            JSONObject mappingMap = JSON.parseObject(mapping);

            // 逐行解析CSV文件
            List<Map<String, String>> dataList = parseCsvFile(file);

            // 更新导入结果统计
            result.setTotalCount(dataList.size());

            GraphDataOperations graphDataOperations = setupNeo4jClient(connection, graph);

            // 批量导入节点数据
            List<GraphVertex> vertices = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Map<String, String> rowData : dataList) {
                try {
                    GraphVertex vertex = new GraphVertex();
                    vertex.setLabel(nodeDef.getLabel());
                    // 设置属性
                    Map<String, Object> properties = new HashMap<>(mappingMap.size());
                    mappingMap.forEach((prop, csvColumn) -> {
                        String value = rowData.get(csvColumn);
                        // 如果映射到uid，则设置为节点的uid
                        if ("uid".equals(prop)) {
                            // 从数据中获取唯一标识符，如果没有则生成
                            if (StringUtils.isBlank(value)) {
                                value = java.util.UUID.randomUUID().toString();
                            }
                            vertex.setUid(value);
                        } else {
                            properties.put(prop, value);
                        }
                    });
                    properties.put("uid", vertex.getUid());
                    vertex.setProperties(properties);
                    vertices.add(vertex);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorMessages.add("处理行数据失败: " + e.getMessage());
                    log.error("处理行数据失败", e);
                }
            }

            // 批量添加节点
            try {
                graphDataOperations.addVertices(vertices);
            } catch (Exception e) {
                log.error("批量添加节点失败", e);
                errorMessages.add("批量添加节点失败: " + e.getMessage());
                failureCount = vertices.size();
                successCount = 0;
            }

            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            result.setErrorMessages(errorMessages.toArray(new String[0]));

            log.info("导入节点数据完成，总数={}，成功={}，失败={}", dataList.size(), successCount, failureCount);
        } catch (Exception e) {
            handleError(result, errorMessages, "导入节点数据失败", e);
        }

        return result;
    }

    /**
     * 设置Neo4j客户端
     *
     * @param connection 图数据库连接信息
     * @param graph      图信息
     * @return GraphDataOperations
     */
    private GraphDataOperations setupNeo4jClient(GraphDatabaseConnection connection, Graph graph) {
        Neo4jConf neo4jConf = new Neo4jConf();
        neo4jConf.setUri(String.format("neo4j://%s:%d", connection.getHost(), connection.getPort()));
        neo4jConf.setUsername(connection.getUsername());
        neo4jConf.setPassword(connection.getPassword());
        neo4jConf.setGraphCode(graph.getCode());

        GraphClient graphClient = new Neo4jClient(neo4jConf);
        return graphClient.opsForGraphData();
    }

    /**
     * 逐行解析CSV文件
     *
     * @param file CSV文件
     * @return 解析后的数据列表
     */
    private List<Map<String, String>> parseCsvFile(MultipartFile file) throws Exception {
        List<Map<String, String>> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // 读取表头
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV文件为空");
            }

            String[] headers = headerLine.split(",");
            // 清理表头字段（去除空格和引号）
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().replaceAll("^\"|\"$", "");
            }

            // 逐行读取数据
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, String> rowData = new HashMap<>();

                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String value = values[i].trim().replaceAll("^\"|\"$", "");
                    rowData.put(headers[i], value);
                }

                dataList.add(rowData);
            }
        }

        return dataList;
    }

    /**
     * 处理错误
     *
     * @param result        导入结果
     * @param errorMessages 错误消息列表
     * @param message       错误消息
     * @param e             异常
     */
    private void handleError(ImportResult result, List<String> errorMessages, String message, Exception e) {
        log.error(message, e);
        errorMessages.add(message + ": " + e.getMessage());
        result.setErrorMessages(errorMessages.toArray(new String[0]));
    }

    @Override
    public ImportResult importEdgeData(Long graphId, Long edgeTypeId, MultipartFile file, String headers, String mapping, String data) {
        ImportResult result = new ImportResult();
        result.setTotalCount(0);
        result.setSuccessCount(0);
        result.setFailureCount(0);
        List<String> errorMessages = new ArrayList<>();

        try {
            // 获取图信息
            Graph graph = graphService.getById(graphId);
            if (graph == null) {
                log.error("图不存在，graphId={}", graphId);
                errorMessages.add("图不存在，graphId=" + graphId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 获取图数据库连接信息
            GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graph.getConnectionId());
                errorMessages.add("图数据库连接不存在，connectionId=" + graph.getConnectionId());
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }


            // 获取边定义信息
            GraphEdgeDef edgeDef = edgeDefService.getById(edgeTypeId);
            if (edgeDef == null) {
                log.error("边类型不存在，edgeTypeId={}", edgeTypeId);
                errorMessages.add("边类型不存在，edgeTypeId=" + edgeTypeId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 解析映射关系
            ObjectMapper objectMapper = new ObjectMapper();
            JSONObject mappingMap = JSON.parseObject(mapping);

            // 逐行解析CSV文件
            List<Map<String, String>> dataList = parseCsvFile(file);

            // 更新导入结果统计
            result.setTotalCount(dataList.size());

            // 构建Neo4j客户端
            Neo4jConf neo4jConf = new Neo4jConf();
            neo4jConf.setUri(String.format("neo4j://%s:%d", connection.getHost(), connection.getPort()));
            neo4jConf.setUsername(connection.getUsername());
            neo4jConf.setPassword(connection.getPassword());
            neo4jConf.setGraphCode(graph.getCode());

            GraphClient graphClient = new Neo4jClient(neo4jConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            // 更新导入结果统计
            result.setTotalCount(dataList.size());

            // 批量导入边数据
            List<GraphEdge> edges = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Map<String, String> rowData : dataList) {
                try {
                    GraphEdge edge = new GraphEdge();
                    edge.setLabel(edgeDef.getLabel());
                    // 设置属性
                    Map<String, Object> properties = new HashMap<>(mappingMap.size());
                    mappingMap.forEach((prop, csvColumn) -> {
                        String value = rowData.get(csvColumn);
                        // 如果映射到uid，则设置为边的uid
                        if ("uid".equals(prop)) {
                            // 从数据中获取唯一标识符，如果没有则生成
                            if (StringUtils.isBlank(value)) {
                                value = java.util.UUID.randomUUID().toString();
                            }
                            edge.setUid(value);
                        } else if ("source".equals(prop)) {
                            edge.setStartUid(value);
                        } else if ("sourceLabel".equals(prop)) {
                            edge.setStartLabel(value);
                        } else if ("target".equals(prop)) {
                            edge.setEndUid(value);
                        } else if ("targetLabel".equals(prop)) {
                            edge.setEndLabel(value);
                        } else {
                            properties.put(prop, value);
                        }
                    });
                    if (StringUtils.isNoneBlank(edge.getStartUid(), edge.getEndUid())) {
                        properties.put("uid", edge.getUid());
                        edge.setProperties(properties);
                        edges.add(edge);
                        successCount++;
                    } else {
                        failureCount++;
                        errorMessages.add("处理行数据失败: 缺少起点或终点信息");
                    }

                } catch (Exception e) {
                    failureCount++;
                    errorMessages.add("处理行数据失败: " + e.getMessage());
                    log.error("处理行数据失败", e);
                }
            }

            // 批量添加边
            try {
                graphDataOperations.addEdges(edges);
            } catch (Exception e) {
                log.error("批量添加边失败", e);
                errorMessages.add("批量添加边失败: " + e.getMessage());
                failureCount = edges.size();
                successCount = 0;
            }

            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            result.setErrorMessages(errorMessages.toArray(new String[0]));

            log.info("导入边数据完成，总数={}，成功={}，失败={}", dataList.size(), successCount, failureCount);
        } catch (Exception e) {
            handleError(result, errorMessages, "导入边数据失败", e);
        }

        return result;
    }
}