/**
 * Vue Router 配置 — 广告物料订单管理系统
 *
 * 路由结构：
 *  /login                  → 登录页
 *  /                       → 重定向 /dashboard
 *  /dashboard              → 工作台
 *  /orders                 → 订单列表
 *  /final-audit            → 定稿审核归档
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
        meta: { title: '订单管理', icon: 'Document' }
      },
      {
        path: 'final-audit',
        name: 'FinalAudit',
        component: () => import('@/views/audit/FinalAudit.vue'),
        meta: { title: '定稿审核归档', icon: 'CircleCheck' }
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

// ========== 前置守卫：登录校验 ==========
router.beforeEach(async (to, from, next) => {
  const token = getToken()

  // 已登录
  if (token) {
    if (to.path === '/login') {
      // 已登录的人访问登录页，直接跳首页
      next('/dashboard')
      return
    }
    // 确保用户信息已加载（刷新页面时 token 有但 pinia 里 userInfo 可能还没）
    const userStore = useUserStore()
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch (e) {
        // Token 无效，清掉跳登录
        userStore.logout()
        next(`/login?redirect=${to.fullPath}`)
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
