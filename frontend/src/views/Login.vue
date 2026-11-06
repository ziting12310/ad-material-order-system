<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="brand-logo">A</div>
        <h1 class="brand-title">广告物料订单管理系统</h1>
        <p class="brand-sub">AMOMS · 高效管理每一份设计订单</p>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-wrap">
        <h2 class="form-title">账号登录</h2>
        <p class="form-tip">请输入您的账号和密码</p>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入账号"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>

        

        <div class="register-link">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度 3-32 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
    // 跳转到来源页或首页
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 错误消息已在 request 拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e40af 0%, #2563eb 50%, #3b82f6 100%);
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  top: -200px;
  right: -150px;
}

.login-page::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
}

.login-card {
  width: 860px;
  height: 520px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* 左侧品牌区 */
.login-brand {
  width: 380px;
  background: linear-gradient(160deg, #1d4ed8 0%, #2563eb 100%);
  color: #ffffff;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
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
  margin: 0;
}

/* 右侧登录表单 */
.login-form-wrap {
  flex: 1;
  padding: 56px 56px 32px;
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
  margin: 0 0 32px;
}

.login-form {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 8px;
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.login-btn:hover {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
}

.demo-tip {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--gray-100);
  font-size: 12px;
  color: var(--gray-400);
}

.demo-tip p {
  margin: 0 0 8px;
  font-weight: 500;
  color: var(--gray-500);
}

.demo-tip ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.demo-tip li {
  font-size: 12px;
  color: var(--gray-400);
}

.demo-tip code {
  background: var(--gray-100);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
}

.register-link {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--gray-100);
  font-size: 13px;
  color: var(--gray-500);
  text-align: center;
}

.register-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
