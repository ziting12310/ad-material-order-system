<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 左侧：基本信息卡片 -->
      <el-col :span="8">
        <el-card class="profile-card" shadow="never">
          <div class="profile-header">
            <el-avatar :size="72" style="background: var(--primary-color)">
              {{ userStore.userInfo?.name?.charAt(0) }}
            </el-avatar>
            <div class="profile-name">
              <h3>{{ userStore.userInfo?.name }}</h3>
              <el-tag :type="roleTagType" size="small">{{ roleName }}</el-tag>
            </div>
          </div>

          <el-descriptions :column="1" border size="default" class="profile-desc">
            <el-descriptions-item label="账号">
              {{ userStore.userInfo?.username }}
            </el-descriptions-item>
            <el-descriptions-item label="角色">
              {{ roleName }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号">
              {{ userStore.userInfo?.phone || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：修改密码 -->
      <el-col :span="16">
        <el-card class="password-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            style="max-width: 480px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（6-32位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="submitting"
                @click="handleChangePassword"
              >
                确认修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api'
import { ROLE_MAP } from '@/constants'

const userStore = useUserStore()

const roleName = computed(() => {
  const role = userStore.userInfo?.role
  return ROLE_MAP[role]?.label || role
})

const roleTagType = computed(() => {
  const role = userStore.userInfo?.role
  if (role === 'BL') return 'primary'
  if (role === 'DS') return 'success'
  return ''
})

// ========== 修改密码 ==========
const passwordFormRef = ref(null)
const submitting = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    // 清除登录态，跳登录页
    userStore.logout()
    setTimeout(() => {
      window.location.href = '/login'
    }, 1000)
  } catch (e) {
    // 错误已在拦截器提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-card {
  border-radius: var(--border-radius);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0 20px;
  border-bottom: 1px solid var(--gray-100);
  margin-bottom: 16px;
}

.profile-name h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  color: var(--gray-800);
}

.profile-desc {
  margin-top: 8px;
}

.password-card {
  border-radius: var(--border-radius);
}

.card-header {
  font-size: 15px;
  font-weight: 600;
  color: var(--gray-800);
}
</style>
