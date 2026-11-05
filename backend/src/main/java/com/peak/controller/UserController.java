package com.peak.controller;

import com.peak.service.UserService;
import com.peak.vo.ResultVO;
import com.peak.vo.UserListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户查询控制器 — A 模块
 *
 * <p>BaseURL：/api/v1/users（yml context-path=/api + 本类 @RequestMapping("/v1/users")）。</p>
 * <p>JWT 鉴权由 JwtInterceptor 统一处理，本接口无需额外角色限制。</p>
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户查询", description = "查询未删除用户列表，供订单分派等下拉选择使用")
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users
     *
     * <p>返回全部未删除用户，不参与分页；字段：id、username、name、role、phone。</p>
     */
    @GetMapping
    @Operation(summary = "查询所有未删除用户")
    public ResultVO<List<UserListItemVO>> listUsers() {
        return ResultVO.success(userService.listUsers());
    }
}
