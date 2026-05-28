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
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
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
    public List<GraphVertex> getNodeDataList(Long graphId, Long nodeTypeId, Integer page, Integer size) {
        try {
            GraphNodeDef nodeDef = nodeDefService.getById(nodeTypeId);
            if (nodeDef == null) {
                log.error("节点类型不存在，nodeTypeId={}", nodeTypeId);
                return new ArrayList<>();
            }

            GraphDataOperations ops = getGraphDataOperations(graphId);
            String label = nodeDef.getLabel();
            int skip = (page - 1) * size;

            // 根据图类型构建不同的查询语句
            String query = buildLabelQuery(graphId, label, skip, size);
            GraphData graphData = ops.query(query);

            if (graphData == null || graphData.getVertices() == null) {
                return new ArrayList<>();
            }

           return graphData.getVertices();
        } catch (Exception e) {
            log.error("查询节点数据列表失败，graphId={}, nodeTypeId={}", graphId, nodeTypeId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getEdgeDataList(Long graphId, Long edgeTypeId, Integer page, Integer size) {
        try {
            GraphEdgeDef edgeDef = edgeDefService.getById(edgeTypeId);
            if (edgeDef == null) {
                log.error("边类型不存在，edgeTypeId={}", edgeTypeId);
                return new ArrayList<>();
            }

            GraphDataOperations ops = getGraphDataOperations(graphId);
            String label = edgeDef.getLabel();
            int skip = (page - 1) * size;

            String query = buildEdgeLabelQuery(graphId, label, skip, size);
            GraphData graphData = ops.query(query);

            if (graphData == null || graphData.getEdges() == null) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (GraphEdge edge : graphData.getEdges()) {
                result.add(edgeToMap(edge));
            }
            return result;
        } catch (Exception e) {
            log.error("查询边数据列表失败，graphId={}, edgeTypeId={}", graphId, edgeTypeId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getNodeData(Long graphId, String nodeId) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);
            String query = buildFindVertexQuery(graphId, nodeId);
            GraphData graphData = ops.query(query);

            if (graphData != null && graphData.getVertices() != null && !graphData.getVertices().isEmpty()) {
                return vertexToMap(graphData.getVertices().get(0));
            }
            return new HashMap<>();
        } catch (Exception e) {
            log.error("获取节点数据详情失败，graphId={}, nodeId={}", graphId, nodeId, e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getEdgeData(Long graphId, String edgeId) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);
            String query = buildFindEdgeQuery(graphId, edgeId);
            GraphData graphData = ops.query(query);

            if (graphData != null && graphData.getEdges() != null && !graphData.getEdges().isEmpty()) {
                return edgeToMap(graphData.getEdges().get(0));
            }
            return new HashMap<>();
        } catch (Exception e) {
            log.error("获取边数据详情失败，graphId={}, edgeId={}", graphId, edgeId, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean addNodeData(Long graphId, Long nodeTypeId, Map<String, Object> data) {
        try {
            GraphNodeDef nodeDef = nodeDefService.getById(nodeTypeId);
            if (nodeDef == null) {
                log.error("节点类型不存在，nodeTypeId={}", nodeTypeId);
                return false;
            }

            GraphDataOperations ops = getGraphDataOperations(graphId);

            GraphVertex vertex = new GraphVertex();
            vertex.setLabel(nodeDef.getLabel());
            vertex.setProperties(data);
            if (data.containsKey("uid")) {
                vertex.setUid(data.get("uid").toString());
            }

            ops.addVertex(vertex);
            log.info("新增节点成功，label={}, uid={}", nodeDef.getLabel(), vertex.getUid());
            return true;
        } catch (Exception e) {
            log.error("新增节点数据失败，graphId={}, nodeTypeId={}", graphId, nodeTypeId, e);
            return false;
        }
    }

    @Override
    public boolean addEdgeData(Long graphId, Long edgeTypeId, Map<String, Object> data) {
        try {
            GraphEdgeDef edgeDef = edgeDefService.getById(edgeTypeId);
            if (edgeDef == null) {
                log.error("边类型不存在，edgeTypeId={}", edgeTypeId);
                return false;
            }

            GraphDataOperations ops = getGraphDataOperations(graphId);

            GraphEdge edge = new GraphEdge();
            edge.setLabel(edgeDef.getLabel());
            edge.setProperties(data);
            if (data.containsKey("uid")) {
                edge.setUid(data.get("uid").toString());
            }
            if (data.containsKey("startUid")) {
                edge.setStartUid(data.get("startUid").toString());
            }
            if (data.containsKey("endUid")) {
                edge.setEndUid(data.get("endUid").toString());
            }
            if (data.containsKey("startLabel")) {
                edge.setStartLabel(data.get("startLabel").toString());
            }
            if (data.containsKey("endLabel")) {
                edge.setEndLabel(data.get("endLabel").toString());
            }

            ops.addEdge(edge);
            log.info("新增边成功，label={}, uid={}", edgeDef.getLabel(), edge.getUid());
            return true;
        } catch (Exception e) {
            log.error("新增边数据失败，graphId={}, edgeTypeId={}", graphId, edgeTypeId, e);
            return false;
        }
    }

    @Override
    public boolean updateNodeData(Long graphId, String nodeId, Map<String, Object> data) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);

            GraphVertex vertex = new GraphVertex();
            vertex.setUid(nodeId);
            if (data.containsKey("label")) {
                vertex.setLabel(data.get("label").toString());
            }
            vertex.setProperties(data);

            ops.updateVertex(vertex);
            log.info("更新节点成功，nodeId={}", nodeId);
            return true;
        } catch (Exception e) {
            log.error("更新节点数据失败，graphId={}, nodeId={}", graphId, nodeId, e);
            return false;
        }
    }

    @Override
    public boolean updateEdgeData(Long graphId, String edgeId, Map<String, Object> data) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);

            GraphEdge edge = new GraphEdge();
            edge.setUid(edgeId);
            if (data.containsKey("label")) {
                edge.setLabel(data.get("label").toString());
            }
            edge.setProperties(data);

            ops.updateEdge(edge);
            log.info("更新边成功，edgeId={}", edgeId);
            return true;
        } catch (Exception e) {
            log.error("更新边数据失败，graphId={}, edgeId={}", graphId, edgeId, e);
            return false;
        }
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

    @Override
    public GraphSummary getGraphSummary(Long graphId) {
        try {
            // 获取图信息
            Graph graph = graphService.getById(graphId);
            if (graph == null) {
                log.error("图不存在，graphId={}", graphId);
                throw new RuntimeException("图不存在，graphId=" + graphId);
            }

            // 获取图数据库连接信息
            GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graph.getConnectionId());
                throw new RuntimeException("图数据库连接不存在，connectionId=" + graph.getConnectionId());
            }

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);
            
            // 创建图客户端
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            
            // 获取图数据操作接口
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();
            
            // 获取图统计信息
            GraphSummary summary = graphDataOperations.getSummary();
            
            // 设置图的基本信息
            summary.setGraphCode(graph.getCode());
            summary.setGraphName(graph.getName());
            
            return summary;
        } catch (Exception e) {
            log.error("获取图统计信息失败，graphId={}", graphId, e);
            throw new RuntimeException("获取图统计信息失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取图数据操作接口
     */
    private GraphDataOperations getGraphDataOperations(Long graphId) {
        Graph graph = graphService.getById(graphId);
        if (graph == null) {
            throw new RuntimeException("图不存在，graphId=" + graphId);
        }

        GraphDatabaseConnection connection = connectionService.getById(graph.getConnectionId());
        if (connection == null) {
            throw new RuntimeException("图数据库连接不存在，connectionId=" + graph.getConnectionId());
        }

        GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graph);
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        return graphClient.opsForGraphData();
    }

    /**
     * 构建按标签分页查询节点的 Cypher 语句
     */
    private String buildLabelQuery(Long graphId, String label, int skip, int size) {
        return String.format("MATCH (n:`%s`) RETURN n SKIP %d LIMIT %d", label, skip, size);
    }

    /**
     * 构建按标签分页查询边的 Cypher 语句
     */
    private String buildEdgeLabelQuery(Long graphId, String label, int skip, int size) {
        return String.format("MATCH p=()-[r:`%s`]->() RETURN p SKIP %d LIMIT %d", label, skip, size);
    }

    /**
     * 构建按 uid 查找节点的 Cypher 语句
     */
    private String buildFindVertexQuery(Long graphId, String nodeId) {
        return String.format("MATCH (n {uid: '%s'}) RETURN n", escapeCypherString(nodeId));
    }

    /**
     * 构建按 uid 查找边的 Cypher 语句
     */
    private String buildFindEdgeQuery(Long graphId, String edgeId) {
        return String.format("MATCH ()-[r {uid: '%s'}]-() RETURN r", escapeCypherString(edgeId));
    }

    /**
     * 将 GraphVertex 转为 Map
     */
    private Map<String, Object> vertexToMap(GraphVertex vertex) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", vertex.getId());
        map.put("uid", vertex.getUid());
        map.put("label", vertex.getLabel());
        if (vertex.getProperties() != null) {
            map.putAll(vertex.getProperties());
        }
        return map;
    }

    /**
     * 将 GraphEdge 转为 Map
     */
    private Map<String, Object> edgeToMap(GraphEdge edge) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", edge.getId());
        map.put("uid", edge.getUid());
        map.put("label", edge.getLabel());
        map.put("startUid", edge.getStartUid());
        map.put("startLabel", edge.getStartLabel());
        map.put("endUid", edge.getEndUid());
        map.put("endLabel", edge.getEndLabel());
        if (edge.getProperties() != null) {
            map.put("properties", edge.getProperties());
        }
        return map;
    }

    /**
     * 转义 Cypher 字符串中的特殊字符（单引号）
     */
    private String escapeCypherString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}