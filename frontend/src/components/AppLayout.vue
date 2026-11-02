<template>
  <!-- 主布局：侧边栏 + 顶栏 + 内容区（对齐 UI_UX_SPEC 浅色企业后台） -->
  <div class="min-h-screen flex bg-gray-50">
    <!-- 侧边栏 -->
    <aside class="w-56 bg-white border-r border-gray-200 flex flex-col shrink-0">
      <div class="px-5 py-4 border-b border-gray-100">
        <h1 class="text-lg font-bold text-brand flex items-center gap-2">
          <span class="w-7 h-7 bg-brand rounded-md flex items-center justify-center text-white text-xs font-bold">A</span>
          AMOMS
        </h1>
        <p class="text-[11px] text-gray-400 mt-0.5">广告物料订单管理</p>
      </div>

      <!-- 菜单（按角色过滤，对齐 PRD §2 RBAC） -->
      <nav class="flex-1 py-3 px-2 space-y-0.5">
        <RouterLink
          v-for="item in menus"
          :key="item.name"
          :to="item.path"
          :class="[
            'flex items-center gap-2 px-3 py-2 text-sm rounded-md transition-colors',
            route.path.startsWith(item.path)
              ? 'bg-brand/10 text-brand font-medium'
              : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
          ]"
        >
          <span class="w-4 text-center">{{ item.icon }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>

      <!-- 底部角色徽章 -->
      <div class="px-4 py-3 border-t border-gray-100">
        <StatusBadge :status="userStore.role" variant="role" />
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 顶栏 -->
      <header class="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-6 shrink-0">
        <h2 class="text-base font-medium text-gray-800">{{ route.meta.title }}</h2>
        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-500">{{ userStore.displayName }}</span>
          <button class="btn-ghost text-sm" @click="onLogout">登出</button>
        </div>
      </header>

      <!-- 内容 -->
      <main class="flex-1 p-6 overflow-auto">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import StatusBadge from './StatusBadge.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 菜单清单 + 角色可见性（PRD §2 权限矩阵）
const allMenus = [
  { name: 'OrderList',   path: '/orders',        label: '订单列表', icon: '📋', roles: ['AM', 'BL', 'DS'] },
  { name: 'OrderCreate', path: '/orders/create', label: '创建订单', icon: '➕', roles: ['AM'] },
  { name: 'MyTasks',     path: '/my-tasks',      label: '我的任务', icon: '✏️', roles: ['DS'] }
]
const menus = computed(() => allMenus.filter(m => m.roles.includes(userStore.role)))

function onLogout() {
  userStore.logout()
  router.push({ name: 'Login' })
}
</script>
