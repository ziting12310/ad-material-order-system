/**
 * Vue Router 配置 — 广告物料订单管理系统
 *
 * 路由结构：
 *  /                       → 重定向 /dashboard
 *  /dashboard              → 工作台（Dashboard）
 *  /orders                 → 订单列表
 *  /final-audit            → 定稿审核归档（核心页面）
 */
import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/layout/Layout.vue'

const routes = [
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

// 后置守卫：更新标题
router.afterEach(to => {
  const base = '广告物料订单管理系统'
  document.title = to.meta.title ? `${to.meta.title} | ${base}` : base
})

export default router
