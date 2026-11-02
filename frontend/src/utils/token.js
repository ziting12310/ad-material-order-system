/**
 * Token 存取工具 — 登录态持久化（localStorage）
 * 后端约定：Header Authorization: Bearer <JWT>
 */
const TOKEN_KEY = 'amoms_token'
const USER_KEY = 'amoms_user'

/** 读取 JWT Token */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/** 写入 Token（登录成功后） */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 清除 Token */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/** 读取用户信息 { id, username, name, role, phone } */
export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

/** 写入用户信息 */
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/** 清除用户信息 */
export function removeUser() {
  localStorage.removeItem(USER_KEY)
}

/** 一键清除全部登录态（登出 / 401 时调用） */
export function clearAuth() {
  removeToken()
  removeUser()
}

/** 是否已登录 */
export function isLoggedIn() {
  return !!getToken()
}
