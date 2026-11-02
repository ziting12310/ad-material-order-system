<template>
  <!-- 404 兜底页 -->
  <div class="min-h-screen flex flex-col items-center justify-center text-center">
    <div class="text-6xl font-bold text-gray-200 mb-4">404</div>
    <h2 class="text-xl text-gray-600 mb-2">页面不存在</h2>
    <p class="text-sm text-gray-400 mb-6">你访问的地址可能已被移除或拼写错误</p>
    <button class="btn-primary" @click="router.push('/')">返回首页</button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>
