<template>
  <div class="profile">
    <el-row :gutter="20">
      <!-- 用户信息卡片 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>个人信息</span>
          </template>
          <div class="user-info">
            <el-avatar :size="80" icon="UserFilled" class="avatar" />
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
      </el-col>
      
      <!-- 编辑信息 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>编辑信息</span>
          </template>
          
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
          >
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword as apiChangePassword } from '@/api/auth'
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

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
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
  
  .mt-20 {
    margin-top: 20px;
  }
}
</style>
