/**
 * Axios 请求封装
 * 后端 BaseURL：http://localhost:8080/api/v1
 * 统一响应结构：{ code, message, data } — code=SUCCESS 为成功
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'

let router = null
let userStore = null

// 延迟获取 router / store，避免循环依赖
function getRouter() {
    if (!router) {
        import('@/router').then(m => { router = m.default })
    }
    return router
}

function redirectLogin() {
    clearAuth()
    const r = getRouter()
    if (r && r.currentRoute.value.path !== '/login') {
        r.push('/login')
    } else {
        // 兜底：router 还没拿到就直接改 hash
        if (window.location.pathname !== '/login') {
            window.location.href = '/login'
        }
    }
}

const request = axios.create({
    baseURL: '/api/v1',
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' }
})

// ==================== 请求拦截器 ====================
request.interceptors.request.use(
    config => {
        const token = getToken()
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error)
)

// ==================== 响应拦截器 ====================
request.interceptors.response.use(
    response => {
        const res = response.data
        // 判断是否为 ResultVO 结构
        if (res && typeof res === 'object' && 'code' in res) {
            if (res.code === 'SUCCESS' || res.code === 0) {
                // 成功：直接返回 data
                return res.data
            }
            // 业务失败
            ElMessage.error(res.message || '操作失败')
            return Promise.reject(new Error(res.message || '操作失败'))
        }
        // 非 ResultVO（如文件流）直接透传
        return res
    },
    error => {
        const status = error.response?.status
        const code = error.response?.data?.code
        const msg = error.response?.data?.message

        switch (status) {
            case 401:
                // 未授权 / Token 过期 / Token 无效 → 清本地 + 跳登录
                if (code === 'TOKEN_EXPIRED') {
                    ElMessage.error('登录已过期，请重新登录')
                } else if (code === 'TOKEN_INVALID') {
                    ElMessage.error('无效的登录凭证')
                } else {
                    ElMessage.error(msg || '请先登录')
                }
                redirectLogin()
                break
            case 403:
                ElMessage.error(msg || '无权访问')
                break
            case 404:
                ElMessage.error(msg || '资源不存在')
                break
            case 409:
                ElMessage.warning(msg || '数据已被修改，请刷新重试')
                break
            case 422:
                ElMessage.warning(msg || '参数校验失败')
                break
            default:
                if (error.code === 'ECONNABORTED') {
                    ElMessage.error('请求超时，请检查网络')
                } else if (!error.response) {
                    ElMessage.error('网络连接失败，请确认后端服务已启动')
                } else {
                    ElMessage.error(msg || `服务器错误 (${status})`)
                }
        }
        return Promise.reject(error)
    }
)

/**
 * 注入 router 实例（供 401 时跳转用）
 * 在 main.js 中调用
 */
export function setupRequestRouter(r) {
    router = r
}

/**
 * 注入 userStore（供清除用户态用）
 */
export function setupRequestStore(s) {
    userStore = s
}

export default request
