<template>
  <!--
    设计师工作台
    功能：统计卡片 + Tab切换（我的任务/审核记录）+ 筛选 + 任务表格 + 游标分页
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
                  <el-option label="待接单" value="ASSIGNED" />
                  <el-option label="设计中" value="DESIGNING" />
                  <el-option label="待审核" value="WAIT_APPROVE" />
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
                <!-- DS：待接单显示接单，设计中显示上传版本，所有状态均可查看详情 -->
                <template v-else>
                  <!-- 待接单：显示接单按钮 -->
                  <el-button
                    v-if="row.status === 'ASSIGNED'"
                    type="primary"
                    size="small"
                    @click="handleAccept(row)"
                  >
                    接单
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

        <!-- 第二个 Tab（DS=审核记录，BL=延期预警） -->
        <el-tab-pane :label="isBL ? '延期预警' : '我参与的审核记录'" :name="isBL ? 'delay' : 'audit'">
          <!-- BL：延期预警 -->
          <div v-if="isBL" class="delay-warning">
            <el-empty description="延期订单会在这里展示" :image-size="120" />
          </div>
          <!-- DS：审核记录 -->
          <div v-else v-loading="auditLoading" class="audit-log-list">
            <div
              v-for="(item, index) in auditLogs"
              :key="item.id || index"
              class="audit-log-item"
            >
              <div class="log-time">{{ formatDateTime(item.operatedAt || item.createdAt) }}</div>
              <div class="log-content">
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
                  备注：{{ item.remark || item.note }}
                </div>
                <div v-if="item.operatorName" class="log-operator">
                  操作人：{{ item.operatorName }}
                </div>
              </div>
            </div>
            <el-empty
              v-if="!auditLoading && auditLogs.length === 0"
              description="暂无审核记录"
              :image-size="80"
            />
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
          <p><span class="label">任务编号：</span><span class="mono-text">{{ uploadDialog.row?.taskNo }}</span></p>
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
          <span class="mono-text">{{ assignDialog.row?.taskNo || assignDialog.row?.id || '—' }}</span>
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
import { listOrders, acceptTask, uploadDesignVersion, assignTask, assignOrder, getOrderDetail, listDesigners } from '@/api'
import { listMyAuditLogs } from '@/api/audit'
import { useClientPagination } from '@/composables/useClientPagination'
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
  if (isBL.value) return '任务管理'
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
  // DS: 待接单  BL: 待分派
  assigned: 0,
  pending: 0,
  designing: 0,
  waitApprove: 0,
  approved: 0
})

// 第一个统计卡片的状态值和标签（DS=待接单/ASSIGNED，BL=待分派/CREATED）
const firstStatStatus = computed(() => isBL.value ? 'CREATED' : 'ASSIGNED')
const firstStatLabel = computed(() => isBL.value ? '待分派' : '待接单')
const firstStatValue = computed(() => isBL.value ? stats.pending : stats.assigned)

async function fetchStats() {
  try {
    // 通过一次列表查询统计各状态数量（简化实现）
    const designerId = userStore.userInfo?.id

    if (isBL.value) {
      // BL：统计全部订单的各状态数量
      const [pendingRes, designingRes, waitRes, approvedRes] = await Promise.all([
        listOrders({ status: 'CREATED', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ status: 'DESIGNING', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ status: 'WAIT_APPROVE', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ status: 'APPROVED', limit: 1 }).catch(() => ({ total: 0 }))
      ])
      stats.pending = pendingRes.total || 0
      stats.designing = designingRes.total || 0
      stats.waitApprove = waitRes.total || 0
      stats.approved = approvedRes.total || 0
    } else {
      // DS：统计我的任务各状态数量
      if (!designerId) return
      const [assignedRes, designingRes, waitRes, approvedRes] = await Promise.all([
        listOrders({ designerId, status: 'ASSIGNED', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ designerId, status: 'DESIGNING', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ designerId, status: 'WAIT_APPROVE', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ designerId, status: 'APPROVED', limit: 1 }).catch(() => ({ total: 0 }))
      ])
      stats.assigned = assignedRes.total || 0
      stats.designing = designingRes.total || 0
      stats.waitApprove = waitRes.total || 0
      stats.approved = approvedRes.total || 0
    }
  } catch (e) {
    // 接口异常时使用 mock 数据
    if (isBL.value) {
      stats.pending = 5
    } else {
      stats.assigned = 3
    }
    stats.designing = isBL.value ? 8 : 5
    stats.waitApprove = isBL.value ? 4 : 2
    stats.approved = isBL.value ? 56 : 28
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

// 从订单列表中提取任务（扁平化）
function extractTasksFromOrders(orders) {
  const tasks = []
  orders.forEach(order => {
    if (order.tasks && order.tasks.length > 0) {
      order.tasks.forEach(task => {
        tasks.push({
          ...task,
          orderId: order.id,
          orderNo: order.orderNo,
          customerName: order.customerName
        })
      })
    } else {
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

  // 应用前端筛选（状态、物料类型）
  return tasks.filter(task => {
    if (filters.status && task.status !== filters.status) return false
    if (filters.materialType && task.materialType !== filters.materialType) return false
    return true
  })
}

/**
 * 拉取全量任务（沿用原有 GET /orders 游标接口，不改后端）
 * 内部循环翻完所有游标页后一次性汇总，再交给前端本地分页（每页 10 条）
 */
async function fetchTaskList() {
  loading.value = true

  try {
    const allTasks = []
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

      // 状态筛选传给后端（订单级状态筛选）
      if (filters.status) params.status = filters.status
      if (filters.materialType) params.materialType = filters.materialType

      const res = await listOrders(params)
      allTasks.push(...extractTasksFromOrders(res.list || []))

      cursor = res.nextCursor || null
      hasMore = !!res.hasMore && !!cursor
    }

    taskList.value = allTasks
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
  // BL 的第一个卡片是待分派(CREATED)，DS 是待接单(ASSIGNED)
  filters.status = status
  fetchTaskList()
}

// ---------- 操作：接单 ----------
async function handleAccept(row) {
  try {
    await ElMessageBox.confirm(
      `确定接受任务 ${row.taskNo || row.id} 吗？`,
      '接单确认',
      {
        confirmButtonText: '确认接单',
        cancelButtonText: '取消',
        type: 'primary'
      }
    )
  } catch (e) {
    return
  }

  const orderId = row.orderId || row.id
  try {
    // 行数据是订单级兜底（无 task.version），先拉订单详情，找分派给我的待接单任务
    const detail = await getOrderDetail(orderId)
    const myTasks = (detail.tasks || []).filter(
      t => t.status === 'ASSIGNED' && t.designerId === userStore.userInfo?.id
    )
    if (myTasks.length === 0) {
      ElMessage.warning('该订单下没有分派给我的待接单任务')
      return
    }
    for (const t of myTasks) {
      if (t.version == null) {
        ElMessage.error(`任务 ${t.taskNo || t.id} 缺少乐观锁 version，无法接单，请刷新重试`)
        return
      }
      await acceptTask(orderId, t.id, t.version)
    }
    ElMessage.success('接单成功')
    // 刷新列表和统计
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
  assignDialog.orderVersion = null
  assignDialog.loading = true
  try {
    // 打开弹窗前拉订单详情，缓存完整任务列表（含乐观锁 version）
    const detail = await getOrderDetail(row.orderId || row.id)
    if (detail?.version == null) {
      ElMessage.error('订单详情未返回乐观锁 version，无法分派，请检查后端 OrderDetailVO')
      return
    }
    assignDialog.orderVersion = detail.version
    assignDialog.tasks = detail.tasks || []
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

/**
 * 分派提交前置校验：依次判断，命中返回提示文案（不命中返回 null）
 * ① 已分派（已有设计师负责人） ② 流程已结束 ③ 任务数据异常（不存在/不属于当前订单）
 */
function getAssignBlockMessage(tasks, orderId, row) {
  // ③ 数据异常：行数据或订单ID缺失
  if (!row || !orderId) return '任务数据异常，无法分派'

  // ① 已分派：任务已有设计师负责人
  if (tasks.some(t => t.designerId != null)) {
    return '当前任务状态为【已分派】，按已有状态不可分派'
  }

  // ② 流程已结束：已定稿 / 已驳回等闭环状态
  if (tasks.some(t => TASK_CLOSED_STATUS.includes(t.status))) {
    return '该任务流程已结束，不可重新分派'
  }

  // ③ 数据异常：任务不属于当前订单
  if (tasks.some(t => t.orderId != null && String(t.orderId) !== String(orderId))) {
    return '任务数据异常，无法分派'
  }

  return null
}

async function handleAssignSubmit() {
  if (!assignFormRef.value) return
  assignFormRef.value.validate(async valid => {
    if (!valid) return

    const row = assignDialog.row
    const tasks = assignDialog.tasks || []
    const orderId = row?.orderId || row?.id

    // 分派前置校验：命中即弹窗提示并阻止提交（后端同规则二次校验）
    const blockMessage = getAssignBlockMessage(tasks, orderId, row)
    if (blockMessage) {
      ElMessage.warning(blockMessage)
      return
    }

    // 提交前校验乐观锁 version 已随弹窗缓存
    if (tasks.length > 0) {
      const missing = tasks.find(t => t.version == null)
      if (missing) {
        ElMessage.error(`任务 ${missing.taskNo || missing.id} 缺少乐观锁 version，请刷新后重试`)
        return
      }
    } else if (assignDialog.orderVersion == null) {
      ElMessage.error('订单缺少乐观锁 version，请刷新后重试')
      return
    }

    assignDialog.loading = true
    try {
      if (tasks.length > 0) {
        // 任务级分派：逐个分派，每个任务带自己的乐观锁 version
        await Promise.all(tasks.map(t =>
          assignTask(orderId, t.id, {
            designerId: assignForm.designerId,
            assignRemark: assignForm.remark,
            version: t.version
          })
        ))
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

// ---------- 审核记录 ----------
const auditLoading = ref(false)
const auditLogs = ref([])

async function fetchAuditLogs() {
  auditLoading.value = true
  try {
    const res = await listMyAuditLogs(50)
    auditLogs.value = res.list || res || []
  } catch (e) {
    // mock 数据
    auditLogs.value = [
      { id: 1, orderNo: 'ORD-2026-00015', action: 'APPROVE', remark: '设计符合要求，确认定稿', operatorName: '张经理', operatedAt: '2026-11-03 14:20:00' },
      { id: 2, orderNo: 'ORD-2026-00012', action: 'REJECT', remark: '颜色需要调整，参考稿为准', operatorName: '李主管', operatedAt: '2026-11-02 16:40:00' },
      { id: 3, orderNo: 'ORD-2026-00008', action: 'APPROVE', remark: '定稿通过', operatorName: '张经理', operatedAt: '2026-10-30 10:15:00' }
    ]
  } finally {
    auditLoading.value = false
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
  // DS：我参与的审核记录
  if (!isBL.value && val === 'audit' && auditLogs.value.length === 0) {
    fetchAuditLogs()
  }
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

/* ========== 审核记录列表 ========== */
.audit-log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.delay-warning {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.audit-log-item {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid var(--gray-100);
  border-radius: var(--border-radius);
  background: #ffffff;
  transition: var(--transition-base);
}

.audit-log-item:hover {
  border-color: var(--gray-200);
  box-shadow: var(--shadow-sm);
}

.log-time {
  flex-shrink: 0;
  width: 140px;
  font-size: 13px;
  color: var(--gray-500);
  padding-top: 2px;
}

.log-content {
  flex: 1;
  min-width: 0;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
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
  margin-bottom: 4px;
}

.log-operator {
  font-size: 12px;
  color: var(--gray-400);
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
