<template>
  <!--
    工作台 Dashboard — 按角色适配 + 真实数据
    AM/BL：待分派、设计中、待定稿、已归档 + 最近订单
    DS：待确定、设计中、待定稿、已完成 + 最近任务
  -->
  <div class="dashboard">
    <!-- 欢迎语 -->
    <div class="welcome-bar">
      <div>
        <h2 class="welcome-title">{{ greeting }}，{{ userStore.userInfo?.name || '用户' }}</h2>
        <p class="welcome-sub">今天也要加油哦 · {{ todayDate }}</p>
      </div>
    </div>

    <!-- 统计卡片组（4张）：客户经理首页不展示统计卡片 -->
    <el-row v-if="!isAM" :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="(card, idx) in statCards" :key="idx">
        <el-card
          class="stat-card"
          :class="['stat-' + card.key, { 'stat-active': statFilterStatus === card.status }]"
          shadow="never"
          v-loading="statsLoading"
          @click="handleStatClick(card)"
        >
          <div class="stat-icon-wrap">
            <el-icon :size="24"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats[card.key] || 0 }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 + 最近列表 -->
    <el-row :gutter="16">
      <!-- 快捷入口 -->
      <el-col :xs="24" :md="8">
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="section-title">快捷操作</span>
          </template>
          <div class="quick-actions">
            <div
              v-for="(item, idx) in quickActions"
              :key="idx"
              class="quick-item"
              @click="handleQuickAction(item)"
            >
              <div class="quick-icon-wrap" :style="{ background: item.bgColor, color: item.color }">
                <el-icon :size="20"><component :is="item.icon" /></el-icon>
              </div>
              <span class="quick-label">{{ item.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 最近订单 / 最近任务 -->
      <el-col :xs="24" :md="16">
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="header-flex">
              <span class="section-title">{{ isDS ? '最近任务' : '最近订单' }}</span>
              <div class="header-right">
                <!-- 统计卡片联动筛选提示 -->
                <el-tag
                  v-if="statFilterStatus"
                  type="primary"
                  effect="light"
                  closable
                  class="filter-tag"
                  @close="clearStatFilter"
                >
                  筛选：{{ statFilterLabel }}
                </el-tag>
                <el-button type="primary" link @click="handleViewAll">
                  查看全部
                </el-button>
              </div>
            </div>
          </template>
          <el-table
            :data="recentList"
            v-loading="recentLoading"
            style="width: 100%"
            :show-header="true"
            size="default"
          >
            <!-- AM/BL：订单列表 -->
            <template v-if="!isDS">
              <el-table-column prop="orderNo" label="订单号" width="160">
                <template #default="{ row }">
                  <span class="mono-text link-text" @click="viewOrderDetail(row)">{{ row.orderNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="customerName" label="公司名称" min-width="140" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="getOrderStatusType(row.status)"
                    effect="light"
                    round
                    size="small"
                  >
                    {{ getOrderStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.createdAt) }}
                </template>
              </el-table-column>
            </template>
            <!-- DS：任务列表 -->
            <template v-else>
              <el-table-column prop="taskNo" label="任务编号" width="140">
                <template #default="{ row }">
                  <span class="mono-text">{{ row.taskNo || row.id || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="订单号" width="150">
                <template #default="{ row }">
                  <span class="mono-text order-no-text">{{ row.orderNo || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="物料类型" width="100">
                <template #default="{ row }">
                  {{ getMaterialLabel(row.materialType) }}
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
            </template>
          </el-table>
          <el-empty
            v-if="!recentLoading && recentList.length === 0"
            :description="isDS ? '暂无任务' : '暂无订单'"
            :image-size="80"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 我参与的审核记录（仅 AM/BL 显示） -->
    <el-card v-if="!isDS" class="section-card audit-card" shadow="never">
      <template #header>
        <div class="header-flex">
          <span class="section-title">我参与的审核记录</span>
          <el-button type="primary" link @click="goToAuditLogs">
            查看全部
          </el-button>
        </div>
      </template>
      <div v-loading="auditLoading" class="audit-timeline-wrap">
        <el-timeline v-if="auditLogs.length > 0" class="audit-timeline">
          <el-timeline-item
            v-for="(item, index) in auditLogs"
            :key="item.id || index"
            :timestamp="formatDateTime(item.operatedAt || item.createdAt)"
            :type="logActionType(item.action)"
            placement="top"
          >
            <div class="audit-log-item">
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
          v-if="!auditLoading && auditLogs.length === 0"
          description="暂无审核记录"
          :image-size="80"
        />
      </div>
    </el-card>

    <!-- 新建订单弹窗（客户经理首页快捷操作直接打开） -->
    <OrderCreateDialog v-model="createDialogVisible" @success="handleCreateSuccess" />

    <!-- 订单详情抽屉 -->
    <OrderDetailDrawer
      v-model="detailDrawerVisible"
      :order-id="currentViewOrderId"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Clock,
  EditPen,
  CircleCheckFilled,
  FolderChecked,
  Document,
  Plus,
  CircleCheck,
  FolderOpened,
  Brush,
  List
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { listOrders, listPendingTasks, countArchived, getTaskStats, listMyAuditLogs } from '@/api'
import {
  ORDER_STATUS_MAP,
  TASK_STATUS_MAP,
  MATERIAL_OPTIONS,
  getOrderStatusLabel,
  getOrderStatusType,
  getTaskStatusLabel,
  getTaskStatusType,
  getMaterialLabel
} from '@/constants'
import OrderDetailDrawer from './order/OrderDetailDrawer.vue'
import OrderCreateDialog from './order/OrderCreateDialog.vue'

const router = useRouter()
const userStore = useUserStore()

// ---------- 角色判断 ----------
const isDS = computed(() => userStore.userInfo?.role === 'DS')
const isAM = computed(() => userStore.userInfo?.role === 'AM')
const isBL = computed(() => userStore.userInfo?.role === 'BL')

// ---------- 问候语 ----------
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayDate = computed(() => {
  const d = new Date()
  const weeks = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日 ' + weeks[d.getDay()]
})

// ---------- 统计卡片配置 ----------
const statCards = computed(() => {
  if (isDS.value) {
    // DS：待确定、设计中、待定稿、已完成
    return [
      { key: 'pendingAccept', label: '待确定', icon: 'Clock', status: 'ASSIGNED', route: '/design-workbench', color: 'info' },
      { key: 'designing', label: '设计中', icon: 'EditPen', status: 'DESIGNING', route: '/design-workbench', color: 'warning' },
      { key: 'waitApprove', label: '待审核', icon: 'CircleCheck', status: 'WAIT_APPROVE', route: '/design-workbench', color: 'warning' },
      { key: 'completed', label: '已完成', icon: 'CircleCheckFilled', status: 'APPROVED', route: '/design-workbench', color: 'success' }
    ]
  }
  // AM / BL：待分派、设计中、待定稿、已归档
  return [
    { key: 'pendingAssign', label: '待分派', icon: 'Clock', status: 'CREATED', route: '/orders', color: 'info' },
    { key: 'designing', label: '设计中', icon: 'EditPen', status: 'DESIGNING', route: '/orders', color: 'warning' },
    { key: 'waitApprove', label: '待审核', icon: 'CircleCheck', status: 'WAIT_APPROVE', route: isAM.value ? '/am-workbench' : '/final-audit', color: 'warning' },
    { key: 'archived', label: '已归档', icon: 'FolderChecked', status: 'ARCHIVED', route: '/archive', color: 'success' }
  ]
})

const stats = reactive({
  pendingAssign: 0,
  designing: 0,
  waitApprove: 0,
  archived: 0,
  pendingAccept: 0,
  completed: 0
})
const statsLoading = ref(false)

// ---------- 快捷操作配置 ----------
const quickActions = computed(() => {
  if (isDS.value) {
    return [
      { label: '设计师工作台', icon: 'Brush', route: '/design-workbench', bgColor: 'var(--primary-light)', color: 'var(--primary-color)' },
      { label: '我的任务', icon: 'List', route: '/design-workbench', bgColor: 'var(--info-light)', color: 'var(--info-color)' },
      { label: '归档查询', icon: 'FolderOpened', route: '/archive', bgColor: 'var(--success-light)', color: 'var(--success-color)' }
    ]
  }
  if (isAM.value) {
    return [
      { label: '新建订单', icon: 'Plus', action: 'createOrder', bgColor: 'var(--primary-light)', color: 'var(--primary-color)' },
      { label: '订单管理', icon: 'Document', route: '/orders', bgColor: 'var(--info-light)', color: 'var(--info-color)' },
      { label: '定稿审核', icon: 'CircleCheck', route: '/am-workbench', bgColor: 'var(--warning-light)', color: '#d97706' },
      { label: '归档查询', icon: 'FolderOpened', route: '/archive', bgColor: 'var(--success-light)', color: 'var(--success-color)' }
    ]
  }
  // BL
  return [
    { label: '订单管理', icon: 'Document', route: '/orders', bgColor: 'var(--info-light)', color: 'var(--info-color)' },
    { label: '定稿审核', icon: 'CircleCheck', route: '/final-audit', bgColor: 'var(--warning-light)', color: '#d97706' },
    { label: '归档查询', icon: 'FolderOpened', route: '/archive', bgColor: 'var(--success-light)', color: 'var(--success-color)' }
  ]
})

// ---------- 最近列表 ----------
const recentList = ref([])
const recentLoading = ref(false)

/** 统计卡片联动筛选状态 — null 表示不筛选，显示全部最近记录 */
const statFilterStatus = ref(null)
const statFilterLabel = ref('')

// ---------- 审核记录 ----------
const auditLogs = ref([])
const auditLoading = ref(false)

// ---------- 详情抽屉 ----------
const detailDrawerVisible = ref(false)
const currentViewOrderId = ref(null)

// ---------- 新建订单弹窗 ----------
const createDialogVisible = ref(false)

// 新建订单成功 → 刷新首页订单列表
function handleCreateSuccess() {
  fetchRecent(statFilterStatus.value)
}

// ---------- 统计数据加载 ----------
async function fetchStats() {
  statsLoading.value = true
  try {
    if (isDS.value) {
      // DS：与「设计师工作台-我的任务」共用同一后端统计接口，保证口径完全一致
      // （当前登录设计师、有效任务状态、排除逻辑删除与作废订单）
      const res = await getTaskStats()
      // 已驳回任务统一计入「待确定」总数
      stats.pendingAccept = (res.assigned || 0) + (res.rejected || 0)
      stats.designing = res.designing || 0
      stats.waitApprove = res.waitApprove || 0
      stats.completed = res.approved || 0
    } else {
      // AM/BL：待分派、设计中、待定稿、已归档
      const [createdRes, designingRes, waitRes] = await Promise.all([
        listOrders({ status: 'CREATED', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ status: 'DESIGNING', limit: 1 }).catch(() => ({ total: 0 })),
        listOrders({ status: 'WAIT_APPROVE', limit: 1 }).catch(() => ({ total: 0 }))
      ])
      stats.pendingAssign = createdRes.total || 0
      stats.designing = designingRes.total || 0
      stats.waitApprove = waitRes.total || 0

      // 已归档走专门的统计接口
      try {
        const archivedRes = await countArchived()
        stats.archived = archivedRes.count || archivedRes.total || 0
      } catch (e) {
        stats.archived = 0
      }
    }
  } catch (e) {
    // 接口异常，保持默认 0
  } finally {
    statsLoading.value = false
  }
}

// ---------- 最近列表加载 ----------
async function fetchRecent(filterStatus = null) {
  recentLoading.value = true
  try {
    // AM/BL 的"已归档"走专门的归档接口，不是 listOrders 的 status 过滤
    const isArchivedFilter = filterStatus === 'ARCHIVED'

    if (isDS.value) {
      // DS：后端 applyDataScopeFilter 已自动按任务级 designer_id 过滤
      const params = { limit: 10 }
      if (filterStatus && filterStatus !== 'ARCHIVED') params.status = filterStatus
      const res = await listOrders(params)
      const orders = res.list || []
      const tasks = []
      orders.forEach(order => {
        if (order.tasks && order.tasks.length > 0) {
          order.tasks.forEach(task => {
            tasks.push({
              ...task,
              orderId: order.id,
              orderNo: order.orderNo
            })
          })
        }
      })
      recentList.value = tasks.slice(0, 5)
    } else if (isArchivedFilter) {
      // AM/BL 已归档：归档接口返回全部订单（含未归档），这里只保留已归档订单
      const archived = []
      let cursor = null
      let hasMore = true
      while (hasMore && archived.length < 5) {
        const params = { limit: 20 }
        if (cursor) params.cursor = cursor
        const res = await listArchivedOrders(params)
        (res.list || []).forEach(item => {
          if (item.archiveStatus === '已归档') archived.push(item)
        })
        cursor = res.nextCursor || null
        hasMore = !!res.hasMore && !!cursor
      }
      recentList.value = archived.slice(0, 5)
    } else {
      // AM/BL 最近5条订单（带可选状态筛选）
      const params = { limit: 5 }
      if (filterStatus) params.status = filterStatus
      const res = await listOrders(params)
      recentList.value = res.list || []
    }
  } catch (e) {
    recentList.value = []
  } finally {
    recentLoading.value = false
  }
}

// ---------- 审核记录加载 ----------
async function fetchAuditLogs() {
  auditLoading.value = true
  try {
    const res = await listMyAuditLogs(5)
    auditLogs.value = res.list || res || []
  } catch (e) {
    auditLogs.value = []
  } finally {
    auditLoading.value = false
  }
}

// ---------- 操作：统计卡片点击 → 联动筛选最近列表 ----------
function handleStatClick(card) {
  // 再次点击同一卡片 = 取消筛选
  if (statFilterStatus.value === card.status) {
    statFilterStatus.value = null
    statFilterLabel.value = ''
  } else {
    statFilterStatus.value = card.status
    statFilterLabel.value = card.label
  }
  fetchRecent(statFilterStatus.value)
}

/** 清除统计卡片筛选 */
function clearStatFilter() {
  statFilterStatus.value = null
  statFilterLabel.value = ''
  fetchRecent(null)
}

// ---------- 操作：快捷操作 ----------
function handleQuickAction(item) {
  if (item.action === 'createOrder') {
    // 客户经理首页：直接打开新建订单弹窗，不再跳转订单管理页
    if (isAM.value) {
      createDialogVisible.value = true
      return
    }
    ElMessage.info('请前往订单管理页新建订单')
    router.push('/orders')
    return
  }
  if (item.route) {
    router.push(item.route)
  }
}

// ---------- 操作：查看全部 ----------
function handleViewAll() {
  if (isDS.value) {
    router.push('/design-workbench')
  } else {
    router.push('/orders')
  }
}

// ---------- 操作：查看订单详情 ----------
function viewOrderDetail(row) {
  currentViewOrderId.value = row.id
  detailDrawerVisible.value = true
}

// ---------- 操作：跳转到审核记录 ----------
function goToAuditLogs() {
  if (isAM.value) {
    router.push('/am-workbench')
  } else {
    router.push({ path: '/final-audit', query: { tab: 'logs' } })
  }
}

// ---------- 审核操作类型映射 ----------
function logActionLabel(action) {
  const map = {
    APPROVE: '确认定稿',
    REJECT: '驳回设计',
    SUBMIT: '提交审核',
    ASSIGN: '任务分派',
    ACCEPT: '设计师接单',
    RESTART: '重新开始设计',
    TASK_RESTART: '重新开始设计',
    CREATE: '创建订单',
    CANCEL: '订单作废',
    DELIVER: '订单交付',
    ARCHIVE: '归档'
  }
  if (!action) return '—'
  const upper = action.toUpperCase()
  return map[upper] || action
}

function logActionType(action) {
  if (!action) return 'info'
  const upper = action.toUpperCase()
  if (upper.includes('APPROVE') || upper.includes('DELIVER') || upper.includes('ARCHIVE')) return 'success'
  if (upper.includes('REJECT') || upper.includes('CANCEL')) return 'danger'
  if (upper.includes('SUBMIT')) return 'warning'
  if (upper.includes('ASSIGN') || upper.includes('ACCEPT') || upper.includes('CREATE')) return 'primary'
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
onMounted(() => {
  fetchStats()
  fetchRecent()
  if (!isDS.value) {
    fetchAuditLogs()
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========== 欢迎语 ========== */
.welcome-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--gray-800);
  margin: 0 0 4px;
}
.welcome-sub {
  font-size: 13px;
  color: var(--gray-400);
  margin: 0;
}

/* ========== 统计卡片 ========== */
.stat-row {
  margin-bottom: 4px;
}

.stat-card {
  cursor: pointer;
  transition: var(--transition-base);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-card.stat-active {
  border-color: var(--primary-color, #409eff);
  border-width: 2px;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.stat-icon-wrap {
  width: 46px;
  height: 46px;
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

/* 各状态配色 */
.stat-pendingAssign .stat-icon-wrap {
  background: var(--info-light);
  color: var(--info-color);
}
.stat-pendingAssign .stat-value {
  color: var(--info-color);
}

.stat-pendingAccept .stat-icon-wrap {
  background: var(--info-light);
  color: var(--info-color);
}
.stat-pendingAccept .stat-value {
  color: var(--info-color);
}

.stat-designing .stat-icon-wrap {
  background: var(--warning-light);
  color: #d97706;
}
.stat-designing .stat-value {
  color: #d97706;
}

.stat-waitApprove .stat-icon-wrap {
  background: #fef3c7;
  color: #b45309;
}
.stat-waitApprove .stat-value {
  color: #b45309;
}

.stat-archived .stat-icon-wrap {
  background: var(--success-light);
  color: #059669;
}
.stat-archived .stat-value {
  color: #059669;
}

.stat-completed .stat-icon-wrap {
  background: var(--success-light);
  color: #059669;
}
.stat-completed .stat-value {
  color: #059669;
}

/* ========== 区块卡片 ========== */
.section-card {
  height: 100%;
}
.section-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid var(--gray-100);
}
.section-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--gray-800);
}

.header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-tag {
  margin-right: 4px;
}

/* ========== 快捷操作 ========== */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 8px;
  border-radius: var(--border-radius);
  background: var(--gray-50);
  cursor: pointer;
  transition: var(--transition-base);
  border: 1px solid transparent;
}

.quick-item:hover {
  background: #ffffff;
  border-color: var(--primary-light);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.quick-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: var(--border-radius);
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-label {
  font-size: 13px;
  color: var(--gray-700);
  font-weight: 500;
}

/* ========== 表格 ========== */
.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: var(--primary-color);
}

.link-text {
  cursor: pointer;
  font-weight: 500;
}

.link-text:hover {
  text-decoration: underline;
}

.order-no-text {
  color: var(--gray-700);
  font-weight: 400;
}

/* ========== 审核记录时间线 ========== */
.audit-card {
  margin-top: 0;
}

.audit-timeline-wrap {
  min-height: 100px;
}

.audit-timeline {
  padding-left: 8px;
  margin: 0;
}

.audit-log-item {
  padding-bottom: 4px;
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

.log-footer {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--gray-400);
}
</style>
