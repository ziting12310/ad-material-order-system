/**
 * 用户状态仓库 — 登录态全局管理（Pinia）
 * 状态来源：API.md §2.1 登录响应 data { token, tokenType, expiresIn, user }
 * 角色：AM 客户经理 / BL 业务负责人 / DS 设计制作人员（GLOSSARY §3.3）
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/modules/auth'
import { getToken, setToken, getUser, setUser, clearAuth, isLoggedIn } from '@/utils/token'

export const useUserStore = defineStore('user', () => {
  // ---------- State ----------
  const token = ref(getToken() || '')
  const user = ref(getUser() || null)

  // ---------- Getters ----------
  /** 是否已登录 */
  const isLogin = computed(() => !!token.value && isLoggedIn())
  /** 用户角色编码 AM/BL/DS */
  const role = computed(() => user.value?.role || null)
  /** 显示名（UI 用） */
  const displayName = computed(() => user.value?.name || user.value?.username || '访客')

  // ---------- Actions ----------
  /**
   * 登录 — POST /api/v1/auth/login
   * 成功同时写 Pinia 内存态 + localStorage
   */
  async function login(username, password) {
    const data = await loginApi({ username, password })
    // request.js 拦截器已解包 ResultVO，data = { token, user }
    token.value = data.token
    user.value = data.user
    setToken(data.token)
    setUser(data.user)
    return data
  }

  /** 登出 — 清 Pinia + localStorage */
  function logout() {
    token.value = ''
    user.value = null
    clearAuth()
  }

  /** 刷新页面时从 localStorage 恢复（App.vue setup 调用） */
  function hydrate() {
    if (!token.value) token.value = getToken() || ''
    if (!user.value) user.value = getUser() || null
  }

  /** Token 过期/非法时调用（request.js 401 分支） */
  function invalidate() {
    token.value = ''
    user.value = null
    clearAuth()
  }

  return { token, user, isLogin, role, displayName, login, logout, hydrate, invalidate }
})
