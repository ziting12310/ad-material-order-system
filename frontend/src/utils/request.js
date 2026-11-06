/**
 * Axios 请求封装
 * 后端 BaseURL：http://localhost:8080/api/v1
 * 统一响应结构：{ code, message, data } — code=SUCCESS 为成功
 *
 * 错误提示策略：
 * - 默认不弹全局错误提示（由调用方页面自己处理，显示空状态等）
 * - 需要弹提示的接口，传 config.silent = false
 * - 401 始终会弹提示并跳登录
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
        if (window.location.pathname !== '/login') {
            window.location.href = '/login'
        }
    }
}

// 防抖：同类型错误 3 秒内只弹一次
let lastErrorMsg = ''
let lastErrorTime = 0
function showError(msg) {
    const now = Date.now()
    if (msg === lastErrorMsg && now - lastErrorTime < 3000) return
    lastErrorMsg = msg
    lastErrorTime = now
    ElMessage.error(msg)
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
        // FormData 请求必须移除 Content-Type，让浏览器自动设置 multipart/form-data + boundary
        // 否则全局默认的 application/json 会覆盖，导致后端报 "not a multipart request"
        if (config.data instanceof FormData) {
            delete config.headers['Content-Type']
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
            if (!response.config.silent) {
                showError(res.message || '操作失败')
            }
            return Promise.reject(new Error(res.message || '操作失败'))
        }
        // 非 ResultVO（如文件流）直接透传
        return res
    },
    error => {
        const status = error.response?.status
        const code = error.response?.data?.code
        const msg = error.response?.data?.message
        const silent = error.config?.silent

        // 401 始终处理（清登录态 + 跳登录）
        if (status === 401) {
            if (!silent) {
                if (code === 'TOKEN_EXPIRED') {
                    showError('登录已过期，请重新登录')
                } else if (code === 'TOKEN_INVALID') {
                    showError('无效的登录凭证')
                } else {
                    showError(msg || '请先登录')
                }
            }
            redirectLogin()
            return Promise.reject(error)
        }

        // 其他错误：silent=true 时不弹提示，交给页面处理
        if (!silent) {
            switch (status) {
                case 403:
                    showError(msg || '无权访问')
                    break
                case 404:
                    showError(msg || '资源不存在')
                    break
                case 409:
                    ElMessage.warning(msg || '数据已被修改，请刷新重试')
                    break
                case 422:
                    ElMessage.warning(msg || '参数校验失败')
                    break
                default:
                    if (error.code === 'ECONNABORTED') {
                        showError('请求超时，请检查网络')
                    } else if (!error.response) {
                        showError('网络连接失败，请确认后端服务已启动')
                    } else {
                        showError(msg || `系统繁忙 (${status})`)
                    }
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
