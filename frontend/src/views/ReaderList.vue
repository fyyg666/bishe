<template>
  <div class="page-container">
    <div class="page-header">
      <h2>读者管理</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>注册读者
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索姓名/用户名/手机/卡号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filterForm.role" placeholder="选择角色" clearable>
            <el-option label="全部" value="" />
            <el-option label="普通读者" value="READER" />
            <el-option label="志愿者" value="VOLUNTEER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="选择状态" clearable>
            <el-option label="全部" value="" />
            <el-option label="正常" value="NORMAL" />
            <el-option label="禁用" value="DISABLED" />
            <el-option label="锁定" value="LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <!-- 骨架屏加载状态 -->
      <el-skeleton v-if="loading && readerList.length === 0" :rows="5" animated />
      
      <!-- 空状态 -->
      <EmptyState 
        v-else-if="!loading && readerList.length === 0" 
        description="暂无读者数据"
        :show-button="true"
        action-text="刷新"
        @action="loadReaderList"
      />
      
      <!-- 数据表格 -->
      <el-table v-else v-loading="loading" :data="readerList" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" width="120" align="center" />
        <el-table-column prop="realName" label="姓名" width="100" align="center" />
        <el-table-column prop="cardNumber" label="读者卡号" width="150" align="center" />
        <el-table-column prop="phone" label="手机号" width="130" align="center" />
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'VOLUNTEER' ? 'success' : 'primary'" size="small">
              {{ row.role === 'VOLUNTEER' ? '志愿者' : '读者' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="积分" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getCreditTagType(row.creditScore)">{{ row.creditScore }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="borrowCount" label="借阅数" width="80" align="center">
          <template #default="{ row }">
            {{ row.borrowCount || 0 }}/{{ row.maxBorrowCount || 5 }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
            <el-button type="warning" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button
              v-if="row.status === 'NORMAL'"
              type="danger"
              link
              @click="handleDisable(row)"
            >
              <el-icon><Close /></el-icon>禁用
            </el-button>
            <el-button
              v-else
              type="success"
              link
              @click="handleEnable(row)"
            >
              <el-icon><Check /></el-icon>启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div v-if="readerList.length > 0" class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <template v-if="!isEdit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="formData.confirmPassword" type="password" placeholder="请确认密码" show-password />
          </el-form-item>
        </template>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <template v-if="isAdmin && isEdit">
          <el-form-item label="角色" prop="role">
            <el-select v-model="formData.role" placeholder="选择角色">
              <el-option label="普通读者" value="READER" />
              <el-option label="志愿者" value="VOLUNTEER" />
            </el-select>
          </el-form-item>
          <el-form-item label="积分" prop="creditScore">
            <el-input-number v-model="formData.creditScore" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="最大借阅数" prop="maxBorrowCount">
            <el-input-number v-model="formData.maxBorrowCount" :min="1" :max="20" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="读者详情" width="600px">
      <el-descriptions v-if="currentReader" :column="2" border>
        <el-descriptions-item label="ID">{{ currentReader.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentReader.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ currentReader.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="读者卡号">{{ currentReader.cardNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentReader.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentReader.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="currentReader.role === 'VOLUNTEER' ? 'success' : 'primary'" size="small">
            {{ currentReader.role === 'VOLUNTEER' ? '志愿者' : '读者' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentReader.status)" size="small">
            {{ getStatusName(currentReader.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="积分">
          <el-tag :type="getCreditTagType(currentReader.creditScore)">
            {{ currentReader.creditScore }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前借阅">
          {{ currentReader.borrowCount || 0 }}/{{ currentReader.maxBorrowCount || 5 }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(currentReader.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import {
  getReaderList,
  getReaderDetail,
  registerReader,
  updateReader,
  updateReaderStatus,
  resetPassword
} from '@/api/reader'
import EmptyState from '@/components/EmptyState.vue'

// 状态
const loading = ref(false)
const submitLoading = ref(false)
const readerList = ref([])

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 筛选
const filterForm = reactive({
  keyword: '',
  role: '',
  status: ''
})

// 对话框
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('注册读者')
const isEdit = ref(false)
const currentReader = ref(null)
const formRef = ref(null)

const formData = reactive({
  id: null,
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  role: 'READER',
  creditScore: 100,
  maxBorrowCount: 5
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== formData.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[\w.-]+@[\w.-]+\.\w+$/, message: '请输入正确的邮箱', trigger: 'blur' }
  ]
}

// 用户状态
const userStore = useUserStore()
const isAdmin = computed(() => {
  return userStore.userInfo?.role === 'ADMIN' || userStore.userInfo?.role === 'LIBRARIAN'
})

// 获取积分标签类型
function getCreditTagType(credit) {
  if (credit >= 80) return 'success'
  if (credit >= 60) return 'warning'
  return 'danger'
}

// 获取状态名称
function getStatusName(status) {
  const map = {
    NORMAL: '正常',
    DISABLED: '禁用',
    LOCKED: '锁定'
  }
  return map[status] || status
}

// 获取状态标签类型
function getStatusTagType(status) {
  const map = {
    NORMAL: 'success',
    DISABLED: 'danger',
    LOCKED: 'warning'
  }
  return map[status] || 'info'
}

// 格式化日期
function formatDate(date) {
  if (!date) return '-'
  return date.substring(0, 10)
}

// 加载读者列表
async function loadReaderList() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...filterForm
    }
    // 如果不是管理员，不传状态筛选
    if (!isAdmin.value) {
      delete params.status
    }
    
    const res = await getReaderList(params)
    if (res.code === 0) {
      readerList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载读者列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1
  loadReaderList()
}

// 重置
function handleReset() {
  filterForm.keyword = ''
  filterForm.role = ''
  filterForm.status = ''
  handleSearch()
}

// 分页大小改变
function handleSizeChange() {
  loadReaderList()
}

// 页码改变
function handleCurrentChange() {
  loadReaderList()
}

// 创建
function handleCreate() {
  dialogTitle.value = '注册读者'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
function handleEdit(row) {
  dialogTitle.value = '编辑读者'
  isEdit.value = true
  formData.id = row.id
  formData.username = row.username
  formData.realName = row.realName
  formData.phone = row.phone
  formData.email = row.email
  formData.role = row.role
  formData.creditScore = row.creditScore
  formData.maxBorrowCount = row.maxBorrowCount
  dialogVisible.value = true
}

// 查看详情
async function handleView(row) {
  try {
    const res = await getReaderDetail(row.id)
    if (res.code === 0) {
      currentReader.value = res.data
      viewDialogVisible.value = true
    }
  } catch (error) {
    console.error('加载读者详情失败:', error)
  }
}

// 禁用
async function handleDisable(row) {
  try {
    await ElMessageBox.confirm(
      `确定要禁用读者【${row.realName || row.username}】吗？`,
      '禁用确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    
    const res = await updateReaderStatus(row.id, true)
    if (res.code === 0) {
      ElMessage.success('已禁用')
      loadReaderList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('禁用失败:', error)
    }
  }
}

// 启用
async function handleEnable(row) {
  try {
    await ElMessageBox.confirm(
      `确定要启用读者【${row.realName || row.username}】吗？`,
      '启用确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    
    const res = await updateReaderStatus(row.id, false)
    if (res.code === 0) {
      ElMessage.success('已启用')
      loadReaderList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('启用失败:', error)
    }
  }
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    if (isEdit.value) {
      // 编辑
      const updateData = {
        realName: formData.realName,
        phone: formData.phone,
        email: formData.email
      }
      if (isAdmin.value) {
        updateData.role = formData.role
        updateData.creditScore = formData.creditScore
        updateData.maxBorrowCount = formData.maxBorrowCount
      }
      const res = await updateReader(formData.id, updateData)
      if (res.code === 0) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadReaderList()
      }
    } else {
      // 创建
      const res = await registerReader({
        username: formData.username,
        password: formData.password,
        realName: formData.realName,
        phone: formData.phone,
        email: formData.email
      })
      if (res.code === 0) {
        ElMessage.success('注册成功')
        dialogVisible.value = false
        loadReaderList()
      }
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
function resetForm() {
  formData.id = null
  formData.username = ''
  formData.password = ''
  formData.confirmPassword = ''
  formData.realName = ''
  formData.phone = ''
  formData.email = ''
  formData.role = 'READER'
  formData.creditScore = 100
  formData.maxBorrowCount = 5
  formRef.value?.clearValidate()
}

// 对话框关闭
function handleDialogClose() {
  resetForm()
}

// 生命周期钩子
onMounted(() => {
  loadReaderList()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
  }
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
