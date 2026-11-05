<template>
  <!--
    客户经理工作台
    功能：Tab 承载【我的订单】【定稿审核】【归档查询】
    定稿审核 Tab 复用 FinalAuditPanel，可执行确认定稿 / 驳回并填写意见
  -->
  <div class="am-workbench">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">客户经理工作台</h2>
      <div class="page-sub">处理待定稿任务，确认定稿或驳回并填写修改意见</div>
    </div>

    <el-card class="tab-card" shadow="never">
      <el-tabs v-model="activeTab" class="am-tabs">
        <!-- 我的订单 -->
        <el-tab-pane label="我的订单" name="orders" lazy>
          <OrderList />
        </el-tab-pane>

        <!-- 定稿审核（可操作） -->
        <el-tab-pane label="定稿审核" name="audit" lazy>
          <FinalAuditPanel :can-audit="true" :show-title="false" />
        </el-tab-pane>

        <!-- 归档查询 -->
        <el-tab-pane label="归档查询" name="archive" lazy>
          <ArchiveList />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import OrderList from '@/views/order/OrderList.vue'
import ArchiveList from '@/views/archive/ArchiveList.vue'
import FinalAuditPanel from '@/views/audit/FinalAuditPanel.vue'

// 默认停在定稿审核 Tab
const activeTab = ref('audit')
</script>

<style scoped>
.am-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--gray-900);
}

.page-sub {
  font-size: 13px;
  color: var(--gray-400);
}

.tab-card {
  border-radius: var(--border-radius);
}

.tab-card :deep(.el-card__body) {
  padding: 0 20px 16px;
}

/* 子页面自带标题在 Tab 内隐藏，避免与工作台标题重复 */
.am-tabs :deep(.page-title),
.am-tabs :deep(.page-sub) {
  display: none;
}
</style>
