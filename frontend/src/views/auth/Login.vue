<template>
  <div class="login-container">
    <div class="login-bg-decoration" />
    <div class="login-box">
      <div class="login-header">
        <div class="logo-icon">
          <el-icon :size="28">
            <Reading />
          </el-icon>
        </div>
        <h1>图书馆管理系统</h1>
        <p>Library Management System</p>
      </div>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            :prefix-icon="UserIcon"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            :prefix-icon="LockIcon"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-wrapper">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="验证码"
              size="large"
              :prefix-icon="KeyIcon"
              class="captcha-input"
              @keyup.enter="handleLogin"
            />
            <img
              :src="captchaImage"
              alt="验证码"
              class="captcha-img"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            size="large"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中…' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">
          注册
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, h, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'

const UserIcon = h(User)
const LockIcon = h(Lock)
const KeyIcon = h(Key)

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: '',
  captchaKey: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    const data = res.data || res
    captchaImage.value = data.captchaImage
    captchaKey.value = data.captchaKey
    loginForm.captchaKey = data.captchaKey
  } catch {
    // 验证码加载失败时不影响登录
  }
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(loginForm)
      ElMessage.success('登录成功')
      // FIXED: SEC-04 校验redirect参数，仅允许以/开头的相对路径
      const redirect = route.query.redirect
      const safeRedirect = redirect && typeof redirect === 'string' && redirect.startsWith('/') 
        ? redirect 
        : '/dashboard'
      router.push(safeRedirect)
    } catch (error) {
      // FIXED: BUG-09 区分网络错误和业务错误
      if (error.message && error.message.includes('Network')) {
        ElMessage.warning('网络连接异常，无法刷新验证码')
      } else {
        loadCaptcha() // 业务失败刷新验证码
      }
      ElMessage.error(error.message || '用户名或密码错误')
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  loadCaptcha()
})
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: oklch(0.97 0.005 55);
}

.login-bg-decoration {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%, oklch(0.6 0.12 248 / 0.08) 0%, transparent 100%),
    radial-gradient(ellipse 60% 40% at 20% 80%, oklch(0.56 0.06 35 / 0.06) 0%, transparent 100%),
    radial-gradient(ellipse 50% 40% at 80% 70%, oklch(0.55 0.1 145 / 0.04) 0%, transparent 100%);
  pointer-events: none;
}

.login-box {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: $space-12 $space-10 $space-8;
  background: $bg-card;
  border-radius: $radius-xl;
  border: 1px solid $border;
  box-shadow: $shadow-lg;
}

.login-header {
  text-align: center;
  margin-bottom: $space-8;

  .logo-icon {
    width: 52px;
    height: 52px;
    margin: 0 auto $space-4;
    background: $primary;
    border-radius: $radius-lg;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    box-shadow: $shadow-primary;
  }

  h1 {
    font-size: $font-size-2xl;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    margin-bottom: $space-1;
  }

  p {
    font-size: $font-size-sm;
    color: $text-secondary;
    letter-spacing: 1px;
  }
}

.login-form {
  :deep(.el-form-item) { margin-bottom: $space-5; }

  :deep(.el-input__wrapper) {
    background: $gray-50;
    border: 1px solid $border-light;
    box-shadow: none !important;
    border-radius: $radius-md;
    padding: 2px $space-4;
    transition: all $transition-base;

    &:hover { border-color: $gray-300; }
    &.is-focus {
      border-color: $primary;
      background: #fff;
    }
  }

  :deep(.el-input__inner) { height: 44px; font-size: $font-size-base; }
  :deep(.el-input__prefix .el-icon) { color: $text-secondary; font-size: 18px; }

  .login-button {
    width: 100%;
    height: 46px;
    font-size: $font-size-base;
    font-weight: $font-weight-medium;
    border-radius: $radius-md;
    letter-spacing: 0.5px;
  }

  .captcha-wrapper {
    display: flex;
    gap: $space-3;
    width: 100%;

    .captcha-input {
      flex: 1;
    }

    .captcha-img {
      width: 120px;
      height: 40px;
      border-radius: $radius-md;
      border: 1px solid $border-light;
      cursor: pointer;
      flex-shrink: 0;
      transition: opacity $transition-fast;

      &:hover { opacity: 0.8; }
    }
  }
}

.login-footer {
  text-align: center;
  margin-top: $space-6;
  padding-top: $space-5;
  border-top: 1px solid $border-light;
  font-size: $font-size-sm;
  color: $text-secondary;

  a {
    color: $primary;
    text-decoration: none;
    font-weight: $font-weight-medium;
    transition: color $transition-fast;
    &:hover { color: $primary-dark; text-decoration: underline; }
  }
}
</style>
