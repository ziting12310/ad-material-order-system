<template>
  <!--
    工作台 Dashboard — 简约统计卡片 + 快捷入口
  -->
  <div class="dashboard">
    <!-- 欢迎语 -->
    <div class="welcome-bar">
      <div>
        <h2 class="welcome-title">{{ greeting }}，{{ userStore.userInfo.name }}</h2>
        <p class="welcome-sub">今天也要加油哦 · {{ todayDate }}</p>
      </div>
    </div>

    <!-- 统计卡片组 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="stat-card stat-waiting" shadow="never" v-loading="statsLoading">
          <div class="stat-icon-wrap">
            <el-icon :size="24"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pending || 0 }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="stat-card stat-approved" shadow="never" v-loading="statsLoading">
          <div class="stat-icon-wrap">
            <el-icon :size="24"><CircleCheckFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.approved || 0 }}</div>
            <div class="stat-label">已定稿</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="stat-card stat-rejected" shadow="never" v-loading="statsLoading">
          <div class="stat-icon-wrap">
            <el-icon :size="24"><CloseCircleFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.rejected || 0 }}</div>
            <div class="stat-label">已驳回</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 + 最近订单 -->
    <el-row :gutter="16">
      <!-- 快捷入口 -->
      <el-col :xs="24" :md="8">
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="section-title">快捷操作</span>
          </template>
          <div class="quick-actions">
            <div class="quick-item" @click="router.push('/orders')">
              <el-icon :size="20" color="#2563eb"><Document /></el-icon>
              <span>订单管理</span>
            </div>
            <div class="quick-item" @click="router.push('/final-audit')">
              <el-icon :size="20" color="#10b981"><CircleCheck /></el-icon>
              <span>定稿审核</span>
            </div>
            <div class="quick-item" @click="router.push('/final-audit')">
              <el-icon :size="20" color="#f59e0b"><FolderOpened /></el-icon>
              <span>归档查询</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 最近待审核 -->
      <el-col :xs="24" :md="16">
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="header-flex">
              <span class="section-title">最近待审核</span>
              <el-button type="primary" link @click="router.push('/final-audit')">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table
            :data="recentList"
            v-loading="recentLoading"
            style="width: 100%"
            :show-header="true"
          >
            <el-table-column prop="orderNo" label="订单编号" width="150">
              <template #default="{ row }">
                <span class="mono-text">{{ row.orderNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="customerName" label="客户名称" />
            <el-table-column prop="materialType" label="物料类型" width="100" />
            <el-table-column prop="createdAt" label="提交时间" width="160">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="warning" effect="light" round size="small">待审核</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-if="!recentLoading && recentList.length === 0"
            description="暂无待审核订单"
            :image-size="80"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAuditStatistics, listAuditOrders } from '@/api/audit'

const router = useRouter()
const userStore = useUserStore()

// 统计数据
const stats = reactive({ pending: 0, approved: 0, rejected: 0 })
const statsLoading = ref(false)

// 最近待审核
const recentList = ref([])
const recentLoading = ref(false)

// 问候语
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

async function fetchStats() {
  statsLoading.value = true
  try {
    const res = await getAuditStatistics()
    if (res) {
      stats.pending = res.pending || 0
      stats.approved = res.approved || 0
      stats.rejected = res.rejected || 0
    }
  } catch (e) {
    // 接口不通时用 mock 数据（课程演示友好）
    stats.pending = 12
    stats.approved = 48
    stats.rejected = 5
  } finally {
    statsLoading.value = false
  }
}

async function fetchRecent() {
  recentLoading.value = true
  try {
    const res = await listAuditOrders({ limit: 5, status: 'WAIT_APPROVE' })
    recentList.value = res.list || []
  } catch (e) {
    // mock 数据
    recentList.value = [
      { id: 1, orderNo: 'ORD-2026-00015', customerName: '可口可乐', materialType: '海报', createdAt: '2026-11-03 14:20:00', status: 'WAIT_APPROVE' },
      { id: 2, orderNo: 'ORD-2026-00014', customerName: '宝洁中国', materialType: '宣传单', createdAt: '2026-11-03 11:05:00', status: 'WAIT_APPROVE' },
      { id: 3, orderNo: 'ORD-2026-00013', customerName: '华为技术', materialType: '画册', createdAt: '2026-11-02 16:40:00', status: 'WAIT_APPROVE' }
    ]
  } finally {
    recentLoading.value = false
  }
}

function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

onMounted(() => {
  fetchStats()
  fetchRecent()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 欢迎语 */
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

/* 统计卡片 */
.stat-row {
  margin-bottom: 4px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
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

.stat-waiting .stat-icon-wrap {
  background: var(--warning-light);
  color: #d97706;
}
.stat-approved .stat-icon-wrap {
  background: var(--success-light);
  color: #059669;
}
.stat-rejected .stat-icon-wrap {
  background: var(--danger-light);
  color: #dc2626;
}

/* 区块卡片 */
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

/* 快捷操作 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 8px;
  border-radius: var(--border-radius);
  background: var(--gray-50);
  cursor: pointer;
  transition: var(--transition-base);
  font-size: 13px;
  color: var(--gray-600);
}

.quick-item:hover {
  background: var(--primary-light);
  color: var(--primary-color);
  transform: translateY(-1px);
}

.mono-text {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: var(--primary-color);
}
</style>
