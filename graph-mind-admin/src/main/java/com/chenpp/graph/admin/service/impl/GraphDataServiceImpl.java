package com.chenpp.graph.admin.service.impl;

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
import com.chenpp.graph.core.util.DataTypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
    private GraphConnectionService connectionService;

    @Autowired
    private GraphVertexDefService vertexDefService;

    @Autowired
    private GraphEdgeDefService edgeDefService;

    @Autowired
    private GraphPropertyDefService propertyDefService;

    private final Map<String, ReentrantLock> importLocks = new ConcurrentHashMap<>();

    private static final int BATCH_SIZE = 100;

    private ReentrantLock getImportLock(Long graphId, String graphCode) {
        String key = graphId != null ? "graphId:" + graphId : "graphCode:" + graphCode;
        return importLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    private void releaseImportLock(Long graphId, String graphCode) {
        String key = graphId != null ? "graphId:" + graphId : "graphCode:" + graphCode;
        importLocks.remove(key);
    }

    @Override
    public ImportResult importVertexData(Long graphId, Long vertexTypeId, Long connectionId, String graphCode, String label, MultipartFile file) {
        return doImportData(graphId, vertexTypeId, connectionId, graphCode, label, file, true);
    }

    @Override
    public ImportResult importEdgeData(Long graphId, Long edgeTypeId, Long connectionId, String graphCode, String label, MultipartFile file) {
        return doImportData(graphId, edgeTypeId, connectionId, graphCode, label, file, false);
    }

    private ImportResult doImportData(Long graphId, Long typeId, Long connectionId, String graphCode,
                                       String label, MultipartFile file, boolean isVertex) {
        String dataType = isVertex ? "顶点" : "边";
        ImportResult result = new ImportResult();
        result.setTotalCount(0);
        result.setSuccessCount(0);
        result.setFailureCount(0);
        List<String> errorMessages = new ArrayList<>();

        ReentrantLock importLock = getImportLock(graphId, graphCode);
        boolean lockAcquired = false;
        try {
            if (!importLock.tryLock(30, TimeUnit.SECONDS)) {
                errorMessages.add("系统繁忙，请稍后再试");
                result.setErrorMessages(errorMessages.toArray(new String[0]));
                return result;
            }
            lockAcquired = true;
            log.info("获取导入锁成功，graphId={}, graphCode={}", graphId, graphCode);

            ImportContext context = prepareImportContext(graphId, typeId, connectionId, graphCode, label, isVertex);
            if (context.hasError()) {
                result.setErrorMessages(context.getErrors().toArray(new String[0]));
                return result;
            }

            List<Map<String, String>> dataList = parseDataCsv(file);
            result.setTotalCount(dataList.size());

            GraphClient graphClient = GraphClientFactory.createGraphClient(context.getGraphConf());
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

            int successCount = isVertex
                    ? batchImportVertices(graphDataOperations, dataList, context.getLabel(), errorMessages)
                    : batchImportEdges(graphDataOperations, dataList, context.getLabel(), errorMessages);

            result.setSuccessCount(successCount);
            result.setFailureCount(dataList.size() - successCount);
            result.setErrorMessages(errorMessages.toArray(new String[0]));

            log.info("导入{}数据完成，总数={}，成功={}，失败={}", dataType, dataList.size(), successCount, dataList.size() - successCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取导入锁被中断", e);
            errorMessages.add("导入操作被中断");
            result.setErrorMessages(errorMessages.toArray(new String[0]));
        } catch (Exception e) {
            log.error("导入{}数据失败", dataType, e);
            errorMessages.add("导入" + dataType + "数据失败: " + e.getMessage());
            result.setErrorMessages(errorMessages.toArray(new String[0]));
        } finally {
            if (lockAcquired) {
                importLock.unlock();
                releaseImportLock(graphId, graphCode);
                log.info("释放导入锁，graphId={}, graphCode={}", graphId, graphCode);
            }
        }

        return result;
    }

    /**
     * 批量导入顶点数据
     */
    private int batchImportVertices(GraphDataOperations graphDataOperations, List<Map<String, String>> dataList, String label, List<String> errorMessages) {
        int successCount = 0;
        List<GraphVertex> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, String> dataRow = dataList.get(i);
            try {
                GraphVertex vertex = new GraphVertex();
                vertex.setUid(dataRow.get(GraphConstants.UID));
                vertex.setLabel(label);
                vertex.setProperties(DataTypeConverter.extractProperties(
                        new HashMap<>(dataRow), Set.of(GraphConstants.LABEL)));
                batch.add(vertex);

                if (batch.size() >= BATCH_SIZE || i == dataList.size() - 1) {
                    try {
                        if (batch.size() > 1) {
                            graphDataOperations.addVertices(batch);
                            successCount += batch.size();
                        } else {
                            graphDataOperations.addVertex(batch.get(0));
                            successCount++;
                        }
                    } catch (Exception batchEx) {
                        log.warn("批量导入失败，尝试逐条插入: {}", batchEx.getMessage());
                        for (int j = 0; j < batch.size(); j++) {
                            GraphVertex v = batch.get(j);
                            try {
                                graphDataOperations.addVertex(v);
                                successCount++;
                            } catch (Exception singleEx) {
                                int rowNum = (i - batch.size() + j + 2);
                                log.error("导入顶点数据失败，第{}行: {}", rowNum, singleEx.getMessage(), singleEx);
                                errorMessages.add(String.format("第%d行导入失败: %s", rowNum, singleEx.getMessage()));
                            }
                        }
                    }
                    batch.clear();
                }
            } catch (Exception e) {
                log.error("导入顶点数据失败，第{}行: {}", i + 2, e.getMessage(), e);
                errorMessages.add(String.format("第%d行导入失败: %s", i + 2, e.getMessage()));
            }
        }

        return successCount;
    }

    /**
     * 导入上下文，用于复用导入逻辑
     */
    private static class ImportContext {
        private GraphConnection connection;
        private String graphCode;
        private String label;
        private List<String> errors = new ArrayList<>();

        public ImportContext(GraphConnection connection, String graphCode, String label) {
            this.connection = connection;
            this.graphCode = graphCode;
            this.label = label;
        }

        public boolean hasError() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public GraphConnection getConnection() {
            return connection;
        }

        public String getGraphCode() {
            return graphCode;
        }

        public String getLabel() {
            return label;
        }

        public GraphConf getGraphConf() {
            return GraphClientFactory.createGraphConf(connection, graphCode);
        }

        public void addError(String error) {
            errors.add(error);
        }
    }

    /**
     * 准备导入上下文
     */
    private ImportContext prepareImportContext(Long graphId, Long typeId, Long connectionId, String graphCode, String label, boolean isVertex) {
        ImportContext context;

        if (connectionId != null && graphCode != null && label != null) {
            GraphConnection connection = connectionService.getById(connectionId);
            if (connection == null) {
                context = new ImportContext(null, null, null);
                context.addError("图数据库连接不存在，connectionId=" + connectionId);
                return context;
            }
            context = new ImportContext(connection, graphCode, label);
        } else if (graphId != null && typeId != null) {
            GraphInfo graphInfo = graphService.getById(graphId);
            if (graphInfo == null) {
                context = new ImportContext(null, null, null);
                context.addError("图不存在，graphId=" + graphId);
                return context;
            }

            GraphConnection connection = connectionService.getById(graphInfo.getConnectionId());
            if (connection == null) {
                context = new ImportContext(null, null, null);
                context.addError("图数据库连接不存在，connectionId=" + graphInfo.getConnectionId());
                return context;
            }

            String targetLabel;
            if (isVertex) {
                GraphVertexDef vertexDef = vertexDefService.getById(typeId);
                if (vertexDef == null) {
                    context = new ImportContext(null, null, null);
                    context.addError("顶点类型不存在，vertexTypeId=" + typeId);
                    return context;
                }
                targetLabel = vertexDef.getLabel();
            } else {
                GraphEdgeDef edgeDef = edgeDefService.getById(typeId);
                if (edgeDef == null) {
                    context = new ImportContext(null, null, null);
                    context.addError("边类型不存在，edgeTypeId=" + typeId);
                    return context;
                }
                targetLabel = edgeDef.getLabel();
            }

            context = new ImportContext(connection, graphInfo.getCode(), targetLabel);
        } else {
            context = new ImportContext(null, null, null);
            context.addError("缺少必要参数，需要提供 (graphId + typeId) 或 (connectionId + graphCode + label)");
        }

        return context;
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

    private List<Map<String, String>> parseDataCsv(MultipartFile file) throws Exception {
        List<Map<String, String>> dataList = new ArrayList<>();

        byte[] rawBytes = file.getBytes();
        String content = new String(rawBytes, StandardCharsets.UTF_8);
        Charset charset = StandardCharsets.UTF_8;
        if (containsGarbledChinese(content)) {
            charset = Charset.forName("GBK");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), charset));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            List<String> headers = csvParser.getHeaderNames();

            for (CSVRecord record : csvParser) {
                Map<String, String> dataMap = new HashMap<>();
                for (String header : headers) {
                    String value = record.get(header);
                    dataMap.put(header, value != null ? value.trim() : null);
                }
                dataList.add(dataMap);
            }
        }

        return dataList;
    }


    /**
     * 批量导入边数据
     */
    private int batchImportEdges(GraphDataOperations graphDataOperations, List<Map<String, String>> dataList, String label, List<String> errorMessages) {
        int successCount = 0;
        List<GraphEdge> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, String> dataRow = dataList.get(i);
            try {
                GraphEdge edge = new GraphEdge();
                edge.setUid(dataRow.get(GraphConstants.UID));
                edge.setLabel(label);
                edge.setStartUid(dataRow.getOrDefault("startUid", dataRow.get("source")));
                edge.setEndUid(dataRow.getOrDefault("endUid", dataRow.get("target")));
                edge.setProperties(DataTypeConverter.extractProperties(
                        dataRow, Set.of("startUid", "endUid", "source", "target", "label")));
                batch.add(edge);

                if (batch.size() >= BATCH_SIZE || i == dataList.size() - 1) {
                    try {
                        if (batch.size() > 1) {
                            graphDataOperations.addEdges(batch);
                            successCount += batch.size();
                        } else {
                            graphDataOperations.addEdge(batch.get(0));
                            successCount++;
                        }
                    } catch (Exception batchEx) {
                        log.warn("批量导入失败，尝试逐条插入: {}", batchEx.getMessage());
                        for (int j = 0; j < batch.size(); j++) {
                            GraphEdge e = batch.get(j);
                            try {
                                graphDataOperations.addEdge(e);
                                successCount++;
                            } catch (Exception singleEx) {
                                int rowNum = (i - batch.size() + j + 2);
                                log.error("导入边数据失败，第{}行: {}", rowNum, singleEx.getMessage(), singleEx);
                                errorMessages.add(String.format("第%d行导入失败: %s", rowNum, singleEx.getMessage()));
                            }
                        }
                    }
                    batch.clear();
                }
            } catch (Exception e) {
                log.error("导入边数据失败，第{}行: {}", i + 2, e.getMessage(), e);
                errorMessages.add(String.format("第%d行导入失败: %s", i + 2, e.getMessage()));
            }
        }

        return successCount;
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

            String query = buildLabelQuery(graphId, connectionId, label, skip, size);
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

    public PageResult<GraphEdge> queryEdgeDataList(Long graphId, Long edgeTypeId, String label, Integer page, Integer size, Long connectionId, String graphCode) {
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

            String query = buildEdgeLabelQuery(graphId, connectionId, label, skip, size);
            GraphData graphData = ops.query(query);
            List<GraphEdge> records = (graphData == null || graphData.getEdges() == null)
                    ? new ArrayList<>()
                    : graphData.getEdges();
            return new PageResult<>(records, total, page, size);
        } catch (Exception e) {
            log.error("查询边数据列表失败，graphId={}, edgeTypeId={}", graphId, edgeTypeId, e);
            return PageResult.empty(page, size);
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
            Map<String, Object> properties = handleProperties(data, vertexDef.getProperties());
            String uid = data.get(GraphConstants.UID).toString();
            vertex.setProperties(properties);
            vertex.setUid(uid);
            ops.addVertex(vertex);
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

            Map<String, Object> properties = handleProperties(data, edgeDef.getProperties());
            String uid = data.get(GraphConstants.UID).toString();
            GraphEdge edge = new GraphEdge();
            edge.setLabel(edgeDef.getLabel());
            edge.setUid(uid);
            edge.setStartLabel(edgeDef.getStartLabel());
            edge.setEndLabel(edgeDef.getEndLabel());
            edge.setProperties(properties);
            ops.addEdge(edge);
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

            Map<String, Object> properties = handleProperties(data, propDefs);
            vertex.setProperties(properties);
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
        return DataTypeConverter.convertValue(value, type);
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
            Map<String, Object> properties = handleProperties(data, propDefs);
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
        if (graphId == null || vertexId == null) {
            return false;
        }

        try {
            GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);
            GraphClient graphClient = GraphClientFactory.createGraphClient(graphConf);
            GraphDataOperations graphDataOperations = graphClient.opsForGraphData();

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

            GraphVertex vertex = new GraphVertex();
            vertex.setUid(vertexId);
            vertex.setLabel(vertexLabel);
            graphDataOperations.deleteVertex(vertex);
            log.info("成功删除顶点，vertexId={}", vertexId);
            return true;
        } catch (Exception e) {
            log.error("删除顶点失败，graphId={}, vertexId={}", graphId, vertexId, e);
            return false;
        }
    }

    @Override
    public boolean deleteEdge(Long graphId, String edgeId, String label, Long connectionId, String graphCode) {
        if (graphId == null || edgeId == null) {
            return false;
        }

        try {
            GraphConf graphConf = GraphClientFactory.resolveGraphConf(graphId, connectionId, graphCode, graphService, connectionService);

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
                int arrowIdx = edgeId.indexOf("->");
                edge.setStartUid(edgeId.substring(0, arrowIdx));
                edge.setEndUid(edgeId.substring(arrowIdx + 2));
            }

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
                return false;
            }

            graphDataOperations.deleteEdge(edge);
            log.info("成功删除边，edgeId={}", edgeId);
            return true;
        } catch (Exception e) {
            log.error("删除边失败，graphId={}, edgeId={}", graphId, edgeId, e);
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


    private String buildLabelQuery(Long graphId, Long connectionId, String label, int skip, int size) {
        GraphInfo graphInfo = graphService.getById(graphId);
        GraphTypeEnum graphType = null;
        if (graphInfo == null) {
            GraphConnection connection = connectionService.getById(connectionId);
            graphType = connection.getGraphTypeEnum();
        } else {
            graphType = graphInfo.getGraphType();
        }

        return switch (graphType) {
            case janus -> String.format("g.V().hasLabel(\"%s\").skip(%d).limit(%d)", label, skip, size);
            case nebula -> String.format("MATCH (n:`%s`) RETURN n SKIP %d LIMIT %d", label, skip, size);
            case neo4j -> String.format("MATCH (n:`%s`) RETURN n SKIP %d LIMIT %d", label, skip, size);
        };
    }

    private String buildEdgeLabelQuery(Long graphId, Long connectionId, String label, int skip, int size) {
        GraphInfo graphInfo = graphService.getById(graphId);
        return switch (graphInfo.getGraphType()) {
            case janus -> String.format("g.E().hasLabel(\"%s\").skip(%d).limit(%d)", label, skip, size);
            case nebula -> String.format("MATCH (a)-[r:`%s`]->(b) RETURN a, r, b SKIP %d LIMIT %d", label, skip, size);
            case neo4j -> String.format("MATCH (a)-[r:`%s`]->(b) RETURN a, r, b SKIP %d LIMIT %d", label, skip, size);
        };
    }


    private Map<String, Object> handleProperties(Map<String, Object> data, List<GraphPropertyDef> propertyDefList) {
        Map<String, Object> properties = (Map<String, Object>) data.get("properties");
        if (propertyDefList != null) {
            for (GraphPropertyDef propDef : propertyDefList) {
                String code = propDef.getCode();
                if (properties.containsKey(code)) {
                    properties.put(code, convertValueType(properties.get(code), propDef.getType()));
                }
            }
        }
        properties.put(GraphConstants.UID, data.get(GraphConstants.UID));
        return properties;
    }
}