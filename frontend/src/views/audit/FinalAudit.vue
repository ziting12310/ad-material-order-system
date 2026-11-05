<template>
  <!--
    定稿审核（业务负责人页面）
    只读查看：列表 / 筛选 / 分页 / 统计卡片 / 查看详情全部保留，
    确认定稿、驳回按钮已移除（canAudit=false）
  -->
  <FinalAuditPanel :can-audit="false" title="定稿审核归档" />
</template>

<script setup>
import FinalAuditPanel from './FinalAuditPanel.vue'
</script>
