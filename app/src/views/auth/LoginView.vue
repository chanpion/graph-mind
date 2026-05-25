<template>
  <div class="login-container">
    <div class="login-background">
      <div class="login-box">
        <div class="login-header">
          <div class="logo">
            <el-icon :size="40" color="#409EFF">
              <HomeFilled />
            </el-icon>
          </div>
          <h2 class="title">图数据库管理系统</h2>
          <p class="subtitle">欢迎登录</p>
        </div>

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
              placeholder="请输入用户名"
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
              clearable
            />
          </el-form-item>

          <el-form-item prop="captcha" v-if="!mockMode">
            <div class="captcha-container">
              <el-input
                v-model="loginForm.captcha"
                placeholder="请输入验证码"
                size="large"
                :prefix-icon="Key"
                clearable
              />
              <div class="captcha-image" @click="refreshCaptcha">
                <img :src="captchaUrl" alt="验证码" />
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
            <el-link type="primary" class="forgot-password">忘记密码？</el-link>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <p>还没有账号？<el-link type="primary">立即注册</el-link></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, HomeFilled } from '@element-plus/icons-vue'
import { useAuthStore } from './stores/useAuthStore'
import authApi from './api/auth'
import { isMockMode } from '@/config/apiConfig'

const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref()
const loading = ref(false)
const captchaUrl = ref('')
const mockMode = isMockMode()

const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  rememberMe: false
})

const expectedCaptcha = ref('')

const loginRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  ...(mockMode ? {} : {
    captcha: [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { len: 4, message: '验证码长度为 4 位', trigger: 'blur' },
      {
        validator: (rule, value, callback) => {
          if (value.toLowerCase() !== expectedCaptcha.value.toLowerCase()) {
            callback(new Error('验证码错误'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ]
  })
})

function generateCaptcha() {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 120
  canvas.height = 40

  ctx.fillStyle = '#f0f2f5'
  ctx.fillRect(0, 0, 120, 40)

  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let captcha = ''
  for (let i = 0; i < 4; i++) {
    captcha += chars.charAt(Math.floor(Math.random() * chars.length))
  }

  ctx.font = 'bold 20px Arial'
  ctx.fillStyle = '#409EFF'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  for (let i = 0; i < captcha.length; i++) {
    const x = 30 + i * 20
    const y = 20
    const rotation = (Math.random() - 0.5) * 0.3
    ctx.save()
    ctx.translate(x, y)
    ctx.rotate(rotation)
    ctx.fillText(captcha[i], 0, 0)
    ctx.restore()
  }

  for (let i = 0; i < 3; i++) {
    ctx.strokeStyle = `rgb(${Math.random() * 255}, ${Math.random() * 255}, ${Math.random() * 255})`
    ctx.beginPath()
    ctx.moveTo(Math.random() * 120, Math.random() * 40)
    ctx.lineTo(Math.random() * 120, Math.random() * 40)
    ctx.stroke()
  }

  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = `rgb(${Math.random() * 255}, ${Math.random() * 255}, ${Math.random() * 255})`
    ctx.fillRect(Math.random() * 120, Math.random() * 40, 1, 1)
  }

  captchaUrl.value = canvas.toDataURL()
  expectedCaptcha.value = captcha.toLowerCase()
}

const refreshCaptcha = () => {
  generateCaptcha()
}

async function handleLogin() {
  try {
    await loginFormRef.value.validate()

    loading.value = true

    const response = await authApi.login({
      username: loginForm.username,
      password: loginForm.password
    })

    authStore.login(response.data.token, response.data)

    if (loginForm.rememberMe) {
      localStorage.setItem('rememberedUser', JSON.stringify({
        username: loginForm.username,
        rememberMe: true
      }))
    } else {
      localStorage.removeItem('rememberedUser')
    }

    ElMessage.success('登录成功')
    router.push('/home/dashboard')
  } catch (error) {
    const errorMessage = error.response?.data?.message || error.message || '登录失败，请重试'
    ElMessage.error(errorMessage)
    if (!mockMode) refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!mockMode) generateCaptcha()
  if (mockMode) {
    loginForm.username = 'admin'
    loginForm.password = 'admin123'
  }
  const rememberedUser = localStorage.getItem('rememberedUser')
  if (rememberedUser) {
    const user = JSON.parse(rememberedUser)
    loginForm.username = user.username
    loginForm.rememberMe = user.rememberMe
  }
})
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-background {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-background::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="white" opacity="0.1"/><circle cx="75" cy="75" r="1" fill="white" opacity="0.1"/><circle cx="50" cy="10" r="0.5" fill="white" opacity="0.1"/><circle cx="10" cy="60" r="0.5" fill="white" opacity="0.1"/><circle cx="90" cy="40" r="0.5" fill="white" opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  pointer-events: none;
}

.login-box {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 40px;
  width: 400px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: slideIn 0.6s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  margin-bottom: 15px;
}

.title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.login-form {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.captcha-container {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-container .el-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
}

.captcha-image:hover {
  border-color: #409eff;
  transform: scale(1.02);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.login-form :deep(.el-form-item:nth-last-child(2)) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.forgot-password {
  font-size: 14px;
}

.login-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #409eff 0%, #36a3f7 100%);
  border: none;
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.3);
}

.login-footer {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.login-footer p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

@media (max-width: 480px) {
  .login-box {
    width: 90%;
    padding: 30px 20px;
  }

  .title {
    font-size: 24px;
  }

  .captcha-container {
    flex-direction: column;
    gap: 10px;
  }

  .captcha-image {
    width: 100%;
    height: 44px;
  }
}
</style>
