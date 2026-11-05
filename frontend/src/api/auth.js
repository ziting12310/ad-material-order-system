/**
 * 认证模块接口
 * 后端路径：/api/v1/auth
 */
import request from '@/utils/request'

/**
 * 用户登录
 * POST /api/v1/auth/login
 * @param {Object} data - { username, password }
 * @returns {Object} { token, tokenType, expiresIn, user }
 */
export function login(data) {
    return request.post('/auth/login', data)
}

/**
 * 获取当前登录用户信息
 * GET /api/v1/auth/userinfo
 * @returns {Object} { id, username, name, role, phone }
 */
export function getUserInfo() {
    return request.get('/auth/userinfo')
}

/**
 * 修改密码
 * PUT /api/v1/auth/password
 * @param {Object} data - { oldPassword, newPassword }
 */
export function changePassword(data) {
    return request.put('/auth/password', data)
}
