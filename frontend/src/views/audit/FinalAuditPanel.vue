<template>
  <!--
    定稿审核面板（可复用组件，任务级）
    功能：统计卡片 + Tab切换（待审核/审核记录/已归档/已驳回）+ 任务表格 + 定稿/驳回弹窗 + 前端本地分页
    权限：canAudit=true 显示确认定稿/驳回（客户经理工作台）；false 只读（业务负责人页面）
  -->
  <div class="final-audit">
    <!-- 页面标题（嵌入工作台 Tab 时可隐藏） -->
    <h2 v-if="showTitle" class="page-title">{{ pageTitle }}</h2>

    <!-- 顶部统计卡片 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-pending" @click="switchToPending">
          <div class="stat-icon-wrap">
            <el-icon :size="24"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-approved" @click="switchToApproved">
          <div class="stat-icon-wrap">
            <el-icon :size="24"><CircleCheckFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.approved }}</div>
            <div class="stat-label">已定稿</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-rejected" @click="switchToRejected">
          <div class="stat-icon-wrap">
            <el-icon :size="24"><CircleCloseFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.rejected }}</div>
            <div class="stat-label">已驳回</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 主内容区 -->
    <el-card class="main-card" shadow="never">
      <el-tabs v-model="activeTab" class="audit-tabs">
        <!-- ========== 待审核 Tab ========== -->
        <el-tab-pane label="待审核" name="pending">
          <!-- 筛选 -->
          <div class="filter-bar">
            <el-form :model="pendingFilters" inline label-width="72px" @submit.prevent>
              <el-form-item label="关键字">
                <el-input
                  v-model="pendingFilters.keyword"
                  placeholder="任务编号/规格"
                  clearable
                  style="width: 200px"
                  @keyup.enter="handlePendingSearch"
                />
              </el-form-item>
              <el-form-item label="物料类型">
                <el-select
                  v-model="pendingFilters.materialType"
                  placeholder="全部类型"
                  clearable
                  style="width: 140px"
                >
                  <el-option
                    v-for="item in MATERIAL_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handlePendingSearch">搜索</el-button>
                <el-button :icon="RefreshLeft" @click="handlePendingReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 任务表格 -->
          <el-table
            v-loading="pendingLoading"
            :data="pagedPendingList"
            border
            style="width: 100%"
            :header-cell-style="headerCellStyle"
            class="pending-table"
          >
            <el-table-column prop="taskNo" label="任务编号" width="150">
              <template #default="{ row }">
                <span class="mono-text">{{ row.taskNo || row.id || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="所属订单" width="160">
              <template #default="{ row }">
                <span class="order-no-text">{{ row.orderNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="customerName" label="公司名称" min-width="120" />
            <el-table-column label="物料类型" width="100">
              <template #default="{ row }">
                {{ getMaterialLabel(row.materialType) }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="right" />
            <el-table-column prop="specification" label="规格" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.specification || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="getTaskStatusType(row.status)"
                  effect="light"
                  round
                  size="small"
                >
                  {{ getTaskStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止日期" width="120">
              <template #default="{ row }">
                {{ row.deadline || row.deliveryDate || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                  查看详情
                </el-button>
                <!-- 仅 canAudit=true（客户经理工作台）可执行定稿/驳回；业务负责人页面只读 -->
                <el-button v-if="canAudit" type="success" size="small" @click="openApprove(row)">
                  确认定稿
                </el-button>
                <el-button v-if="canAudit" type="danger" size="small" plain @click="openReject(row)">
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页（前端本地分页，每页 10 条） -->
          <div class="load-more-wrap">
            <div v-if="pendingError && !pendingLoading" class="error-state">
              <el-result icon="error" title="加载失败" sub-title="数据加载失败，请检查后端服务是否正常">
                <template #extra>
                  <el-button type="primary" @click="handlePendingSearch">重新加载</el-button>
                </template>
              </el-result>
            </div>
            <el-empty
              v-else-if="!pendingLoading && pendingList.length === 0"
              :description="hasPendingFilters ? '暂无符合条件的数据' : '暂无待审核任务'"
              :image-size="100"
            />
            <el-pagination
              v-else
              v-model:current-page="pendingPage"
              :page-size="CLIENT_PAGE_SIZE"
              :total="pendingList.length"
              :pager-count="5"
              layout="total, prev, pager, next, jumper"
              background
            />
          </div>
        </el-tab-pane>

        <!-- ========== 审核记录 Tab ========== -->
        <el-tab-pane label="审核记录" name="logs">
          <div v-loading="logsLoading" class="audit-log-list">
            <el-timeline v-if="auditLogs.length > 0">
              <el-timeline-item
                v-for="(item, index) in pagedAuditLogs"
                :key="item.id || index"
                :timestamp="formatDateTime(item.operatedAt || item.createdAt)"
                :type="logActionType(item.action)"
                placement="top"
              >
                <div class="log-card">
                  <div class="log-header">
                    <span class="log-order">订单：{{ item.orderNo || '—' }}</span>
                    <el-tag
                      :type="logActionType(item.action)"
                      effect="light"
                      size="small"
                      round
                    >
                      {{ logActionLabel(item.action) }}
                    </el-tag>
                  </div>
                  <div v-if="item.remark || item.note" class="log-remark">
                    {{ item.remark || item.note }}
                  </div>
                  <div class="log-footer">
                    <span v-if="item.operatorName" class="log-operator">
                      操作人：{{ item.operatorName }}
                    </span>
                    <span v-if="item.taskNo" class="log-task">
                      任务：{{ item.taskNo }}
                    </span>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty
              v-if="!logsLoading && auditLogs.length === 0"
              description="暂无审核记录"
              :image-size="80"
            />
            <!-- 分页（前端本地分页，每页 10 条） -->
            <div v-if="auditLogs.length > 0" class="load-more-wrap">
              <el-pagination
                v-model:current-page="logsPage"
                :page-size="CLIENT_PAGE_SIZE"
                :total="auditLogs.length"
                :pager-count="5"
                layout="total, prev, pager, next, jumper"
                background
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- ========== 已归档 Tab ========== -->
        <el-tab-pane label="已归档" name="archive">
          <!-- 筛选 -->
          <div class="filter-bar">
            <el-form :model="archiveFilters" inline label-width="72px" @submit.prevent>
              <el-form-item label="关键字">
                <el-input
                  v-model="archiveFilters.keyword"
                  placeholder="订单号/客户名"
                  clearable
                  style="width: 200px"
                  @keyup.enter="handleArchiveSearch"
                />
              </el-form-item>
              <el-form-item label="物料类型">
                <el-select
                  v-model="archiveFilters.materialType"
                  placeholder="全部类型"
                  clearable
                  style="width: 140px"
                >
                  <el-option
                    v-for="item in MATERIAL_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="交付日期">
                <el-date-picker
                  v-model="archiveFilters.deliveryDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 260px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleArchiveSearch">搜索</el-button>
                <el-button :icon="RefreshLeft" @click="handleArchiveReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 已定稿（原归档）表格：改用 listApprovedTasks —— 与卡片数字同一口径 -->
          <el-table
            v-loading="archiveLoading"
            :data="pagedArchiveList"
            border
            style="width: 100%"
            :header-cell-style="headerCellStyle"
            class="archive-table"
          >
            <el-table-column prop="taskNo" label="任务编号" width="150">
              <template #default="{ row }">
                <span class="mono-text">{{ row.taskNo || row.id || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="所属订单" width="160">
              <template #default="{ row }">
                <span class="order-no-text">{{ row.orderNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="customerName" label="公司名称" min-width="140" />
            <el-table-column label="物料类型" width="100" align="center">
              <template #default="{ row }">
                {{ getMaterialLabel(row.materialType) }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="right" />
            <el-table-column prop="specification" label="规格" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.specification || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'DELIVERED' ? 'success' : 'primary'"
                  effect="light"
                  round
                  size="small"
                >
                  {{ row.status === 'DELIVERED' ? '已交付' : '已定稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止日期" width="120">
              <template #default="{ row }">
                {{ row.deadline || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页（前端本地分页，每页 10 条） -->
          <div class="load-more-wrap">
            <el-empty
              v-if="!archiveLoading && archiveList.length === 0"
              :description="hasArchiveFilters ? '暂无符合条件的数据' : '暂无数据'"
              :image-size="100"
            />
            <el-pagination
              v-else
              v-model:current-page="archivePage"
              :page-size="CLIENT_PAGE_SIZE"
              :total="archiveList.length"
              :pager-count="5"
              layout="total, prev, pager, next, jumper"
              background
            />
          </div>
        </el-tab-pane>

        <!-- ========== 已驳回 Tab ========== -->
        <el-tab-pane label="已驳回" name="rejected">
          <!-- 筛选 -->
          <div class="filter-bar">
            <el-form :model="rejectedFilters" inline label-width="72px" @submit.prevent>
              <el-form-item label="关键字">
                <el-input
                  v-model="rejectedFilters.keyword"
                  placeholder="任务编号/规格"
                  clearable
                  style="width: 200px"
                  @keyup.enter="handleRejectedSearch"
                />
              </el-form-item>
              <el-form-item label="物料类型">
                <el-select
                  v-model="rejectedFilters.materialType"
                  placeholder="全部类型"
                  clearable
                  style="width: 140px"
                >
                  <el-option
                    v-for="item in MATERIAL_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleRejectedSearch">搜索</el-button>
                <el-button :icon="RefreshLeft" @click="handleRejectedReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 任务表格 -->
          <el-table
            v-loading="rejectedLoading"
            :data="pagedRejectedList"
            border
            style="width: 100%"
            :header-cell-style="headerCellStyle"
            class="pending-table"
          >
            <el-table-column prop="taskNo" label="任务编号" width="150">
              <template #default="{ row }">
                <span class="mono-text">{{ row.taskNo || row.id || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="所属订单" width="160">
              <template #default="{ row }">
                <span class="order-no-text">{{ row.orderNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="customerName" label="公司名称" min-width="120" />
            <el-table-column label="物料类型" width="100">
              <template #default="{ row }">
                {{ getMaterialLabel(row.materialType) }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="right" />
            <el-table-column prop="specification" label="规格" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.specification || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="getTaskStatusType(row.status)"
                  effect="light"
                  round
                  size="small"
                >
                  {{ getTaskStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止日期" width="120">
              <template #default="{ row }">
                {{ row.deadline || row.deliveryDate || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页（前端本地分页，每页 10 条） -->
          <div class="load-more-wrap">
            <el-empty
              v-if="!rejectedLoading && rejectedList.length === 0"
              :description="hasRejectedFilters ? '暂无符合条件的数据' : '暂无已驳回任务'"
              :image-size="100"
            />
            <el-pagination
              v-else
              v-model:current-page="rejectedPage"
              :page-size="CLIENT_PAGE_SIZE"
              :total="rejectedList.length"
              :pager-count="5"
              layout="total, prev, pager, next, jumper"
              background
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ========== 确认定稿弹窗 ========== -->
    <el-dialog
      v-model="approveDialog.visible"
      title="确认定稿"
      width="460px"
      :close-on-click-modal="false"
      align-center
    >
      <div class="dialog-body">
        <div class="dialog-icon success">
          <el-icon :size="28"><CircleCheckFilled /></el-icon>
        </div>
        <div class="dialog-text">
          <p class="dialog-title">确定确认该任务定稿？</p>
          <p class="dialog-sub">
            任务编号：<span class="mono-text">{{ approveDialog.row?.taskNo || approveDialog.row?.id }}</span>
          </p>
          <p class="dialog-sub">
            所属订单：{{ approveDialog.row?.orderNo || '—' }}
          </p>
          <p class="dialog-sub">
            公司名称：{{ approveDialog.row?.customerName || '—' }}
          </p>
          <p class="dialog-tip">
            定稿后任务进入归档状态，不可再修改。
          </p>
        </div>
      </div>
      <el-form :model="approveForm" label-width="72px" class="approve-form">
        <el-form-item label="定稿备注">
          <el-input
            v-model="approveForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入定稿备注（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="approveDialog.loading" @click="handleApprove">
          确认定稿
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 驳回弹窗 ========== -->
    <el-dialog
      v-model="rejectDialog.visible"
      title="驳回定稿"
      width="480px"
      :close-on-click-modal="false"
      align-center
    >
      <el-form
        ref="rejectFormRef"
        :model="rejectForm"
        :rules="rejectRules"
        label-width="80px"
      >
        <el-form-item label="任务编号">
          <span class="mono-text">{{ rejectDialog.row?.taskNo || rejectDialog.row?.id }}</span>
        </el-form-item>
        <el-form-item label="所属订单">
          {{ rejectDialog.row?.orderNo || '—' }}
        </el-form-item>
        <el-form-item label="公司名称">
          {{ rejectDialog.row?.customerName || '—' }}
        </el-form-item>
        <el-form-item label="驳回原因" prop="rejectRemark">
          <el-input
            v-model="rejectForm.rejectRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因，将反馈给设计人员"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.loading" @click="handleReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 归档详情抽屉 ========== -->
    <el-drawer
      v-model="archiveDrawer.visible"
      :title="''"
      direction="rtl"
      size="720px"
      :close-on-click-modal="false"
      class="archive-detail-drawer"
      @open="handleArchiveDrawerOpen"
    >
      <template #header>
        <div class="drawer-header">
          <div class="header-left">
            <span class="order-no">{{ archiveDrawer.row?.orderNo || '—' }}</span>
            <el-tag type="success" effect="light" round size="small">已归档</el-tag>
          </div>
          <div class="header-right">
            <span class="customer-name">{{ archiveDrawer.row?.customerName || '—' }}</span>
          </div>
        </div>
      </template>

      <div v-loading="archiveDrawer.loading" class="drawer-body">
        <el-descriptions :column="2" border size="default" class="detail-descriptions">
          <el-descriptions-item label="订单编号">
            <span class="mono-text">{{ archiveDetail.orderNo || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="公司名称">
            {{ archiveDetail.customerName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ archiveDetail.contactName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ archiveDetail.contactPhone || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="交付日期">
            {{ archiveDetail.deliveryDate || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="归档时间">
            {{ formatDateTime(archiveDetail.archivedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ archiveDetail.remark || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 物料任务 -->
        <div class="section-title">物料任务</div>
        <el-table
          :data="archiveDetail.tasks || []"
          border
          size="default"
          class="task-table"
        >
          <el-table-column label="任务编号" width="130">
            <template #default="{ row }">
              <span class="mono-text">{{ row.taskNo || row.id || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="物料" width="100">
            <template #default="{ row }">
              {{ getMaterialLabel(row.materialType) }}
            </template>
          </el-table-column>
          <el-table-column label="数量" width="80" align="right">
            <template #default="{ row }">
              {{ row.quantity || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="规格" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.specification || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="设计师" width="100">
            <template #default="{ row }">
              {{ row.designerName || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag
                :type="getTaskStatusType(row.status)"
                effect="light"
                round
                size="small"
              >
                {{ getTaskStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!archiveDetail.tasks || archiveDetail.tasks.length === 0"
          description="暂无物料任务"
          :image-size="60"
        />
      </div>
    </el-drawer>

    <!-- 订单详情抽屉（复用 OrderDetailDrawer，所有 tab 的"查看详情"都走这里） -->
    <OrderDetailDrawer
      v-model="detailDrawerVisible"
      :order-id="detailOrderId"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  RefreshLeft,
  Clock,
  CircleCheckFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue'
import {
  listPendingTasks,
  listApprovedTasks,
  listRejectedTasks,
  listMyAuditLogs,
  getArchivedOrder,
  getAuditStats,
  approveTask,
  rejectTask
} from '@/api/audit'
import { listOrders } from '@/api/order'
import OrderDetailDrawer from '@/views/order/OrderDetailDrawer.vue'
import {
  MATERIAL_OPTIONS,
  getMaterialLabel,
  getTaskStatusLabel,
  getTaskStatusType
} from '@/constants'

// ---------- 权限 / 展示配置（由父页面通过 props 控制） ----------
const props = defineProps({
  // 是否显示「确认定稿 / 驳回」按钮与弹窗（客户经理工作台为 true，业务负责人只读为 false）
  canAudit: { type: Boolean, default: false },
  // 是否显示页面标题（嵌入工作台 Tab 时可关闭）
  showTitle: { type: Boolean, default: true },
  // 页面标题文案
  title: { type: String, default: '定稿审核' }
})

// 订单详情抽屉（查看详情按钮复用的组件）
const detailDrawerVisible = ref(false)
const detailOrderId = ref(null)

// 页面标题
const pageTitle = computed(() => props.title)

// ---------- 常量 ----------
const PAGE_SIZE = 20
// 前端本地分页：每页 10 条（后端一次性返回全量后内存切片）
const CLIENT_PAGE_SIZE = 10
const headerCellStyle = {
  background: 'var(--gray-50)',
  color: 'var(--gray-600)',
  fontWeight: 500
}

// ---------- Tab ----------
const activeTab = ref('pending')

// 是否有待审核筛选条件
const hasPendingFilters = computed(() => {
  return !!(pendingFilters.keyword || pendingFilters.materialType)
})

// 是否有归档筛选条件
const hasArchiveFilters = computed(() => {
  return !!(archiveFilters.keyword || archiveFilters.materialType ||
    (archiveFilters.deliveryDateRange && archiveFilters.deliveryDateRange.length === 2))
})

function switchToPending() {
  activeTab.value = 'pending'
  fetchPendingList()
  fetchStats()
}
function switchToApproved() {
  // 已定稿卡片 → 切换到已归档 tab
  activeTab.value = 'archive'
  fetchArchiveList()
  fetchStats()
}
function switchToRejected() {
  // 已驳回卡片 → 切换到已驳回 tab
  activeTab.value = 'rejected'
  fetchRejectedList()
  fetchStats()
}

// ---------- 统计 ----------
const stats = reactive({
  pending: 0,
  approved: 0,
  rejected: 0
})

async function fetchStats() {
  try {
    const res = await getAuditStats()
    stats.pending = res.pendingCount || 0
    stats.approved = res.approvedCount || 0
    stats.rejected = res.rejectedCount || 0
  } catch (e) {
    // 降级兜底：至少不显示 NaN
    stats.pending = stats.pending || 0
    stats.approved = stats.approved || 0
    stats.rejected = stats.rejected || 0
  }
}

// ========== 待审核 Tab ==========
const pendingFilters = reactive({
  keyword: '',
  materialType: ''
})
const pendingLoading = ref(false)
const pendingList = ref([])          // 全量待审核任务（后端一次拉完）
const pendingPage = ref(1)           // 当前页码
const pendingError = ref(false)

// 当前页展示的待审核数据：内存切片
const pagedPendingList = computed(() => {
  const start = (pendingPage.value - 1) * CLIENT_PAGE_SIZE
  return pendingList.value.slice(start, start + CLIENT_PAGE_SIZE)
})

/**
 * 拉取全量待审核任务（沿用原游标接口，循环翻完所有页后汇总；后端不改）
 * 刷新后自动回到第 1 页
 */
async function fetchPendingList() {
  pendingLoading.value = true
  pendingError.value = false

  try {
    const allTasks = []
    let cursor = null
    let hasMore = true

    while (hasMore) {
      const params = { limit: PAGE_SIZE }
      if (cursor) params.cursor = cursor
      if (pendingFilters.keyword) params.keyword = pendingFilters.keyword
      if (pendingFilters.materialType) params.materialType = pendingFilters.materialType

      const res = await listPendingTasks(params)
      allTasks.push(...(res.list || []))

      cursor = res.nextCursor || null
      hasMore = !!res.hasMore && !!cursor
    }

    pendingList.value = allTasks
    pendingPage.value = 1
  } catch (e) {
    pendingError.value = true
    pendingList.value = []
    pendingPage.value = 1
  } finally {
    pendingLoading.value = false
  }
}

function handlePendingSearch() {
  fetchPendingList()
}

function handlePendingReset() {
  pendingFilters.keyword = ''
  pendingFilters.materialType = ''
  fetchPendingList()
}

// ========== 审核记录 Tab ==========
const logsLoading = ref(false)
const auditLogs = ref([])
const logsPage = ref(1)

// 当前页展示的审核记录：内存切片
const pagedAuditLogs = computed(() => {
  const start = (logsPage.value - 1) * CLIENT_PAGE_SIZE
  return auditLogs.value.slice(start, start + CLIENT_PAGE_SIZE)
})

async function fetchAuditLogs() {
  logsLoading.value = true
  try {
    const res = await listMyAuditLogs(50)
    auditLogs.value = res.list || res || []
    logsPage.value = 1
  } catch (e) {
    // mock 数据
    auditLogs.value = [
      { id: 1, orderNo: 'ORD-2026-00015', taskNo: 'T-0015-01', action: 'APPROVE', remark: '设计符合要求，确认定稿', operatorName: '张经理', operatedAt: '2026-11-03 14:20:00' },
      { id: 2, orderNo: 'ORD-2026-00012', taskNo: 'T-0012-01', action: 'REJECT', remark: '颜色需要调整，参考稿为准', operatorName: '李主管', operatedAt: '2026-11-02 16:40:00' },
      { id: 3, orderNo: 'ORD-2026-00008', taskNo: 'T-0008-01', action: 'APPROVE', remark: '定稿通过', operatorName: '张经理', operatedAt: '2026-10-30 10:15:00' },
      { id: 4, orderNo: 'ORD-2026-00005', taskNo: 'T-0005-02', action: 'REJECT', remark: '版式需要重新调整', operatorName: '王总监', operatedAt: '2026-10-28 09:30:00' }
    ]
  } finally {
    logsLoading.value = false
  }
}

// 审核操作类型映射
function logActionLabel(action) {
  const map = {
    APPROVE: '确认定稿',
    REJECT: '驳回设计',
    SUBMIT: '提交审核',
    ASSIGN: '任务分派',
    ACCEPT: '设计师接单',
    CANCEL: '订单作废'
  }
  if (!action) return '—'
  return map[action.toUpperCase()] || action
}

function logActionType(action) {
  if (!action) return 'info'
  const upper = action.toUpperCase()
  if (upper.includes('APPROVE')) return 'success'
  if (upper.includes('REJECT')) return 'danger'
  if (upper.includes('SUBMIT')) return 'warning'
  if (upper.includes('ASSIGN') || upper.includes('ACCEPT')) return 'primary'
  return 'info'
}

// ========== 已归档 Tab ==========
const archiveFilters = reactive({
  keyword: '',
  materialType: '',
  deliveryDateRange: []
})
const archiveLoading = ref(false)
const archiveList = ref([])          // 全量已定稿任务（后端一次拉完）
const archivePage = ref(1)           // 当前页码

// 当前页展示的已定稿数据：内存切片
const pagedArchiveList = computed(() => {
  const start = (archivePage.value - 1) * CLIENT_PAGE_SIZE
  return archiveList.value.slice(start, start + CLIENT_PAGE_SIZE)
})

/**
 * 拉取全量已定稿任务（沿用原游标接口，循环翻完所有页后汇总；后端不改）
 * 刷新后自动回到第 1 页
 */
async function fetchArchiveList() {
  archiveLoading.value = true
  try {
    const allTasks = []
    let cursor = null
    let hasMore = true
    while (hasMore) {
      const params = { limit: PAGE_SIZE }
      if (cursor) params.cursor = cursor
      if (archiveFilters.keyword) params.keyword = archiveFilters.keyword
      if (archiveFilters.materialType) params.materialType = archiveFilters.materialType
      // 交付日期区间 → 拆分传给后端（两个独立参数）
      if (archiveFilters.deliveryDateRange && archiveFilters.deliveryDateRange.length === 2) {
        params.deliveryDateFrom = archiveFilters.deliveryDateRange[0]
        params.deliveryDateTo = archiveFilters.deliveryDateRange[1]
      }
      // 已定稿列表：listApprovedTasks —— 与卡片数字 getAuditStats.approvedCount 共用同一套 queryWrapper
      const res = await listApprovedTasks(params)
      allTasks.push(...(res.list || []))
      cursor = res.nextCursor || null
      hasMore = !!res.hasMore && !!cursor
    }
    archiveList.value = allTasks
    archivePage.value = 1
  } catch (e) {
    archiveList.value = []
    archivePage.value = 1
  } finally {
    archiveLoading.value = false
  }
}

function handleArchiveSearch() {
  fetchArchiveList()
}

function handleArchiveReset() {
  archiveFilters.keyword = ''
  archiveFilters.materialType = ''
  archiveFilters.deliveryDateRange = []
  fetchArchiveList()
}

// 归档详情抽屉
const archiveDrawer = reactive({
  visible: false,
  loading: false,
  row: null
})
const archiveDetail = ref({})

function handleViewArchive(row) {
  archiveDrawer.row = row
  archiveDrawer.visible = true
}

async function handleArchiveDrawerOpen() {
  archiveDrawer.loading = true
  archiveDetail.value = {}
  try {
    const row = archiveDrawer.row
    if (row?.id) {
      const res = await getArchivedOrder(row.id)
      archiveDetail.value = res || {}
    }
  } catch (e) {
    // 用行数据兜底
    archiveDetail.value = { ...archiveDrawer.row }
  } finally {
    archiveDrawer.loading = false
  }
}

// ========== 已驳回 Tab ==========
const rejectedFilters = reactive({
  keyword: '',
  materialType: ''
})
const rejectedLoading = ref(false)
const rejectedList = ref([])         // 全量已驳回任务（后端一次拉完）
const rejectedPage = ref(1)          // 当前页码
const rejectedError = ref(false)

const hasRejectedFilters = computed(() => {
  return !!(rejectedFilters.keyword || rejectedFilters.materialType)
})

// 当前页展示的已驳回数据：内存切片
const pagedRejectedList = computed(() => {
  const start = (rejectedPage.value - 1) * CLIENT_PAGE_SIZE
  return rejectedList.value.slice(start, start + CLIENT_PAGE_SIZE)
})

/**
 * 拉取全量已驳回任务（沿用原游标接口，循环翻完所有页后汇总；后端不改）
 * 刷新后自动回到第 1 页
 */
async function fetchRejectedList() {
  rejectedLoading.value = true
  rejectedError.value = false
  try {
    const allTasks = []
    let cursor = null
    let hasMore = true
    while (hasMore) {
      const params = { limit: PAGE_SIZE }
      if (cursor) params.cursor = cursor
      if (rejectedFilters.keyword) params.keyword = rejectedFilters.keyword
      if (rejectedFilters.materialType) params.materialType = rejectedFilters.materialType
      const res = await listRejectedTasks(params)
      allTasks.push(...(res.list || []))
      cursor = res.nextCursor || null
      hasMore = !!res.hasMore && !!cursor
    }
    rejectedList.value = allTasks
    rejectedPage.value = 1
  } catch (e) {
    rejectedError.value = true
    rejectedList.value = []
    rejectedPage.value = 1
  } finally {
    rejectedLoading.value = false
  }
}

function handleRejectedSearch() {
  fetchRejectedList()
}

function handleRejectedReset() {
  rejectedFilters.keyword = ''
  rejectedFilters.materialType = ''
  fetchRejectedList()
}

// ========== 确认定稿弹窗 ==========
const approveDialog = reactive({
  visible: false,
  loading: false,
  row: null
})
const approveForm = reactive({
  remark: ''
})

function openApprove(row) {
  approveDialog.row = row
  approveForm.remark = ''
  approveDialog.visible = true
  approveDialog.loading = false
}

async function handleApprove() {
  const row = approveDialog.row
  if (!row) return
  // 行数据来自 /audit/pending（OrderTaskVO），自带乐观锁 version
  if (row.version == null) {
    ElMessage.error('当前行缺少乐观锁 version，请刷新列表后重试')
    return
  }

  approveDialog.loading = true
  try {
    // 任务级定稿：body 必须携带任务乐观锁 version
    const orderId = row.orderId || row.id
    const taskId = row.id
    await approveTask(orderId, taskId, { remark: approveForm.remark, version: row.version })
    ElMessage.success('定稿成功')
    approveDialog.visible = false
    // 刷新列表和统计（重新拉全量，自动回到第 1 页）
    fetchPendingList()
    fetchStats()
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    approveDialog.loading = false
  }
}

// ========== 驳回弹窗 ==========
const rejectDialog = reactive({
  visible: false,
  loading: false,
  row: null
})
const rejectFormRef = ref(null)
const rejectForm = reactive({ rejectRemark: '' })
const rejectRules = {
  rejectRemark: [
    { required: true, message: '请输入驳回原因', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ]
}

function openReject(row) {
  rejectDialog.row = row
  rejectForm.rejectRemark = ''
  rejectDialog.visible = true
  rejectDialog.loading = false
}

async function handleReject() {
  if (!rejectFormRef.value) return
  rejectFormRef.value.validate(async valid => {
    if (!valid) return
    const row = rejectDialog.row
    if (!row) return
    // 行数据来自 /audit/pending（OrderTaskVO），自带乐观锁 version
    if (row.version == null) {
      ElMessage.error('当前行缺少乐观锁 version，请刷新列表后重试')
      return
    }

    rejectDialog.loading = true
    try {
      // 任务级驳回：后端字段为 rejectNote，body 必须携带任务乐观锁 version
      const orderId = row.orderId || row.id
      const taskId = row.id
      await rejectTask(orderId, taskId, { rejectNote: rejectForm.rejectRemark, version: row.version })
      ElMessage.success('已驳回')
      rejectDialog.visible = false
      // 刷新列表和统计（重新拉全量，自动回到第 1 页）
      fetchPendingList()
      fetchStats()
    } catch (e) {
      // 错误已由拦截器处理
    } finally {
      rejectDialog.loading = false
    }
  })
}

// ========== 查看详情（复用订单详情抽屉） ==========
/**
 * 后端 OrderTaskVO 主键字段：
 *   id      = 任务ID（不是订单ID，不能传给订单详情抽屉）
 *   orderId = 所属订单ID（查看详情必须用这个）
 * 注意 application.yml 配置了 default-property-inclusion: non_null，
 * 字段为 null 时不会出现在 JSON 中（如“已驳回”列表），因此需要兜底解析。
 */
async function handleViewDetail(row) {
  // 1. 打印行数据，确认后端真实返回的字段名
  console.log('[定稿审核] 查看详情 row =', row)

  // 2. 取后端真实字段 orderId（兼容可能的 snake_case 写法）
  let orderId = row?.orderId ?? row?.order_id ?? row?.order?.id ?? null

  // 3. 兜底：列表只返回了订单号时，用订单号反查订单ID（复用已有 GET /orders 接口）
  if (!orderId && row?.orderNo) {
    try {
      const res = await listOrders({ keyword: row.orderNo, limit: 20 })
      const matched = (res.list || []).find(o => o.orderNo === row.orderNo)
      orderId = matched?.id ?? null
    } catch (e) {
      orderId = null
    }
  }

  // 4. 仍为空则提示并终止，不跳转
  if (!orderId) {
    ElMessage.warning('当前行缺少订单ID，无法查看详情')
    return
  }

  detailOrderId.value = orderId
  detailDrawerVisible.value = true
}

// ---------- 工具函数 ----------
function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = n => String(n).padStart(2, '0')
  return (
    d.getFullYear() +
    '-' + pad(d.getMonth() + 1) +
    '-' + pad(d.getDate()) +
    ' ' + pad(d.getHours()) +
    ':' + pad(d.getMinutes())
  )
}

// ---------- Tab 切换时懒加载 ----------
watch(activeTab, (val) => {
  if (val === 'logs' && auditLogs.value.length === 0) {
    fetchAuditLogs()
  }
  if (val === 'archive' && archiveList.value.length === 0) {
    fetchArchiveList()
  }
  if (val === 'rejected' && rejectedList.value.length === 0) {
    fetchRejectedList()
  }
})

// ---------- 生命周期 ----------
onMounted(() => {
  fetchStats()
  fetchPendingList(false)
})
</script>

<style scoped>
.final-audit {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--gray-900);
}

/* ========== 统计卡片 ========== */
.stat-row {
  margin-bottom: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-200);
  background: #ffffff;
  cursor: pointer;
  transition: var(--transition-base);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: var(--border-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--gray-800);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--gray-500);
}

.stat-pending .stat-icon-wrap {
  background: var(--warning-light);
  color: #d97706;
}
.stat-pending .stat-value {
  color: #d97706;
}

.stat-approved .stat-icon-wrap {
  background: var(--success-light);
  color: #059669;
}
.stat-approved .stat-value {
  color: #059669;
}

.stat-rejected .stat-icon-wrap {
  background: var(--danger-light);
  color: #dc2626;
}
.stat-rejected .stat-value {
  color: #dc2626;
}

/* ========== 主卡片 ========== */
.main-card {
  border-radius: var(--border-radius);
}

.main-card :deep(.el-card__body) {
  padding: 0;
}

.audit-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  border-bottom: 1px solid var(--gray-100);
}

.audit-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.audit-tabs :deep(.el-tab-pane) {
  padding: 20px;
}

/* ========== 筛选栏 ========== */
.filter-bar {
  margin-bottom: 16px;
  padding: 16px 20px;
  background: var(--gray-50);
  border-radius: var(--border-radius);
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 0;
}

/* ========== 表格通用 ========== */
.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 500;
}

.order-no-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--gray-700);
  font-size: 13px;
}

/* ========== 加载更多 ========== */
.load-more-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  border-top: 1px solid var(--gray-100);
  min-height: 60px;
}

.no-more {
  font-size: 13px;
  color: var(--gray-400);
}

.error-state {
  padding: 40px 0;
}

/* ========== 审核记录时间线 ========== */
.audit-log-list {
  min-height: 200px;
  padding: 0 12px;
}

.log-card {
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid var(--gray-100);
  border-radius: var(--border-radius);
}

.log-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.log-order {
  font-size: 14px;
  font-weight: 500;
  color: var(--gray-800);
}

.log-remark {
  font-size: 13px;
  color: var(--gray-600);
  line-height: 1.5;
  margin-bottom: 6px;
}

.log-footer {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--gray-400);
}

/* ========== 确认定稿弹窗 ========== */
.dialog-body {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 0 20px;
}

.dialog-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dialog-icon.success {
  background: var(--success-light);
  color: #059669;
}

.dialog-text {
  flex: 1;
}

.dialog-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--gray-800);
  margin: 0 0 10px;
}

.dialog-sub {
  font-size: 13px;
  color: var(--gray-500);
  margin: 4px 0;
}

.dialog-tip {
  font-size: 12px;
  color: #d97706;
  background: var(--warning-light);
  padding: 6px 10px;
  border-radius: var(--border-radius-sm);
  margin-top: 10px;
}

.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
}

.approve-form {
  margin-top: 8px;
  border-top: 1px solid var(--gray-100);
  padding-top: 16px;
}

/* ========== 归档详情抽屉 ========== */
.archive-detail-drawer :deep(.el-drawer) {
  border-radius: var(--border-radius) 0 0 var(--border-radius);
}

.archive-detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 0;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-100);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-no {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.header-right {
  font-size: 14px;
  color: var(--gray-700);
  font-weight: 500;
}

.drawer-body {
  padding: 0 20px 20px;
  height: calc(100vh - 65px);
  overflow-y: auto;
}

.detail-descriptions {
  margin-top: 16px;
}

.section-title {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 500;
  color: var(--gray-800);
}

.task-table {
  --el-table-header-bg-color: var(--gray-50);
  --el-table-border-color: var(--gray-200);
}
</style>
