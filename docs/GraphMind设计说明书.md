# GraphMind 图数据库管理平台设计说明书

**软件名称**：GraphMind 图数据库管理平台  
**版本号**：V1.0  
**开发单位**：陈鹏飞  
**编写日期**：2026年07月02日

---

## 目录

一、项目概述 ........................................................................................................ 1
二、系统架构设计 ................................................................................................ 2
三、功能模块详细设计 ...................................................................................... 5
四、数据库与数据结构设计 ................................................................................ 25
五、API 接口设计 ........................................................................................... 35
六、关键技术实现 ........................................................................................... 45
七、运行环境设计 ........................................................................................... 55
八、输入输出设计 ........................................................................................... 60
九、出错处理设计 ........................................................................................... 70
十、安全性设计 ............................................................................................. 75
十一、部署方案 ............................................................................................. 80
十二、项目文件结构 .......................................................................................... 85

---

## 一、项目概述

### 1.1 项目名称
GraphMind 图数据库管理平台

### 1.2 项目简介
GraphMind 是一款面向企业级的图数据库管理平台，旨在提供统一的图数据库操作界面，支持多种主流图数据库（NebulaGraph、JanusGraph、Neo4j）的连接、管理、查询和分析。平台采用前后端分离架构，提供可视化的图数据管理、Schema 设计、图分析等功能，帮助用户高效地管理和利用图数据。

### 1.3 开发背景
随着大数据和人工智能技术的发展，图数据在社交网络分析、知识图谱、推荐系统等领域得到广泛应用。不同的图数据库系统（如 NebulaGraph、JanusGraph、Neo4j）具有各自的特点和优势，但缺乏统一的管理工具。GraphMind 正是为了解决这一问题而设计，为用户提供一个统一的、可视化的图数据库管理平台。

### 1.4 主要功能
- **图数据库连接管理**：支持多种图数据库的连接配置和测试
- **图数据管理**：节点和边的增删改查操作
- **图 Schema 设计**：可视化设计图的节点类型、边类型和属性
- **图查询**：支持 Cypher/Gremlin/NGQL 等查询语言的执行
- **图分析**：提供 K 层展开、最短路径等图分析算法
- **图可视化**：基于 D3.js 的交互式图形展示

### 1.5 技术特点
- **多数据库支持**：通过适配器模式统一支持 NebulaGraph、JanusGraph、Neo4j
- **前后端分离**：Vue.js 前端 + Spring Boot 后端
- **可视化设计**：D3.js 力导向图展示，直观呈现图数据关系
- **安全性**：JWT 无状态认证，细粒度权限控制

---

## 二、系统架构设计

### 2.1 总体架构
GraphMind 采用前后端分离的架构设计，整体分为三层：表示层、服务层和数据层。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        表示层 (Presentation Layer)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Vue.js SPA │  │  Element Plus│  │    D3.js     │  │   ECharts    │ │
│  │              │  │              │  │   图可视化    │  │   统计图表   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│                        服务层 (Service Layer)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Controller │  │   Service    │  │   Security   │  │   Util       │ │
│  │   REST API   │  │   业务逻辑   │  │   JWT认证    │  │   工具类     │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│                        数据层 (Data Layer)                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   MySQL      │  │  JanusGraph  │  │  NebulaGraph │  │    Neo4j     │ │
│  │  元数据存储  │  │   图数据库    │  │   图数据库    │  │   图数据库   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 模块名称 | 职责描述 | 技术栈 |
|---------|---------|-------|
| **graph-mind-admin** | 管理后台服务，提供 REST API | Spring Boot 3.x + MyBatis Plus |
| **graph-mind-core** | 核心模块，定义通用图模型和接口 | Java 21 |
| **graph-mind-janus** | JanusGraph 数据库适配器 | gremlin-driver |
| **graph-mind-nebula** | NebulaGraph 数据库适配器 | nebula-java |
| **graph-mind-neo4j** | Neo4j 数据库适配器 | neo4j-java-driver |
| **graph-mind-common** | 公共模块，共享工具类和异常处理 | Apache Commons |
| **frontend** | 前端应用，提供可视化界面 | Vue.js 3 + Element Plus + D3.js |

### 2.3 核心设计模式

#### 2.3.1 适配器模式（Adapter Pattern）
为支持多种图数据库，设计了统一的图操作接口，通过适配器模式实现对不同图数据库的支持：

```
GraphOperations Interface
├── JanusGraphOperations
├── NebulaGraphOperations
└── Neo4jGraphOperations

GraphDataOperations Interface
├── JanusGraphDataOperations
├── NebulaGraphDataOperations
└── Neo4jGraphDataOperations
```

**设计意图**：通过适配器模式，将不同图数据库的差异封装在具体实现中，上层业务逻辑无需关心底层数据库类型，实现了数据库无关性。

#### 2.3.2 工厂模式（Factory Pattern）
采用工厂模式管理不同图数据库的连接配置，支持动态切换图数据库类型：

```
GraphClientFactory
├── JanusClientFactory
├── NebulaClientFactory
└── Neo4jClientFactory
```

**设计意图**：根据配置动态创建对应的图数据库客户端，实现连接的统一管理和动态切换。

#### 2.3.3 模板方法模式（Template Method Pattern）
在 GraphDataServiceImpl 中抽取通用导入模板方法，减少 importVertexData 和 importEdgeData 的重复代码。

**设计意图**：将相同的导入流程抽取为模板方法，不同的部分通过抽象方法实现，提高代码复用性。

### 2.4 数据流向

```
前端请求 → Controller → Service → GraphClientFactory → 图数据库
                                                              ↓
前端响应 ← Controller ← Service ← GraphDataOperations ← 图数据库返回结果
```

---

## 三、功能模块详细设计

### 3.1 系统登录模块

#### 3.1.1 功能描述
用户登录认证，获取 JWT 令牌，系统采用无状态认证机制。

#### 3.1.2 处理流程

```
用户访问登录页面
      ↓
输入用户名和密码
      ↓
点击登录按钮
      ↓
前端发起 POST /api/auth/login 请求
      ↓
后端 AuthController 接收请求
      ↓
调用 UserDetailsServiceImpl 验证用户
      ↓
验证成功 → 生成 JWT Token
      ↓
返回 Token 给前端
      ↓
前端存储 Token 到 localStorage
      ↓
跳转至首页
```

#### 3.1.3 输入输出

| 输入项 | 类型 | 说明 |
|-------|------|------|
| username | String | 用户名，必填，长度 3-50 字符 |
| password | String | 密码，必填，长度 6-100 字符 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| token | String | JWT 令牌 |
| tokenType | String | 令牌类型，固定为 Bearer |
| expiresIn | Long | 过期时间（毫秒） |
| username | String | 用户名 |

#### 3.1.4 关键代码

```java
// AuthController.java
@PostMapping("/login")
public Result<LoginResponse> login(@RequestBody LoginRequest request) {
    // 用户验证
    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
    if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
        throw new GraphException(ErrorCode.INVALID_CREDENTIALS);
    }
    // 生成 Token
    String token = jwtUtil.generateToken(userDetails.getUsername());
    return Result.success(new LoginResponse(token, "Bearer", 86400000L, request.getUsername()));
}
```

### 3.2 图数据库连接管理模块

#### 3.2.1 功能描述
管理图数据库连接配置，支持连接测试和多数据库类型。

#### 3.2.2 处理流程

##### 3.2.2.1 新增连接流程

```
用户进入连接管理页面
      ↓
点击新增连接按钮
      ↓
填写连接信息（名称、类型、主机、端口等）
      ↓
点击保存按钮
      ↓
前端发起 POST /api/connections 请求
      ↓
后端 GraphConnectionController 接收请求
      ↓
调用 GraphConnectionService 保存连接配置
      ↓
返回连接 ID
      ↓
前端刷新连接列表
```

##### 3.2.2.2 测试连接流程

```
用户在连接列表中选择一个连接
      ↓
点击测试连接按钮
      ↓
前端发起 POST /api/connections/{id}/test 请求
      ↓
后端根据连接类型创建对应的客户端
      ↓
尝试连接图数据库
      ↓
获取数据库版本和统计信息
      ↓
返回测试结果（响应时间、版本、节点数、边数）
```

#### 3.2.3 输入输出

##### 3.2.3.1 新增连接

| 输入项 | 类型 | 说明 |
|-------|------|------|
| name | String | 连接名称，必填 |
| type | String | 数据库类型（neo4j/nebula/janus），必填 |
| host | String | 主机地址，必填 |
| port | Integer | 端口号，必填 |
| username | String | 用户名，可选 |
| password | String | 密码，可选 |
| description | String | 描述，可选 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| id | Long | 连接 ID |

##### 3.2.3.2 测试连接

| 输入项 | 类型 | 说明 |
|-------|------|------|
| id | Long | 连接 ID，路径参数 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| responseTime | Long | 响应时间（毫秒） |
| version | String | 数据库版本 |
| nodes | Long | 节点数量 |
| edges | Long | 边数量 |

#### 3.2.4 关键代码

```java
// GraphConnectionServiceImpl.java
public ConnectionTestResult testConnection(Long id) {
    GraphConnection connection = getById(id);
    if (connection == null) {
        throw new GraphException("连接不存在");
    }
    GraphClient client = GraphClientFactory.create(connection);
    try {
        long startTime = System.currentTimeMillis();
        client.connect();
        long responseTime = System.currentTimeMillis() - startTime;
        GraphSummary summary = client.getSummary();
        return new ConnectionTestResult(responseTime, summary.getVersion(), 
                                       summary.getVertexCount(), summary.getEdgeCount());
    } finally {
        client.close();
    }
}
```

### 3.3 图管理模块

#### 3.3.1 功能描述
管理图的创建、查询和删除，支持分页和搜索。

#### 3.3.2 处理流程

##### 3.3.2.1 创建图流程

```
用户进入图管理页面
      ↓
点击创建图按钮
      ↓
填写图信息（名称、编码、连接、描述）
      ↓
点击保存按钮
      ↓
前端发起 POST /api/graphs 请求
      ↓
后端 GraphController 接收请求
      ↓
校验图编码唯一性
      ↓
调用 GraphService 保存图信息
      ↓
返回图 ID
      ↓
前端刷新图列表
```

##### 3.3.2.2 查询图列表流程

```
用户进入图管理页面
      ↓
前端发起 GET /api/graphs 请求（带分页参数）
      ↓
后端 GraphController 接收请求
      ↓
调用 GraphService 查询图列表
      ↓
返回分页结果
      ↓
前端渲染图列表
```

#### 3.3.3 输入输出

##### 3.3.3.1 创建图

| 输入项 | 类型 | 说明 |
|-------|------|------|
| name | String | 图名称，必填 |
| code | String | 图编码，必填，唯一 |
| connectionId | Long | 连接 ID，必填 |
| description | String | 描述，可选 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| id | Long | 图 ID |

##### 3.3.3.2 查询图列表

| 输入项 | 类型 | 说明 |
|-------|------|------|
| page | Integer | 页码，默认 1 |
| pageSize | Integer | 每页大小，默认 10 |
| keyword | String | 搜索关键词，可选 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| records | List | 图列表 |
| total | Long | 总记录数 |
| current | Integer | 当前页码 |
| pages | Integer | 总页数 |

#### 3.3.4 关键代码

```java
// GraphServiceImpl.java
public IPage<GraphInfo> list(Page<GraphInfo> page, String keyword) {
    LambdaQueryWrapper<GraphInfo> queryWrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(keyword)) {
        queryWrapper.like(GraphInfo::getName, keyword)
                    .or().like(GraphInfo::getCode, keyword);
    }
    queryWrapper.orderByDesc(GraphInfo::getCreateTime);
    return graphDao.selectPage(page, queryWrapper);
}
```

### 3.4 图 Schema 管理模块

#### 3.4.1 功能描述
设计和管理图的 Schema 结构，包括节点类型、边类型和属性定义。

#### 3.4.2 处理流程

##### 3.4.2.1 新增节点类型流程

```
用户进入图建模页面
      ↓
选择目标图
      ↓
点击新增节点类型按钮
      ↓
填写节点类型信息（标签、名称）
      ↓
添加属性定义（名称、数据类型、是否必填）
      ↓
点击保存按钮
      ↓
前端发起 POST /api/graphs/schema/vertices 请求
      ↓
后端 GraphSchemaController 接收请求
      ↓
调用 GraphVertexDefService 保存节点类型及属性
      ↓
返回成功消息
```

##### 3.4.2.2 发布 Schema 流程

```
用户在图建模页面
      ↓
点击发布 Schema 按钮
      ↓
前端发起 POST /api/graphs/schema/publish 请求
      ↓
后端获取当前图的所有节点和边定义
      ↓
根据图类型生成对应的 Schema 创建语句
      ↓
调用图数据库客户端执行 Schema 创建
      ↓
更新本地元数据状态为已发布
      ↓
返回发布结果
```

##### 3.4.2.3 发现 Schema 流程

```
用户进入图建模页面
      ↓
点击发现 Schema 按钮
      ↓
前端发起 GET /api/graphs/schema/vertices 请求（带 connectionId 和 graphCode）
      ↓
后端调用 GraphSchemaService.discoverVertexDefs
      ↓
根据连接类型创建图数据库客户端
      ↓
执行 Schema 查询语句
      ↓
解析返回结果，转换为 GraphVertexDef 列表
      ↓
合并本地已有的定义
      ↓
返回合并后的节点类型列表
```

#### 3.4.3 输入输出

##### 3.4.3.1 新增节点类型

| 输入项 | 类型 | 说明 |
|-------|------|------|
| graphId | Long | 图 ID，必填 |
| label | String | 节点标签，必填 |
| name | String | 显示名称，可选 |
| properties | List | 属性定义列表 |
| - code | String | 属性编码 |
| - name | String | 属性名称 |
| - dataType | String | 数据类型 |
| - isRequired | Boolean | 是否必填 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| message | String | 操作结果消息 |

##### 3.4.3.2 发布 Schema

| 输入项 | 类型 | 说明 |
|-------|------|------|
| graphId | Long | 图 ID，必填 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| message | String | 操作结果消息 |

#### 3.4.4 关键代码

```java
// GraphSchemaServiceImpl.java
public void publishSchema(Long graphId, Long connectionId, String graphCode) {
    GraphInfo graphInfo = graphService.getById(graphId);
    if (graphInfo == null) {
        throw new GraphException("图不存在");
    }
    
    List<GraphVertexDef> vertices = vertexDefService.getVertexDefsByGraphId(graphId, 0);
    List<GraphEdgeDef> edges = edgeDefService.getEdgeDefsByGraphId(graphId, 0);
    
    GraphClient client = GraphClientFactory.create(graphInfo.getConnectionId());
    try {
        client.connect();
        GraphOperations operations = client.getGraphOperations();
        
        GraphSchema schema = new GraphSchema();
        schema.setGraphCode(graphInfo.getCode());
        schema.setEntities(vertices.stream()
                .map(v -> buildEntity(v))
                .collect(Collectors.toList()));
        schema.setRelations(edges.stream()
                .map(e -> buildRelation(e))
                .collect(Collectors.toList()));
        
        operations.createGraph(schema);
        
        updateMetadataStatus(graphId, vertices, edges);
    } finally {
        client.close();
    }
}
```

### 3.5 图数据管理模块

#### 3.5.1 功能描述
管理图中的节点和边数据，支持增删改查和批量导入。

#### 3.5.2 处理流程

##### 3.5.2.1 新增节点数据流程

```
用户进入图数据页面
      ↓
选择目标图和节点类型
      ↓
点击新增节点按钮
      ↓
填写节点属性值
      ↓
点击保存按钮
      ↓
前端发起 POST /api/graphs/data/nodes/{nodeTypeId} 请求
      ↓
后端获取节点类型定义和属性类型映射
      ↓
根据属性类型转换属性值格式
      ↓
调用图数据库客户端添加节点
      ↓
返回操作结果
```

##### 3.5.2.2 批量导入节点数据流程

```
用户进入图数据页面
      ↓
选择目标图和节点类型
      ↓
点击导入数据按钮
      ↓
上传 CSV 文件
      ↓
前端解析 CSV 内容
      ↓
前端发起 POST /api/graphs/data/nodes/{nodeTypeId}/import 请求
      ↓
后端解析 CSV 数据
      ↓
获取属性类型映射
      ↓
批量转换属性值
      ↓
调用图数据库客户端批量插入
      ↓
返回导入结果（成功数、失败数、错误信息）
```

#### 3.5.3 输入输出

##### 3.5.3.1 新增节点数据

| 输入项 | 类型 | 说明 |
|-------|------|------|
| graphId | Long | 图 ID，路径参数 |
| nodeTypeId | Long | 节点类型 ID，路径参数 |
| properties | Map | 属性键值对 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| success | Boolean | 是否成功 |

##### 3.5.3.2 批量导入节点数据

| 输入项 | 类型 | 说明 |
|-------|------|------|
| graphId | Long | 图 ID，路径参数 |
| nodeTypeId | Long | 节点类型 ID，路径参数 |
| file | MultipartFile | CSV 文件 |
| mapping | String | 字段映射关系（JSON） |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| successCount | Integer | 成功数量 |
| failureCount | Integer | 失败数量 |
| totalCount | Integer | 总数量 |
| errorMessages | List | 错误信息列表 |

#### 3.5.4 关键代码

```java
// GraphDataServiceImpl.java
public ImportResult importVertexData(Long graphId, Long nodeTypeId, MultipartFile file, 
                                     String mappingJson) {
    GraphVertexDef vertexDef = vertexDefService.getById(nodeTypeId);
    if (vertexDef == null) {
        throw new GraphException("节点类型不存在");
    }
    
    Map<String, DataType> propertyTypes = buildPropertyTypeMap(vertexDef.getProperties());
    
    List<String[]> rows = parseCsvFile(file);
    List<GraphVertex> vertices = rows.stream()
            .map(row -> buildVertex(row, vertexDef.getLabel(), propertyTypes))
            .collect(Collectors.toList());
    
    GraphClient client = getGraphClient(graphId);
    try {
        client.connect();
        GraphDataOperations operations = client.getDataOperations();
        return operations.addVertices(vertices);
    } finally {
        client.close();
    }
}
```

### 3.6 图查询模块

#### 3.6.1 功能描述
执行图查询语句，支持多种查询语言。

#### 3.6.2 处理流程

```
用户进入图查询页面
      ↓
选择目标图
      ↓
在查询编辑器中输入查询语句
      ↓
点击执行按钮
      ↓
前端发起 POST /api/graphs/{graphId}/query 请求
      ↓
后端根据图类型选择对应的查询解析器
      ↓
调用图数据库客户端执行查询
      ↓
解析查询结果
      ↓
转换为统一的 GraphData 格式
      ↓
返回查询结果
      ↓
前端使用 D3.js 渲染图形
```

#### 3.6.3 输入输出

| 输入项 | 类型 | 说明 |
|-------|------|------|
| graphId | Long | 图 ID，路径参数 |
| cypher | String | 查询语句，必填 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| vertices | List | 节点列表 |
| edges | List | 边列表 |

#### 3.6.4 关键代码

```java
// GraphQueryServiceImpl.java
public GraphData executeQuery(Long graphId, String query) {
    GraphInfo graphInfo = graphService.getById(graphId);
    if (graphInfo == null) {
        throw new GraphException("图不存在");
    }
    
    GraphClient client = GraphClientFactory.create(graphInfo.getConnectionId());
    try {
        client.connect();
        GraphDataOperations operations = client.getDataOperations();
        return operations.query(query);
    } finally {
        client.close();
    }
}
```

### 3.7 图分析模块

#### 3.7.1 功能描述
提供图分析算法，包括 K 层展开和最短路径查询。

#### 3.7.2 处理流程

##### 3.7.2.1 K 层展开流程

```
用户进入图分析页面
      ↓
选择 K 层展开算法
      ↓
选择目标图和目标顶点类型
      ↓
选择查询属性和输入查询值
      ↓
设置拓展层数和返回最大路径数
      ↓
点击执行按钮
      ↓
前端发起 POST /api/graphs/{graphId}/expand 请求
      ↓
后端根据查询条件找到目标节点
      ↓
调用图数据库客户端执行 K 层展开
      ↓
返回展开结果（节点和边）
      ↓
前端渲染图形，高亮目标节点
```

##### 3.7.2.2 最短路径查询流程

```
用户进入图分析页面
      ↓
选择最短路径算法
      ↓
选择目标图
      ↓
输入起点和终点
      ↓
设置最大深度
      ↓
点击执行按钮
      ↓
前端发起 POST /api/graphs/{graphId}/path/shortest 请求
      ↓
后端调用图数据库客户端执行最短路径算法
      ↓
返回路径结果
      ↓
前端渲染图形，高亮路径
```

#### 3.7.3 输入输出

##### 3.7.3.1 K 层展开

| 输入项 | 类型 | 说明 |
|-------|------|------|
| graphId | Long | 图 ID，路径参数 |
| nodeId | String | 节点 ID |
| label | String | 节点标签 |
| depth | Integer | 拓展层数，默认 1 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| vertices | List | 节点列表（含目标节点标记） |
| edges | List | 边列表 |

##### 3.7.3.2 最短路径

| 输入项 | 类型 | 说明 |
|-------|------|------|
| graphId | Long | 图 ID，路径参数 |
| startNodeId | String | 起点 ID |
| endNodeId | String | 终点 ID |
| maxDepth | Integer | 最大深度，默认 5 |

| 输出项 | 类型 | 说明 |
|-------|------|------|
| pathLength | Integer | 路径长度 |
| vertexCount | Integer | 顶点数 |
| path | List | 路径节点列表 |

#### 3.7.4 关键代码

```java
// GraphDataOperations.java
public GraphData expand(String nodeId, int depth) {
    // 根据图类型生成对应的展开查询
    // JanusGraph: g.V(nodeId).repeat(both()).times(depth)
    // NebulaGraph: GO depth STEPS FROM nodeId OVER *
    // Neo4j: MATCH (n)-[*1..depth]-(m) WHERE n.id = nodeId
}

public GraphPath shortestPath(String startId, String endId, int maxDepth) {
    // 根据图类型生成对应的最短路径查询
}
```

### 3.8 图可视化模块

#### 3.8.1 功能描述
基于 D3.js 的力导向图可视化展示。

#### 3.8.2 处理流程

```
后端返回 GraphData（vertices + edges）
      ↓
前端 transformApiResponseToGraphData 转换数据格式
      ↓
创建 D3.js 力导向图模拟
      ↓
渲染节点和边
      ↓
添加节点拖拽和缩放功能
      ↓
添加节点/边点击事件
      ↓
高亮目标节点（如果有）
```

#### 3.8.3 关键代码

```javascript
// GraphVisualView.vue
const drawGraph = (graphData) => {
    const { nodes, edges } = transformApiResponseToGraphData(graphData);
    
    const simulation = d3.forceSimulation(nodes)
        .force("link", d3.forceLink(edges).id(d => d.id))
        .force("charge", d3.forceManyBody())
        .force("center", d3.forceCenter(width / 2, height / 2));
    
    // 渲染边
    const link = svg.append("g")
        .selectAll("line")
        .data(edges)
        .join("line")
        .attr("stroke", "#999")
        .attr("stroke-width", d => Math.sqrt(d.value));
    
    // 渲染节点
    const node = svg.append("g")
        .selectAll("circle")
        .data(nodes)
        .join("circle")
        .attr("r", d => d.group === 'center' ? 24 : 16)
        .attr("fill", d => color(d.group))
        .call(d3.drag()
            .on("start", dragstarted)
            .on("drag", dragged)
            .on("end", dragended));
    
    // 渲染标签
    const label = svg.append("g")
        .selectAll("text")
        .data(nodes)
        .join("text")
        .text(d => d.label)
        .attr("x", 0)
        .attr("y", 20);
    
    simulation.on("tick", () => {
        link.attr("x1", d => d.source.x)
            .attr("y1", d => d.source.y)
            .attr("x2", d => d.target.x)
            .attr("y2", d => d.target.y);
        node.attr("cx", d => d.x).attr("cy", d => d.y);
        label.attr("x", d => d.x).attr("y", d => d.y + 20);
    });
};
```

---

## 四、数据库与数据结构设计

### 4.1 元数据数据库（MySQL）

#### 4.1.1 用户表（user）

| 字段名 | 类型 | 约束 | 说明 |
|-------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt加密） |
| phone_number | VARCHAR(20) | NULL | 手机号 |
| email | VARCHAR(100) | NULL | 邮箱 |
| status | INT | DEFAULT 1 | 状态（0禁用，1启用） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引设计**：
- 主键索引：id
- 唯一索引：username

#### 4.1.2 图数据库连接表（graph_connection）

| 字段名 | 类型 | 约束 | 说明 |
|-------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 连接ID |
| name | VARCHAR(100) | NOT NULL | 连接名称 |
| type | VARCHAR(20) | NOT NULL | 数据库类型（neo4j/nebula/janus） |
| host | VARCHAR(200) | NOT NULL | 主机地址 |
| port | INT | NOT NULL | 端口号 |
| username | VARCHAR(100) | NULL | 用户名 |
| password | VARCHAR(255) | NULL | 密码 |
| status | INT | DEFAULT 0 | 状态（0未连接，1已连接） |
| description | VARCHAR(500) | NULL | 描述 |
| params | TEXT | NULL | 额外参数（JSON格式） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引设计**：
- 主键索引：id
- 普通索引：type, status

#### 4.1.3 图信息表（graph_info）

| 字段名 | 类型 | 约束 | 说明 |
|-------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 图ID |
| name | VARCHAR(100) | NOT NULL | 图名称 |
| code | VARCHAR(100) | NOT NULL, UNIQUE | 图编码 |
| description | VARCHAR(500) | NULL | 描述 |
| connection_id | BIGINT | NOT NULL, FOREIGN KEY | 连接ID |
| graph_type | VARCHAR(50) | DEFAULT 'PROPERTY_GRAPH' | 图类型 |
| status | INT | DEFAULT 0 | 状态（0未发布，1已发布） |
| creator | VARCHAR(50) | NULL | 创建人 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引设计**：
- 主键索引：id
- 唯一索引：code
- 普通索引：connection_id, status

#### 4.1.4 节点类型定义表（graph_vertex_def）

| 字段名 | 类型 | 约束 | 说明 |
|-------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 定义ID |
| graph_id | BIGINT | NOT NULL, FOREIGN KEY | 图ID |
| label | VARCHAR(100) | NOT NULL | 节点标签 |
| name | VARCHAR(100) | NULL | 显示名称 |
| status | INT | DEFAULT 0 | 状态 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引设计**：
- 主键索引：id
- 复合索引：graph_id + label

#### 4.1.5 边类型定义表（graph_edge_def）

| 字段名 | 类型 | 约束 | 说明 |
|-------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 定义ID |
| graph_id | BIGINT | NOT NULL, FOREIGN KEY | 图ID |
| label | VARCHAR(100) | NOT NULL | 边标签 |
| name | VARCHAR(100) | NULL | 显示名称 |
| from_label | VARCHAR(100) | NULL | 起点标签 |
| to_label | VARCHAR(100) | NULL | 终点标签 |
| status | INT | DEFAULT 0 | 状态 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引设计**：
- 主键索引：id
- 复合索引：graph_id + label

#### 4.1.6 属性定义表（graph_property_def）

| 字段名 | 类型 | 约束 | 说明 |
|-------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 属性ID |
| entity_id | BIGINT | NOT NULL | 所属实体ID（节点或边） |
| entity_type | VARCHAR(20) | NOT NULL | 实体类型（VERTEX/EDGE） |
| code | VARCHAR(100) | NOT NULL | 属性编码 |
| name | VARCHAR(100) | NULL | 显示名称 |
| data_type | VARCHAR(20) | NOT NULL | 数据类型 |
| is_required | INT | DEFAULT 0 | 是否必填 |
| sort_order | INT | DEFAULT 0 | 排序 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引设计**：
- 主键索引：id
- 复合索引：entity_id + entity_type

### 4.2 图数据库（NebulaGraph/JanusGraph/Neo4j）

图数据库用于存储实际的图数据，其 Schema 结构与元数据数据库中的定义保持同步。

#### 4.2.1 节点（Vertex）

| 字段名 | 说明 |
|-------|------|
| uid | 节点唯一标识 |
| label | 节点标签（对应 graph_vertex_def.label） |
| properties | 属性键值对（对应 graph_property_def） |

#### 4.2.2 边（Edge）

| 字段名 | 说明 |
|-------|------|
| uid | 边唯一标识 |
| label | 边标签（对应 graph_edge_def.label） |
| startUid | 起点节点 ID |
| endUid | 终点节点 ID |
| properties | 属性键值对（对应 graph_property_def） |

### 4.3 核心数据结构

#### 4.3.1 GraphVertex（图节点）

```java
public class GraphVertex {
    private String id;
    private String uid;
    private String label;
    private Map<String, Object> properties;
    private String group;
    // getter/setter
}
```

#### 4.3.2 GraphEdge（图边）

```java
public class GraphEdge {
    private String id;
    private String uid;
    private String label;
    private String startUid;
    private String endUid;
    private String startLabel;
    private String endLabel;
    private Map<String, Object> properties;
    // getter/setter
}
```

#### 4.3.3 GraphData（图数据）

```java
public class GraphData {
    private List<GraphVertex> vertices;
    private List<GraphEdge> edges;
    // getter/setter
}
```

#### 4.3.4 GraphSchema（图 Schema）

```java
public class GraphSchema {
    private String graphCode;
    private List<GraphEntity> entities;
    private List<GraphRelation> relations;
    // getter/setter
}
```

#### 4.3.5 GraphEntity（图实体）

```java
public class GraphEntity {
    private String label;
    private List<GraphProperty> properties;
    // getter/setter
}
```

#### 4.3.6 GraphRelation（图关系）

```java
public class GraphRelation {
    private String label;
    private String sourceLabel;
    private String targetLabel;
    private List<GraphProperty> properties;
    // getter/setter
}
```

#### 4.3.7 GraphProperty（图属性）

```java
public class GraphProperty {
    private String name;
    private DataType dataType;
    private boolean required;
    // getter/setter
}
```

#### 4.3.8 DataType（数据类型枚举）

```java
public enum DataType {
    String, Integer, Int, Long, Double, Float, Boolean, Date, DateTime, 
    StringArray, IntArray, LongArray, DoubleArray
}
```

---

## 五、API 接口设计

### 5.1 基础信息
- **Base URL**: `/api`
- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **认证方式**: JWT Token（通过 `Authorization: Bearer {token}` 请求头传递）

### 5.2 统一响应格式

所有接口统一返回 `Result<T>` 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 响应码，200 表示成功 |
| `message` | string | 响应消息 |
| `data` | T | 响应数据，类型由具体接口决定 |

### 5.3 分页模型（`PageResult<T>`）

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 100,
    "records": [],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 5.4 接口分类

| 模块 | 基础路径 | 主要接口 |
|-----|---------|---------|
| 认证 | `/api/auth` | 登录、登出 |
| 用户管理 | `/api/users` | 用户增删改查、重置密码 |
| 连接管理 | `/api/connections` | 连接增删改查、测试 |
| 图管理 | `/api/graphs` | 图增删改查 |
| Schema 管理 | `/api/graphs/schema` | 节点/边定义管理、发布 |
| 数据管理 | `/api/graphs/data` | 节点/边数据增删改查、导入 |
| 查询 | `/api/graphs/query` | 执行查询、K层展开、路径查询 |
| 分析 | `/api/graphs/analysis` | 图分析算法 |

### 5.5 详细接口列表

#### 5.5.1 认证 API

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 用户登出 |

#### 5.5.2 用户管理 API

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/users` | GET | 获取用户列表（分页） |
| `/api/users/{userId}` | GET | 获取用户详情 |
| `/api/users` | POST | 新增用户 |
| `/api/users/{userId}` | PUT | 更新用户 |
| `/api/users/{userId}` | DELETE | 删除用户 |
| `/api/users/{userId}/password/reset` | POST | 重置密码 |

#### 5.5.3 连接管理 API

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/connections` | GET | 获取连接列表（分页） |
| `/api/connections/{id}` | GET | 获取连接详情 |
| `/api/connections` | POST | 新增连接 |
| `/api/connections/{id}` | PUT | 更新连接 |
| `/api/connections/{id}` | DELETE | 删除连接 |
| `/api/connections/{id}/test` | POST | 测试连接 |

#### 5.5.4 图管理 API

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/graphs` | GET | 获取图列表（分页） |
| `/api/graphs/{id}` | GET | 获取图详情 |
| `/api/graphs` | POST | 新增图 |
| `/api/graphs/{id}` | PUT | 更新图 |
| `/api/graphs/{id}` | DELETE | 删除图 |

#### 5.5.5 Schema 管理 API

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/graphs/schema/vertices` | GET | 获取节点定义列表 |
| `/api/graphs/schema/vertices` | POST | 新增节点定义 |
| `/api/graphs/schema/vertex` | PUT | 更新节点定义 |
| `/api/graphs/schema/vertex` | DELETE | 删除节点定义 |
| `/api/graphs/schema/edges` | GET | 获取边定义列表 |
| `/api/graphs/schema/edges` | POST | 新增边定义 |
| `/api/graphs/schema/edge` | PUT | 更新边定义 |
| `/api/graphs/schema/edge` | DELETE | 删除边定义 |
| `/api/graphs/schema/publish` | POST | 发布 Schema |
| `/api/graphs/schema/export` | GET | 导出 Schema |
| `/api/graphs/schema/import` | POST | 导入 Schema |

#### 5.5.6 数据管理 API

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/graphs/{graphId}/data/nodes/{nodeTypeId}` | GET | 查询节点数据列表 |
| `/api/graphs/{graphId}/data/nodes/{nodeTypeId}` | POST | 新增节点数据 |
| `/api/graphs/{graphId}/data/nodes/{nodeId}` | PUT | 更新节点数据 |
| `/api/graphs/{graphId}/data/nodes/{nodeId}` | DELETE | 删除节点数据 |
| `/api/graphs/{graphId}/data/edges/{edgeTypeId}` | GET | 查询边数据列表 |
| `/api/graphs/{graphId}/data/edges/{edgeTypeId}` | POST | 新增边数据 |
| `/api/graphs/{graphId}/data/edges/{edgeId}` | PUT | 更新边数据 |
| `/api/graphs/{graphId}/data/edges/{edgeId}` | DELETE | 删除边数据 |
| `/api/graphs/{graphId}/data/nodes/{nodeTypeId}/import` | POST | 批量导入节点数据 |
| `/api/graphs/{graphId}/data/edges/{edgeTypeId}/import` | POST | 批量导入边数据 |
| `/api/graphs/{graphId}/summary` | GET | 获取图统计信息 |

#### 5.5.7 查询 API

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/graphs/{graphId}/query` | POST | 执行图查询 |
| `/api/graphs/{graphId}/expand` | POST | K 层展开 |
| `/api/graphs/{graphId}/path/shortest` | POST | 最短路径查询 |
| `/api/graphs/{graphId}/path/all` | POST | 所有路径查询 |

### 5.6 错误码定义

#### 5.6.1 HTTP 标准错误码

| 错误码 | 描述 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 405 | 请求方法不被允许 |
| 408 | 请求超时 |
| 500 | 服务器内部错误 |

#### 5.6.2 核心错误码

| 错误码 | 描述 |
|--------|------|
| 1001 | 连接失败 |
| 1002 | 查询执行失败 |
| 1003 | Schema 校验失败 |
| 1004 | 事务执行失败 |
| 1005 | 不支持的操作 |
| 1006 | 无效的配置 |

#### 5.6.3 业务错误码

| 错误码 | 描述 |
|--------|------|
| 2001 | 用户不存在 |
| 2002 | 用户已存在 |
| 2003 | 角色不存在 |
| 2004 | 权限不足 |
| 2005 | 用户名或密码错误 |
| 2006 | 账户已被禁用 |
| 2007 | 图不存在 |
| 2008 | 图已存在 |
| 2009 | 无效的 Schema 定义 |
| 2010 | 图数据库连接失败 |
| 2011 | 图查询失败 |
| 2012 | 图数据导入失败 |

---

## 六、关键技术实现

### 6.1 图数据库抽象层

#### 6.1.1 核心接口定义

```java
public interface GraphClient {
    void connect();
    void close();
    GraphOperations getGraphOperations();
    GraphDataOperations getDataOperations();
    GraphSummary getSummary();
}

public interface GraphOperations {
    GraphSchema discoverSchema(String graphCode);
    void createGraph(GraphSchema schema);
    void dropGraph(String graphCode);
    boolean exists(String graphCode);
}

public interface GraphDataOperations {
    GraphVertex addVertex(GraphVertex vertex);
    void updateVertex(GraphVertex vertex);
    void deleteVertex(String uid, String label);
    List<GraphVertex> addVertices(List<GraphVertex> vertices);
    
    GraphEdge addEdge(GraphEdge edge);
    void updateEdge(GraphEdge edge);
    void deleteEdge(String uid);
    List<GraphEdge> addEdges(List<GraphEdge> edges);
    
    GraphData query(String query);
    GraphData expand(String nodeId, int depth);
    GraphPath shortestPath(String startId, String endId, int maxDepth);
    GraphPath queryAllPaths(String startId, String endId, int maxDepth);
}
```

#### 6.1.2 适配器实现结构

```
GraphClient
├── JanusClient
│   └── getGraphOperations() → JanusGraphOperations
│   └── getDataOperations() → JanusGraphDataOperations
├── NebulaClient
│   └── getGraphOperations() → NebulaGraphOperations
│   └── getDataOperations() → NebulaGraphDataOperations
└── Neo4jClient
    └── getGraphOperations() → Neo4jGraphOperations
    └── getDataOperations() → Neo4jGraphDataOperations
```

### 6.2 类型安全的属性值处理

#### 6.2.1 NebulaUtil 格式化方法

```java
public static String formatValue(Object value, DataType dataType) {
    if (value == null) {
        return "NULL";
    }
    
    if (dataType != null && value instanceof String) {
        String strValue = ((String) value).trim();
        if ("null".equalsIgnoreCase(strValue) || "NULL".equals(strValue)) {
            return "NULL";
        }
        
        return switch (dataType) {
            case Short, Integer, Int, Long -> {
                try {
                    if (strValue.contains(".")) {
                        yield String.valueOf((long) Double.parseDouble(strValue));
                    } else {
                        yield String.valueOf(Long.parseLong(strValue));
                    }
                } catch (NumberFormatException e) {
                    yield "\"" + strValue.replace("\"", "\\\"") + "\"";
                }
            }
            case Float, Double -> {
                try {
                    yield String.valueOf(Double.parseDouble(strValue));
                } catch (NumberFormatException e) {
                    yield "\"" + strValue.replace("\"", "\\\"") + "\"";
                }
            }
            case Boolean -> {
                if ("true".equalsIgnoreCase(strValue) || "1".equals(strValue)) {
                    yield "true";
                } else if ("false".equalsIgnoreCase(strValue) || "0".equals(strValue)) {
                    yield "false";
                } else {
                    yield "\"" + strValue + "\"";
                }
            }
            case Date, DateTime -> {
                yield "\"" + formatDateTime(strValue) + "\"";
            }
            default -> "\"" + strValue.replace("\"", "\\\"") + "\"";
        };
    }
    
    if (value instanceof Number) {
        return value.toString();
    }
    if (value instanceof Boolean) {
        return value.toString();
    }
    if (value instanceof Date) {
        return "\"" + DateFormatUtils.format((Date) value, "yyyy-MM-dd HH:mm:ss") + "\"";
    }
    
    String strValue = value.toString();
    return "\"" + strValue.replace("\"", "\\\"") + "\"";
}
```

#### 6.2.2 JanusUtil 类型转换方法

```java
public static Object convertPropertyValue(Object value, DataType dataType) {
    if (value == null) {
        return null;
    }
    
    if (value instanceof String) {
        String strValue = ((String) value).trim();
        if ("null".equalsIgnoreCase(strValue)) {
            return null;
        }
        
        return switch (dataType) {
            case Integer, Int -> Integer.parseInt(strValue);
            case Long -> Long.parseLong(strValue);
            case Double -> Double.parseDouble(strValue);
            case Float -> Float.parseFloat(strValue);
            case Boolean -> Boolean.parseBoolean(strValue);
            case Date -> parseDate(strValue);
            case DateTime -> parseDateTime(strValue);
            default -> strValue;
        };
    }
    
    return value;
}
```

### 6.3 缓存机制

```java
// GraphDataServiceImpl.java
private final ConcurrentHashMap<Long, GraphInfo> graphInfoCache = new ConcurrentHashMap<>();
private final ConcurrentHashMap<Long, GraphConnection> connectionCache = new ConcurrentHashMap<>();

private GraphInfo getGraphInfo(Long graphId) {
    return graphInfoCache.computeIfAbsent(graphId, id -> graphService.getById(id));
}

private GraphConnection getConnection(Long connectionId) {
    return connectionCache.computeIfAbsent(connectionId, id -> connectionService.getById(id));
}
```

### 6.4 前端可视化

```javascript
// 力导向图配置
const simulation = d3.forceSimulation(nodes)
    .force("link", d3.forceLink(edges).id(d => d.id))
    .force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(width / 2, height / 2));

// 目标节点高亮
node.attr("filter", d => d.group === 'center' ? "url(#glow)" : null)
    .attr("stroke-width", d => d.group === 'center' ? 3 : 1.5)
    .attr("r", d => d.group === 'center' ? 24 : 16);
```

### 6.5 数据转换工具

```java
// transformApiResponseToGraphData 函数
public static GraphData transformApiResponseToGraphData(Object apiResponse) {
    if (apiResponse == null) {
        return new GraphData();
    }
    
    Map<String, Object> rawData = (Map<String, Object>) apiResponse;
    
    if (rawData.containsKey("vertices") && rawData.containsKey("edges")) {
        List<Map<String, Object>> vertices = (List<Map<String, Object>>) rawData.get("vertices");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) rawData.get("edges");
        
        Set<String> nodeIds = new HashSet<>();
        List<GraphVertex> graphVertices = new ArrayList<>();
        for (Map<String, Object> v : vertices) {
            GraphVertex vertex = new GraphVertex();
            vertex.setId((String) v.get("uid"));
            vertex.setUid((String) v.get("uid"));
            vertex.setLabel((String) v.get("label"));
            vertex.setProperties((Map<String, Object>) v.get("properties"));
            graphVertices.add(vertex);
            nodeIds.add((String) v.get("uid"));
        }
        
        List<GraphEdge> graphEdges = new ArrayList<>();
        for (Map<String, Object> e : edges) {
            String source = (String) e.get("startUid");
            String target = (String) e.get("endUid");
            if (nodeIds.contains(source) && nodeIds.contains(target)) {
                GraphEdge edge = new GraphEdge();
                edge.setId((String) e.get("uid"));
                edge.setUid((String) e.get("uid"));
                edge.setLabel((String) e.get("label"));
                edge.setStartUid(source);
                edge.setEndUid(target);
                edge.setProperties((Map<String, Object>) e.get("properties"));
                graphEdges.add(edge);
            }
        }
        
        GraphData data = new GraphData();
        data.setVertices(graphVertices);
        data.setEdges(graphEdges);
        return data;
    }
    
    return new GraphData();
}
```

---

## 七、运行环境设计

### 7.1 硬件环境

#### 7.1.1 最低配置

| 项目 | 要求 |
|-----|------|
| CPU | Intel Core i5-8400 或同等处理器 |
| 内存 | 8GB RAM |
| 硬盘 | 50GB 可用空间 |
| 网络 | 100Mbps 以太网 |

#### 7.1.2 推荐配置

| 项目 | 要求 |
|-----|------|
| CPU | Intel Core i7-10700 或同等处理器 |
| 内存 | 16GB RAM |
| 硬盘 | 100GB SSD 可用空间 |
| 网络 | 1Gbps 以太网 |

### 7.2 软件环境

#### 7.2.1 后端环境

| 软件 | 版本 | 说明 |
|-----|------|------|
| JDK | 21 | 后端开发语言 |
| Spring Boot | 3.2.x | 后端框架 |
| Spring Security | 6.2.x | 安全框架 |
| MyBatis Plus | 3.5.x | ORM 框架 |
| MySQL | 8.0+ | 元数据存储 |
| Redis | 7.0+ | 缓存（可选） |

#### 7.2.2 前端环境

| 软件 | 版本 | 说明 |
|-----|------|------|
| Node.js | 20.x | 前端运行环境 |
| Vue.js | 3.4.x | 前端框架 |
| Vite | 5.2.x | 构建工具 |
| Element Plus | 2.6.x | UI 组件库 |
| D3.js | 7.8.x | 图可视化 |
| ECharts | 5.5.x | 统计图表 |

#### 7.2.3 图数据库环境

| 软件 | 版本 | 说明 |
|-----|------|------|
| NebulaGraph | 3.5+ | 分布式图数据库 |
| JanusGraph | 1.3+ | 分布式图数据库 |
| Neo4j | 5.0+ | 图数据库 |

### 7.3 网络环境

#### 7.3.1 开发环境

| 项目 | 要求 |
|-----|------|
| 协议 | HTTP |
| 端口 | 前端 5173，后端 18080 |
| 访问方式 | 本地访问 localhost |

#### 7.3.2 生产环境

| 项目 | 要求 |
|-----|------|
| 协议 | HTTPS |
| 端口 | 443（HTTPS） |
| 访问方式 | 通过域名访问 |
| 防火墙 | 开放 443、80 端口 |

### 7.4 浏览器支持

| 浏览器 | 版本 | 说明 |
|-------|------|------|
| Chrome | 100+ | 推荐 |
| Firefox | 100+ | 支持 |
| Safari | 15+ | 支持 |
| Edge | 100+ | 支持 |

---

## 八、输入输出设计

### 8.1 用户登录模块

#### 8.1.1 输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| username | String | 字符串 | 必填，长度 3-50 |
| password | String | 字符串 | 必填，长度 6-100 |

#### 8.1.2 输出

| 输出项 | 类型 | 格式 | 说明 |
|-------|------|------|------|
| token | String | JWT 令牌 | 用于后续请求认证 |
| tokenType | String | Bearer | 令牌类型 |
| expiresIn | Long | 毫秒 | 过期时间 |
| username | String | 字符串 | 用户名 |

#### 8.1.3 处理逻辑

```
1. 接收用户名和密码
2. 校验用户名是否存在
3. 校验密码是否正确（BCrypt 比对）
4. 生成 JWT Token（有效期 24 小时）
5. 返回 Token 信息
```

### 8.2 连接管理模块

#### 8.2.1 新增连接输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| name | String | 字符串 | 必填，长度 1-100 |
| type | String | neo4j/nebula/janus | 必填 |
| host | String | IP 地址或域名 | 必填 |
| port | Integer | 数字 | 必填，范围 1-65535 |
| username | String | 字符串 | 可选 |
| password | String | 字符串 | 可选 |
| description | String | 字符串 | 可选，长度 0-500 |

#### 8.2.2 测试连接输出

| 输出项 | 类型 | 格式 | 说明 |
|-------|------|------|------|
| responseTime | Long | 毫秒 | 连接响应时间 |
| version | String | 字符串 | 数据库版本号 |
| nodes | Long | 数字 | 节点数量 |
| edges | Long | 数字 | 边数量 |

#### 8.2.3 处理逻辑

```
1. 接收连接参数
2. 根据类型创建对应的图数据库客户端
3. 尝试建立连接
4. 获取数据库版本和统计信息
5. 关闭连接
6. 返回测试结果
```

### 8.3 图管理模块

#### 8.3.1 创建图输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| name | String | 字符串 | 必填，长度 1-100 |
| code | String | 字符串 | 必填，长度 1-100，唯一 |
| connectionId | Long | 数字 | 必填 |
| description | String | 字符串 | 可选 |

#### 8.3.2 查询图列表输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| page | Integer | 数字 | 默认 1，最小 1 |
| pageSize | Integer | 数字 | 默认 10，范围 1-100 |
| keyword | String | 字符串 | 可选，模糊搜索名称或编码 |

#### 8.3.3 查询图列表输出

| 输出项 | 类型 | 说明 |
|-------|------|------|
| records | List | 图信息列表 |
| - id | Long | 图 ID |
| - name | String | 图名称 |
| - code | String | 图编码 |
| - status | Integer | 状态 |
| - createTime | String | 创建时间 |
| total | Long | 总记录数 |
| current | Integer | 当前页码 |
| pages | Integer | 总页数 |

### 8.4 Schema 管理模块

#### 8.4.1 新增节点类型输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| graphId | Long | 数字 | 必填 |
| label | String | 字符串 | 必填，唯一 |
| name | String | 字符串 | 可选 |
| properties | List | 对象数组 | 属性定义列表 |
| - code | String | 字符串 | 属性编码 |
| - name | String | 字符串 | 属性名称 |
| - dataType | String | 枚举值 | 数据类型 |
| - isRequired | Boolean | 布尔值 | 是否必填 |

#### 8.4.2 发布 Schema 输出

| 输出项 | 类型 | 格式 | 说明 |
|-------|------|------|------|
| message | String | 字符串 | 操作结果消息 |

#### 8.4.3 处理逻辑

```
1. 获取图信息和连接配置
2. 查询本地节点和边定义
3. 根据图类型生成 Schema 创建语句
4. 执行 Schema 创建
5. 更新本地元数据状态
6. 返回结果
```

### 8.5 数据管理模块

#### 8.5.1 新增节点数据输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| graphId | Long | 数字 | 必填 |
| nodeTypeId | Long | 数字 | 必填 |
| properties | Map | 键值对 | 属性值 |

#### 8.5.2 批量导入输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| graphId | Long | 数字 | 必填 |
| nodeTypeId | Long | 数字 | 必填 |
| file | MultipartFile | CSV 文件 | 必填 |
| mapping | String | JSON | 字段映射 |

#### 8.5.3 导入结果输出

| 输出项 | 类型 | 说明 |
|-------|------|------|
| successCount | Integer | 成功导入数量 |
| failureCount | Integer | 失败数量 |
| totalCount | Integer | 总数量 |
| errorMessages | List | 错误信息列表 |

#### 8.5.4 处理逻辑

```
1. 获取节点类型定义
2. 构建属性类型映射
3. 解析 CSV 文件
4. 转换属性值格式
5. 批量插入图数据库
6. 返回导入结果
```

### 8.6 图查询模块

#### 8.6.1 输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| graphId | Long | 数字 | 必填 |
| cypher | String | 查询语句 | 必填 |

#### 8.6.2 输出

| 输出项 | 类型 | 说明 |
|-------|------|------|
| vertices | List | 节点列表 |
| - id | String | 节点 ID |
| - label | String | 节点标签 |
| - properties | Map | 属性 |
| edges | List | 边列表 |
| - id | String | 边 ID |
| - label | String | 边标签 |
| - source | String | 起点 ID |
| - target | String | 终点 ID |
| - properties | Map | 属性 |

#### 8.6.3 处理逻辑

```
1. 获取图信息和连接配置
2. 根据图类型选择查询解析器
3. 执行查询语句
4. 解析查询结果
5. 转换为统一格式
6. 返回结果
```

### 8.7 图分析模块

#### 8.7.1 K 层展开输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| graphId | Long | 数字 | 必填 |
| nodeId | String | 字符串 | 必填 |
| label | String | 字符串 | 可选 |
| depth | Integer | 数字 | 默认 1，范围 1-5 |

#### 8.7.2 最短路径输入

| 输入项 | 类型 | 格式 | 约束 |
|-------|------|------|------|
| graphId | Long | 数字 | 必填 |
| startNodeId | String | 字符串 | 必填 |
| endNodeId | String | 字符串 | 必填 |
| maxDepth | Integer | 数字 | 默认 5，范围 1-10 |

#### 8.7.3 最短路径输出

| 输出项 | 类型 | 说明 |
|-------|------|------|
| pathLength | Integer | 路径长度 |
| vertexCount | Integer | 顶点数 |
| path | List | 路径节点列表 |

---

## 九、出错处理设计

### 9.1 错误分类

| 错误类别 | 说明 | 处理方式 |
|---------|------|---------|
| 参数错误 | 请求参数缺失或格式错误 | 返回 400 错误码，提示具体错误信息 |
| 认证错误 | 用户未登录或 Token 无效 | 返回 401 错误码 |
| 权限错误 | 用户无权限访问资源 | 返回 403 错误码 |
| 资源不存在 | 请求的资源不存在 | 返回 404 错误码 |
| 业务错误 | 业务逻辑校验失败 | 返回自定义错误码和消息 |
| 系统错误 | 服务器内部错误 | 返回 500 错误码，记录详细日志 |
| 数据库错误 | 数据库连接或操作失败 | 返回 500 错误码，记录详细日志 |
| 图数据库错误 | 图数据库连接或查询失败 | 返回业务错误码，提示用户重试 |

### 9.2 错误处理流程

#### 9.2.1 后端错误处理流程

```
用户发起请求
      ↓
Controller 接收请求
      ↓
参数校验（@Valid）
      ↓
┌─────────────────────┐
│ 校验失败？          │
└─────────┬───────────┘
          ↓ Yes
   返回 400 错误
          ↓ No
Service 层处理业务逻辑
      ↓
┌─────────────────────┐
│ 业务校验失败？      │
└─────────┬───────────┘
          ↓ Yes
   抛出 GraphException
          ↓ No
调用数据层操作
      ↓
┌─────────────────────┐
│ 操作成功？          │
└─────────┬───────────┘
          ↓ Yes
   返回成功结果
          ↓ No
┌─────────────────────┐
│ 异常类型判断        │
└─────────┬───────────┘
          ↓
   GraphException → 返回业务错误码
   RuntimeException → 返回 500，记录日志
   其他异常 → 返回 500，记录日志
```

#### 9.2.2 前端错误处理流程

```
发起 API 请求
      ↓
┌─────────────────────┐
│ 请求成功？          │
└─────────┬───────────┘
          ↓ Yes
   处理响应数据
          ↓ No
┌─────────────────────┐
│ 错误码判断          │
└─────────┬───────────┘
          ↓
   401 → 跳转登录页
   403 → 提示无权限
   404 → 提示资源不存在
   500 → 提示系统错误，请重试
   其他 → 显示具体错误信息
```

### 9.3 统一异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(GraphException.class)
    public Result<?> handleGraphException(GraphException e) {
        log.warn("Graph exception: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation exception: {}", message);
        return Result.error(400, message);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication exception: {}", e.getMessage());
        return Result.error(401, "未授权");
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.error(403, "无权限");
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("System error: ", e);
        return Result.error(500, "系统内部错误，请稍后重试");
    }
}
```

### 9.4 错误码体系

#### 9.4.1 HTTP 状态码

| 状态码 | 含义 | 使用场景 |
|-------|------|---------|
| 200 | 成功 | 所有成功的请求 |
| 400 | 请求错误 | 参数校验失败、请求格式错误 |
| 401 | 未授权 | Token 无效或过期、未登录 |
| 403 | 禁止访问 | 权限不足 |
| 404 | 资源不存在 | 请求的资源未找到 |
| 500 | 服务器错误 | 系统内部异常 |

#### 9.4.2 业务错误码

| 错误码 | 含义 | 使用模块 |
|-------|------|---------|
| 1001 | 连接失败 | 连接管理 |
| 1002 | 查询执行失败 | 查询模块 |
| 1003 | Schema 校验失败 | Schema 管理 |
| 1004 | 事务执行失败 | 数据管理 |
| 1005 | 不支持的操作 | 通用 |
| 1006 | 无效的配置 | 配置管理 |
| 2001 | 用户不存在 | 用户管理 |
| 2002 | 用户已存在 | 用户管理 |
| 2003 | 角色不存在 | 用户管理 |
| 2004 | 权限不足 | 权限管理 |
| 2005 | 用户名或密码错误 | 认证 |
| 2006 | 账户已被禁用 | 用户管理 |
| 2007 | 图不存在 | 图管理 |
| 2008 | 图已存在 | 图管理 |
| 2009 | 无效的 Schema 定义 | Schema 管理 |
| 2010 | 图数据库连接失败 | 连接管理 |
| 2011 | 图查询失败 | 查询模块 |
| 2012 | 图数据导入失败 | 数据管理 |

### 9.5 日志记录

#### 9.5.1 日志级别

| 级别 | 使用场景 |
|-----|---------|
| DEBUG | 开发调试信息，详细的执行流程 |
| INFO | 正常业务流程记录，如接口调用、操作成功 |
| WARN | 警告信息，如参数校验失败、重试操作 |
| ERROR | 错误信息，如异常堆栈、系统故障 |

#### 9.5.2 日志格式

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36}:%L - %msg%n
```

#### 9.5.3 日志内容规范

- **请求日志**：记录请求路径、方法、参数、用户信息
- **响应日志**：记录响应时间、状态码、数据大小
- **异常日志**：记录异常类型、堆栈信息、上下文参数
- **业务日志**：记录关键业务操作，如数据导入、Schema 发布

---

## 十、安全性设计

### 10.1 认证与授权

#### 10.1.1 JWT 认证机制

```
用户登录
      ↓
验证用户名密码
      ↓
生成 JWT Token（含用户信息、过期时间）
      ↓
返回 Token 给前端
      ↓
前端存储 Token 到 localStorage
      ↓
后续请求携带 Authorization: Bearer {token}
      ↓
后端过滤器验证 Token
      ↓
验证通过 → 继续处理请求
验证失败 → 返回 401 错误
```

#### 10.1.2 Token 结构

```json
{
  "sub": "user1",
  "exp": 1719878400,
  "iat": 1719792000,
  "roles": ["ADMIN", "USER"]
}
```

| 字段 | 说明 |
|-----|------|
| sub | 用户名 |
| exp | 过期时间戳 |
| iat | 签发时间戳 |
| roles | 用户角色列表 |

#### 10.1.3 权限控制

采用 RBAC（基于角色的访问控制）模型：

| 角色 | 权限 |
|-----|------|
| ADMIN | 全部权限 |
| USER | 查看和操作自己创建的图 |

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {
    // 管理员才能删除用户
}

@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public GraphData query(Long graphId) {
    // 管理员和普通用户都可以查询
}
```

### 10.2 数据安全

#### 10.2.1 密码加密

用户密码使用 BCrypt 算法加密存储：

```java
// 加密
String encodedPassword = passwordEncoder.encode(rawPassword);

// 验证
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

#### 10.2.2 敏感数据保护

- **配置文件**：数据库密码、密钥等敏感信息使用环境变量或配置中心管理
- **日志输出**：禁止在日志中输出密码、Token 等敏感信息
- **传输加密**：生产环境使用 HTTPS 协议

#### 10.2.3 SQL 注入防护

使用 MyBatis Plus 的参数化查询，避免 SQL 注入：

```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getUsername, username);  // 参数化查询，安全
```

### 10.3 防护措施

#### 10.3.1 请求限流

对高频接口（如登录、查询）实施限流，防止暴力攻击：

```java
@RateLimiter(name = "login", fallback = "loginFallback")
public Result<LoginResponse> login(@RequestBody LoginRequest request) {
    // 登录逻辑
}
```

#### 10.3.2 XSS 防护

前端使用 Element Plus 的表单组件，自动进行 XSS 过滤。后端使用 Spring Security 的 XSS 过滤器。

#### 10.3.3 CSRF 防护

由于使用 JWT 无状态认证，CSRF 风险较低。对于敏感操作（如删除、修改），要求请求携带有效的 JWT Token。

#### 10.3.4 会话管理

- Token 有效期：24 小时
- 过期处理：返回 401 错误，前端自动跳转登录页
- 刷新机制：可选实现 Token 刷新接口

---

## 十一、部署方案

### 11.1 部署架构

```
┌──────────────────────────────────────────────────────────┐
│                    Nginx 反向代理                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   HTTPS:443  │  │   HTTP:80    │  │   负载均衡    │   │
│  └──────┬───────┘  └──────────────┘  └──────┬───────┘   │
└─────────┼──────────────────────────────────┼────────────┘
          ↓                                  ↓
┌──────────────────────┐        ┌──────────────────────┐
│      前端应用         │        │      后端服务集群     │
│   Vue.js + Nginx     │        │  Spring Boot × N     │
└──────────────────────┘        └──────────┬───────────┘
                                           ↓
                    ┌──────────────────────┼──────────────┐
                    ↓                      ↓              ↓
            ┌─────────────┐      ┌─────────────┐  ┌─────────────┐
            │   MySQL     │      │    Redis    │  │  图数据库    │
            │  元数据存储  │      │   缓存      │  │ (Nebula/    │
            └─────────────┘      └─────────────┘   │  Janus/     │
                                                   │  Neo4j)     │
                                                   └─────────────┘
```

### 11.2 环境要求

#### 11.2.1 开发环境

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 21 | 后端开发 |
| Node.js | 20.x | 前端开发 |
| MySQL | 8.0+ | 本地数据库 |
| Docker | 20.10+ | 图数据库容器 |

#### 11.2.2 测试环境

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 21 | 后端运行 |
| Nginx | 1.24+ | 前端部署 |
| MySQL | 8.0+ | 测试数据库 |
| Redis | 7.0+ | 缓存 |

#### 11.2.3 生产环境

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 21 | 后端运行 |
| Nginx | 1.24+ | 负载均衡 |
| MySQL | 8.0+ | 生产数据库（主从） |
| Redis | 7.0+ | 缓存（集群） |
| 图数据库 | 根据需求选择 | NebulaGraph/JanusGraph/Neo4j |

### 11.3 容器化部署

#### 11.3.1 Docker Compose 配置

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: graph-mind-mysql
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: graph_mind
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    restart: always

  redis:
    image: redis:7.0
    container_name: graph-mind-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: always

  backend:
    build:
      context: ./graph-mind-admin
      dockerfile: Dockerfile
    container_name: graph-mind-backend
    ports:
      - "18080:18080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis
    restart: always

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: graph-mind-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: always

volumes:
  mysql_data:
  redis_data:
```

#### 11.3.2 Dockerfile（后端）

```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/graph-mind-admin.jar app.jar

EXPOSE 18080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

#### 11.3.3 Dockerfile（前端）

```dockerfile
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:1.24-alpine

COPY --from=build /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 11.4 配置管理

#### 11.4.1 配置文件结构

```
config/
├── application.yml          # 默认配置
├── application-dev.yml      # 开发环境
├── application-test.yml     # 测试环境
└── application-prod.yml     # 生产环境
```

#### 11.4.2 关键配置说明

| 配置项 | 说明 | 示例值 |
|-------|------|-------|
| server.port | 服务端口 | 18080 |
| spring.datasource.* | 数据库连接配置 | MySQL 连接信息 |
| spring.redis.* | Redis 连接配置 | Redis 连接信息 |
| jwt.secret | JWT 密钥 | 32位以上随机字符串 |
| jwt.expiration | Token 有效期 | 86400000（毫秒） |

### 11.5 运维监控

#### 11.5.1 健康检查

```java
@RestController
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
```

#### 11.5.2 日志管理

- 使用 ELK（Elasticsearch + Logstash + Kibana）进行日志收集和分析
- 日志按日期滚动，保留 30 天
- 关键业务日志单独索引，便于查询

#### 11.5.3 监控指标

| 指标 | 说明 |
|-----|------|
| CPU 使用率 | 服务器 CPU 负载 |
| 内存使用率 | 服务器内存使用情况 |
| 数据库连接数 | MySQL 连接池状态 |
| 请求响应时间 | API 接口响应时间 |
| 错误率 | 接口错误请求比例 |

---

## 十二、项目文件结构

### 12.1 后端项目结构

```
graph-mind/
├── graph-mind-admin/              # 管理后台服务
│   ├── src/main/java/com/chenpp/graph/admin/
│   │   ├── controller/            # REST API 控制器
│   │   │   ├── AuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── GraphController.java
│   │   │   ├── GraphConnectionController.java
│   │   │   ├── GraphSchemaController.java
│   │   │   └── GraphDataController.java
│   │   ├── service/               # 业务逻辑层
│   │   │   ├── AuthService.java
│   │   │   ├── UserService.java
│   │   │   ├── GraphService.java
│   │   │   ├── GraphConnectionService.java
│   │   │   ├── GraphSchemaService.java
│   │   │   └── GraphDataService.java
│   │   ├── service/impl/          # 业务逻辑实现
│   │   │   ├── AuthServiceImpl.java
│   │   │   ├── UserServiceImpl.java
│   │   │   ├── GraphServiceImpl.java
│   │   │   ├── GraphConnectionServiceImpl.java
│   │   │   ├── GraphSchemaServiceImpl.java
│   │   │   └── GraphDataServiceImpl.java
│   │   ├── dao/                   # 数据访问层
│   │   │   ├── UserDao.java
│   │   │   ├── GraphInfoDao.java
│   │   │   ├── GraphConnectionDao.java
│   │   │   ├── GraphVertexDefDao.java
│   │   │   ├── GraphEdgeDefDao.java
│   │   │   └── GraphPropertyDefDao.java
│   │   ├── entity/                # 数据库实体
│   │   │   ├── User.java
│   │   │   ├── GraphInfo.java
│   │   │   ├── GraphConnection.java
│   │   │   ├── GraphVertexDef.java
│   │   │   ├── GraphEdgeDef.java
│   │   │   └── GraphPropertyDef.java
│   │   ├── dto/                   # 数据传输对象
│   │   │   ├── request/
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── GraphCreateRequest.java
│   │   │   │   └── ImportRequest.java
│   │   │   └── response/
│   │   │       ├── LoginResponse.java
│   │   │       ├── GraphDataResponse.java
│   │   │       └── ImportResult.java
│   │   ├── config/                # 配置类
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtConfig.java
│   │   │   └── MyBatisPlusConfig.java
│   │   ├── security/              # 安全相关
│   │   │   ├── JwtTokenFilter.java
│   │   │   ├── JwtUtil.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── exception/             # 异常处理
│   │   │   ├── GraphException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── GraphMindAdminApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml        # 应用配置
│   │   ├── application-dev.yml    # 开发环境配置
│   │   └── mapper/                # MyBatis 映射文件
│   └── pom.xml

├── graph-mind-core/               # 核心模块
│   ├── src/main/java/com/chenpp/graph/core/
│   │   ├── model/                 # 通用模型
│   │   │   ├── GraphVertex.java
│   │   │   ├── GraphEdge.java
│   │   │   ├── GraphData.java
│   │   │   ├── GraphSchema.java
│   │   │   ├── GraphEntity.java
│   │   │   └── GraphRelation.java
│   │   ├── enums/                 # 枚举定义
│   │   │   └── DataType.java
│   │   └── util/                  # 通用工具
│   │       └── GraphUtils.java
│   └── pom.xml

├── graph-mind-janus/              # JanusGraph 适配器
│   ├── graph-janus-core/
│   │   ├── src/main/java/com/chenpp/graph/janus/
│   │   │   ├── JanusClient.java
│   │   │   ├── JanusClientFactory.java
│   │   │   ├── JanusGraphOperations.java
│   │   │   ├── JanusGraphDataOperations.java
│   │   │   └── util/
│   │   │       └── JanusUtil.java
│   │   └── pom.xml
│   └── pom.xml

├── graph-mind-nebula/             # NebulaGraph 适配器
│   ├── graph-nebula-core/
│   │   ├── src/main/java/com/chenpp/graph/nebula/
│   │   │   ├── NebulaClient.java
│   │   │   ├── NebulaClientFactory.java
│   │   │   ├── NebulaGraphOperations.java
│   │   │   ├── NebulaGraphDataOperations.java
│   │   │   └── util/
│   │   │       └── NebulaUtil.java
│   │   └── pom.xml
│   └── pom.xml

├── graph-mind-neo4j/              # Neo4j 适配器
│   ├── graph-neo4j-core/
│   │   ├── src/main/java/com/chenpp/graph/neo4j/
│   │   │   ├── Neo4jClient.java
│   │   │   ├── Neo4jClientFactory.java
│   │   │   ├── Neo4jGraphOperations.java
│   │   │   ├── Neo4jGraphDataOperations.java
│   │   │   └── util/
│   │   │       └── Neo4jUtil.java
│   │   └── pom.xml
│   └── pom.xml

├── graph-mind-common/             # 公共模块
│   ├── src/main/java/com/chenpp/graph/common/
│   │   ├── result/                # 统一返回结果
│   │   │   ├── Result.java
│   │   │   └── PageResult.java
│   │   ├── exception/             # 通用异常
│   │   │   └── BusinessException.java
│   │   └── util/                  # 通用工具类
│   │       └── StringUtils.java
│   └── pom.xml

└── pom.xml                        # 父工程配置
```

### 12.2 前端项目结构

```
frontend/
├── public/                        # 静态资源
│   ├── index.html                 # 入口 HTML
│   └── favicon.ico                # 网站图标

├── src/
│   ├── api/                       # API 接口定义
│   │   ├── auth.js                # 认证接口
│   │   ├── user.js                # 用户接口
│   │   ├── graph.js               # 图管理接口
│   │   ├── connection.js          # 连接管理接口
│   │   ├── schema.js              # Schema 管理接口
│   │   ├── data.js                # 数据管理接口
│   │   └── analysis.js            # 分析接口
│   ├── components/                # 公共组件
│   │   ├── Layout.vue             # 布局组件
│   │   ├── Sidebar.vue            # 侧边栏
│   │   ├── Header.vue             # 头部导航
│   │   ├── Breadcrumb.vue         # 面包屑
│   │   └── GraphVisual.vue        # 图可视化组件
│   ├── views/                     # 页面视图
│   │   ├── login/                 # 登录页面
│   │   │   └── LoginView.vue
│   │   ├── dashboard/             # 仪表盘
│   │   │   └── DashboardView.vue
│   │   ├── graph/                 # 图管理
│   │   │   └── GraphManagementView.vue
│   │   ├── connection/            # 连接管理
│   │   │   └── ConnectionView.vue
│   │   ├── schema/                # Schema 设计
│   │   │   └── SchemaDesignView.vue
│   │   ├── data/                  # 数据管理
│   │   │   └── GraphDataView.vue
│   │   ├── query/                 # 图查询
│   │   │   └── GraphQueryView.vue
│   │   └── analysis/              # 图分析
│   │       └── GraphAnalysisView.vue
│   ├── router/                    # 路由配置
│   │   └── index.js
│   ├── store/                     # 状态管理
│   │   ├── user.js                # 用户状态
│   │   ├── graph.js               # 图状态
│   │   └── app.js                 # 应用状态
│   ├── utils/                     # 工具函数
│   │   ├── request.js             # HTTP 请求封装
│   │   ├── auth.js                # 认证工具
│   │   ├── graph.js               # 图数据处理
│   │   └── date.js                # 日期格式化
│   ├── styles/                    # 样式文件
│   │   ├── reset.css              # 全局重置样式
│   │   ├── variables.css          # CSS 变量
│   │   └── global.css             # 全局样式
│   ├── App.vue                    # 根组件
│   └── main.js                    # 入口文件

├── package.json                   # 依赖配置
├── vite.config.js                 # Vite 配置
├── .env                           # 环境变量
├── .env.development               # 开发环境变量
├── .env.production                # 生产环境变量
└── index.html                     # 入口页面
```

---

## 附录

### A. 核心类图

```
┌─────────────────────────────────────────────────────────────────┐
│                        GraphClient 接口                          │
│  + connect()                                                    │
│  + close()                                                      │
│  + getGraphOperations(): GraphOperations                        │
│  + getDataOperations(): GraphDataOperations                     │
│  + getSummary(): GraphSummary                                   │
└─────────────────────────────────────────────────────────────────┘
                              △
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────┴───────┐      ┌──────┴──────┐      ┌───────┴───────┐
│ JanusClient   │      │ NebulaClient │      │   Neo4jClient │
└───────┬───────┘      └──────┬──────┘      └───────┬───────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│JanusGraphOps    │    │NebulaGraphOps   │    │Neo4jGraphOps    │
│JanusGraphDataOps│    │NebulaGraphDataOps│    │Neo4jGraphDataOps│
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### B. 数据流转图

```
前端请求                                    后端处理
   │                                           │
   │── POST /api/graphs/data/nodes ────────────→│
   │                                           │
   │                                           │── 校验参数
   │                                           │
   │                                           │── 获取图信息（缓存）
   │                                           │
   │                                           │── 获取连接配置（缓存）
   │                                           │
   │                                           │── 根据属性类型转换值
   │                                           │
   │                                           │── 调用图数据库客户端
   │                                           │
   │◀── 返回操作结果 ──────────────────────────│
   │                                           │
   │                                           ▼
   │                                     图数据库
   │                                           │
   │                                           └── INSERT VERTEX/EDGE
```

### C. 数据库 ER 图

```
┌───────────┐      ┌─────────────────┐      ┌─────────────┐
│    user   │      │graph_connection │      │ graph_info  │
├───────────┤      ├─────────────────┤      ├─────────────┤
│ id (PK)   │      │ id (PK)         │      │ id (PK)     │
│ username  │      │ name            │      │ name        │
│ password  │      │ type            │      │ code (UK)   │
│ status    │      │ host            │      │ connection_id│
└───────────┘      │ port            │      │ status      │
                   │ username        │      └──────┬──────┘
                   │ password        │             │
                   └─────────────────┘             │
                                                   ▼
                                    ┌─────────────────────────┐
                                    │    graph_vertex_def     │
                                    ├─────────────────────────┤
                                    │ id (PK)                 │
                                    │ graph_id (FK)           │
                                    │ label                   │
                                    │ name                    │
                                    └─────────────────────────┘
                                                   ▼
                                    ┌─────────────────────────┐
                                    │    graph_edge_def       │
                                    ├─────────────────────────┤
                                    │ id (PK)                 │
                                    │ graph_id (FK)           │
                                    │ label                   │
                                    │ from_label              │
                                    │ to_label                │
                                    └─────────────────────────┘
                                                   ▼
                                    ┌─────────────────────────┐
                                    │   graph_property_def    │
                                    ├─────────────────────────┤
                                    │ id (PK)                 │
                                    │ entity_id               │
                                    │ entity_type (VERTEX/EDGE)│
                                    │ code                    │
                                    │ data_type               │
                                    │ is_required             │
                                    └─────────────────────────┘
```

### D. 版本历史

| 版本 | 日期 | 变更说明 |
|-----|------|---------|
| V1.0 | 2026-07-02 | 初始版本，完成基础功能设计 |

---

**文档结束**5