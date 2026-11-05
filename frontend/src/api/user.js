/**
 * 用户模块接口
 * 后端路径：/api/v1/users
 */
import request from '@/utils/request'

/**
 * 用户列表（按角色过滤）
 * GET /api/v1/users?role=DS
 */
export function listUsers(params) {
    return request.get('/users', { params, silent: true })
}

/**
 * 设计师列表
 * GET /api/v1/users?role=DS
 */
export function listDesigners() {
    return request.get('/users', { params: { role: 'DS' }, silent: true })
}

/**
 * 获取用户详情
 * GET /api/v1/users/{id}
 */
export function getUser(id) {
    return request.get(`/users/${id}`, { silent: true })
}
