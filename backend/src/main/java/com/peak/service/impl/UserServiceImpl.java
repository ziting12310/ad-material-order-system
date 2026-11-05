package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.entity.User;
import com.peak.mapper.UserMapper;
import com.peak.service.UserService;
import com.peak.vo.UserListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public List<UserListItemVO> listUsers(String role) {
        // @TableLogic 会自动过滤 is_deleted = 1
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId);
        // 分派下拉传 role=DS，仅返回设计人员；不传则返回全部
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role.trim().toUpperCase());
        }
        List<User> users = userMapper.selectList(wrapper);

        return users.stream()
                .map(this::toListItemVO)
                .toList();
    }

    private UserListItemVO toListItemVO(User user) {
        UserListItemVO vo = new UserListItemVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        vo.setPhone(user.getPhone());
        return vo;
    }
}
