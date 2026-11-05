import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo as getUserInfoApi } from '@/api/auth'
import { getToken, setToken, clearAuth, setUserCache, getUserCache } from '@/utils/auth'

/**
 * 角色名称映射
 */
const ROLE_NAME_MAP = {
    AM: '客户经理',
    BL: '业务负责人',
    DS: '设计人员'
}

export const useUserStore = defineStore('user', () => {
    const token = ref(getToken())
    const userInfo = ref(getUserCache())

    const isLogin = computed(() => !!token.value && !!userInfo.value)

    const roleName = computed(() => {
        if (!userInfo.value) return ''
        return ROLE_NAME_MAP[userInfo.value.role] || userInfo.value.role
    })

    /**
     * 登录
     * 1. 调登录接口拿 token + 用户信息
     * 2. 存 localStorage + pinia
     */
    async function login(username, password) {
        const res = await loginApi({ username, password })
        token.value = res.token
        userInfo.value = res.user
        setToken(res.token)
        setUserCache(res.user)
        return res
    }

    /**
     * 刷新用户信息（页面加载时调用，确保用户态是最新的）
     * 如果 Token 无效，会在 request 拦截器里 401 → 清本地缓存 → 跳登录
     */
    async function fetchUserInfo() {
        try {
            const user = await getUserInfoApi()
            userInfo.value = user
            setUserCache(user)
            return user
        } catch (e) {
            // 401 的话已经在拦截器里处理了，这里确保本地状态干净
            return null
        }
    }

    /**
     * 退出登录
     * 清除本地所有认证信息
     */
    function logout() {
        token.value = ''
        userInfo.value = null
        clearAuth()
    }

    return {
        token,
        userInfo,
        isLogin,
        roleName,
        login,
        fetchUserInfo,
        logout
    }
})
