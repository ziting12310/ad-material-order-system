package com.peak.service;

import com.peak.vo.UserListItemVO;

import java.util.List;

/** 用户查询服务 — A 模块 */
public interface UserService {

    /** 查询所有未删除用户，按 id 升序 */
    List<UserListItemVO> listUsers();
}
