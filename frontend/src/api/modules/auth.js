/**
 * 认证接口模块 — 对齐 API.md §2.1 / §2.2
 * 注意：modules 下统一使用 import request from '../request'
 */
import request from '../request'

/**
 * 用户登录
 * POST /api/v1/auth/login
 * @param {Object} data { username, password }
 * @returns { token, tokenType, expiresIn, user }
 */
export const login = data => request.post('/auth/login', data)

/**
 * 文件直传签名（OSS presign）
 * POST /api/v1/files/presign
 * @param {Object} data { fileType, fileName, fileSize, contentType }
 */
export const presign = data => request.post('/files/presign', data)
