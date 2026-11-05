<template>
  <div class="register-page">
    <div class="register-card">
      <!-- 左侧品牌区 -->
      <div class="register-brand">
        <div class="brand-logo">A</div>
        <h1 class="brand-title">广告物料订单管理系统</h1>
        <p class="brand-sub">AMOMS · 高效管理每一份设计订单</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>订单全流程管理</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>多物料任务分派</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>设计稿版本追溯</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>定稿审核归档</span>
          </div>
        </div>
      </div>

      <!-- 右侧注册表单 -->
      <div class="register-form-wrap">
        <h2 class="form-title">注册账号</h2>
        <p class="form-tip">注册后默认设计人员角色，用于接单和上传设计稿</p>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          label-width="0"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入账号（3~20位）"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="name">
            <el-input
              v-model="registerForm.name"
              placeholder="请输入姓名"
              size="large"
              :prefix-icon="UserFilled"
              clearable
            />
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              size="large"
              :prefix-icon="Phone"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请设置密码（至少6位）"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form>

        <div class="login-link">
          已有账号？
          <router-link to="/login">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Lock, Phone, Check } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()

const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  name: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度 3~20 位', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 20, message: '姓名不能超过 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      name: registerForm.name,
      phone: registerForm.phone
    })
    ElMessage.success('注册成功，请登录')
    router.push({
      path: '/login',
      query: { username: registerForm.username }
    })
  } catch (e) {
    // 错误消息已在 request 拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e40af 0%, #2563eb 50%, #3b82f6 100%);
  position: relative;
  overflow: hidden;
}

.register-page::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  top: -200px;
  right: -150px;
}

.register-page::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
}

.register-card {
  width: 900px;
  min-height: 560px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* 左侧品牌区 */
.register-brand {
  width: 380px;
  background: linear-gradient(160deg, #1d4ed8 0%, #2563eb 100%);
  color: #ffffff;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
}

.brand-logo {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
}

.brand-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px;
  line-height: 1.3;
}

.brand-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 32px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.feature-item .el-icon {
  color: #86efac;
  font-size: 16px;
}

/* 右侧注册表单 */
.register-form-wrap {
  flex: 1;
  padding: 48px 56px 32px;
  display: flex;
  flex-direction: column;
}

.form-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--gray-800);
  margin: 0 0 8px;
}

.form-tip {
  font-size: 13px;
  color: var(--gray-400);
  margin: 0 0 24px;
}

.register-form {
  margin-bottom: 16px;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 4px;
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.register-btn:hover {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
}

.login-link {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--gray-100);
  font-size: 13px;
  color: var(--gray-500);
  text-align: center;
}

.login-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
