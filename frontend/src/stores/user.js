import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 用户信息 Store — 简化版，预留位置
 * 课程项目阶段不做真实登录，默认模拟一个 BL 角色用户
 */
export const useUserStore = defineStore('user', () => {
    // 模拟用户信息（课程项目直接写死，后续接登录接口替换）
    const userInfo = ref({
        id: 1,
        username: 'admin',
        name: '张经理',
        role: 'BL',             // BL 业务负责人 / AM 客户经理 / DS 设计人员
        roleName: '业务负责人',
        avatar: ''
    })

    const isLogin = ref(true)   // 默认已登录（课程演示用）

    function logout() {
        isLogin.value = false
        userInfo.value = null
    }

    return { userInfo, isLogin, logout }
})
