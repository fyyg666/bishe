<template>
  <div class="page-container">
    <div class="page-header">
      <h2>催缺管理</h2>
      <el-button
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>新建催缺
      </el-button>
    </div>

    <el-card class="filter-card">
      <el-form
        :inline="true"
        :model="filterForm"
      >
        <el-form-item label="催缺状态">
          <el-select
            v-model="filterForm.claimStatus"
            placeholder="选择状态"
            clearable
          >
            <el-option
              label="全部"
              value=""
            />
            <el-option
              label="待处理"
              value="PENDING"
            />
            <el-option
              label="已发送"
              value="SENT"
            />
            <el-option
              label="已确认"
              value="ACKNOWLEDGED"
            />
            <el-option
              label="已解决"
              value="RESOLVED"
            />
            <el-option
              label="已关闭"
              value="CLOSED"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="催缺类型">
          <el-select
            v-model="filterForm.claimType"
            placeholder="选择类型"
            clearable
          >
            <el-option
              label="全部"
              value=""
            />
            <el-option
              label="缺刊"
              value="MISSING"
            />
            <el-option
              label="破损"
              value="DAMAGED"
            />
            <el-option
              label="错发"
              value="WRONG"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-skeleton
        v-if="loading && claimList.length === 0"
        :rows="5"
        animated
      />

      <EmptyState
        v-else-if="!loading && claimList.length === 0"
        description="暂无催缺记录"
        :show-button="true"
        action-text="刷新"
        @action="loadList"
      />

      <el-table
        v-else
        v-loading="loading"
        :data="claimList"
        stripe
        style="width: 100%"
      >
        <el-table-column
          prop="claimNumber"
          label="催缺单号"
          width="180"
          align="center"
        />
        <el-table-column
          prop="subscriptionTitle"
          label="刊名"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          label="卷/期"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            {{ row.volume || '-' }}/{{ row.issue || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="claimType"
          label="催缺类型"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            {{ claimTypeMap[row.claimType] || row.claimType }}
          </template>
        </el-table-column>
        <el-table-column
          prop="claimStatus"
          label="催缺状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.claimStatus]">
              {{ statusMap[row.claimStatus] || row.claimStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="claimDate"
          label="催缺日期"
          width="110"
          align="center"
        />
        <el-table-column
          prop="vendorName"
          label="供应商"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          label="操作"
          width="200"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.claimStatus === 'PENDING' || row.claimStatus === 'SENT' || row.claimStatus === 'ACKNOWLEDGED'"
              type="success"
              link
              @click="handleResolve(row)"
            >
              <el-icon><Check /></el-icon>处理
            </el-button>
            <el-button
              v-if="row.claimStatus !== 'CLOSED'"
              type="info"
              link
              @click="handleClose(row)"
            >
              <el-icon><Close /></el-icon>关闭
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="claimList.length > 0"
        class="pagination"
      >
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

    <el-dialog
      v-model="createDialogVisible"
      title="新建催缺"
      width="600px"
      @close="handleCreateDialogClose"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createFormRules"
        label-width="100px"
      >
        <el-form-item
          label="订阅ID"
          prop="subscriptionId"
        >
          <el-input
            v-model.number="createForm.subscriptionId"
            placeholder="请输入订阅ID"
          />
        </el-form-item>
        <el-form-item
          label="到刊记录ID"
          prop="issueId"
        >
          <el-input
            v-model.number="createForm.issueId"
            placeholder="请输入到刊记录ID（可选）"
          />
        </el-form-item>
        <el-form-item
          label="供应商ID"
          prop="vendorId"
        >
          <el-input
            v-model.number="createForm.vendorId"
            placeholder="请输入供应商ID（可选）"
          />
        </el-form-item>
        <el-form-item
          label="催缺类型"
          prop="claimType"
        >
          <el-select
            v-model="createForm.claimType"
            placeholder="选择催缺类型"
          >
            <el-option
              label="缺刊"
              value="MISSING"
            />
            <el-option
              label="破损"
              value="DAMAGED"
            />
            <el-option
              label="错发"
              value="WRONG"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="催缺日期"
          prop="claimDate"
        >
          <el-date-picker
            v-model="createForm.claimDate"
            type="date"
            placeholder="选择催缺日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item
          label="问题描述"
          prop="description"
        >
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入问题描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleCreateSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resolveDialogVisible"
      title="处理催缺"
      width="500px"
    >
      <el-form
        ref="resolveFormRef"
        :model="resolveForm"
        :rules="resolveFormRules"
        label-width="100px"
      >
        <el-form-item
          label="处理结果"
          prop="resolution"
        >
          <el-input
            v-model="resolveForm.resolution"
            type="textarea"
            :rows="4"
            placeholder="请输入处理结果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleResolveSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'SerialClaimList' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listClaims,
  createClaim,
  resolveClaim,
  closeClaim
} from '@/api/serial'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const submitLoading = ref(false)
const claimList = ref([])
const filterForm = reactive({
  claimStatus: '',
  claimType: ''
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  subscriptionId: null,
  issueId: null,
  vendorId: null,
  claimType: 'MISSING',
  claimDate: '',
  description: ''
})
const createFormRules = {
  subscriptionId: [
    { required: true, message: '请输入订阅ID', trigger: 'blur' }
  ],
  claimType: [
    { required: true, message: '请选择催缺类型', trigger: 'change' }
  ],
  claimDate: [
    { required: true, message: '请选择催缺日期', trigger: 'change' }
  ]
}

const resolveDialogVisible = ref(false)
const resolveFormRef = ref(null)
const resolveForm = reactive({
  id: null,
  resolution: ''
})
const resolveFormRules = {
  resolution: [
    { required: true, message: '请输入处理结果', trigger: 'blur' }
  ]
}

const claimTypeMap = {
  MISSING: '缺刊',
  DAMAGED: '破损',
  WRONG: '错发'
}

const statusMap = {
  PENDING: '待处理',
  SENT: '已发送',
  ACKNOWLEDGED: '已确认',
  RESOLVED: '已解决',
  CLOSED: '已关闭'
}

const statusTagType = {
  PENDING: 'warning',
  SENT: 'primary',
  ACKNOWLEDGED: 'info',
  RESOLVED: 'success',
  CLOSED: 'info'
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...filterForm
    }
    const res = await listClaims(params)
    if (res.code === 0) {
      claimList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载催缺列表失败:', error)
    ElMessage.error('加载催缺列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadList()
}

function handleReset() {
  filterForm.claimStatus = ''
  filterForm.claimType = ''
  handleSearch()
}

function handleSizeChange() {
  loadList()
}

function handleCurrentChange() {
  loadList()
}

function handleCreate() {
  resetCreateForm()
  createDialogVisible.value = true
}

function resetCreateForm() {
  createForm.subscriptionId = null
  createForm.issueId = null
  createForm.vendorId = null
  createForm.claimType = 'MISSING'
  createForm.claimDate = ''
  createForm.description = ''
  createFormRef.value?.clearValidate()
}

function handleCreateDialogClose() {
  resetCreateForm()
}

async function handleCreateSubmit() {
  if (!createFormRef.value) return

  try {
    await createFormRef.value.validate()
    submitLoading.value = true

    const res = await createClaim(createForm)
    if (res.code === 0) {
      ElMessage.success('催缺记录创建成功')
      createDialogVisible.value = false
      loadList()
    }
  } catch (error) {
    console.error('创建催缺记录失败:', error)
  } finally {
    submitLoading.value = false
  }
}

function handleResolve(row) {
  resolveForm.id = row.id
  resolveForm.resolution = ''
  resolveDialogVisible.value = true
}

async function handleResolveSubmit() {
  if (!resolveFormRef.value) return

  try {
    await resolveFormRef.value.validate()
    submitLoading.value = true

    const res = await resolveClaim(resolveForm.id, { resolution: resolveForm.resolution })
    if (res.code === 0) {
      ElMessage.success('催缺记录处理成功')
      resolveDialogVisible.value = false
      loadList()
    }
  } catch (error) {
    console.error('处理催缺记录失败:', error)
  } finally {
    submitLoading.value = false
  }
}

async function handleClose(row) {
  try {
    await ElMessageBox.confirm(
      `确定要关闭催缺单 ${row.claimNumber} 吗？`,
      '关闭确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await closeClaim(row.id)
    if (res.code === 0) {
      ElMessage.success('催缺记录已关闭')
      loadList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭催缺记录失败:', error)
      ElMessage.error('关闭催缺记录失败')
    }
  }
}

onMounted(() => {
  loadList()
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
