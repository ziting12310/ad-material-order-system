/**
 * Axios 基础封装 — 统一请求/响应拦截器
 *
 * 后端契约（API.md §1.1）：
 *  - BaseURL：/api/v1（Vite 代理 /api → http://localhost:8080/api）
 *  - 统一响应 ResultVO：{ code, message, data, traceId }（code=SUCCESS 为成功）
 *  - 鉴权：Header Authorization: Bearer <JWT>
 *
 * 拦截器职责：
 *  - request：自动注入 Bearer Token
 *  - response：
 *      ① HTTP 200 且 code=SUCCESS → 直接返回 data（业务层拿到干净数据）
 *      ② HTTP 200 但 code≠SUCCESS → 按错误码 Toast 并 reject
 *      ③ HTTP 401 → 清登录态 + 跳登录页
 *      ④ HTTP 403/404/409/422/5xx/超时 → 统一 Toast
 */
import axios from 'axios'
import router from '@/router'
import { getToken, clearAuth } from '@/utils/token'

/** 轻量 Toast（MVP 用 console + alert 级别区分，后续可换组件式 Toast） */
function toast(type, msg) {
  const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' }
  console.log(`[Toast:${type}]`, icons[type] || '', msg)
  if (type === 'error') window.alert(msg)
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
      config.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// ==================== 响应拦截器 ====================
request.interceptors.response.use(
  response => {
    const res = response.data
    // ResultVO 结构判定：含 code 字段
    if (res && res.code !== undefined) {
      if (res.code === 'SUCCESS') {
        // 成功：直接返回 data，业务层无需关心 ResultVO 壳
        return res.data
      }
      // 业务失败：按错误码分类（message 字段，兼容后端 msg 命名）
      handleBusinessError(res.code, res.message || res.msg)
      return Promise.reject(new Error(res.message || res.msg || '请求失败'))
    }
    // 非 ResultVO 结构（如文件流）直接透传
    return res
  },
  error => {
    const status = error.response?.status
    const body = error.response?.data
    const msg = body?.message || body?.msg

    switch (status) {
      case 401:
        // Token 无效/过期 → 清登录态 + 跳登录页
        clearAuth()
        toast('error', msg || '登录已过期，请重新登录')
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
        break
      case 403:
        toast('error', msg || '无权访问该资源')
        break
      case 404:
        toast('error', msg || '请求的资源不存在')
        break
      case 409:
        // 乐观锁冲突（ORDER_VERSION_CONFLICT）
        toast('warning', msg || '数据已被他人修改，请刷新后重试')
        break
      case 422:
        // 参数校验失败（Jakarta Validation）
        toast('warning', msg || '参数校验失败')
        break
      default:
        if (error.code === 'ECONNABORTED') {
          toast('error', '请求超时，请检查网络')
        } else if (!error.response) {
          toast('error', '网络连接失败，请确认后端已启动（localhost:8080）')
        } else {
          toast('error', msg || `服务器错误 (${status})`)
        }
    }
    return Promise.reject(error)
  }
)

/**
 * 业务错误码分类处理（HTTP 200 但 code≠SUCCESS）
 * 错误码字典见 API.md §错误码
 */
function handleBusinessError(code, message) {
  // 鉴权类 → 清 Token + 跳登录
  if (['UNAUTHORIZED', 'TOKEN_EXPIRED', 'TOKEN_INVALID'].includes(code)) {
    clearAuth()
    toast('error', message || '登录已过期')
    router.push({ name: 'Login' })
    return
  }
  // 权限类
  if (['PERMISSION_DENIED', 'DATA_SCOPE_DENIED'].includes(code)) {
    toast('error', message || '无权执行该操作')
    return
  }
  // 参数校验类
  if (code === 'PARAM_VALIDATION_ERROR') {
    toast('warning', message || '参数校验失败')
    return
  }
  // 状态冲突 / 乐观锁
  if (['ORDER_STATUS_CONFLICT', 'ORDER_VERSION_CONFLICT'].includes(code)) {
    toast('warning', message)
    return
  }
  // 兜底
  toast('error', message || '操作失败')
}

export default request
