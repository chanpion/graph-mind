import { useAuthStore } from '@/views/auth/stores/useAuthStore'
import { useTabStore } from '@/stores/tabs'

/**
 * 导航守卫
 * - auth guard: 检查 token，未登录重定向到 /login
 * - tab guard: 自动注册标签页
 */
export function setupGuards(router) {
  router.beforeEach((to, from, next) => {
    // 设置页面标题
    if (to.meta?.title) {
      document.title = `${to.meta.title} - Graph Mind 图数据库管理平台`
    }

    // 登录页无需认证
    if (to.path === '/login') {
      const authStore = useAuthStore()
      if (authStore.token) {
        next('/home/dashboard')
        return
      }
      next()
      return
    }

    // 检查认证
    const authStore = useAuthStore()
    if (!authStore.token) {
      next('/login')
      return
    }

    // 检查角色权限
    if (to.meta?.roles) {
      const hasRole = to.meta.roles.some(role => authStore.hasRole(role))
      if (!hasRole) {
        next('/home/dashboard')
        return
      }
    }

    next()
  })

  // 路由切换后注册标签页
  router.afterEach((to) => {
    if (to.meta?.title && !to.meta?.hidden) {
      try {
        const tabStore = useTabStore()
        tabStore.addOrActivateTab({
          id: to.name || to.path,
          title: to.meta.title,
          path: to.fullPath,
          icon: to.meta.icon || '',
          closable: to.meta.closable !== false
        })
      } catch {
        // tabStore 可能尚未初始化
      }
    }
  })
}
