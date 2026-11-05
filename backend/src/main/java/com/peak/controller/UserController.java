package com.peak.controller;

import com.peak.common.annotation.RequireRole;
import com.peak.common.enums.UserRole;
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
 * <p>鉴权：JwtInterceptor 负责登录校验（401）；本接口再加角色限制，
 * 仅 AM / BL 可查询用户列表，DS 访问返回 403（由 RoleCheckAspect 统一拦截）。</p>
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户查询", description = "查询未删除用户列表，供订单分派等下拉选择使用（仅 AM/BL）")
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users
     *
     * <p>返回全部未删除用户，不参与分页；字段：id、username、name、role、phone。</p>
     * <p>角色限制：AM（客户经理）/ BL（业务主管）可查；DS（设计人员）返回 403。</p>
     */
    @GetMapping
    @RequireRole({UserRole.AM, UserRole.BL})
    @Operation(summary = "查询所有未删除用户", description = "仅 AM / BL 角色可调用，供分派下拉使用")
    public ResultVO<List<UserListItemVO>> listUsers() {
        return ResultVO.success(userService.listUsers());
    }
}
