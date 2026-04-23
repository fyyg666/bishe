<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 左侧用户信息 -->
      <el-col :span="8">
        <el-card>
          <div class="user-card">
            <el-avatar :size="80" icon="UserFilled" class="avatar" />
            <h3 class="username">{{ userInfo.username || '用户' }}</h3>
            <el-tag>{{ getRoleText(userInfo.role) }}</el-tag>
            <div class="credit-section">
              <div class="credit-score">
                <span class="score">{{ creditInfo.score || 0 }}</span>
                <span class="label">信用积分</span>
              </div>
              <el-progress
                :percentage="creditPercentage"
                :color="creditColor"
                :stroke-width="8"
                style="margin-top: 10px"
              />
            </div>
          </div>
        </el-card>

        <!-- 快捷导航 -->
        <el-card class="mt-20">
          <template #header>
            <span>快捷导航</span>
          </template>
          <div class="quick-nav">
            <div class="nav-item" @click="$router.push('/profile/credit')">
              <el-icon><Coin /></el-icon>
              <span>积分详情</span>
            </div>
            <div class="nav-item" @click="$router.push('/borrows')">
              <el-icon><Reading /></el-icon>
              <span>我的借阅</span>
            </div>
            <div class="nav-item" @click="$router.push('/seats')">
              <el-icon><Grid /></el-icon>
              <span>座位预约</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧编辑区域 -->
      <el-col :span="16">
        <!-- 个人信息编辑 -->
        <el-card>
          <template #header>
            <span>个人信息</span>
          </template>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
          >
            <el-form-item label="用户名">
              <el-input :model-value="userInfo.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSubmit">
                {{ loading ? '保存中...' : '保存' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card class="mt-20">
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="100px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">
                {{ pwdLoading ? '修改中...' : '修改密码' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword as apiChangePassword } from '@/api/auth'
import { getCreditInfo } from '@/api/credit'

const userStore = useUserStore()

const formRef = ref(null)
const pwdFormRef = ref(null)
const loading = ref(false)
const pwdLoading = ref(false)

const userInfo = ref({})
const creditInfo = ref({ score: 0, level: '' })

const form = reactive({
  realName: '',
  email: '',
  phone: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const creditPercentage = computed(() => {
  const score = creditInfo.value.score || 0
  return Math.min(Math.max((score / 1000) * 100, 0), 100)
})

const creditColor = computed(() => {
  const score = creditInfo.value.score || 0
  if (score >= 800) return '#67c23a'
  if (score >= 600) return '#409eff'
  if (score >= 400) return '#e6a23c'
  return '#f56c6c'
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
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

onMounted(() => {
  loadUserInfo()
  loadCreditInfo()
})

function loadUserInfo() {
  userInfo.value = userStore.userInfo || {}
  Object.keys(form).forEach(key => {
    form[key] = userInfo.value[key] || ''
  })
}

async function loadCreditInfo() {
  try {
    const res = await getCreditInfo()
    creditInfo.value = res.data || res
  } catch (error) {
    console.error('加载积分信息失败:', error)
  }
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
      // TODO: 调用更新用户信息API
      ElMessage.success('保存成功')
    } catch (error) {
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
      await apiChangePassword({
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
.profile-page {
  padding: 20px;
}

.user-card {
  text-align: center;
  padding: 20px 0;

  .avatar {
    margin-bottom: 15px;
    background: #409eff;
  }

  .username {
    font-size: 18px;
    color: #303133;
    margin-bottom: 8px;
  }

  .credit-section {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;

    .credit-score {
      .score {
        font-size: 36px;
        font-weight: bold;
        color: #409eff;
      }

      .label {
        display: block;
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

.mt-20 {
  margin-top: 20px;
}

.quick-nav {
  .nav-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 0;
    cursor: pointer;
    border-bottom: 1px solid #ebeef5;
    transition: color 0.3s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      color: #409eff;
    }

    .el-icon {
      font-size: 18px;
    }

    span {
      font-size: 14px;
    }
  }
}
</style>
