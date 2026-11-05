<template>
  <!--
    后台整体布局：左侧导航 + 顶部栏 + 主内容区 + 面包屑
    简约商务风格，大量留白，轻阴影
  -->
  <el-container class="layout-container">
    <!-- 左侧侧边栏 -->
    <el-aside :width="sidebarWidth" class="layout-aside">
      <div class="logo-area">
        <el-icon :size="24" color="#2563eb"><Promotion /></el-icon>
        <span class="logo-text">AMOMS</span>
      </div>
      <p class="logo-sub">广告物料订单管理</p>

      <el-menu
        :default-active="activeMenu"
        class="layout-menu"
        router
        background-color="transparent"
        text-color="#475569"
        active-text-color="#2563eb"
      >
        <el-menu-item
          v-for="item in menuList"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主区域 -->
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <!-- 面包屑 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, idx) in breadcrumbs" :key="idx">
              {{ item }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="通知" placement="bottom">
            <el-icon class="header-icon"><Bell /></el-icon>
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" style="background: var(--primary-color)">
                {{ userStore.userInfo?.name?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.name }}</span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarWidth = 'var(--sidebar-width)'

// 完整菜单配置（roles 控制可见性）
const allMenus = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer' },
  { path: '/orders', title: '订单管理', icon: 'Document', roles: ['AM', 'BL'] },
  { path: '/design-workbench', title: '设计师工作台', icon: 'Brush', roles: ['DS', 'BL'] },
  { path: '/final-audit', title: '定稿审核', icon: 'CircleCheck', roles: ['AM', 'BL'] },
  { path: '/archive', title: '归档查询', icon: 'FolderChecked' }
]

// 按角色过滤菜单
const menuList = computed(() => {
  const role = userStore.userInfo?.role
  if (!role) return allMenus.filter(m => !m.roles)
  return allMenus.filter(m => !m.roles || m.roles.includes(role))
})

// 当前激活菜单
const activeMenu = computed(() => {
  if (route.meta.activeMenu) return route.meta.activeMenu
  return route.path
})

// 面包屑
const breadcrumbs = computed(() => {
  const title = route.meta.title
  if (!title || title === '工作台') return []
  return [title]
})

// 下拉菜单操作
function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* ========== 侧边栏 ========== */
.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid var(--gray-200);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 4px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--gray-800);
  letter-spacing: 0.5px;
}

.logo-sub {
  font-size: 12px;
  color: var(--gray-400);
  padding: 0 20px 16px;
  margin: 0;
  border-bottom: 1px solid var(--gray-100);
}

.layout-menu {
  flex: 1;
  border-right: none;
  padding: 12px 12px;
}

.layout-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  border-radius: 6px;
  margin-bottom: 4px;
}

.layout-menu :deep(.el-menu-item.is-active) {
  background-color: var(--primary-light);
  font-weight: 500;
}

.layout-menu :deep(.el-menu-item:hover) {
  background-color: var(--gray-50);
}

/* ========== 顶部栏 ========== */
.layout-header {
  background-color: #ffffff;
  border-bottom: 1px solid var(--gray-200);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: var(--header-height) !important;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 18px;
  color: var(--gray-500);
  cursor: pointer;
  transition: var(--transition-base);
}

.header-icon:hover {
  color: var(--primary-color);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
  color: var(--gray-700);
}

/* ========== 主内容 ========== */
.layout-main {
  background-color: var(--gray-50);
  padding: 24px;
  overflow-y: auto;
}

/* 页面过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
