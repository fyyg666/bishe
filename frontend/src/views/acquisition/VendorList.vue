<template>
  <div class="page-container">
    <div class="page-header">
      <h2>供应商管理</h2>
      <el-button
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>新增供应商
      </el-button>
    </div>

    <el-card class="filter-card">
      <el-form
        :inline="true"
        :model="filterForm"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索名称/联系人"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filterForm.status"
            placeholder="选择状态"
            clearable
          >
            <el-option
              label="全部"
              value=""
            />
            <el-option
              label="启用"
              value="ACTIVE"
            />
            <el-option
              label="停用"
              value="INACTIVE"
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
        v-if="loading && vendorList.length === 0"
        :rows="5"
        animated
      />

      <EmptyState
        v-else-if="!loading && vendorList.length === 0"
        description="暂无供应商数据"
        :show-button="true"
        action-text="刷新"
        @action="loadVendorList"
      />

      <el-table
        v-else
        v-loading="loading"
        :data="vendorList"
        stripe
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
          align="center"
        />
        <el-table-column
          prop="name"
          label="供应商名称"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="contact"
          label="联系人"
          width="100"
          align="center"
        />
        <el-table-column
          prop="phone"
          label="电话"
          width="130"
          align="center"
        />
        <el-table-column
          prop="email"
          label="邮箱"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="200"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="warning"
              link
              @click="handleEdit(row)"
            >
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
            >
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="vendorList.length > 0"
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
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="名称"
          prop="name"
        >
          <el-input
            v-model="formData.name"
            placeholder="请输入供应商名称"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item
          label="联系人"
          prop="contact"
        >
          <el-input
            v-model="formData.contact"
            placeholder="请输入联系人"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item
          label="电话"
          prop="phone"
        >
          <el-input
            v-model="formData.phone"
            placeholder="请输入电话"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item
          label="邮箱"
          prop="email"
        >
          <el-input
            v-model="formData.email"
            placeholder="请输入邮箱"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item
          label="地址"
          prop="address"
        >
          <el-input
            v-model="formData.address"
            placeholder="请输入地址"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-select
            v-model="formData.status"
            placeholder="选择状态"
          >
            <el-option
              label="启用"
              value="ACTIVE"
            />
            <el-option
              label="停用"
              value="INACTIVE"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'VendorList' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getVendorList,
  createVendor,
  updateVendor,
  deleteVendor
} from '@/api/vendor'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const submitLoading = ref(false)
const vendorList = ref([])
const filterForm = reactive({
  keyword: '',
  status: ''
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增供应商')
const isEdit = ref(false)
const formRef = ref(null)

const formData = reactive({
  id: null,
  name: '',
  contact: '',
  phone: '',
  email: '',
  address: '',
  status: 'ACTIVE'
})

const formRules = {
  name: [
    { required: true, message: '请输入供应商名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度为2-100个字符', trigger: 'blur' }
  ]
}

async function loadVendorList() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...filterForm
    }
    const res = await getVendorList(params)
    if (res.code === 0) {
      vendorList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载供应商列表失败:', error)
    ElMessage.error('加载供应商列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadVendorList()
}

function handleReset() {
  filterForm.keyword = ''
  filterForm.status = ''
  handleSearch()
}

function handleSizeChange() {
  loadVendorList()
}

function handleCurrentChange() {
  loadVendorList()
}

function handleCreate() {
  dialogTitle.value = '新增供应商'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑供应商'
  isEdit.value = true
  formData.id = row.id
  formData.name = row.name
  formData.contact = row.contact
  formData.phone = row.phone
  formData.email = row.email
  formData.address = row.address
  formData.status = row.status
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除供应商《${row.name}》吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await deleteVendor(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadVendorList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除供应商失败:', error)
      ElMessage.error('删除供应商失败')
    }
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const res = isEdit.value
      ? await updateVendor(formData.id, formData)
      : await createVendor(formData)

    if (res.code === 0) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadVendorList()
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

function resetForm() {
  formData.id = null
  formData.name = ''
  formData.contact = ''
  formData.phone = ''
  formData.email = ''
  formData.address = ''
  formData.status = 'ACTIVE'
  formRef.value?.clearValidate()
}

function handleDialogClose() {
  resetForm()
}

onMounted(() => {
  loadVendorList()
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
