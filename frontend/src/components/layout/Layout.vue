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
              <div class="user-meta">
                <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
                <el-tag :type="roleTagType" size="small" effect="light" round class="user-role-tag">
                  {{ roleLabel }}
                </el-tag>
              </div>
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
      <el-main class="layout-main" v-loading="pageLoading" :element-loading-text="'加载中...'">
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
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 路由切换加载态（0.2s 防抖，快速切换不显示 loading）
const pageLoading = ref(false)
let loadingTimer = null

// 路由前置守卫：开始计时
router.beforeEach((to, from, next) => {
  // 清除之前的定时器
  if (loadingTimer) {
    clearTimeout(loadingTimer)
    loadingTimer = null
  }
  // 0.2s 防抖：如果 0.2s 内路由切换完成，则不显示 loading
  loadingTimer = setTimeout(() => {
    pageLoading.value = true
  }, 200)
  next()
})

// 路由后置守卫：关闭加载
router.afterEach(() => {
  // 延迟一帧，确保新页面 DOM 开始渲染
  requestAnimationFrame(() => {
    if (loadingTimer) {
      clearTimeout(loadingTimer)
      loadingTimer = null
    }
    // 再给一帧时间让内容渲染
    requestAnimationFrame(() => {
      pageLoading.value = false
    })
  })
})

const sidebarWidth = 'var(--sidebar-width)'

// 完整菜单配置（roles 控制可见性）
const allMenus = [
  { path: '/dashboard', title: '首页', icon: 'Odometer' },
  { path: '/orders', title: '订单管理', icon: 'Document', roles: ['AM', 'BL'] },
  { path: '/design-workbench', title: '设计师工作台', icon: 'Brush', roles: ['DS', 'BL'] },
  { path: '/am-workbench', title: '客户经理工作台', icon: 'Briefcase', roles: ['AM'] },
  { path: '/final-audit', title: '定稿审核', icon: 'CircleCheck', roles: ['BL'] },
  { path: '/archive', title: '归档查询', icon: 'FolderChecked' }
]

// 规范化角色（统一转大写，兼容后端可能返回的不同格式）
const currentRole = computed(() => {
  const raw = userStore.userInfo?.role
  if (!raw) return ''
  const upper = String(raw).toUpperCase().trim()
  // 兼容 role code 别名
  const aliasMap = {
    'ACCOUNT_MANAGER': 'AM',
    'BUSINESS_LEADER': 'BL',
    'DESIGNER': 'DS',
    'DESIGN_STAFF': 'DS'
  }
  return aliasMap[upper] || upper
})

// 按角色过滤菜单
const menuList = computed(() => {
  const role = currentRole.value
  // 没有角色时只显示无角色限制的菜单（工作台 + 归档）
  if (!role) return allMenus.filter(m => !m.roles)
  // 有角色时：无角色限制的 + 包含当前角色的
  return allMenus.filter(m => !m.roles || m.roles.includes(role))
})

// 当前角色名称
const roleLabel = computed(() => {
  const map = { AM: '客户经理', BL: '业务负责人', DS: '设计人员' }
  return map[currentRole.value] || currentRole.value || '未知角色'
})

// 当前角色标签颜色
const roleTagType = computed(() => {
  const map = { AM: 'primary', BL: 'success', DS: 'warning' }
  return map[currentRole.value] || 'info'
})

// 当前激活菜单
const activeMenu = computed(() => {
  if (route.meta.activeMenu) return route.meta.activeMenu
  return route.path
})

// 面包屑
const breadcrumbs = computed(() => {
  const title = route.meta.title
  if (!title || title === '工作台' || title === '首页') return []
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

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.2;
}

.user-name {
  font-size: 14px;
  color: var(--gray-700);
}

.user-role-tag {
  font-size: 11px;
  padding: 0 6px;
  height: 18px;
  line-height: 16px;
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
