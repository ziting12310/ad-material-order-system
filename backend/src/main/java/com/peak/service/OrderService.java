package com.peak.service;

import com.peak.dto.OrderApproveRequest;
import com.peak.dto.OrderAssignRequest;
import com.peak.dto.OrderCancelRequest;
import com.peak.dto.OrderCreateRequest;
import com.peak.dto.OrderConfirmDTO;
import com.peak.dto.OrderDeliverRequest;
import com.peak.dto.OrderRejectRequest;
import com.peak.dto.OrderUpdateRequest;
import com.peak.dto.VersionOnlyRequest;
import com.peak.vo.CursorPage;
import com.peak.vo.OrderAssignResponseVO;
import com.peak.vo.OrderApproveResponseVO;
import com.peak.vo.OrderCreateResponseVO;
import com.peak.vo.OrderDeliverResponseVO;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OperationLogVO;
import com.peak.vo.OrderListItemVO;
import com.peak.vo.OrderRejectResponseVO;
import com.peak.vo.OrderStatusUpdateResponseVO;

/**
 * 订单核心服务接口 — API.md §3（覆盖 13 个 Domain Actions 中 11 个订单级动作）
 *
 * <p>职责边界：
 *  - 业务校验（权限 / 状态流转 / 乐观锁）
 *  - 状态推进（调用 MP updateById 触发 @Version 乐观锁）
 *  - VO 组装（Entity → VO，联表 users.name 拿设计师/创建人姓名）
 *  - 不写 SQL，全部委托 OrderMapper
 * </p>
 *
 * <p>状态流转依据：ARCHITECTURE.md 状态机 T-01 ~ T-10</p>
 */
public interface OrderService {

    // ---------- 查询 ----------

    /**
     * 订单列表（Cursor 游标分页）— API.md §3.1 ListOrders
     * <p>按角色过滤数据范围：AM 看自己创建 / BL 看全部 / DS 看分派给自己的</p>
     *
     * @param userId      当前登录用户 ID
     * @param userRole    当前用户角色（AM / BL / DS）
     * @param cursor      上一页游标（null 表示第一页）
     * @param limit       每页数量（默认 20，最大 100）
     * @param materialType 可选物料类型过滤
     * @param status      可选状态过滤
     * @param keyword     可选关键字（订单号 / 客户名称）
     */
    CursorPage<OrderListItemVO> listOrders(Long userId, String userRole,
                                           String cursor, int limit,
                                           String materialType, String status, String keyword,
                                           Long designerId, Long creatorId,
                                           String deliveryDateFrom, String deliveryDateTo);

    /**
     * 订单详情（含参考文件 / 设计稿版本 / 修改意见聚合）— API.md §3.3 GetOrder
     * <p>数据范围校验：仅创建人 / 设计人 / BL 可查</p>
     */
    OrderDetailVO getOrderDetail(Long orderId, Long userId, String userRole);

    // ---------- 写入（全部带乐观锁校验 + @Transactional）----------

    /** 创建订单 — API.md §3.2 CreateOrder；状态 WAIT_CREATE → CREATED（T-01） */
    OrderCreateResponseVO createOrder(OrderCreateRequest dto, Long creatorId);

    /** 编辑订单 — API.md §3.4 UpdateOrder；仅 CREATED 状态可编辑 */
    OrderCreateResponseVO updateOrder(Long orderId, OrderUpdateRequest dto, Long operatorId);

    /** 分派设计人员 — API.md §3.5 AssignOrder；CREATED → ASSIGNED（T-02） */
    OrderAssignResponseVO assignOrder(Long orderId, OrderAssignRequest dto, Long operatorId);

    /** 设计师接单 — API.md §3.6 AcceptTask；ASSIGNED → DESIGNING（T-03） */
    OrderStatusUpdateResponseVO acceptTask(Long orderId, VersionOnlyRequest dto, Long designerId);

    /** 驳回设计稿 — API.md §3.7 RejectDesign；WAIT_APPROVE → DESIGNING（T-06）+ 自动生成一条 Feedback */
    OrderRejectResponseVO rejectDesign(Long orderId, OrderRejectRequest dto, Long operatorId);

    /** 确认终稿 — API.md §3.8 ApproveFinal；WAIT_APPROVE → APPROVED（T-08）+ 标记当前版本为 final */
    OrderApproveResponseVO approveFinal(Long orderId, OrderApproveRequest dto, Long operatorId);

    /** 标记交付 — API.md §3.9 MarkDelivered；APPROVED → DELIVERED（T-09）+ 归档 archived=1 */
    OrderDeliverResponseVO markDelivered(Long orderId, OrderDeliverRequest dto, Long operatorId);

    /** 作废订单 — API.md §3.10 CancelOrder；任意非终态 → CANCELLED（T-10） */
    OrderStatusUpdateResponseVO cancelOrder(Long orderId, OrderCancelRequest dto, Long operatorId);

    /** 删除订单 — 仅 BL 可删除 CREATED 状态订单（逻辑删除） */
    void deleteOrder(Long orderId, Long operatorId);

    // ---------- 多物料模型新增 ----------

    /**
     * 同步订单状态（根据所有任务状态聚合）
     * 由 OrderTaskServiceImpl 在任务状态变化后调用
     */
    void syncOrderStatus(Long orderId, Long operatorId);

    /**
     * BL 确认物料清单和截止时间
     * 确认后订单进入 CONFIRMED 状态，后续变更必须留操作日志
     */
    void confirmOrder(Long orderId, OrderConfirmDTO dto, Long operatorId);

    /** 查询订单操作历史 */
    java.util.List<OperationLogVO> listOperationLogs(Long orderId, Long userId, String userRole);
}
