<template>
  <!--
    订单详情 — 核心页面（US-003 ~ US-007）
    按钮矩阵按「状态 × 角色」动态渲染（逻辑对齐 PRD §2 权限矩阵）：
      CREATED      → BL分派 / AM编辑 / AM·BL作废
      ASSIGNED     → DS接单 / BL作废
      DESIGNING    → DS上传设计稿 / BL作废
      WAIT_APPROVE → AM·BL提交意见 / BL驳回 / BL确认终稿
      APPROVED     → BL标记交付
      DELIVERED/CANCELLED → 只读
  -->
  <div class="space-y-4">
    <!-- 标题 + 状态徽章 -->
    <div class="flex items-center gap-3">
      <h1 class="text-lg font-semibold text-gray-800 font-mono">{{ detail?.orderNo }}</h1>
      <StatusBadge v-if="detail" :status="detail.status" />
      <span v-if="detail?.isArchived" class="text-xs text-gray-400">（已归档）</span>
    </div>

    <div v-if="loading" class="text-gray-400 text-center py-20">加载中…</div>

    <template v-else-if="detail">
      <!-- 基础信息卡片 -->
      <div class="card p-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-y-3 gap-x-10 text-sm">
          <div><span class="text-gray-500 w-20 inline-block">客户名称：</span>{{ detail.customerName }}</div>
          <div><span class="text-gray-500 w-20 inline-block">联系人：</span>{{ detail.contactName }}</div>
          <div><span class="text-gray-500 w-20 inline-block">联系电话：</span>{{ detail.contactPhone }}</div>
          <div><span class="text-gray-500 w-20 inline-block">物料类型：</span>{{ MATERIAL_LABEL[detail.materialType] || detail.materialType }}</div>
          <div><span class="text-gray-500 w-20 inline-block">数量：</span>{{ detail.quantity }}</div>
          <div><span class="text-gray-500 w-20 inline-block">交付日期：</span>{{ detail.deliveryDate }}</div>
          <div class="md:col-span-2"><span class="text-gray-500 w-20 inline-block">规格：</span>{{ detail.specification }}</div>
          <div v-if="detail.remark" class="md:col-span-2"><span class="text-gray-500 w-20 inline-block">备注：</span>{{ detail.remark }}</div>
          <div><span class="text-gray-500 w-20 inline-block">创建人：</span>{{ detail.createdByName }}</div>
          <div><span class="text-gray-500 w-20 inline-block">设计师：</span>{{ detail.designerName || '—' }}</div>
        </div>

        <!-- 操作按钮区（状态 × 角色矩阵） -->
        <div v-if="actionButtons.length" class="mt-6 pt-5 border-t border-gray-100 flex flex-wrap gap-2">
          <button
            v-for="btn in actionButtons"
            :key="btn.key"
            :class="btn.cls"
            @click="btn.action"
          >{{ btn.label }}</button>
        </div>
      </div>

      <!-- 设计稿版本列表 -->
      <div class="card p-6">
        <h3 class="font-medium mb-3">设计稿版本（{{ detail.designVersions?.length || 0 }}）</h3>
        <div v-if="detail.designVersions?.length" class="space-y-2">
          <div
            v-for="v in detail.designVersions"
            :key="v.id"
            class="flex items-center justify-between border border-gray-100 rounded-md px-4 py-3 hover:border-brand/30"
          >
            <div class="flex items-center gap-3 min-w-0">
              <span class="px-2 py-0.5 bg-brand/10 text-brand rounded text-xs font-medium shrink-0">{{ v.versionNo }}</span>
              <span v-if="v.isFinal" class="px-2 py-0.5 bg-green-100 text-green-700 rounded text-xs shrink-0">终稿</span>
              <a :href="v.fileUrl" target="_blank" class="text-sm text-brand hover:underline truncate">{{ v.fileName }}</a>
              <span class="text-xs text-gray-400 truncate">{{ v.versionNote }}</span>
            </div>
            <span class="text-xs text-gray-400 shrink-0 ml-4">{{ v.uploadedByName }} · {{ formatTime(v.uploadedAt) }}</span>
          </div>
        </div>
        <p v-else class="text-gray-400 text-sm">暂无设计稿</p>
      </div>

      <!-- 修改意见列表 -->
      <div class="card p-6">
        <h3 class="font-medium mb-3">修改意见（{{ detail.feedbacks?.length || 0 }}）</h3>
        <div v-if="detail.feedbacks?.length" class="space-y-3">
          <div v-for="fb in detail.feedbacks" :key="fb.id" class="border-l-2 border-gray-200 pl-4 py-1">
            <div class="text-sm text-gray-700">{{ fb.content }}</div>
            <div class="mt-1 text-xs text-gray-400 flex items-center gap-3">
              <span>{{ fb.submittedByName }}</span>
              <StatusBadge :status="fb.status" variant="feedback" />
              <span>{{ fb.versionNo }} · {{ formatTime(fb.submittedAt) }}</span>
            </div>
          </div>
        </div>
        <p v-else class="text-gray-400 text-sm">暂无修改意见</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getOrderDetail, assignOrder, acceptTask, rejectDesign,
  approveFinal, markDelivered, cancelOrder
} from '@/api'
import { submitFeedback } from '@/api'
import { useUserStore } from '@/stores/user'
import StatusBadge from '@/components/StatusBadge.vue'

const route = useRoute()
const userStore = useUserStore()
const detail = ref(null)
const loading = ref(false)

const MATERIAL_LABEL = { POSTER: '海报', FLYER: '宣传单', BUSINESS_CARD: '名片', BROCHURE: '画册', BANNER: '喷绘', OTHER: '其他' }

onMounted(fetchDetail)

async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getOrderDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ========================================================================
// 按钮矩阵 — 状态 × 角色（权限规则引用 PRD §2，不重复定义）
// ========================================================================
const actionButtons = computed(() => {
  if (!detail.value) return []
  const { status, version, designerId } = detail.value
  const role = userStore.role
  const isMyTask = designerId === userStore.user?.id
  const buttons = []

  if (status === 'CREATED') {
    if (role === 'BL') buttons.push({ key: 'assign', label: '分派设计人员', cls: 'btn-primary', action: onAssign })
    if (role === 'AM' || role === 'BL') buttons.push({ key: 'cancel', label: '作废订单', cls: 'btn-danger', action: onCancel })
  } else if (status === 'ASSIGNED') {
    if (role === 'DS' && isMyTask) buttons.push({ key: 'accept', label: '接单并开始设计', cls: 'btn-primary', action: onAccept })
    if (role === 'BL') buttons.push({ key: 'cancel', label: '作废订单', cls: 'btn-danger', action: onCancel })
  } else if (status === 'DESIGNING') {
    if (role === 'DS' && isMyTask) buttons.push({ key: 'upload', label: '上传设计稿', cls: 'btn-primary', action: onUpload })
    if (role === 'BL') buttons.push({ key: 'cancel', label: '作废订单', cls: 'btn-danger', action: onCancel })
  } else if (status === 'WAIT_APPROVE') {
    if (role === 'AM' || role === 'BL') buttons.push({ key: 'feedback', label: '提交修改意见', cls: 'btn', action: onFeedback })
    if (role === 'BL') {
      buttons.push({ key: 'reject', label: '驳回（需重设计）', cls: 'btn-warning', action: onReject })
      buttons.push({ key: 'approve', label: '确认终稿', cls: 'btn-primary', action: onApprove })
    }
  } else if (status === 'APPROVED') {
    if (role === 'BL') buttons.push({ key: 'deliver', label: '标记交付', cls: 'btn-primary', action: onDeliver })
  }
  // DELIVERED / CANCELLED → 只读无按钮
  return buttons
})

// ========================================================================
// 操作方法 — 全部携带 version 乐观锁，成功后刷新详情
// ========================================================================

/** 分派（BL）：CREATED → ASSIGNED */
async function onAssign() {
  const designerId = prompt('请输入设计人员 ID（测试：3=ds_demo）')
  if (!designerId) return
  await assignOrder(detail.value.id, { designerId: Number(designerId), version: detail.value.version })
  await fetchDetail()
}

/** 接单（DS）：ASSIGNED → DESIGNING */
async function onAccept() {
  await acceptTask(detail.value.id, { version: detail.value.version })
  await fetchDetail()
}

/** 上传设计稿（DS）：DESIGNING → WAIT_APPROVE（MVP 简化：手动填文件信息，后续接 OSS presign） */
async function onUpload() {
  const fileName = prompt('设计稿文件名（如 poster_v1.png）')
  if (!fileName) return
  const versionNote = prompt('版本说明（1-200 字符）') || ''
  // MVP 阶段 fileId/fileUrl 简化占位，生产走 presign → OSS 直传
  const { uploadDesignVersion } = await import('@/api')
  await uploadDesignVersion({
    orderId: detail.value.id,
    fileId: `oss/${detail.value.id}/${Date.now()}_${fileName}`,
    fileName,
    fileSize: 0,
    fileType: fileName.split('.').pop().toLowerCase(),
    versionNote,
    version: detail.value.version
  })
  await fetchDetail()
}

/** 提交修改意见（AM/BL）：WAIT_APPROVE → DESIGNING */
async function onFeedback() {
  const content = prompt('请输入修改意见（1-500 字符）')
  if (!content) return
  await submitFeedback({ orderId: detail.value.id, content, version: detail.value.version })
  await fetchDetail()
}

/** 驳回（BL）：WAIT_APPROVE → DESIGNING（自动生成一条意见） */
async function onReject() {
  const rejectNote = prompt('请输入驳回原因（1-500 字符）')
  if (!rejectNote) return
  await rejectDesign(detail.value.id, { rejectNote, version: detail.value.version })
  await fetchDetail()
}

/** 确认终稿（BL）：WAIT_APPROVE → APPROVED */
async function onApprove() {
  await approveFinal(detail.value.id, { version: detail.value.version })
  await fetchDetail()
}

/** 标记交付（BL）：APPROVED → DELIVERED（自动归档） */
async function onDeliver() {
  const deliverRemark = prompt('交付备注（选填，≤100 字符）') || ''
  await markDelivered(detail.value.id, { deliverRemark, version: detail.value.version })
  await fetchDetail()
}

/** 作废（AM/BL）：非终态 → CANCELLED */
async function onCancel() {
  const cancelReason = prompt('请输入作废原因（1-200 字符）')
  if (!cancelReason) return
  await cancelOrder(detail.value.id, { cancelReason, version: detail.value.version })
  await fetchDetail()
}
</script>
