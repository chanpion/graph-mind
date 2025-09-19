import {createRouter, createWebHistory} from 'vue-router'
import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'

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
            meta: {title: '登录', hidden: true}
        },
        {
            path: '/',
            redirect: '/home',  // 直接重定向到首页
            meta: {hidden: true, closable: true}
        },
        {
            path: '/home',
            name: 'Home',
            component: Home,
            meta: {title: '首页', closable: true},
            children: [
                // 系统管理子模块
                {
                    path: 'admin',
                    name: 'Admin',
                    component: MainLayout,
                    meta: {title: '系统管理', closable: true},
                    children: [
                        {
                            path: 'user',
                            name: 'User',
                            component: () => import('@/views/system/User.vue'),
                            meta: {title: '用户管理', closable: true}
                        },
                        {
                            path: 'role',
                            name: 'Role',
                            component: () => import('@/views/system/Role.vue'),
                            meta: {title: '角色管理', closable: true}
                        },
                        {
                            path: 'permission',
                            name: 'Permission',
                            component: () => import('@/views/system/Permission.vue'),
                            meta: {title: '权限管理', closable: true}
                        },
                        {
                            path: 'config',
                            name: 'Config',
                            component: () => import('@/views/system/AppConfig.vue'),
                            meta: {title: '系统配置', closable: true}
                        },
                        {
                            path: 'profile',
                            name: 'Profile',
                            component: () => import('@/views/system/Profile.vue'),
                            meta: {title: '个人中心', closable: true}
                        }
                    ]
                },
                // 图库管理子模块
                {
                    path: 'graph',
                    name: 'Graph',
                    component: MainLayout,
                    meta: {title: '图库管理', icon: 'Histogram', closable: true},
                    children: [
                        {
                            path: 'list',
                            name: 'GraphList',
                            component: () => import('@/views/graph/GraphList.vue'),
                            meta: {title: '图列表', icon: 'List'}
                        },
                        {
                            path: 'detail/:id',
                            name: 'GraphDetail',
                            component: () => import('@/views/graph/GraphDetail.vue'),
                            meta: {title: '图设计', icon: 'Document'},
                            props: true
                        },
                        {
                            path: 'connection',
                            name: 'Connection',
                            component: () => import('@/views/graph/Connection.vue'),
                            meta: {title: '连接管理', icon: 'Connection'}
                        },
                        {
                            path: 'process',
                            name: 'DataProcess',
                            component: () => import('@/views/graph/DataProcess.vue'),
                            meta: {title: '图加工', icon: 'DataLine'}
                        },
                        {
                            path: 'visual',
                            name: 'GraphVisual',
                            component: () => import('@/views/graph/GraphVisual.vue'),
                            meta: {title: '图可视化', icon: 'View'}
                        },
                        {
                            path: 'summary',
                            name: 'GraphSummary',
                            component: () => import('@/views/graph/GraphSummary.vue'),
                            meta: {title: '图统计', icon: 'PieChart'}
                        },
                        {
                            path: 'analysis',
                            name: 'GraphAnalysis',
                            component: () => import('@/views/graph/GraphAnalysis.vue'),
                            meta: {title: '图分析', icon: 'DataAnalysis'}
                        }
                    ]
                }
            ]
        }
    ],

    // 滚动行为
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition
        } else {
            return {top: 0}
        }
    }
})

// 路由守卫
router.beforeEach((to, from, next) => {
    // 设置页面标题
    if (to.meta && to.meta.title) {
        document.title = to.meta.title + ' - 图数据库管理系统'
    }

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