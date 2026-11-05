package com.peak.service;

import com.peak.vo.UserListItemVO;

import java.util.List;

/** 用户查询服务 — A 模块 */
public interface UserService {

    /**
     * 查询未删除用户，按 id 升序
     *
     * @param role 角色过滤（AM / BL / DS）；为空时返回全部用户
     */
    List<UserListItemVO> listUsers(String role);
}
