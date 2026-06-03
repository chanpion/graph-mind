/**
 * 路由表配置
 * 所有路由在此定义，lazy loaded
 */
export const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    redirect: '/home/dashboard',
    meta: { hidden: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { title: '首页' },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '首页', icon: 'House' }
      },
      // 连接管理
      {
        path: 'connections',
        name: 'Connections',
        component: () => import('@/views/connections/ConnectionView.vue'),
        meta: { title: '连接管理', icon: 'Connection' }
      },
      // 图管理
      {
        path: 'graphs',
        name: 'Graphs',
        component: () => import('@/views/graphs/GraphListView.vue'),
        meta: { title: '图管理', icon: 'Grid' }
      },
      // D3 可视化
      {
        path: 'visualization',
        name: 'GraphVisualization',
        component: () => import('@/views/visualization/GraphVisualView.vue'),
        meta: { title: '图查询', icon: 'Share' }
      },
      // 数据建模
      {
        path: 'modeling',
        name: 'DataModeling',
        component: () => import('@/views/modeling/ModelingView.vue'),
        meta: { title: '图建模', icon: 'DataLine' }
      },
      // 数据 CRUD
      {
        path: 'data',
        name: 'GraphData',
        component: () => import('@/views/data/GraphDataView.vue'),
        meta: { title: '图数据', icon: 'List' }
      },
      // 图分析
      {
        path: 'analysis',
        name: 'GraphAnalysis',
        component: () => import('@/views/analysis/GraphAnalysisView.vue'),
        meta: { title: '图分析', icon: 'DataAnalysis' }
      },
      // 图统计
      {
        path: 'summary',
        name: 'GraphSummary',
        component: () => import('@/views/summary/GraphSummaryView.vue'),
        meta: { title: '图统计', icon: 'PieChart' }
      },
      // 用户管理
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/UserView.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      // 系统设置
      {
        path: 'admin',
        name: 'Admin',
        meta: { title: '系统设置', icon: 'Setting' },
        children: [
          {
            path: 'profile',
            name: 'Profile',
            component: () => import('@/views/system/ProfileView.vue'),
            meta: { title: '个人中心', icon: 'Avatar' }
          }
        ]
      },
      // 404
      {
        path: ':pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/system/NotFoundView.vue'),
        meta: { title: '页面不存在', hidden: true }
      }
    ]
  }
]
