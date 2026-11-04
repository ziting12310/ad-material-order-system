/**
 * Axios 请求封装
 * 后端 BaseURL：http://localhost:8080/api/v1
 * 统一响应结构：{ code, message, data } — code=SUCCESS 为成功
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: '/api/v1',
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' }
})

// ==================== 请求拦截器 ====================
request.interceptors.request.use(
    config => {
        // 预留 Token 注入位置（后续接登录再补）
        // const token = getToken()
        // if (token) config.headers['Authorization'] = `Bearer ${token}`
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
        const msg = error.response?.data?.message

        switch (status) {
            case 401:
                ElMessage.error(msg || '登录已过期，请重新登录')
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

export default request
