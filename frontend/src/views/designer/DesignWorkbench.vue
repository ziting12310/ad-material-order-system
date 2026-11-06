<template>
  <!--
    设计师工作台
    功能：统计卡片 + Tab切换（我的任务/延期预警）+ 筛选 + 任务表格 + 游标分页
  -->
  <div class="design-workbench">
    <!-- 页面标题 -->
    <h2 class="page-title">{{ pageTitle }}</h2>

    <!-- 顶部统计卡片 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-assigned" @click="quickFilter(firstStatStatus)">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ firstStatValue }}</div>
            <div class="stat-label">{{ firstStatLabel }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-designing" @click="quickFilter('DESIGNING')">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><Edit /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.designing }}</div>
            <div class="stat-label">设计中</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-waiting" @click="quickFilter('WAIT_APPROVE')">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.waitApprove }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-approved" @click="quickFilter('APPROVED')">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><CircleCheckFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.approved }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Tab 切换 -->
    <el-card class="tab-card" shadow="never">
      <el-tabs v-model="activeTab" class="workbench-tabs">
        <!-- 任务 Tab（DS=我的任务，BL=全部任务） -->
        <el-tab-pane :label="isBL ? '全部任务' : '我的任务'" name="tasks">
          <!-- 筛选 -->
          <div class="filter-bar">
            <el-form :model="filters" inline label-width="60px" @submit.prevent>
              <el-form-item label="状态">
                <el-select
                  v-model="filters.status"
                  placeholder="全部状态"
                  clearable
                  style="width: 130px"
                >
                  <!-- BL 多一个"待分派"状态 -->
                  <el-option v-if="isBL" label="待分派" value="CREATED" />
                  <el-option label="待确定" value="ASSIGNED" />
                  <el-option label="设计中" value="DESIGNING" />
                  <el-option label="待审核" value="WAIT_APPROVE" />
                  <el-option label="已驳回" value="REJECTED" />
                  <el-option label="已完成" value="APPROVED" />
                </el-select>
              </el-form-item>
              <el-form-item label="物料">
                <el-select
                  v-model="filters.materialType"
                  placeholder="全部类型"
                  clearable
                  style="width: 130px"
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
                <el-button type="primary" size="default" :icon="Search" @click="handleSearch">
                  搜索
                </el-button>
                <el-button size="default" :icon="RefreshLeft" @click="handleReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 任务表格 -->
          <el-table
            v-loading="loading"
            :data="pagedTasks"
            border
            style="width: 100%"
            :header-cell-style="headerCellStyle"
            class="task-table"
          >
            <el-table-column label="所属订单" width="160">
              <template #default="{ row }">
                <span class="order-no-text">{{ row.orderNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="物料类型" width="100">
              <template #default="{ row }">
                {{ getMaterialLabel(row.materialType) }}
              </template>
            </el-table-column>
            <!-- BL 多一列"设计师" -->
            <el-table-column v-if="isBL" label="设计师" width="100">
              <template #default="{ row }">
                {{ row.designerName || '—' }}
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
                {{ row.deadline || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" :width="isBL ? 160 : 160" fixed="right" align="center">
              <template #default="{ row }">
                <!-- BL：CREATED 显示分派，所有状态均可查看详情 -->
                <template v-if="isBL">
                  <el-button
                    v-if="row.status === 'CREATED'"
                    type="primary"
                    size="small"
                    @click="handleAssign(row)"
                  >
                    分派
                  </el-button>
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="handleViewDetail(row)"
                  >
                    查看详情
                  </el-button>
                </template>
                <!-- DS：待确定显示待确定按钮，设计中显示上传版本，所有状态均可查看详情 -->
                <template v-else>
                  <!-- 待确定：显示待确定按钮 -->
                  <el-button
                    v-if="row.status === 'ASSIGNED'"
                    type="primary"
                    size="small"
                    @click="handleAccept(row)"
                  >
                    待确定
                  </el-button>
                  <!-- 已驳回：归入待确定，红色【待确定】按钮触发重新开始设计 -->
                  <el-button
                    v-if="row.status === 'REJECTED'"
                    type="danger"
                    size="small"
                    @click="handleRestart(row)"
                  >
                    待确定
                  </el-button>
                  <!-- 设计中：显示上传版本按钮 -->
                  <el-button
                    v-else-if="row.status === 'DESIGNING'"
                    type="primary"
                    size="small"
                    plain
                    @click="handleUploadVersion(row)"
                  >
                    上传版本
                  </el-button>
                  <!-- 查看详情：所有状态可见 -->
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="handleViewDetail(row)"
                  >
                    查看详情
                  </el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页（前端本地分页，每页 10 条） -->
          <div class="load-more-wrap">
            <el-empty
              v-if="!loading && taskList.length === 0"
              :description="hasTaskFilters ? '暂无符合条件的数据' : '暂无数据'"
              :image-size="100"
            />
            <ClientPagination
              v-else
              :current-page="pageNum"
              :page-size="taskPageSize"
              :total="taskTotal"
              @current-change="handlePageChange"
            />
          </div>
        </el-tab-pane>

        <!-- BL：延期预警 -->
        <el-tab-pane v-if="isBL" label="延期预警" name="delay">
          <div class="delay-warning">
            <el-empty description="延期订单会在这里展示" :image-size="120" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 上传版本弹窗（预留） -->
    <el-dialog
      v-model="uploadDialog.visible"
      title="上传设计版本"
      width="520px"
      :close-on-click-modal="false"
    >
      <div class="upload-dialog-body">
        <div class="dialog-info">
          <p><span class="label">物料类型：</span>{{ getMaterialLabel(uploadDialog.row?.materialType) }}</p>
        </div>
        <el-form :model="uploadForm" label-width="80px">
          <el-form-item label="版本说明">
            <el-input
              v-model="uploadForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入本次版本的修改说明"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="设计文件">
            <FileUpload
              v-model="uploadForm.files"
              :max-size="100 * 1024 * 1024"
              :max-count="5"
              scene="design"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="uploadDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="uploadDialog.loading" @click="handleSubmitVersion">
          提交版本
        </el-button>
      </template>
    </el-dialog>

    <!-- 分派弹窗（BL） -->
    <el-dialog
      v-model="assignDialog.visible"
      title="分派任务"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="assignFormRef"
        :model="assignForm"
        :rules="assignRules"
        label-width="80px"
      >
        <el-form-item label="任务编号">
          <span class="mono-text">{{ assignDialog.row?.displayTaskNo || assignDialog.row?.taskNo || assignDialog.row?.id || '—' }}</span>
        </el-form-item>
        <el-form-item label="所属订单">
          {{ assignDialog.row?.orderNo || '—' }}
        </el-form-item>
        <el-form-item label="选择设计师" prop="designerId">
          <el-select
            v-model="assignForm.designerId"
            placeholder="请选择设计师"
            filterable
            style="width: 100%"
            :loading="designerLoading"
          >
            <el-option
              v-for="d in designerList"
              :key="d.id"
              :label="d.name + ' (' + (d.username || d.id) + ')'"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分派备注" prop="remark">
          <el-input
            v-model="assignForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入分派备注（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="assignDialog.loading" @click="handleAssignSubmit">
          确认分派
        </el-button>
      </template>
    </el-dialog>

    <!-- 订单详情抽屉（工作台行是订单级数据，复用订单详情抽屉） -->
    <OrderDetailDrawer
      v-model="detailDrawerVisible"
      :order-id="detailOrderId"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  RefreshLeft,
  Bell,
  Edit,
  Clock,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import { listOrders, acceptTask, uploadDesignVersion, assignTask, assignOrder, getOrderDetail, listDesigners, getTaskStats, restartDesign } from '@/api'
import { useClientPagination } from '@/composables/useClientPagination'
import { applyDisplayTaskNo } from '@/composables/useDisplayTaskNo'
import ClientPagination from '@/components/common/ClientPagination.vue'
import {
  MATERIAL_OPTIONS,
  getMaterialLabel,
  getTaskStatusLabel,
  getTaskStatusType
} from '@/constants'
import { useUserStore } from '@/stores/user'
import FileUpload from '@/components/common/FileUpload.vue'
import OrderDetailDrawer from '@/views/order/OrderDetailDrawer.vue'

const userStore = useUserStore()
const isAM = computed(() => userStore.userInfo?.role === 'AM')
const isBL = computed(() => userStore.userInfo?.role === 'BL')
const isDS = computed(() => userStore.userInfo?.role === 'DS')

// 页面标题
const pageTitle = computed(() => {
  if (isBL.value) return '订单分派'
  return '设计师工作台'
})

// ---------- 常量 ----------
const headerCellStyle = {
  background: 'var(--gray-50)',
  color: 'var(--gray-600)',
  fontWeight: 500
}

// ---------- Tab ----------
const activeTab = ref('tasks')

// 是否有任务筛选条件
const hasTaskFilters = computed(() => {
  return !!(filters.status || filters.materialType)
})

// ---------- 统计 ----------
const stats = reactive({
  // DS: 待确定  BL: 待分派
  assigned: 0,
  pending: 0,
  designing: 0,
  waitApprove: 0,
  approved: 0
})

// 第一个统计卡片的状态值和标签（DS=待确定/ASSIGNED，BL=待分派/CREATED）
const firstStatStatus = computed(() => isBL.value ? 'CREATED' : 'ASSIGNED')
const firstStatLabel = computed(() => isBL.value ? '待分派' : '待确定')
const firstStatValue = computed(() => isBL.value ? stats.pending : stats.assigned)

async function fetchStats() {
  try {
    // 顶部卡片统一取后端统计接口，口径与下方任务列表完全一致
    const res = await getTaskStats()
    stats.pending = res.created || 0
    // 已驳回任务统一计入「待确定」总数
    stats.assigned = (res.assigned || 0) + (res.rejected || 0)
    stats.designing = res.designing || 0
    stats.waitApprove = res.waitApprove || 0
    stats.approved = res.approved || 0
  } catch (e) {
    stats.pending = 0
    stats.assigned = 0
    stats.designing = 0
    stats.waitApprove = 0
    stats.approved = 0
  }
}

// ---------- 筛选 ----------
const filters = reactive({
  status: '',
  materialType: ''
})

// ---------- 任务列表 & 前端本地分页 ----------
const loading = ref(false)
const taskList = ref([])                                    // 全量任务（后端一次性拉完）

// 统一的前端本地分页 Hook（每页固定 10 条）
const {
  pagedData: pagedTasks,
  currentPage: pageNum,
  total: taskTotal,
  pageSize: taskPageSize,
  resetPage: resetTaskPage,
  handlePageChange
} = useClientPagination(taskList)

// 从订单列表扁平化为任务（与顶部统计接口保持同一口径）
function flattenOrders(orders) {
  const tasks = []
  const currentUserId = userStore.userInfo?.id
  orders.forEach(order => {
    // 作废订单不参与工作台展示与统计
    if (order.status === 'CANCELLED') return
    if (order.tasks && order.tasks.length > 0) {
      order.tasks.forEach(task => {
        // DS：只展示任务归属为当前登录设计师的任务
        if (isDS.value && task.designerId !== currentUserId) return
        tasks.push({
          ...task,
          orderId: order.id,
          orderNo: order.orderNo,
          customerName: order.customerName
        })
      })
    } else if (!isDS.value) {
      // 订单级兜底（没有任务列表时，把订单本身作为一条任务）
      tasks.push({
        id: order.id,
        taskNo: order.orderNo,
        orderId: order.id,
        orderNo: order.orderNo,
        customerName: order.customerName,
        materialType: order.materialType,
        quantity: order.quantity,
        specification: order.specification,
        status: order.status,
        deadline: order.deliveryDate
      })
    }
  })
  // 给任务行追加 displayTaskNo（公司名_NNN 格式）
  applyDisplayTaskNo(tasks)
  return tasks
}

// 前端筛选（状态、物料类型均为「任务级」字段；已驳回归入待确定）
function applyTaskFilters(tasks) {
  return tasks.filter(task => {
    if (filters.status) {
      const statusMatched = filters.status === 'ASSIGNED'
        ? (task.status === 'ASSIGNED' || task.status === 'REJECTED')
        : task.status === filters.status
      if (!statusMatched) return false
    }
    if (filters.materialType && task.materialType !== filters.materialType) return false
    return true
  })
}

/**
 * 「全部状态」下的展示排序（仅业务负责人 / BL）
 * 优先级：待分派(CREATED) > 设计中(DESIGNING) > 待审核(WAIT_APPROVE) > 已完成(APPROVED/DELIVERED)
 * 其它状态（待确定/已驳回等）排在最后；同优先级保持原有顺序（稳定排序）
 * 选中具体状态时不做排序，保持接口原有顺序
 */
const BL_ALL_STATUS_PRIORITY = {
  CREATED: 0,
  DESIGNING: 1,
  WAIT_APPROVE: 2,
  APPROVED: 3,
  DELIVERED: 3
}

function applyAllStatusPriority(tasks) {
  if (!isBL.value || filters.status) return tasks
  return tasks
    .map((task, index) => ({ task, index }))
    .sort((a, b) => {
      const ra = BL_ALL_STATUS_PRIORITY[a.task.status] ?? 4
      const rb = BL_ALL_STATUS_PRIORITY[b.task.status] ?? 4
      return ra === rb ? a.index - b.index : ra - rb
    })
    .map(item => item.task)
}

/**
 * 拉取全量任务（沿用原有 GET /orders 游标接口，不改后端）
 * 内部循环翻完所有游标页后一次性汇总，再交给前端本地分页（每页 10 条）
 */
async function fetchTaskList() {
  loading.value = true

  try {
    const allTasks = []      // 筛选后的任务（列表展示）
    let cursor = null
    let hasMore = true

    while (hasMore) {
      const params = {
        limit: 100
      }

      // 后端 applyDataScopeFilter 已按角色自动过滤：
      //   DS → 只含其任务 designer_id = 当前用户 的订单集合（任务级过滤，正确）
      //   BL → 全部订单
      //   AM → 仅自己创建的订单
      // 不再手动传 designerId，避免后端 applyOrderFilters 用订单级 orders.designer_id（任务级分派下为 NULL）做二次过滤导致 DS 查不到数据

      if (cursor) params.cursor = cursor

      // 注意：status / materialType 是「任务级」筛选，必须在前端按 task 字段过滤。
      // 若下发给后端，会被 applyOrderFilters 当成「订单级」过滤，
      // 导致订单状态未同步时漏掉仍有待分派任务的订单（出现"任务已分派却显示待分派/无法分派"）。
      // 前端过滤见 applyTaskFilters。

      const res = await listOrders(params)
      const batch = flattenOrders(res.list || [])
      allTasks.push(...applyTaskFilters(batch))

      cursor = res.nextCursor || null
      hasMore = !!res.hasMore && !!cursor
    }

    taskList.value = applyAllStatusPriority(allTasks)
    resetTaskPage()   // 数据刷新后回到第一页
  } catch (e) {
    taskList.value = []
    resetTaskPage()
  } finally {
    loading.value = false
  }
}

// ---------- 搜索 / 重置 ----------
function handleSearch() {
  fetchTaskList()
}

function handleReset() {
  filters.status = ''
  filters.materialType = ''
  fetchTaskList()
}

// 点击统计卡片快速筛选
function quickFilter(status) {
  activeTab.value = 'tasks'
  // BL 的第一个卡片是待分派(CREATED)，DS 是待确定(ASSIGNED)
  filters.status = status
  fetchTaskList()
}

// ---------- 操作：待确定 ----------
async function handleAccept(row) {
  try {
    await ElMessageBox.confirm(
      `确定将该订单【${row.orderNo || '—'}】标记为接单状态吗？`,
      '确认接单',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'primary'
      }
    )
  } catch (e) {
    return
  }

  const orderId = row.orderId || row.id
  try {
    // 行数据是订单级兜底（无 task.version），先拉订单详情，找分派给我的待确定任务
    const detail = await getOrderDetail(orderId)
    const myTasks = (detail.tasks || []).filter(
      t => t.status === 'ASSIGNED' && t.designerId === userStore.userInfo?.id
    )
    if (myTasks.length === 0) {
      ElMessage.warning('该订单下没有分派给我的待确定任务')
      return
    }
    for (const t of myTasks) {
      if (t.version == null) {
        ElMessage.error(`任务 ${t.displayTaskNo || t.taskNo || t.id} 缺少乐观锁 version，无法标记为待确定，请刷新重试`)
        return
      }
      await acceptTask(orderId, t.id, t.version)
    }
    ElMessage.success('已标记为待确定')
    // 刷新列表和统计
    fetchTaskList()
    fetchStats()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

// ---------- 操作：重新开始设计 ----------
async function handleRestart(row) {
  const orderId = row.orderId || row.id
  if (!row.id || row.version == null) {
    ElMessage.error('任务数据缺少版本号，请刷新后重试')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定将任务 ${row.displayTaskNo || row.taskNo || row.id} 重新开始设计吗？`,
      '重新开始设计',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (e) {
    return
  }
  try {
    await restartDesign(orderId, row.id, row.version)
    ElMessage.success('已重新开始设计')
    fetchTaskList()
    fetchStats()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

// ---------- 操作：上传版本 ----------
const uploadDialog = reactive({
  visible: false,
  loading: false,
  row: null
})
const uploadForm = reactive({
  description: '',
  files: []
})

function handleUploadVersion(row) {
  uploadDialog.row = row
  uploadForm.description = ''
  uploadForm.files = []
  uploadDialog.visible = true
  uploadDialog.loading = false
}

async function handleSubmitVersion() {
  if (uploadForm.files.length === 0) {
    ElMessage.warning('请上传设计文件')
    return
  }
  if (!uploadForm.description || !uploadForm.description.trim()) {
    ElMessage.warning('请输入版本说明')
    return
  }

  const orderId = uploadDialog.row?.orderId || uploadDialog.row?.id
  uploadDialog.loading = true
  try {
    // 行数据是订单级兜底（无 task.version），先拉订单详情，找我名下"设计中"的任务
    const detail = await getOrderDetail(orderId)
    const myTasks = (detail.tasks || []).filter(
      t => t.status === 'DESIGNING' && t.designerId === userStore.userInfo?.id
    )
    if (myTasks.length === 0) {
      ElMessage.warning('该订单下没有进行中（设计中）的任务')
      return
    }
    const task = myTasks[0]
    if (task.version == null) {
      ElMessage.error('任务缺少乐观锁 version，无法上传版本，请刷新重试')
      return
    }

    // 后端为单文件接口（POST /design-versions），逐个文件提交，fileType 取扩展名
    const ALLOWED = ['psd', 'ai', 'jpg', 'png', 'pdf', 'webp']
    for (const f of uploadForm.files) {
      const fileName = f.fileName || f.name
      const ext = (String(fileName).split('.').pop() || '').toLowerCase()
      if (!ALLOWED.includes(ext)) {
        ElMessage.error(`文件 ${fileName} 格式不支持（仅限 psd/ai/jpg/png/pdf/webp）`)
        return
      }
      await uploadDesignVersion({
        orderId,
        taskId: task.id,
        fileId: f.fileId,
        fileName,
        fileSize: f.fileSize || f.size,
        fileType: ext,
        versionNote: uploadForm.description.trim(),
        version: task.version
      })
    }
    ElMessage.success('版本提交成功')
    uploadDialog.visible = false
    // 刷新列表
    fetchTaskList()
    fetchStats()
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    uploadDialog.loading = false
  }
}

// ---------- 操作：查看详情 ----------
// 工作台行是订单级兜底数据（row.id 即订单 id），详情复用订单详情抽屉
const detailDrawerVisible = ref(false)
const detailOrderId = ref(null)

function handleViewDetail(row) {
  detailOrderId.value = row.orderId || row.id
  detailDrawerVisible.value = true
}

// ---------- 操作：分派任务（BL） ----------
const assignDialog = reactive({
  visible: false,
  loading: false,
  row: null,
  tasks: [],          // 打开弹窗时从订单详情缓存的完整任务列表（含乐观锁 version）
  task: null,         // 当前行对应的那一条任务（任务级分派只提交这一条）
  orderVersion: null  // 订单本体乐观锁（无任务订单走订单级分派时使用）
})
const assignFormRef = ref(null)
const assignForm = reactive({
  designerId: null,
  remark: ''
})
const assignRules = {
  designerId: [
    { required: true, message: '请选择设计师', trigger: 'change' }
  ]
}
const designerList = ref([])
const designerLoading = ref(false)

async function fetchDesigners() {
  designerLoading.value = true
  try {
    const res = await listDesigners()
    designerList.value = res.list || res || []
  } catch (e) {
    designerList.value = []
  } finally {
    designerLoading.value = false
  }
}

async function handleAssign(row) {
  assignDialog.row = row
  assignForm.designerId = null
  assignForm.remark = ''
  assignDialog.tasks = []
  assignDialog.task = null
  assignDialog.orderVersion = null
  assignDialog.loading = true
  try {
    const orderId = row.orderId || row.id
    // 打开弹窗前拉订单详情，取该任务的最新状态与乐观锁 version
    const detail = await getOrderDetail(orderId)
    if (detail?.version == null) {
      ElMessage.error('订单详情未返回乐观锁 version，无法分派，请检查后端 OrderDetailVO')
      return
    }
    assignDialog.orderVersion = detail.version
    const detailTasks = detail.tasks || []
    assignDialog.tasks = detailTasks

    // 只锁定「当前这一行」的任务，避免把订单内已分派的任务一起提交，
    // 也避免因其他任务已分派而被整体拦截
    const matched = detailTasks.find(t => String(t.id) === String(row.id))
    assignDialog.task =
      matched && (!row.taskNo || matched.taskNo === row.taskNo) ? matched : null
  } catch (e) {
    ElMessage.error('加载订单详情失败，请重试')
    return
  } finally {
    assignDialog.loading = false
  }
  assignDialog.visible = true
  if (designerList.value.length === 0) {
    fetchDesigners()
  }
}

// 任务流程已结束的状态（已定稿 / 已驳回 / 已交付 / 已作废）
const TASK_CLOSED_STATUS = ['APPROVED', 'REJECTED', 'DELIVERED', 'CANCELLED']
// 已分派/进行中的状态（已存在设计师负责人，不可再次分派）
const TASK_HANDLED_STATUS = ['ASSIGNED', 'DESIGNING', 'WAIT_APPROVE']

/**
 * 分派前置校验（只针对当前行这一条任务）：
 * ① 已分派 ② 流程已结束 ③ 任务数据异常（不存在 / 不属于当前订单）
 * 命中返回提示文案，全部通过返回 null
 */
function getAssignBlockMessage(task, orderId, row) {
  if (!row || !orderId) return '任务数据异常，无法分派'

  // 任务级：校验当前这一条任务
  if (task) {
    if (task.orderId != null && String(task.orderId) !== String(orderId)) {
      return '任务数据异常，无法分派'
    }
    if (task.designerId != null || TASK_HANDLED_STATUS.includes(task.status)) {
      return '当前任务状态为【已分派】，按已有状态不可分派'
    }
    if (TASK_CLOSED_STATUS.includes(task.status)) {
      return '该任务流程已结束，不可重新分派'
    }
    if (task.status !== 'CREATED') {
      return '当前任务状态不可分派'
    }
    return null
  }

  // 订单级兜底：订单下没有任务记录，按订单状态判断
  if (row.designerId != null || row.status !== 'CREATED') {
    return '当前任务状态为【已分派】，按已有状态不可分派'
  }
  return null
}

async function handleAssignSubmit() {
  if (!assignFormRef.value) return
  assignFormRef.value.validate(async valid => {
    if (!valid) return

    const row = assignDialog.row
    const task = assignDialog.task
    const tasks = assignDialog.tasks || []
    const orderId = row?.orderId || row?.id

    // 分派前置校验：命中即提示并阻止提交（后端同规则二次校验）
    const blockMessage = getAssignBlockMessage(task, orderId, row)
    if (blockMessage) {
      ElMessage.warning(blockMessage)
      return
    }

    if (task && task.version == null) {
      ElMessage.error(`任务 ${task.displayTaskNo || task.taskNo || task.id} 缺少乐观锁 version，请刷新后重试`)
      return
    }
    // 当前行有任务但详情里查不到该任务 → 数据异常
    if (!task && tasks.length > 0) {
      ElMessage.warning('任务数据异常，无法分派')
      return
    }
    if (!task && assignDialog.orderVersion == null) {
      ElMessage.error('订单缺少乐观锁 version，请刷新后重试')
      return
    }

    assignDialog.loading = true
    try {
      if (task) {
        // 任务级分派：只提交当前行这一条任务，带任务自身乐观锁 version
        await assignTask(orderId, task.id, {
          designerId: assignForm.designerId,
          assignRemark: assignForm.remark,
          version: task.version
        })
      } else {
        // 无任务订单：走订单级分派接口，携带订单乐观锁 version
        await assignOrder(orderId, {
          designerId: assignForm.designerId,
          assignRemark: assignForm.remark,
          version: assignDialog.orderVersion
        })
      }
      ElMessage.success('分派成功')
      assignDialog.visible = false
      fetchTaskList()
      fetchStats()
    } catch (e) {
      // 错误已由拦截器处理
    } finally {
      assignDialog.loading = false
    }
  })
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

// ---------- 生命周期 ----------
watch(activeTab, (val) => {
  // BL：延期预警
  if (isBL.value && val === 'delay') {
    // 后续可添加延期预警数据加载逻辑
  }
})

onMounted(() => {
  fetchStats()
  fetchTaskList()
})
</script>

<style scoped>
.design-workbench {
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
  gap: 14px;
  padding: 18px 20px;
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
  width: 44px;
  height: 44px;
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
  font-size: 26px;
  font-weight: 600;
  color: var(--gray-800);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--gray-500);
}

.stat-assigned .stat-icon-wrap {
  background: var(--info-light);
  color: var(--info-color);
}
.stat-assigned .stat-value {
  color: var(--info-color);
}

.stat-designing .stat-icon-wrap {
  background: var(--warning-light);
  color: #d97706;
}
.stat-designing .stat-value {
  color: #d97706;
}

.stat-waiting .stat-icon-wrap {
  background: #fef3c7;
  color: #b45309;
}
.stat-waiting .stat-value {
  color: #b45309;
}

.stat-approved .stat-icon-wrap {
  background: var(--success-light);
  color: #059669;
}
.stat-approved .stat-value {
  color: #059669;
}

/* ========== Tab 卡片 ========== */
.tab-card {
  border-radius: var(--border-radius);
}

.tab-card :deep(.el-card__body) {
  padding: 0;
}

.workbench-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  border-bottom: 1px solid var(--gray-100);
}

.workbench-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.workbench-tabs :deep(.el-tab-pane) {
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

/* ========== 任务表格 ========== */
.task-table {
  margin-bottom: 0;
}

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
  margin-top: 0;
}

.no-more {
  font-size: 13px;
  color: var(--gray-400);
}

.delay-warning {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ========== 上传版本弹窗 ========== */
.upload-dialog-body {
  padding: 8px 0;
}

.dialog-info {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--gray-50);
  border-radius: var(--border-radius);
}

.dialog-info p {
  margin: 4px 0;
  font-size: 13px;
  color: var(--gray-700);
}

.dialog-info .label {
  color: var(--gray-500);
}

.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
}
</style>
