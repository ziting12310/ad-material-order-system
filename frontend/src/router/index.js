/**
 * Vue Router 配置 — 对齐 UI_UX_SPEC.md 页面清单
 *
 * 路由表：
 *  /login          → LoginView        登录页（免鉴权）
 *  /orders         → OrderListView    订单列表（需登录，全角色）
 *  /orders/create  → OrderCreateView  创建订单（AM）
 *  /orders/:id     → OrderDetailView  订单详情（需登录）
 *  /my-tasks       → MyTasksView      我的任务（DS）
 *  /404            → NotFoundView     兜底
 *
 * 守卫 beforeEach：
 *  ① 已登录访问 Login → 跳首页
 *  ② 需鉴权未登录 → 跳 Login（带 redirect 回跳）
 *  ③ meta.roles 角色不匹配 → 跳订单列表
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  { path: '/', redirect: '/orders' },
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('@/views/OrderListView.vue'),
    meta: { requiresAuth: true, title: '订单列表' }
  },
  {
    path: '/orders/create',
    name: 'OrderCreate',
    component: () => import('@/views/OrderCreateView.vue'),
    meta: { requiresAuth: true, title: '创建订单', roles: ['AM'] }
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { requiresAuth: true, title: '订单详情' },
    props: true
  },
  {
    path: '/my-tasks',
    name: 'MyTasks',
    component: () => import('@/views/MyTasksView.vue'),
    meta: { requiresAuth: true, title: '我的任务', roles: ['DS'] }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { requiresAuth: false, title: '页面不存在' }
  },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 全局前置守卫：鉴权 + 角色 + 登录页重定向
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  userStore.hydrate()
  const loggedIn = userStore.isLogin

  // ① 已登录访问登录页 → 跳首页
  if (to.name === 'Login' && loggedIn) {
    next({ path: '/' })
    return
  }
  // ② 需鉴权但未登录 → 跳登录页（带 redirect 登录后回跳）
  if (to.meta.requiresAuth && !loggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  // ③ 角色校验（meta.roles 数组）
  if (Array.isArray(to.meta.roles) && to.meta.roles.length > 0 && loggedIn) {
    if (!to.meta.roles.includes(userStore.role)) {
      next({ name: 'OrderList' })
      return
    }
  }
  next()
})

// 后置守卫：更新浏览器标题
router.afterEach(to => {
  const base = 'AMOMS'
  document.title = to.meta.title ? `${to.meta.title} | ${base}` : base
})

export default router
