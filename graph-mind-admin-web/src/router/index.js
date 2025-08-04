import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'
import Index from '@/views/Index.vue'

// 系统管理模块
import User from '@/views/system/User.vue'
import Role from '@/views/system/Role.vue'
import Permission from '@/views/system/Permission.vue'

// 图库管理模块
import Connection from '@/views/graph/Connection.vue'
import GraphList from '@/views/graph/GraphList.vue'
import GraphDetail from '@/views/graph/GraphDetail.vue'
import GraphVisual from '@/views/graph/GraphVisual.vue'
import DataProcess from '@/views/graph/DataProcess.vue'

// 布局组件
const MainLayout = () => import('@/components/layout/MainLayout.vue')

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'Login',
            component: Login,
            meta: { title: '登录', hidden: true }
        },
        {
            path: '/',
            redirect: '/home',  // 直接重定向到首页
            meta: { hidden: true ,closable: true }
        },
        {
            path: '/home',
            name: 'Home',
            component: Home,
            meta: { title: '首页',closable: true  },
            children: [
                // 系统管理子模块
                {
                    path: 'admin',
                    name: 'Admin',
                    component: MainLayout,
                    meta: { title: '系统管理' ,closable: true },
                    children: [
                        { path: 'user', name: 'User', component: User, meta: { title: '用户管理',closable: true  } },
                        { path: 'role', name: 'Role', component: Role, meta: { title: '角色管理',closable: true  } },
                        { path: 'permission', name: 'Permission', component: Permission, meta: { title: '权限管理',closable: true  } }
                    ]
                },
                // 图库管理子模块
                {
                    path: 'graph',
                    name: 'Graph',
                    component: MainLayout,
                    meta: { title: '图库管理', closable: true },
                    children: [
                        { path: 'connection', name: 'Connection', component: Connection, meta: { title: '图连接管理',closable: true  } },
                        { path: 'list', name: 'GraphList', component: GraphList, meta: { title: '图管理',closable: true  } },
                        { path: 'detail/:id', name: 'GraphDetail', component: GraphDetail, meta: { title: '图详情',closable: true  } },
                        { path: 'visual', name: 'GraphVisual', component: GraphVisual, meta: { title: '图可视化',closable: true  } },
                        { path: 'process', name: 'DataProcess', component: DataProcess, meta: { title: '图数据',closable: true  } }
                    ]
                }
            ]
        }
    ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 如果访问登录页，直接放行
  if (to.path === '/login') {
    // 如果已经登录，跳转到首页
    if (token) {
      next('/home')
      return
    }
    next()
    return
  }
  
  // 如果没有token，跳转到登录页
  if (!token) {
    next('/login')
    return
  }
  
  // 其他情况正常放行
  next()
})

export default router