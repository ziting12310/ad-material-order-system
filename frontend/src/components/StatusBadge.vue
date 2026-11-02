<template>
  <!--
    状态徽章 — 对齐 UI_UX_SPEC §状态徽章色
    variant='order'    订单 7 状态（CREATED 灰 / ASSIGNED 蓝 / DESIGNING 橙 / WAIT_APPROVE 紫 / APPROVED 青 / DELIVERED 绿 / CANCELLED 深灰）
    variant='role'     角色徽章（AM/BL/DS）
    variant='feedback' 意见状态（PENDING/PROCESSED）
  -->
  <span :class="badgeClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  variant: { type: String, default: 'order' }
})

// 订单状态（GLOSSARY §3.1 OrderStatus）
const ORDER_MAP = {
  CREATED:      { label: '待分派', cls: 'badge-created' },
  ASSIGNED:     { label: '已分派', cls: 'badge-assigned' },
  DESIGNING:    { label: '设计中', cls: 'badge-designing' },
  WAIT_APPROVE: { label: '待定稿', cls: 'badge-wait_approve' },
  APPROVED:     { label: '已定稿', cls: 'badge-approved' },
  DELIVERED:    { label: '已交付', cls: 'badge-delivered' },
  CANCELLED:    { label: '已作废', cls: 'badge-cancelled' }
}

// 角色（GLOSSARY §3.3 UserRole）
const ROLE_MAP = {
  AM: { label: '客户经理', cls: 'badge-assigned' },
  BL: { label: '业务负责人', cls: 'badge-approved' },
  DS: { label: '设计制作人员', cls: 'badge-designing' }
}

// 意见状态（GLOSSARY §3.4 FeedbackStatus）
const FB_MAP = {
  PENDING:   { label: '待处理', cls: 'badge-designing' },
  PROCESSED: { label: '已处理', cls: 'badge-approved' }
}

const currentMap = computed(() =>
  props.variant === 'role' ? ROLE_MAP : props.variant === 'feedback' ? FB_MAP : ORDER_MAP
)
const badgeClass = computed(() => currentMap.value[props.status]?.cls || 'badge-created')
const label = computed(() => currentMap.value[props.status]?.label || props.status)
</script>
