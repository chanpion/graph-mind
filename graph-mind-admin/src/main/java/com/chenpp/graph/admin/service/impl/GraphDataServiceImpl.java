package com.chenpp.graph.admin.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chenpp.graph.admin.enums.GraphTypeEnum;
import com.chenpp.graph.admin.model.GraphInfo;
import com.chenpp.graph.admin.model.GraphConnection;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import com.chenpp.graph.admin.model.GraphVertexDef;
import com.chenpp.graph.admin.model.GraphPropertyDef;
import com.chenpp.graph.admin.model.ImportResult;
import com.chenpp.graph.admin.model.PageResult;
import com.chenpp.graph.admin.service.GraphDataService;
import com.chenpp.graph.admin.service.GraphConnectionService;
import com.chenpp.graph.admin.service.GraphEdgeDefService;
import com.chenpp.graph.admin.service.GraphVertexDefService;
import com.chenpp.graph.admin.service.GraphPropertyDefService;
import com.chenpp.graph.admin.service.GraphService;
import com.chenpp.graph.admin.util.GraphClientFactory;
import com.chenpp.graph.core.GraphClient;
import com.chenpp.graph.core.GraphDataOperations;
import com.chenpp.graph.core.model.GraphConf;
import com.chenpp.graph.core.model.GraphData;
import com.chenpp.graph.core.model.GraphEdge;
import com.chenpp.graph.core.model.GraphSummary;
import com.chenpp.graph.core.model.GraphVertex;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图数据服务实现类
 * 专门处理图数据的导入、查询等操作
 *
 * @author April.Chen
 * @date 2025/8/11 15:35
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GraphDataServiceImpl implements GraphDataService {

    @Autowired
    private GraphService graphService;

    @Autowired
    private GraphVertexDefService vertexDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphConnectionService connectionService;

    @Autowired
    private GraphPropertyDefService propertyDefService;

    @Override
    public ImportResult importNodeData(Long graphId, Long vertexTypeId, MultipartFile file, String config) {
        ImportResult result = new ImportResult();
        result.setTotalCount(0);
        result.setSuccessCount(0);
        result.setFailureCount(0);
        List<String> errorMessages = new ArrayList<>();

        try {
            // 获取图信息
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                log.error("图不存在，graphId={}", graphId);
                errorMessages.add("图不存在，graphId=" + graphId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 获取图数据库连接信息
            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                errorMessages.add("图数据库连接不存在，connectionId=" + graphInfo.getConnectionId());
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 获取节点定义信息
            GraphVertexDef vertexDef = vertexDefService.getById(vertexTypeId);
            if (vertexDef == null) {
                log.error("节点类型不存在，vertexTypeId={}", vertexTypeId);
                errorMessages.add("节点类型不存在，vertexTypeId=" + vertexTypeId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 解析配置（含 mapping、delimiter、hasHeader 等）
            JSONObject configJson = JSON.parseObject(config);
            String delimiter = configJson.getString("delimiter");
            if (StringUtils.isBlank(delimiter)) {
                delimiter = ",";
            }

            // 逐行解析CSV文件
            List<Map<String, String>> dataList = parseCsvFile(file, delimiter);

            // 获取节点定义的属性code列表，用于过滤不在schema中的属性
            QueryWrapper<GraphPropertyDef> propQuery = new QueryWrapper<GraphPropertyDef>()
                    .eq("entity_id", vertexTypeId)
                    .eq("property_type", "vertex");
            List<GraphPropertyDef> propDefs = propertyDefService.list(propQuery);
            Set<String> vertexSchemaPropertyCodes = propDefs.stream()
                    .map(GraphPropertyDef::getCode)
                    .collect(Collectors.toSet());

            // 更新导入结果统计
            result.setTotalCount(dataList.size());

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());

            // 创建图客户端并批量导入节点数据
            try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

                // 批量导入节点数据
                int successCount = 0;
                int failureCount = 0;

                for (Map<String, String> dataRow : dataList) {
                    try {
                        GraphVertex vertex = new GraphVertex();
                        vertex.setUid(dataRow.get("uid"));
                        vertex.setLabel(dataRow.get("label"));

                        // 设置节点属性
                        Map<String, Object> properties = new HashMap<>();

                        dataRow.forEach((key, value) -> {
                            if (vertexSchemaPropertyCodes.contains(key)) {
                                properties.put(key, value);
                            }
                        });
                        vertex.setProperties(properties);
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
            }
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
     * @param file      CSV文件
     * @param delimiter 分隔符
     * @return 解析后的数据列表
     * @throws Exception 解析异常
     */
    private List<Map<String, String>> parseCsvFile(MultipartFile file, String delimiter) throws Exception {
        List<Map<String, String>> dataList = new ArrayList<>();
        // 先读取原始字节，用于编码回退
        byte[] rawBytes = file.getBytes();

        // 先尝试 UTF-8 解析，若出现乱码则回退到 GBK
        String content = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
        if (containsGarbledChinese(content)) {
            content = new String(rawBytes, java.nio.charset.Charset.forName("GBK"));
        }

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV文件为空");
            }

            String[] headers = headerLine.split(delimiter);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(delimiter);
                Map<String, String> dataMap = new HashMap<>();
                for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                    dataMap.put(headers[i].trim(), values[i].trim());
                }
                dataList.add(dataMap);
            }
        }
        return dataList;
    }

    /**
     * 检测字符串中是否包含中文乱码（出现非法 UTF-8 替换字符 U+FFFD）
     */
    private boolean containsGarbledChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // 检查是否包含 UTF-8 替换字符
        if (text.contains("\uFFFD")) {
            return true;
        }
        // 检查是否包含非法的乱码序列（如连续多个不可打印字符）
        int strangeCount = 0;
        for (char c : text.toCharArray()) {
            if (c == '\uFFFD' || (c > 0x7F && c < 0xA0)) {
                strangeCount++;
            }
        }
        return strangeCount > 3;
    }


    @Override
    public ImportResult importEdgeData(Long graphId, Long edgeTypeId, MultipartFile file, String config) {
        ImportResult result = new ImportResult();
        result.setTotalCount(0);
        result.setSuccessCount(0);
        result.setFailureCount(0);
        List<String> errorMessages = new ArrayList<>();

        try {
            // 获取图信息
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                log.error("图不存在，graphId={}", graphId);
                errorMessages.add("图不存在，graphId=" + graphId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            // 获取图数据库连接信息
            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                errorMessages.add("图数据库连接不存在，connectionId=" + graphInfo.getConnectionId());
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
            List<Map<String, String>> dataList = parseCsvFile(file, ",");

            // 获取边定义的属性code列表，用于过滤不在schema中的属性
            QueryWrapper<GraphPropertyDef> edgePropQuery = new QueryWrapper<GraphPropertyDef>()
                    .eq("entity_id", edgeTypeId)
                    .eq("property_type", "edge");
            Set<String> edgeSchemaPropertyCodes = propertyDefService.list(edgePropQuery).stream()
                    .map(GraphPropertyDef::getCode)
                    .collect(Collectors.toSet());

            // 更新导入结果统计
            result.setTotalCount(dataList.size());

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());

            // 创建图客户端并批量导入边数据
            try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

                // 批量导入边数据
                int successCount = 0;
                int failureCount = 0;

                for (Map<String, String> dataRow : dataList) {
                    try {
                        GraphEdge edge = new GraphEdge();
                        edge.setUid(dataRow.get("uid"));
                        edge.setLabel(edgeDef.getLabel());
                        edge.setStartLabel(edgeDef.getStartLabel());
                        edge.setEndLabel(edgeDef.getEndLabel());
                        edge.setStartUid(dataRow.getOrDefault("startUid", dataRow.get("source")));
                        edge.setEndUid(dataRow.getOrDefault("endUid", dataRow.get("target")));

                        // 设置边属性
                        Map<String, Object> properties = new HashMap<>();
                        dataRow.forEach((col, value) -> {
                            if (edgeSchemaPropertyCodes.contains(col)) {
                                properties.put(col, value);
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
            }
        } catch (Exception e) {
            log.error("导入边数据失败", e);
            errorMessages.add("导入边数据失败: " + e.getMessage());
            result.setErrorMessages(errorMessages.toArray(new String[0]));
        }

        return result;
    }

    @Override
    public PageResult<GraphVertex> getNodeDataList(Long graphId, Long vertexTypeId, String label, Integer page, Integer size) {
        return getNodeDataList(graphId, vertexTypeId, label, page, size, null, null);
    }

    public PageResult<GraphVertex> getNodeDataList(Long graphId, Long vertexTypeId, String label, Integer page, Integer size, Long connectionId, String graphCode) {
        try {
            if (label == null) {
                GraphVertexDef vertexDef = vertexDefService.getById(vertexTypeId);
                if (vertexDef == null) {
                    log.error("节点类型不存在，vertexTypeId={}", vertexTypeId);
                    return PageResult.empty(page, size);
                }
                label = vertexDef.getLabel();
            }

            GraphDataOperations ops = getGraphDataOperations(graphId, connectionId, graphCode);
            int skip = (page - 1) * size;

            long total = ops.countVertices(label);

            String query = buildLabelQuery(graphId, label, skip, size);
            GraphData graphData = ops.query(query);

            List<GraphVertex> records = (graphData == null || graphData.getVertices() == null)
                    ? new ArrayList<>()
                    : graphData.getVertices();

            return new PageResult<>(records, total, page, size);
        } catch (Exception e) {
            log.error("查询节点数据列表失败，graphId={}, vertexTypeId={}", graphId, vertexTypeId, e);
            return PageResult.empty(page, size);
        }
    }

    @Override
    public PageResult<Map<String, Object>> getEdgeDataList(Long graphId, Long edgeTypeId, String label, Integer page, Integer size) {
        return getEdgeDataList(graphId, edgeTypeId, label, page, size, null, null);
    }

    public PageResult<Map<String, Object>> getEdgeDataList(Long graphId, Long edgeTypeId, String label, Integer page, Integer size, Long connectionId, String graphCode) {
        try {
            if (label == null) {
                GraphEdgeDef edgeDef = edgeDefService.getById(edgeTypeId);
                if (edgeDef == null) {
                    log.error("边类型不存在，edgeTypeId={}", edgeTypeId);
                    return PageResult.empty(page, size);
                }
                label = edgeDef.getLabel();
            }

            GraphDataOperations ops = getGraphDataOperations(graphId, connectionId, graphCode);
            int skip = (page - 1) * size;

            long total = ops.countEdges(label);

            String query = buildEdgeLabelQuery(graphId, label, skip, size);
            GraphData graphData = ops.query(query);

            List<Map<String, Object>> records = new ArrayList<>();
            if (graphData != null && graphData.getEdges() != null) {
                for (GraphEdge edge : graphData.getEdges()) {
                    records.add(edgeToMap(edge));
                }
            }
            return new PageResult<>(records, total, page, size);
        } catch (Exception e) {
            log.error("查询边数据列表失败，graphId={}, edgeTypeId={}", graphId, edgeTypeId, e);
            return PageResult.empty(page, size);
        }
    }

    @Override
    public GraphVertex getNodeData(Long graphId, String vertexId) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);
            String query = buildFindVertexQuery(graphId, vertexId);
            GraphData graphData = ops.query(query);

            if (graphData != null && graphData.getVertices() != null && !graphData.getVertices().isEmpty()) {
                return graphData.getVertices().get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("获取节点数据详情失败，graphId={}, vertexId={}", graphId, vertexId, e);
            return null;
        }
    }

    @Override
    public GraphEdge getEdgeData(Long graphId, String edgeId) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);
            String query = buildFindEdgeQuery(graphId, edgeId);
            GraphData graphData = ops.query(query);

            if (graphData != null && graphData.getEdges() != null && !graphData.getEdges().isEmpty()) {
                return graphData.getEdges().get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("获取边数据详情失败，graphId={}, edgeId={}", graphId, edgeId, e);
            return null;
        }
    }

    @Override
    public boolean addNodeData(Long graphId, Long vertexTypeId, Map<String, Object> data) {
        try {
            GraphVertexDef vertexDef = vertexDefService.getById(vertexTypeId);
            if (vertexDef == null) {
                log.error("节点类型不存在，vertexTypeId={}", vertexTypeId);
                return false;
            }

            GraphDataOperations ops = getGraphDataOperations(graphId);

            GraphVertex vertex = new GraphVertex();
            vertex.setLabel(vertexDef.getLabel());

            // 处理嵌套属性结构：前端发送 {label: "...", properties: {uid, name, ...}}
            Map<String, Object> properties;
            if (data.containsKey("properties") && data.get("properties") instanceof Map) {
                properties = (Map<String, Object>) data.get("properties");
            } else {
                properties = new HashMap<>(data);
                properties.remove("label");
                properties.remove("properties");
            }
            vertex.setProperties(properties);
            // uid 可能在前端数据顶层或 properties 中
            if (properties.containsKey("uid")) {
                vertex.setUid(properties.get("uid").toString());
            } else if (data.containsKey("uid")) {
                vertex.setUid(data.get("uid").toString());
                properties.put("uid", data.get("uid").toString());
            }

            ops.addVertex(vertex);
            log.info("新增节点成功，label={}, uid={}", vertexDef.getLabel(), vertex.getUid());
            return true;
        } catch (Exception e) {
            log.error("新增节点数据失败，graphId={}, vertexTypeId={}", graphId, vertexTypeId, e);
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

            // 处理嵌套属性结构：前端发送 {label, startUid, endUid, properties: {uid, ...}}
            Map<String, Object> properties;
            if (data.containsKey("properties") && data.get("properties") instanceof Map) {
                properties = (Map<String, Object>) data.get("properties");
            } else {
                properties = new HashMap<>(data);
                properties.remove("label");
                properties.remove("properties");
            }
            edge.setProperties(properties);
            // uid 可能在前端数据顶层或 properties 中
            if (properties.containsKey("uid")) {
                edge.setUid(properties.get("uid").toString());
            } else if (data.containsKey("uid")) {
                edge.setUid(data.get("uid").toString());
                properties.put("uid", data.get("uid").toString());
            }
            // startUid/endUid 可能在前端数据顶层或 properties 中
            if (data.containsKey("startUid")) {
                edge.setStartUid(data.get("startUid").toString());
            } else if (properties.containsKey("startUid")) {
                edge.setStartUid(properties.get("startUid").toString());
            }
            if (data.containsKey("endUid")) {
                edge.setEndUid(data.get("endUid").toString());
            } else if (properties.containsKey("endUid")) {
                edge.setEndUid(properties.get("endUid").toString());
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
    public boolean updateNodeData(Long graphId, String vertexId, Map<String, Object> data) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);

            GraphVertex vertex = new GraphVertex();
            vertex.setUid(vertexId);
            if (data.containsKey("label")) {
                vertex.setLabel(data.get("label").toString());
            }
            // 处理嵌套属性结构
            Map<String, Object> properties;
            if (data.containsKey("properties") && data.get("properties") instanceof Map) {
                properties = (Map<String, Object>) data.get("properties");
            } else {
                properties = new HashMap<>(data);
                properties.remove("label");
                properties.remove("properties");
            }
            vertex.setProperties(properties);
            if (properties.containsKey("uid")) {
                vertex.setUid(properties.get("uid").toString());
            } else if (data.containsKey("uid")) {
                vertex.setUid(data.get("uid").toString());
            }

            ops.updateVertex(vertex);
            log.info("更新节点成功，vertexId={}", vertexId);
            return true;
        } catch (Exception e) {
            log.error("更新节点数据失败，graphId={}, vertexId={}", graphId, vertexId, e);
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
            // 处理嵌套属性结构
            Map<String, Object> properties;
            if (data.containsKey("properties") && data.get("properties") instanceof Map) {
                properties = (Map<String, Object>) data.get("properties");
            } else {
                properties = new HashMap<>(data);
                properties.remove("label");
                properties.remove("properties");
            }
            // startUid/endUid 可能在前端数据顶层或 properties 中
            if (data.containsKey("startUid")) {
                edge.setStartUid(data.get("startUid").toString());
            } else if (properties.containsKey("startUid")) {
                edge.setStartUid(properties.get("startUid").toString());
            }
            if (data.containsKey("endUid")) {
                edge.setEndUid(data.get("endUid").toString());
            } else if (properties.containsKey("endUid")) {
                edge.setEndUid(properties.get("endUid").toString());
            }
            edge.setProperties(properties);

            ops.updateEdge(edge);
            log.info("更新边成功，edgeId={}", edgeId);
            return true;
        } catch (Exception e) {
            log.error("更新边数据失败，graphId={}, edgeId={}", graphId, edgeId, e);
            return false;
        }
    }

    @Override
    public boolean deleteNode(Long graphId, String vertexId, String label) {
        List<String> vertexIds = new ArrayList<>();
        vertexIds.add(vertexId);
        return deleteNodes(graphId, vertexIds, label);
    }

    @Override
    public boolean deleteNodes(Long graphId, List<String> vertexIds, String label) {
        if (graphId == null || vertexIds == null || vertexIds.isEmpty()) {
            return false;
        }

        try {
            // 获取图信息
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                log.error("图不存在，graphId={}", graphId);
                return false;
            }

            // 获取图数据库连接信息
            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                return false;
            }

            // 构建图配置信息
            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());

            // 创建图客户端并删除节点
            try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

                GraphVertexDef vertexDef = vertexDefService.getOne(new QueryWrapper<GraphVertexDef>().eq("graph_id", graphId).eq("label", label));
                // 删除节点
                for (String vertexId : vertexIds) {
                    try {
                        GraphVertex vertex = new GraphVertex();
                        vertex.setUid(vertexId);
                        vertex.setLabel(vertexDef.getLabel());
                        graphDataOperations.deleteVertex(vertex);
                        log.info("成功删除节点，vertexId={}", vertexId);
                    } catch (Exception e) {
                        log.error("删除节点失败，vertexId={}", vertexId, e);
                        // 继续删除其他节点，不因单个节点失败而中断整个过程
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除节点失败，graphId={}", graphId, e);
            return false;
        }
    }

    @Override
    public GraphSummary getGraphSummary(Long graphId, Long connectionId, String graphCode) {
        try {
            GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);

            try (GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf)) {
                GraphDataOperations graphDataOperations = graphClient.opsForGraphData();
                GraphSummary summary = graphDataOperations.getSummary();
                summary.setGraphCode(graphConf.getGraphCode());
                return summary;
            }
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
        return getGraphDataOperations(graphId, null, null);
    }

    private GraphDataOperations getGraphDataOperations(Long graphId, Long connectionId, String graphCode) {
        GraphConf graphConf;
        if (graphId != null && graphId > 0) {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                throw new RuntimeException("图不存在，graphId=" + graphId);
            }
            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                throw new RuntimeException("图数据库连接不存在，connectionId=" + graphInfo.getConnectionId());
            }
            graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
        } else {
            graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
        }
        GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
        return graphClient.opsForGraphData();
    }

    private String resolveDbType(Long graphId, Long connectionId, String graphCode) {
        if (graphId != null && graphId > 0) {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo != null) {
                GraphConnection conn = connectionService.getById(graphInfo.getConnectionId());
                if (conn != null) return conn.getGraphType().name();
            }
        }
        if (connectionId != null) {
            GraphConnection conn = connectionService.getById(connectionId);
            if (conn != null) return conn.getGraphType().name();
        }
        return null;
    }

    /**
     * 构建按标签分页查询节点的查询语句（根据数据库类型生成不同语法）
     */
    private String buildLabelQuery(Long graphId, String label, int skip, int size) {
        if (isGremlinGraph(graphId)) {
            return String.format("g.V().hasLabel(\"%s\").skip(%d).limit(%d)", label, skip, size);
        }
        if (isNebulaGraph(graphId)) {
            return String.format("MATCH (n:`%s`) RETURN n SKIP %d LIMIT %d", label, skip, size);
        }
        return String.format("MATCH (n:`%s`) RETURN n SKIP %d LIMIT %d", label, skip, size);
    }

    /**
     * 构建按标签分页查询边的查询语句
     */
    private String buildEdgeLabelQuery(Long graphId, String label, int skip, int size) {
        if (isGremlinGraph(graphId)) {
            return String.format("g.E().hasLabel(\"%s\").skip(%d).limit(%d)", label, skip, size);
        }
        if (isNebulaGraph(graphId)) {
            return String.format("MATCH (a)-[r:`%s`]->(b) RETURN a, r, b SKIP %d LIMIT %d", label, skip, size);
        }
        return String.format("MATCH (a)-[r:`%s`]->(b) RETURN a, r, b SKIP %d LIMIT %d", label, skip, size);
    }

    /**
     * 构建按 uid 查找节点的查询语句
     */
    private String buildFindVertexQuery(Long graphId, String vertexId) {
        if (isGremlinGraph(graphId)) {
            return String.format("g.V().has(\"uid\", \"%s\")", escapeGremlinString(vertexId));
        }
        return String.format("MATCH (n {uid: '%s'}) RETURN n", escapeCypherString(vertexId));
    }

    /**
     * 构建按 uid 查找边的查询语句
     */
    private String buildFindEdgeQuery(Long graphId, String edgeId) {
        if (isGremlinGraph(graphId)) {
            return String.format("g.E().has(\"uid\", \"%s\")", escapeGremlinString(edgeId));
        }
        return String.format("MATCH (a)-[r {uid: '%s'}]->(b) RETURN a, r, b", escapeCypherString(edgeId));
    }

    /**
     * 判断图数据库是否使用Gremlin查询语言
     */
    private boolean isGremlinGraph(Long graphId) {
        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo == null) {
            return false;
        }
        GraphConnection conn = connectionService.getById(graphInfo.getConnectionId());
        return conn != null && conn.getGraphType() == GraphTypeEnum.janus;
    }

    private boolean isNebulaGraph(Long graphId) {
        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo == null) {
            return false;
        }
        GraphConnection conn = connectionService.getById(graphInfo.getConnectionId());
        return conn != null && conn.getGraphType() == GraphTypeEnum.nebula;
    }

    /**
     * Gremlin字符串转义
     */
    private String escapeGremlinString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
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