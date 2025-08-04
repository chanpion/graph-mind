import Mock from 'mockjs'

// 设置延迟时间
Mock.setup({
  timeout: '200-600'
})

// 图连接管理相关接口
const connectionApis = {
  // 获取连接列表
  'GET /api/connections': {
    code: 200,
    message: '获取成功',
    data: {
      'records|10-20': [{
        'id|+1': 1,
        'name': '@ctitle(5, 10)',
        'type|1': ['neo4j', 'nebula', 'janus'],
        'host': '@ip',
        'port|1': [7687, 9669, 8182, 7474, 8529],
        'database': '@word(5, 10)',
        'username': '@word(4, 8)',
        'status|0-2': 0,
        'poolSize|1-20': 10,
        'timeout|10-60': 30,
        'description': '@csentence(10, 30)',
        'createTime': '@datetime',
        'updateTime': '@datetime'
      }],
      'total': '@integer(10, 50)'
    }
  },

  // 新增连接
  'POST /api/connections': {
    code: 200,
    message: '添加成功',
    data: {
      'id': '@integer(1000, 9999)'
    }
  },

  // 更新连接
  'PUT /api/connections/:id': {
    code: 200,
    message: '更新成功',
    data: null
  },

  // 删除连接
  'DELETE /api/connections/:id': {
    code: 200,
    message: '删除成功',
    data: null
  },

  // 连接数据库
  'POST /api/connections/:id/connect': {
    code: 200,
    message: '连接成功',
    data: {
      'status': 'connected',
      'lastConnectTime': '@now'
    }
  },

  // 断开连接
  'POST /api/connections/:id/disconnect': {
    code: 200,
    message: '断开成功',
    data: {
      'status': 'disconnected'
    }
  },

  // 测试连接
  'POST /api/connections/:id/test': {
    code: 200,
    message: '连接测试成功',
    data: {
      'responseTime': '@integer(10, 500)',
      'version': '@word(5, 15)',
      'nodes|1-1000': '@integer(100, 10000)',
      'edges|1-1000': '@integer(100, 10000)'
    }
  }
}

// 注册所有接口
Object.keys(connectionApis).forEach(key => {
  const [method, url] = key.split(' ')
  Mock.mock(new RegExp(url.replace(/:\w+/g, '\\d+')), method.toLowerCase(), connectionApis[key])
})

// 图管理相关接口
const graphApis = {
  // 获取图列表
  'GET /api/graphs': {
    code: 200,
    message: '获取成功',
    data: {
      'records|5-10': [{
        'id|+1': 1,
        'name': '@ctitle(3, 8)图',
        'code': '@word(5, 15)',
        'description': '@csentence(10, 30)',
        'status|0-1': 1,
        'connectionId|1-3': 1,
        'createTime': '@datetime',
        'updateTime': '@datetime'
      }],
      'total': '@integer(5, 10)'
    }
  },

  // 根据连接ID获取图列表
  'GET /api/graphs/connection/:connectionId': {
    code: 200,
    message: '获取成功',
    data: {
      'records|3-8': [{
        'id|+1': 1,
        'name': '@ctitle(3, 8)图',
        'code': '@word(5, 15)',
        'description': '@csentence(10, 30)',
        'status|0-1': 1,
        'connectionId': /\d+/,
        'createTime': '@datetime',
        'updateTime': '@datetime'
      }],
      'total': '@integer(3, 8)'
    }
  },

  // 新增图
  'POST /api/graphs': {
    code: 200,
    message: '添加成功',
    data: {
      'id': '@integer(1000, 9999)'
    }
  },

  // 更新图
  'PUT /api/graphs/:id': {
    code: 200,
    message: '更新成功',
    data: null
  },

  // 删除图
  'DELETE /api/graphs/:id': {
    code: 200,
    message: '删除成功',
    data: null
  },

  // 获取图详情
  'GET /api/graphs/:id': {
    code: 200,
    message: '获取成功',
    data: {
      'id': /\d+/,
      'name': '@ctitle(3, 8)图',
      'code': '@word(5, 15)',
      'description': '@csentence(10, 30)',
      'status|0-1': 1,
      'connectionId|1-3': 1,
      'createTime': '@datetime',
      'updateTime': '@datetime'
    }
  }
}

// 注册图管理接口
Object.keys(graphApis).forEach(key => {
  const [method, url] = key.split(' ')
  Mock.mock(new RegExp(url.replace(/:\w+/g, '\\d+')), method.toLowerCase(), graphApis[key])
})

// 图详情相关接口
Mock.mock(/\/api\/graphs\/\d+\/nodes/, 'get', {
  code: 200,
  message: '获取成功',
  data: {
    'list|2-4': [
      {
        'id|+1': 1,
        'name|+1': ['Person', 'Company', 'Product'],
        'description': '@csentence(8, 20)',
        'status|1': ['active', 'inactive'],
        'properties|2-4': [
          { 'name': '@word(3,8)', 'type|1': ['string', 'int', 'date', 'float'], 'desc': '@cword(2,6)' }
        ]
      }
    ]
  }
})
Mock.mock(/\/api\/graphs\/\d+\/edges/, 'get', {
  code: 200,
  message: '获取成功',
  data: {
    'list|1-3': [
      {
        'id|+1': 1,
        'name|+1': ['WorksAt', 'Owns', 'Buys'],
        'from|1': ['Person', 'Company', 'Product'],
        'to|1': ['Company', 'Product', 'Person'],
        'description': '@csentence(8, 20)',
        'status|1': ['active', 'inactive'],
        'properties|1-3': [
          { 'name': '@word(3,8)', 'type|1': ['string', 'int', 'date', 'float'], 'desc': '@cword(2,6)' }
        ]
      }
    ]
  }
})

// 点定义增删改查
Mock.mock(/\/api\/graphs\/\d+\/nodes/, 'post', {
  code: 200,
  message: '新增成功',
  data: null
})
Mock.mock(/\/api\/graphs\/\d+\/nodes\/\d+/, 'put', {
  code: 200,
  message: '更新成功',
  data: null
})
Mock.mock(/\/api\/graphs\/\d+\/nodes\/\d+/, 'delete', {
  code: 200,
  message: '删除成功',
  data: null
})
// 边定义增删改查
Mock.mock(/\/api\/graphs\/\d+\/edges/, 'post', {
  code: 200,
  message: '新增成功',
  data: null
})
Mock.mock(/\/api\/graphs\/\d+\/edges\/\d+/, 'put', {
  code: 200,
  message: '更新成功',
  data: null
})
Mock.mock(/\/api\/graphs\/\d+\/edges\/\d+/, 'delete', {
  code: 200,
  message: '删除成功',
  data: null
})

// 查询图数据
Mock.mock(/\/api\/graphs\/\d+\/query/, 'post', {
  code: 200,
  message: '查询成功',
  data: {
    nodes: [
      { id: '1', label: 'Alice', properties: [ { name: 'age', value: 30 }, { name: 'city', value: 'Beijing' } ] },
      { id: '2', label: 'Bob', properties: [ { name: 'age', value: 28 }, { name: 'city', value: 'Shanghai' } ] },
      { id: '3', label: 'Company', properties: [ { name: 'type', value: 'Tech' } ] }
    ],
    edges: [
      { id: 'e1', source: '1', target: '2', label: 'knows', properties: [ { name: 'since', value: 2015 } ] },
      { id: 'e2', source: '2', target: '3', label: 'worksAt', properties: [ { name: 'since', value: 2020 } ] }
    ]
  }
})

// 图数据加工接口
Mock.mock(/\/api\/graphs\/\d+\/import\/prepare/, 'post', {
  code: 200,
  message: '准备成功',
  data: {
    headers: ['id', 'name', 'age', 'email'],
    sampleData: [
      { id: '1', name: '张三', age: '25', email: 'zhangsan@example.com' },
      { id: '2', name: '李四', age: '30', email: 'lisi@example.com' }
    ]
  }
})

Mock.mock(/\/api\/graphs\/\d+\/import\/execute/, 'post', {
  code: 200,
  message: '导入成功',
  data: {
    successCount: '@integer(50, 1000)',
    failCount: '@integer(0, 10)',
    duration: '@integer(1000, 10000)'
  }
})

// 数据库类型配置
export const databaseTypes = {
  neo4j: {
    name: 'Neo4j',
    defaultPort: 7687,
    description: '最流行的图数据库，支持Cypher查询语言',
    features: ['Cypher查询语言', 'ACID事务', '图算法库', '可视化工具']
  },
  nebula: {
    name: 'Nebula Graph',
    defaultPort: 9669,
    description: '分布式图数据库，支持大规模图数据处理',
    features: ['分布式架构', '高可用性', '图计算引擎', '多租户支持']
  },
  janus: {
    name: 'JanusGraph',
    defaultPort: 8182,
    description: '分布式图数据库，支持多种存储后端',
    features: ['多种存储后端', '图遍历语言', '事务支持', '索引优化']
  }
}

export default Mock 