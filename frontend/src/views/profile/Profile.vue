<template>
  <div class="profile">
    <el-row :gutter="20">
      <!-- 用户信息卡片 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>个人信息</span>
          </template>
          <div class="user-info">
            <el-avatar
              :size="80"
              icon="UserFilled"
              class="avatar"
            />
            <div class="info-item">
              <span class="label">用户名</span>
              <span class="value">{{ userInfo.username }}</span>
            </div>
            <div class="info-item">
              <span class="label">真实姓名</span>
              <span class="value">{{ userInfo.realName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">角色</span>
              <el-tag>{{ getRoleText(userInfo.role) }}</el-tag>
            </div>
            <div class="info-item">
              <span class="label">邮箱</span>
              <span class="value">{{ userInfo.email || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">手机号</span>
              <span class="value">{{ userInfo.phone || '-' }}</span>
            </div>
          </div>
        </el-card>
        
        <!-- 信用等级卡片 -->
        <el-card shadow="hover" class="credit-card">
          <template #header>
            <span>信用等级</span>
          </template>
          <div class="credit-info">
            <div class="credit-score">{{ creditScore }}</div>
            <div class="credit-level">{{ getCreditLevel(creditScore).name }}</div>
            <el-progress
              :percentage="getCreditLevel(creditScore).progress"
              :color="getCreditLevel(creditScore).color"
              :stroke-width="8"
              style="margin: 12px 0;"
            />
            <el-button type="primary" link @click="$router.push('/profile/credit')">
              查看信用详情 >
            </el-button>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：编辑信息 + 修改密码 -->
      <el-col :span="16">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <span>编辑信息</span>
              </template>
              
              <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                label-width="80px"
                size="default"
              >
                <el-form-item
                  label="真实姓名"
                  prop="realName"
                >
                  <el-input
                    v-model="form.realName"
                    placeholder="请输入真实姓名"
                  />
                </el-form-item>
                
                <el-form-item
                  label="邮箱"
                  prop="email"
                >
                  <el-input
                    v-model="form.email"
                    placeholder="请输入邮箱"
                  />
                </el-form-item>
                
                <el-form-item
                  label="手机号"
                  prop="phone"
                >
                  <el-input
                    v-model="form.phone"
                    placeholder="请输入手机号"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="loading"
                    @click="handleSubmit"
                  >
                    {{ loading ? '保存中...' : '保存修改' }}
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <span>修改密码</span>
              </template>
              
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="80px"
                size="default"
              >
                <el-form-item
                  label="原密码"
                  prop="oldPassword"
                >
                  <el-input
                    v-model="pwdForm.oldPassword"
                    type="password"
                    show-password
                    placeholder="请输入原密码"
                  />
                </el-form-item>
                
                <el-form-item
                  label="新密码"
                  prop="newPassword"
                >
                  <el-input
                    v-model="pwdForm.newPassword"
                    type="password"
                    show-password
                    placeholder="请输入新密码"
                  />
                  <el-progress
                    v-if="pwdForm.newPassword"
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
                    v-model="pwdForm.confirmPassword"
                    type="password"
                    show-password
                    placeholder="请再次输入新密码"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="pwdLoading"
                    @click="handleChangePassword"
                  >
                    {{ pwdLoading ? '修改中...' : '修改密码' }}
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword as apiChangePassword } from '@/api/reader'
import { updateReader } from '@/api/reader'

const userStore = useUserStore()

const formRef = ref(null)
const pwdFormRef = ref(null)
const loading = ref(false)
const pwdLoading = ref(false)

const userInfo = ref({})
const form = reactive({
  realName: '',
  email: '',
  phone: ''
})

const creditScore = computed(() => userInfo.value?.creditScore ?? userStore.userInfo?.creditScore ?? 0)

function getCreditLevel(score) {
  if (score >= 240) return { name: '白金💎', progress: 100, color: '#409EFF' }
  if (score >= 180) return { name: '金牌🟡', progress: 75, color: '#E6A23C' }
  if (score >= 120) return { name: '银牌⚪', progress: 50, color: '#909399' }
  if (score >= 60) return { name: '铜牌🟤', progress: 25, color: '#CD7F32' }
  return { name: '普通', progress: 0, color: '#C0C4CC' }
}

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码强度计算
const passwordStrength = computed(() => {
  const pwd = pwdForm.newPassword
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

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

onMounted(() => {
  loadUserInfo()
})

function loadUserInfo() {
  userInfo.value = userStore.userInfo || {}
  Object.keys(form).forEach(key => {
    form[key] = userInfo.value[key] || ''
  })
}

function getRoleText(role) {
  const map = { ADMIN: '管理员', LIBRARIAN: '图书管理员', READER: '读者' }
  return map[role] || role
}

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      // FIXED: P1-FE-05 - 调用更新读者信息API
      const userId = userStore.userInfo?.id
      if (userId) {
        await updateReader(userId, {
          realName: form.realName,
          email: form.email,
          phone: form.phone
        })
        // 同步更新store中的用户信息
        userStore.userInfo = { ...userStore.userInfo, ...form }
      }
      ElMessage.success('保存成功')
    } catch {
      ElMessage.error('保存失败')
    } finally {
      loading.value = false
    }
  })
}

async function handleChangePassword() {
  if (!pwdFormRef.value) return
  
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    pwdLoading.value = true
    try {
      await apiChangePassword(userStore.userInfo?.id, {
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      ElMessage.success('密码修改成功')
      pwdFormRef.value.resetFields()
    } catch (error) {
      ElMessage.error(error.message || '修改失败')
    } finally {
      pwdLoading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.profile {
  .user-info {
    text-align: center;
    
    .avatar {
      margin-bottom: 20px;
      background: #409eff;
    }
    
    .info-item {
      display: flex;
      justify-content: space-between;
      padding: 12px 0;
      border-bottom: 1px solid #ebeef5;
      
      &:last-child {
        border-bottom: none;
      }
      
      .label {
        color: #909399;
      }
      
      .value {
        color: #303133;
        font-weight: 500;
      }
    }
  }

  // 确保右侧两列卡片等高
  :deep(.el-col) > .el-card {
    height: 100%;
  }

  // 密码强度指示器
  .password-strength {
    margin-top: 6px;
  }
  .strength-text {
    position: absolute;
    right: 10px;
    bottom: 2px;
    font-size: 12px;
    color: #909399;
  }

  // 信用等级卡片
  .credit-card {
    margin-top: 20px;

    .credit-info {
      text-align: center;

      .credit-score {
        font-size: 40px;
        font-weight: 700;
        color: #1a1a2e;
        line-height: 1.2;
      }

      .credit-level {
        font-size: 16px;
        color: #606266;
        margin-top: 4px;
      }
    }
  }
}
</style>
