/**
 * Vue Router 配置 — 广告物料订单管理系统
 *
 * 路由结构：
 *  /login                  → 登录页
 *  /                       → 重定向 /dashboard
 *  /dashboard              → 工作台
 *  /orders                 → 订单列表
 *  /design-workbench       → 设计师工作台
 *  /final-audit            → 定稿审核
 *  /archive                → 归档查询
 *  /profile                → 个人中心
 */
import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/layout/Layout.vue'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'Document', roles: ['AM', 'BL'] }
      },
      {
        path: 'design-workbench',
        name: 'DesignWorkbench',
        component: () => import('@/views/designer/DesignWorkbench.vue'),
        meta: { title: '设计师工作台', icon: 'Brush', roles: ['DS', 'BL'] }
      },
      {
        path: 'final-audit',
        name: 'FinalAudit',
        component: () => import('@/views/audit/FinalAudit.vue'),
        meta: { title: '定稿审核', icon: 'CircleCheck', roles: ['AM', 'BL'] }
      },
      {
        path: 'archive',
        name: 'ArchiveList',
        component: () => import('@/views/archive/ArchiveList.vue'),
        meta: { title: '归档查询', icon: 'FolderChecked' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', hidden: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 白名单：不需要登录的页面
const WHITE_LIST = ['/login']

// ========== 前置守卫：登录校验 + 角色权限 ==========
router.beforeEach(async (to, from, next) => {
  const token = getToken()

  // 已登录
  if (token) {
    if (to.path === '/login') {
      next('/dashboard')
      return
    }
    // 确保用户信息已加载
    const userStore = useUserStore()
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch (e) {
        userStore.logout()
        next(`/login?redirect=${to.fullPath}`)
        return
      }
    }
    // 角色权限校验
    if (to.meta.roles && to.meta.roles.length) {
      if (!to.meta.roles.includes(userStore.userInfo.role)) {
        next('/dashboard')
        return
      }
    }
    next()
    return
  }

  // 未登录
  if (WHITE_LIST.includes(to.path) || to.meta.noAuth) {
    next()
  } else {
    next(`/login?redirect=${to.fullPath}`)
  }
})

// ========== 后置守卫：更新标题 ==========
router.afterEach(to => {
  const base = '广告物料订单管理系统'
  document.title = to.meta.title ? `${to.meta.title} | ${base}` : base
})

export default router
