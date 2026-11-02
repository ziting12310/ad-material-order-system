<template>
  <!-- 未登录 → 全屏登录页；已登录 → 侧边栏布局 -->
  <router-view v-if="!userStore.isLogin" />
  <AppLayout v-else>
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 刷新时从 localStorage 恢复登录态
userStore.hydrate()
</script>
