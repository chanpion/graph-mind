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
import com.chenpp.graph.core.constant.GraphConstants;
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
    public ImportResult importVertexData(Long graphId, Long vertexTypeId, MultipartFile file, String config) {
        ImportResult result = new ImportResult();
        result.setTotalCount(0);
        result.setSuccessCount(0);
        result.setFailureCount(0);
        List<String> errorMessages = new ArrayList<>();

        try {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                log.error("图不存在，graphId={}", graphId);
                errorMessages.add("图不存在，graphId=" + graphId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                errorMessages.add("图数据库连接不存在，connectionId=" + graphInfo.getConnectionId());
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            GraphVertexDef vertexDef = vertexDefService.getById(vertexTypeId);
            if (vertexDef == null) {
                log.error("顶点类型不存在，vertexTypeId={}", vertexTypeId);
                errorMessages.add("顶点类型不存在，vertexTypeId=" + vertexTypeId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            JSONObject configJson = JSON.parseObject(config);
            String delimiter = configJson.getString("delimiter");
            if (StringUtils.isBlank(delimiter)) {
                delimiter = ",";
            }

            List<Map<String, String>> dataList = parseCsvFile(file, delimiter);

            QueryWrapper<GraphPropertyDef> propQuery = new QueryWrapper<GraphPropertyDef>()
                    .eq("entity_id", vertexTypeId)
                    .eq("property_type", GraphConstants.VERTEX);
            List<GraphPropertyDef> propDefs = propertyDefService.list(propQuery);
            Set<String> vertexSchemaPropertyCodes = propDefs.stream()
                    .map(GraphPropertyDef::getCode)
                    .collect(Collectors.toSet());

            Map<String, String> propTypeMap = propDefs.stream()
                    .collect(Collectors.toMap(GraphPropertyDef::getCode, GraphPropertyDef::getType));

            result.setTotalCount(dataList.size());

            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());

            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            int successCount = 0;
            int failureCount = 0;

            for (Map<String, String> dataRow : dataList) {
                try {
                    GraphVertex vertex = new GraphVertex();
                    vertex.setUid(dataRow.get(GraphConstants.UID));
                    vertex.setLabel(dataRow.get("label"));

                    Map<String, Object> properties = new HashMap<>();

                    dataRow.forEach((key, value) -> {
                        if (vertexSchemaPropertyCodes.contains(key)) {
                            Object convertedValue = convertValueByType(value, propTypeMap.get(key));
                            properties.put(key, convertedValue);
                        }
                    });
                    vertex.setProperties(properties);
                    graphDataOperations.addVertex(vertex);
                    successCount++;
                } catch (Exception e) {
                    log.error("导入顶点数据失败: {}", e.getMessage(), e);
                    failureCount++;
                }
            }

            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            result.setErrorMessages(errorMessages.toArray(new String[0]));

            log.info("导入顶点数据完成，总数={}，成功={}，失败={}", dataList.size(), successCount, failureCount);
        } catch (Exception e) {
            log.error("导入顶点数据失败", e);
            errorMessages.add("导入顶点数据失败: " + e.getMessage());
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
        if (text.contains("\uFFFD")) {
            return true;
        }
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
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                log.error("图不存在，graphId={}", graphId);
                errorMessages.add("图不存在，graphId=" + graphId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }

            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                errorMessages.add("图数据库连接不存在，connectionId=" + graphInfo.getConnectionId());
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }


            GraphEdgeDef edgeDef = edgeDefService.getById(edgeTypeId);
            if (edgeDef == null) {
                log.error("边类型不存在，edgeTypeId={}", edgeTypeId);
                errorMessages.add("边类型不存在，edgeTypeId=" + edgeTypeId);
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }
            List<Map<String, String>> dataList = parseCsvFile(file, ",");

            QueryWrapper<GraphPropertyDef> edgePropQuery = new QueryWrapper<GraphPropertyDef>()
                    .eq("entity_id", edgeTypeId)
                    .eq("property_type", GraphConstants.EDGE);
            Set<String> edgeSchemaPropertyCodes = propertyDefService.list(edgePropQuery).stream()
                    .map(GraphPropertyDef::getCode)
                    .collect(Collectors.toSet());

            result.setTotalCount(dataList.size());

            GraphConf graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());

            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            int successCount = 0;
            int failureCount = 0;

            for (Map<String, String> dataRow : dataList) {
                try {
                    GraphEdge edge = new GraphEdge();
                    edge.setUid(dataRow.get(GraphConstants.UID));
                    edge.setLabel(edgeDef.getLabel());
                    edge.setStartLabel(edgeDef.getStartLabel());
                    edge.setEndLabel(edgeDef.getEndLabel());
                    edge.setStartUid(dataRow.getOrDefault("startUid", dataRow.get("source")));
                    edge.setEndUid(dataRow.getOrDefault("endUid", dataRow.get("target")));

                    Map<String, Object> properties = new HashMap<>();
                    dataRow.forEach((col, value) -> {
                        if (edgeSchemaPropertyCodes.contains(col)) {
                            properties.put(col, value);
                        }
                    });
                    edge.setProperties(properties);
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


    public PageResult<GraphVertex> queryVertexDataList(Long graphId, Long vertexTypeId, String label, Integer page, Integer size, Long connectionId, String graphCode) {
        try {
            if (label == null) {
                GraphVertexDef vertexDef = vertexDefService.getById(vertexTypeId);
                if (vertexDef == null) {
                    log.error("顶点类型不存在，vertexTypeId={}", vertexTypeId);
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
            log.error("查询顶点数据列表失败，graphId={}, vertexTypeId={}", graphId, vertexTypeId, e);
            return PageResult.empty(page, size);
        }
    }

    public PageResult<Map<String, Object>> queryEdgeDataList(Long graphId, Long edgeTypeId, String label, Integer page, Integer size, Long connectionId, String graphCode) {
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
    public GraphVertex getVertexData(Long graphId, String vertexId) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId);
            String query = buildFindVertexQuery(graphId, vertexId);
            GraphData graphData = ops.query(query);

            if (graphData != null && graphData.getVertices() != null && !graphData.getVertices().isEmpty()) {
                return graphData.getVertices().get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("获取顶点数据详情失败，graphId={}, vertexId={}", graphId, vertexId, e);
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
    public boolean addVertexData(Long graphId, Long vertexTypeId, Long connectionId, String graphCode, Map<String, Object> data) {
        try {
            GraphVertexDef vertexDef = vertexDefService.getById(vertexTypeId);
            if (vertexDef == null) {
                log.error("顶点类型不存在，vertexTypeId={}", vertexTypeId);
                return false;
            }

            GraphDataOperations ops = getGraphDataOperations(graphId, connectionId, graphCode);

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
            // 根据属性定义进行类型转换
            if (vertexDef.getProperties() != null) {
                for (GraphPropertyDef propDef : vertexDef.getProperties()) {
                    String code = propDef.getCode();
                    if (properties.containsKey(code)) {
                        properties.put(code, convertValueType(properties.get(code), propDef.getType()));
                    }
                }
            }
            vertex.setProperties(properties);
            // uid 可能在前端数据顶层或 properties 中
            if (properties.containsKey(GraphConstants.UID)) {
                vertex.setUid(properties.get(GraphConstants.UID).toString());
            } else if (data.containsKey(GraphConstants.UID)) {
                vertex.setUid(data.get(GraphConstants.UID).toString());
                properties.put(GraphConstants.UID, data.get(GraphConstants.UID).toString());
            }

            ops.addVertex(vertex);
            log.info("新增顶点成功，label={}, uid={}", vertexDef.getLabel(), vertex.getUid());
            return true;
        } catch (Exception e) {
            log.error("新增顶点数据失败，graphId={}, vertexTypeId={}", graphId, vertexTypeId, e);
            return false;
        }
    }

    @Override
    public boolean addEdgeData(Long graphId, Long edgeTypeId, Long connectionId, String graphCode, Map<String, Object> data) {
        try {
            GraphEdgeDef edgeDef = edgeDefService.getById(edgeTypeId);
            if (edgeDef == null) {
                log.error("边类型不存在，edgeTypeId={}", edgeTypeId);
                return false;
            }

            GraphDataOperations ops = getGraphDataOperations(graphId, connectionId, graphCode);

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
            // 根据属性定义进行类型转换
            if (edgeDef.getProperties() != null) {
                for (GraphPropertyDef propDef : edgeDef.getProperties()) {
                    String code = propDef.getCode();
                    if (properties.containsKey(code)) {
                        properties.put(code, convertValueType(properties.get(code), propDef.getType()));
                    }
                }
            }
            edge.setProperties(properties);
            // uid 可能在前端数据顶层或 properties 中
            if (properties.containsKey(GraphConstants.UID)) {
                edge.setUid(properties.get(GraphConstants.UID).toString());
            } else if (data.containsKey(GraphConstants.UID)) {
                edge.setUid(data.get(GraphConstants.UID).toString());
                properties.put(GraphConstants.UID, data.get(GraphConstants.UID).toString());
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
    public boolean updateVertexData(Long graphId, String vertexId, Long connectionId, String graphCode, Map<String, Object> data) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId, connectionId, graphCode);

            GraphVertex vertex = new GraphVertex();
            vertex.setUid(vertexId);
            if (data.containsKey("label")) {
                vertex.setLabel(data.get("label").toString());
            }

            // 根据 label 获取顶点定义，以获取属性类型信息
            String label = vertex.getLabel();
            List<GraphPropertyDef> propDefs = new ArrayList<>();
            if (label != null) {
                GraphVertexDef vertexDef = vertexDefService.getOne(
                    new QueryWrapper<GraphVertexDef>().eq("graph_id", graphId).eq("label", label)
                );
                if (vertexDef != null && vertexDef.getProperties() != null) {
                    propDefs = vertexDef.getProperties();
                }
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
            // 根据属性定义进行类型转换
            for (GraphPropertyDef propDef : propDefs) {
                String code = propDef.getCode();
                if (properties.containsKey(code)) {
                    properties.put(code, convertValueType(properties.get(code), propDef.getType()));
                }
            }
            vertex.setProperties(properties);
            if (properties.containsKey(GraphConstants.UID)) {
                vertex.setUid(properties.get(GraphConstants.UID).toString());
            } else if (data.containsKey(GraphConstants.UID)) {
                vertex.setUid(data.get(GraphConstants.UID).toString());
            }

            ops.updateVertex(vertex);
            log.info("更新顶点成功，vertexId={}", vertexId);
            return true;
        } catch (Exception e) {
            log.error("更新顶点数据失败，graphId={}, vertexId={}", graphId, vertexId, e);
            return false;
        }
    }

    /**
     * 根据属性类型转换值
     */
    private Object convertValueType(Object value, String type) {
        if (value == null) {
            return null;
        }
        String typeLower = type != null ? type.toLowerCase() : "string";

        try {
            switch (typeLower) {
                case "long":
                case "int64":
                case "integer":
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    return Long.parseLong(value.toString());
                case "double":
                case "float":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(value.toString());
                case "boolean":
                    if (value instanceof Boolean) {
                        return value;
                    }
                    String str = value.toString().toLowerCase();
                    return "true".equals(str) || "1".equals(str);
                case "string":
                default:
                    return value.toString();
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to convert value '{}' to type {}, using as-is", value, type);
            return value;
        }
    }

    @Override
    public boolean updateEdgeData(Long graphId, String edgeId, Long connectionId, String graphCode, Map<String, Object> data) {
        try {
            GraphDataOperations ops = getGraphDataOperations(graphId, connectionId, graphCode);

            GraphEdge edge = new GraphEdge();
            edge.setUid(edgeId);
            if (data.containsKey("label")) {
                edge.setLabel(data.get("label").toString());
            }

            // 根据 label 获取边定义，以获取属性类型信息
            String label = edge.getLabel();
            List<GraphPropertyDef> propDefs = new ArrayList<>();
            if (label != null) {
                GraphEdgeDef edgeDef = edgeDefService.getOne(
                    new QueryWrapper<GraphEdgeDef>().eq("graph_id", graphId).eq("label", label)
                );
                if (edgeDef != null && edgeDef.getProperties() != null) {
                    propDefs = edgeDef.getProperties();
                }
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
            // 根据属性定义进行类型转换
            for (GraphPropertyDef propDef : propDefs) {
                String code = propDef.getCode();
                if (properties.containsKey(code)) {
                    properties.put(code, convertValueType(properties.get(code), propDef.getType()));
                }
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
    public boolean deleteVertex(Long graphId, String vertexId, String label, Long connectionId, String graphCode) {
        List<String> vertexIds = new ArrayList<>();
        vertexIds.add(vertexId);
        return deleteVertices(graphId, vertexIds, label, connectionId, graphCode);
    }

    @Override
    public boolean deleteVertices(Long graphId, List<String> vertexIds, String label, Long connectionId, String graphCode) {
        if (graphId == null || vertexIds == null || vertexIds.isEmpty()) {
            return false;
        }

        try {
            // 获取图信息（仅在 graphId > 0 时需要）
            GraphConf graphConf;
            if (graphId != null && graphId > 0) {
                GraphInfo graphInfo = graphService.getById(graphId);
                if (graphInfo == null) {
                    log.error("图不存在，graphId={}", graphId);
                    return false;
                }
                GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
                if (connection == null) {
                    log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                    return false;
                }
                graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
            } else {
                graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
            }

            // 创建图客户端并删除顶点
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            // 对于发现的图（graphId < 0），label 直接从请求参数获取，无需查数据库
            String vertexLabel = label;
            if (graphId > 0) {
                GraphVertexDef vertexDef = vertexDefService.getOne(new QueryWrapper<GraphVertexDef>().eq("graph_id", graphId).eq("label", label));
                if (vertexDef != null) {
                    vertexLabel = vertexDef.getLabel();
                }
            }
            if (vertexLabel == null) {
                log.warn("顶点类型不存在，graphId={}, label={}", graphId, label);
                return false;
            }
            // 删除顶点
            for (String vertexId : vertexIds) {
                try {
                    GraphVertex vertex = new GraphVertex();
                    vertex.setUid(vertexId);
                    vertex.setLabel(vertexLabel);
                    graphDataOperations.deleteVertex(vertex);
                    log.info("成功删除顶点，vertexId={}", vertexId);
                } catch (Exception e) {
                    log.error("删除顶点失败，vertexId={}", vertexId, e);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除顶点失败，graphId={}", graphId, e);
            return false;
        }
    }

    @Override
    public boolean deleteEdge(Long graphId, String edgeId, String label, Long connectionId, String graphCode) {
        List<String> edgeIds = new ArrayList<>();
        edgeIds.add(edgeId);
        return deleteEdges(graphId, edgeIds, label, connectionId, graphCode);
    }

    @Override
    public boolean deleteEdges(Long graphId, List<String> edgeIds, String label, Long connectionId, String graphCode) {
        if (graphId == null || edgeIds == null || edgeIds.isEmpty()) {
            return false;
        }

        try {
            GraphConf graphConf;
            if (graphId > 0) {
                GraphInfo graphInfo = graphService.getById(graphId);
                if (graphInfo == null) {
                    log.error("图不存在，graphId={}", graphId);
                    return false;
                }
                GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
                if (connection == null) {
                    log.error("图数据库连接不存在，connectionId={}", graphInfo.getConnectionId());
                    return false;
                }
                graphConf = GraphClientFactory.createGraphConf(connection, graphInfo.getCode());
            } else {
                graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
            }

            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            String edgeLabel = label;
            if (graphId > 0) {
                GraphEdgeDef edgeDef = edgeDefService.getOne(new QueryWrapper<GraphEdgeDef>().eq("graph_id", graphId).eq("label", label));
                if (edgeDef != null) {
                    edgeLabel = edgeDef.getLabel();
                }
            }
            if (edgeLabel == null) {
                log.warn("边类型不存在，graphId={}, label={}", graphId, label);
                return false;
            }

            for (String edgeId : edgeIds) {
                try {
                    GraphEdge edge = new GraphEdge();
                    edge.setUid(edgeId);
                    edge.setLabel(edgeLabel);

                    // 解析 edgeId，格式：startUid -> endUid @label 或 复合uid
                    if (edgeId.contains("->") && edgeId.contains("@")) {
                        int arrowIdx = edgeId.indexOf("->");
                        int atIdx = edgeId.indexOf("@");
                        if (atIdx > arrowIdx) {
                            edge.setStartUid(edgeId.substring(0, arrowIdx));
                            edge.setEndUid(edgeId.substring(arrowIdx + 2, atIdx));
                        }
                    } else if (edgeId.contains("->")) {
                        // 格式：startUid->endUid
                        int arrowIdx = edgeId.indexOf("->");
                        edge.setStartUid(edgeId.substring(0, arrowIdx));
                        edge.setEndUid(edgeId.substring(arrowIdx + 2));
                    }

                    // 如果 startUid/endUid 仍未解析出来，查询边数据
                    if (edge.getStartUid() == null || edge.getEndUid() == null) {
                        try {
                            String query = String.format("MATCH ()-[e:%s]->() WHERE e.uid == '%s' OR id(e) == '%s' RETURN e", edgeLabel, edgeId, edgeId);
                            GraphData graphData = graphDataOperations.query(query);
                            if (graphData != null && graphData.getEdges() != null && !graphData.getEdges().isEmpty()) {
                                GraphEdge foundEdge = graphData.getEdges().get(0);
                                edge.setStartUid(foundEdge.getStartUid());
                                edge.setEndUid(foundEdge.getEndUid());
                            }
                        } catch (Exception qe) {
                            log.warn("查询边数据失败，edgeId={}: {}", edgeId, qe.getMessage());
                        }
                    }

                    if (edge.getStartUid() == null || edge.getEndUid() == null) {
                        log.error("无法获取边的起点和终点信息，edgeId={}", edgeId);
                        continue;
                    }

                    graphDataOperations.deleteEdge(edge);
                    log.info("成功删除边，edgeId={}", edgeId);
                } catch (Exception e) {
                    log.error("删除边失败，edgeId={}", edgeId, e);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除边失败，graphId={}", graphId, e);
            return false;
        }
    }

    @Override
    public GraphSummary getGraphSummary(Long graphId, Long connectionId, String graphCode) {
        try {
            GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);

            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();
            GraphSummary summary = graphDataOperations.getSummary();
            summary.setGraphCode(graphConf.getGraphCode());
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


    /**
     * 构建按标签分页查询顶点的查询语句（根据数据库类型生成不同语法）
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
     * 构建按 uid 查找顶点的查询语句
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
        return conn != null && conn.getGraphTypeEnum() == GraphTypeEnum.janus;
    }

    private boolean isNebulaGraph(Long graphId) {
        GraphInfo graphInfo = graphService.getById(graphId);
        if (graphInfo == null) {
            return false;
        }
        GraphConnection conn = connectionService.getById(graphInfo.getConnectionId());
        return conn != null && conn.getGraphTypeEnum() == GraphTypeEnum.nebula;
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
     * 将 GraphEdge 转为 Map
     */
    private Map<String, Object> edgeToMap(GraphEdge edge) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", edge.getId());
        map.put(GraphConstants.UID, edge.getUid());
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

    /**
     * 根据属性类型转换值
     *
     * @param value    字符串值
     * @param dataType 属性类型
     * @return 转换后的值
     */
    private Object convertValueByType(String value, String dataType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        if (dataType == null) {
            return value;
        }

        String normalizedType = dataType.trim().toUpperCase();

        try {
            switch (normalizedType) {
                case "INTEGER", "INT", "LONG", "INT64" -> {
                    return Long.parseLong(value.trim());
                }
                case "FLOAT", "DOUBLE" -> {
                    return Double.parseDouble(value.trim());
                }
                case "BOOLEAN", "BOOL"-> {
                    return Boolean.parseBoolean(value.trim());
                }
                default -> {
                    return value;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to convert value '{}' to type '{}', using original string value", value, dataType);
            return value;
        }
    }
}