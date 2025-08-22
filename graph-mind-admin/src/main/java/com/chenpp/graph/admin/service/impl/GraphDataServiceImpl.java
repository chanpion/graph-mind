package com.chenpp.graph.admin.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chenpp.graph.admin.model.Graph;
import com.chenpp.graph.admin.model.GraphDatabaseConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphNodeDef;
import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.service.GraphDataService;
import com.chenpp.graph.admin.service.GraphDatabaseConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphNodeDefService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphVertex;
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
            Map<String, String> mappingMap = objectMapper.readValue(mapping, new TypeReference<Map<String, String>>() {
            });

            // 逐行解析CSV文件
            List<Map<String, String>> dataList = parseCsvFile(file);

            // 更新导入结果统计
            result.setTotalCount(dataList.size());

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);

            // 创建图客户端
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            // 批量导入节点数据
            int successCount = 0;
            int failureCount = 0;

            for (Map<String, String> dataRow : dataList) {
                try {
                    GraphVertex vertex = new GraphVertex();
                    vertex.setLabel(nodeDef.getLabel());

                    // 设置节点属性
                    Map<String, Object> properties = new HashMap<>();
                    for (Map.Entry<String, String> entry : mappingMap.entrySet()) {
                        String header = entry.getKey();
                        String propertyName = entry.getValue();
                        if (StringUtils.isNotBlank(propertyName) && dataRow.containsKey(header)) {
                            properties.put(propertyName, dataRow.get(header));
                        }
                    }
                    vertex.setProperties(properties);
                    vertex.setUid(properties.get("uid").toString());
                    // 添加节点
                    graphDataOperations.addVertex(vertex);
                    successCount++;
                } catch (Exception e) {
                    log.error("导入节点数据失败: {}", e.getMessage(), e);
                    failureCount++;
                }
            }

            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            result.setErrorMessages(errorMessages.toArray(new String[0]));

            log.info("导入节点数据完成，总数={}，成功={}，失败={}", dataList.size(), successCount, failureCount);
        } catch (Exception e) {
            log.error("导入节点数据失败", e);
            errorMessages.add("导入节点数据失败: " + e.getMessage());
            result.setErrorMessages(errorMessages.toArray(new String[0]));
        }

        return result;
    }


    /**
     * 解析CSV文件
     *
     * @param file CSV文件
     * @return 解析后的数据列表
     * @throws Exception 解析异常
     */
    private List<Map<String, String>> parseCsvFile(MultipartFile file) throws Exception {
        List<Map<String, String>> dataList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV文件为空");
            }

            String[] headers = headerLine.split(",");
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, String> dataMap = new HashMap<>();
                for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                    dataMap.put(headers[i].trim(), values[i].trim());
                }
                dataList.add(dataMap);
            }
        }
        return dataList;
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

            // 解析映射关系和数据
            JSONObject mappingMap = JSON.parseObject(mapping);
            List<Map<String, String>> dataList = parseCsvFile(file);

            // 更新导入结果统计
            result.setTotalCount(dataList.size());

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);

            // 创建图客户端
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            // 批量导入边数据
            int successCount = 0;
            int failureCount = 0;

            for (Map<String, String> dataRow : dataList) {
                try {
                    GraphEdge edge = new GraphEdge();
                    edge.setUid(dataRow.get(mappingMap.getString("uid")));
                    edge.setLabel(edgeDef.getLabel());
                    edge.setStartLabel(dataRow.get(mappingMap.getString("sourceLabel")));
                    edge.setEndLabel(dataRow.get(mappingMap.getString("targetLabel")));
                    edge.setStartUid(dataRow.get(mappingMap.getString("source")));
                    edge.setEndUid(dataRow.get(mappingMap.getString("target")));

                    // 设置边属性
                    Map<String, Object> properties = new HashMap<>();

                    mappingMap.forEach((col, filed) -> {
                        if (!StringUtils.equalsAny(col, "sourceLabel", "targetLabel", "source", "target")) {
                            properties.put(col, dataRow.get(filed));
                        }
                    });
                    edge.setProperties(properties);

                    // 添加边
                    graphDataOperations.addEdge(edge);
                    successCount++;
                } catch (Exception e) {
                    log.error("导入边数据失败: {}", e.getMessage(), e);
                    failureCount++;
                }
            }

            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            result.setErrorMessages(errorMessages.toArray(new String[0]));

            log.info("导入边数据完成，总数={}，成功={}，失败={}", dataList.size(), successCount, failureCount);
        } catch (Exception e) {
            log.error("导入边数据失败", e);
            errorMessages.add("导入边数据失败: " + e.getMessage());
            result.setErrorMessages(errorMessages.toArray(new String[0]));
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getNodeDataList(Long graphId, Long nodeTypeId, Integer page, Integer size) {
        // TODO: 实现查询节点数据列表逻辑
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getEdgeDataList(Long graphId, Long edgeTypeId, Integer page, Integer size) {
        // TODO: 实现查询边数据列表逻辑
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getNodeData(Long graphId, String nodeId) {
        // TODO: 实现获取节点数据详情逻辑
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getEdgeData(Long graphId, String edgeId) {
        // TODO: 实现获取边数据详情逻辑
        return new HashMap<>();
    }

    @Override
    public boolean addNodeData(Long graphId, Long nodeTypeId, Map<String, Object> data) {
        // TODO: 实现新增节点数据逻辑
        return true;
    }

    @Override
    public boolean addEdgeData(Long graphId, Long edgeTypeId, Map<String, Object> data) {
        // TODO: 实现新增边数据逻辑
        return true;
    }

    @Override
    public boolean updateNodeData(Long graphId, String nodeId, Map<String, Object> data) {
        // TODO: 实现更新节点数据逻辑
        return true;
    }

    @Override
    public boolean updateEdgeData(Long graphId, String edgeId, Map<String, Object> data) {
        // TODO: 实现更新边数据逻辑
        return true;
    }

    @Override
    public boolean deleteNode(Long graphId, String nodeId, String label) {
        List<String> nodeIds = new ArrayList<>();
        nodeIds.add(nodeId);
        return deleteNodes(graphId, nodeIds, label);
    }

    @Override
    public boolean deleteNodes(Long graphId, List<String> nodeIds, String label) {
        if (graphId == null || nodeIds == null || nodeIds.isEmpty()) {
            return false;
        }

        try {
            // 获取图信息
            Graph graph = graphService.getById(graphId);
            if (graph == null) {
                log.error("图不存在，graphId={}", graphId);
                return false;
            }

            // 获取图数据库连接信息
            GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graph.getConnectionId());
                return false;
            }

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);

            // 创建图客户端
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            GraphNodeDef nodeDef = nodeDefService.getOne(new QueryWrapper<GraphNodeDef>().eq("graph_id", graphId).eq("label", label));
            // 删除节点
            for (String nodeId : nodeIds) {
                try {
                    GraphVertex vertex = new GraphVertex();
                    vertex.setUid(nodeId);
                    vertex.setLabel(nodeDef.getLabel());
                    graphDataOperations.deleteVertex(vertex);
                    log.info("成功删除节点，nodeId={}", nodeId);
                } catch (Exception e) {
                    log.error("删除节点失败，nodeId={}", nodeId, e);
                    // 继续删除其他节点，不因单个节点失败而中断整个过程
                }
            }

            return true;
        } catch (Exception e) {
            log.error("批量删除节点失败，graphId={}", graphId, e);
            return false;
        }
    }
}