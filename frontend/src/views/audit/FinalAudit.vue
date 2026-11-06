<template>
  <!--
    定稿审核页面（仅客户经理 AM 可访问）
    只读查看：列表 / 筛选 / 分页 / 统计卡片 / 查看详情全部保留，
    确认定稿、驳回按钮已移除（canAudit=false）

    注：路由 meta.roles 已限制 AM，但组件内再次守卫防止 BL 直接通过 URL 访问
  -->
  <div v-if="isAM" class="final-audit-page">
    <FinalAuditPanel :can-audit="false" title="定稿审核归档" />
  </div>
  <div v-else class="no-permission">
    <el-empty description="暂无权限访问该页面">
      <el-button type="primary" @click="goDashboard">返回工作台</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElEmpty, ElButton } from 'element-plus'
import FinalAuditPanel from './FinalAuditPanel.vue'

const userStore = useUserStore()
const router = useRouter()

const isAM = computed(() => userStore.userInfo?.role === 'AM')

function goDashboard() {
  router.push('/dashboard')
}
</script>

<style scoped>
.no-permission {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
