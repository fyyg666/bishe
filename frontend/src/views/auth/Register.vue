<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1>用户注册</h1>
        <p>Create Your Account</p>
      </div>
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="rules"
        class="register-form"
        label-width="80px"
        @submit.prevent="handleRegister"
      >
        <el-form-item
          label="用户名"
          prop="username"
        >
          <el-input
            v-model="registerForm.username"
            placeholder="3-20 个字符"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item
          label="密码"
          prop="password"
        >
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="至少 6 位"
            prefix-icon="Lock"
            show-password
          />
          <el-progress
            v-if="registerForm.password"
            :percentage="passwordStrength.percentage"
            :status="passwordStrength.status || undefined"
            :stroke-width="6"
            :show-text="false"
            class="password-strength"
          />
          <span
            v-if="passwordStrength.text"
            class="strength-text"
          >{{ passwordStrength.text }}</span>
        </el-form-item>
        <el-form-item
          label="确认密码"
          prop="confirmPassword"
        >
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item
          label="邮箱"
          prop="email"
        >
          <el-input
            v-model="registerForm.email"
            placeholder="example@mail.com"
            prefix-icon="Message"
          />
        </el-form-item>
        <el-form-item
          label="手机号"
          prop="phone"
        >
          <el-input
            v-model="registerForm.phone"
            placeholder="11 位手机号"
            prefix-icon="Phone"
          />
        </el-form-item>
        <el-form-item
          label="真实姓名"
          prop="realName"
        >
          <el-input
            v-model="registerForm.realName"
            placeholder="请输入真实姓名"
            prefix-icon="UserFilled"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="register-button"
            @click="handleRegister"
          >
            {{ loading ? '注册中…' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">
          登录
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '', password: '', confirmPassword: '',
  email: '', phone: '', realName: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位,需含大小写字母、数字和特殊字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

// 密码强度计算
const passwordStrength = computed(() => {
  const pwd = registerForm.password
  if (!pwd || pwd.length < 8) return { score: 0, percentage: 0, status: '', text: '' }

  const hasUpper = /[A-Z]/.test(pwd)
  const hasLower = /[a-z]/.test(pwd)
  const hasDigit = /\d/.test(pwd)
  const hasSpecial = /[^A-Za-z0-9]/.test(pwd)

  if (hasUpper && hasLower && hasDigit && hasSpecial) {
    return { score: 5, percentage: 100, status: 'success', text: '强' }
  }
  if (hasUpper && hasLower) {
    return { score: 3, percentage: 60, status: 'warning', text: '中' }
  }
  return { score: 1, percentage: 25, status: 'exception', text: '弱' }
})

async function handleRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const submitData = { ...registerForm }
      await register(submitData)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      ElMessage.error(error.message || '注册失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

.register-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-page;
  padding: $space-10 $space-5;
}

.register-box {
  width: 480px;
  padding: $space-10 $space-10 $space-8;
  background: $bg-card;
  border-radius: $radius-xl;
  border: 1px solid $border;
  box-shadow: $shadow-lg;
}

.register-header {
  text-align: center;
  margin-bottom: $space-8;

  h1 {
    font-size: $font-size-2xl;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    margin-bottom: $space-1;
  }
  p {
    font-size: $font-size-sm;
    color: $text-secondary;
  }
}

.register-form {
  :deep(.el-form-item) { margin-bottom: $space-4; }
  :deep(.el-input__wrapper) {
    background: $gray-50;
    border: 1px solid $border-light;
    box-shadow: none !important;
    border-radius: $radius-md;
    transition: all $transition-base;
    &:hover { border-color: $gray-300; }
    &.is-focus { border-color: $primary; background: #fff; }
  }
  :deep(.el-input__inner) { height: 40px; }
  :deep(.el-form-item__label) { color: $text-regular; }

  .register-button { width: 100%; }
  .password-strength { margin-top: 6px; }
  .strength-text {
    position: absolute;
    right: 10px;
    bottom: 2px;
    font-size: 12px;
    color: $text-secondary;
  }
}

.register-footer {
  text-align: center;
  margin-top: $space-5;
  padding-top: $space-4;
  border-top: 1px solid $border-light;
  font-size: $font-size-sm;
  color: $text-secondary;

  a { color: $primary; text-decoration: none; font-weight: $font-weight-medium;
    &:hover { text-decoration: underline; }
  }
}
</style>
