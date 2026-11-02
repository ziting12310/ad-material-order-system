<template>
  <!-- 登录页 — 全屏居中卡片（UI_UX_SPEC §Login） -->
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-brand to-brand-800">
    <div class="w-[380px] bg-white rounded-xl shadow-xl p-8">
      <!-- Logo -->
      <div class="text-center mb-6">
        <div class="w-12 h-12 bg-brand rounded-lg mx-auto flex items-center justify-center text-white text-xl font-bold">A</div>
        <h1 class="mt-3 text-xl font-semibold text-gray-800">AMOMS</h1>
        <p class="text-sm text-gray-400">广告物料订单管理系统</p>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="onSubmit" class="space-y-4">
        <div>
          <label class="block text-sm text-gray-600 mb-1">账号</label>
          <input v-model.trim="form.username" class="input" placeholder="请输入账号" autocomplete="username" />
        </div>
        <div>
          <label class="block text-sm text-gray-600 mb-1">密码</label>
          <input v-model="form.password" type="password" class="input" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button type="submit" :disabled="loading" class="btn-primary w-full h-10">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <!-- 测试账号提示（schema.sql 初始化数据） -->
      <div class="mt-6 text-xs text-gray-400 space-y-1 border-t border-gray-100 pt-4">
        <div>🧪 测试账号：</div>
        <div>客户经理 AM ： am_demo / 123456</div>
        <div>业务负责人 BL： bl_demo / 123456</div>
        <div>设计人员 DS ： ds_demo / 123456</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    // 登录成功 → 回跳 redirect 或首页
    router.push(route.query.redirect || '/')
  } catch (e) {
    // request.js 拦截器已统一 Toast，此处不重复处理
  } finally {
    loading.value = false
  }
}
</script>
