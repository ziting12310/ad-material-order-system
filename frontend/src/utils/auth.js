/**
 * Token 存储工具
 * 统一管理 localStorage 中 JWT 的读写清除
 */
const TOKEN_KEY = 'amoms_token'
const USER_KEY = 'amoms_user'

/** 读取 Token */
export function getToken() {
    return localStorage.getItem(TOKEN_KEY) || ''
}

/** 保存 Token */
export function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token)
}

/** 清除 Token */
export function removeToken() {
    localStorage.removeItem(TOKEN_KEY)
}

/** 保存用户信息（刷新后恢复用） */
export function setUserCache(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/** 读取用户缓存 */
export function getUserCache() {
    const str = localStorage.getItem(USER_KEY)
    if (!str) return null
    try {
        return JSON.parse(str)
    } catch (e) {
        return null
    }
}

/** 清除用户缓存 */
export function removeUserCache() {
    localStorage.removeItem(USER_KEY)
}

/** 清除全部认证信息 */
export function clearAuth() {
    removeToken()
    removeUserCache()
}
