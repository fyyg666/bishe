<template>
  <div class="page-container">
    <div class="page-header">
      <h2>分馆管理</h2>
      <div class="header-actions">
        <el-radio-group v-model="viewMode" size="default">
          <el-radio-button value="table">列表</el-radio-button>
          <el-radio-button value="tree">树形</el-radio-button>
        </el-radio-group>
        <el-button
          type="primary"
          @click="handleCreate"
        >
          <el-icon><Plus /></el-icon>新建分馆
        </el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form
        :inline="true"
        :model="filterForm"
      >
        <el-form-item label="状态">
          <el-select
            v-model="filterForm.status"
            placeholder="选择状态"
            clearable
          >
            <el-option
              label="启用"
              value="1"
            />
            <el-option
              label="停用"
              value="0"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="loadBranchList"
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
        v-if="loading && branchList.length === 0"
        :rows="5"
        animated
      />

      <EmptyState
        v-else-if="!loading && branchList.length === 0 && viewMode === 'table'"
        description="暂无分馆数据"
        :show-button="true"
        action-text="刷新"
        @action="loadBranchList"
      />

      <el-table
        v-if="viewMode === 'table'"
        v-loading="loading"
        :data="branchList"
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
          label="分馆名称"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="code"
          label="分馆编码"
          width="120"
          align="center"
        />
        <el-table-column
          prop="address"
          label="地址"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="phone"
          label="电话"
          width="130"
          align="center"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="parentName"
          label="上级分馆"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            {{ row.parentName || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="260"
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
              :type="row.status === 1 ? 'info' : 'success'"
              link
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
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

      <el-tree
        v-if="viewMode === 'tree'"
        v-loading="loading"
        :data="treeData"
        :props="treeProps"
        node-key="id"
        default-expand-all
      >
        <template #default="{ data }">
          <div class="tree-node">
            <span class="tree-node-label">{{ data.name }}</span>
            <el-tag
              size="small"
              :type="data.status === 1 ? 'success' : 'info'"
              style="margin-left: 8px"
            >
              {{ data.status === 1 ? '启用' : '停用' }}
            </el-tag>
            <span
              class="tree-node-code"
              style="margin-left: 8px; color: #909399"
            >{{ data.code }}</span>
            <span class="tree-node-actions">
              <el-button
                type="warning"
                link
                size="small"
                @click.stop="handleEdit(data)"
              >编辑</el-button>
              <el-button
                :type="data.status === 1 ? 'info' : 'success'"
                link
                size="small"
                @click.stop="handleToggleStatus(data)"
              >{{ data.status === 1 ? '停用' : '启用' }}</el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click.stop="handleDelete(data)"
              >删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
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
          label="分馆名称"
          prop="name"
        >
          <el-input
            v-model="formData.name"
            placeholder="请输入分馆名称"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item
          label="分馆编码"
          prop="code"
        >
          <el-input
            v-model="formData.code"
            placeholder="请输入分馆编码(如MAIN, EAST)"
            maxlength="50"
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
          label="开放时间"
          prop="openingHours"
        >
          <el-input
            v-model="formData.openingHours"
            placeholder="请输入开放时间描述"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item
          label="上级分馆"
          prop="parentId"
        >
          <el-select
            v-model="formData.parentId"
            placeholder="选择上级分馆"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
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
              :value="1"
            />
            <el-option
              label="停用"
              :value="0"
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
defineOptions({ name: 'BranchList' })

import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listBranches,
  getBranchTree,
  createBranch,
  updateBranch,
  deleteBranch
} from '@/api/branch'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const submitLoading = ref(false)
const branchList = ref([])
const treeData = ref([])
const viewMode = ref('table')

const filterForm = reactive({
  status: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新建分馆')
const isEdit = ref(false)
const formRef = ref(null)

const formData = reactive({
  id: null,
  name: '',
  code: '',
  address: '',
  phone: '',
  email: '',
  openingHours: '',
  status: 1,
  parentId: null
})

const formRules = {
  name: [
    { required: true, message: '请输入分馆名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度为2-100个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入分馆编码', trigger: 'blur' },
    { min: 1, max: 50, message: '编码长度为1-50个字符', trigger: 'blur' }
  ]
}

const treeProps = {
  children: 'children',
  label: 'name'
}

const parentOptions = computed(() => {
  return branchList.value.filter(b => b.id !== formData.id)
})

async function loadBranchList() {
  loading.value = true
  try {
    const params = {}
    if (filterForm.status !== '') {
      params.status = filterForm.status
    }
    const res = await listBranches(params)
    if (res.code === 0) {
      branchList.value = res.data || []
    }
  } catch (error) {
    console.error('加载分馆列表失败:', error)
    ElMessage.error('加载分馆列表失败')
  } finally {
    loading.value = false
  }
}

async function loadBranchTree() {
  loading.value = true
  try {
    const res = await getBranchTree()
    if (res.code === 0) {
      treeData.value = res.data || []
    }
  } catch (error) {
    console.error('加载分馆树失败:', error)
    ElMessage.error('加载分馆树失败')
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filterForm.status = ''
  loadBranchList()
}

function handleCreate() {
  dialogTitle.value = '新建分馆'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑分馆'
  isEdit.value = true
  formData.id = row.id
  formData.name = row.name
  formData.code = row.code
  formData.address = row.address || ''
  formData.phone = row.phone || ''
  formData.email = row.email || ''
  formData.openingHours = row.openingHours || ''
  formData.status = row.status
  formData.parentId = row.parentId
  dialogVisible.value = true
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}分馆《${row.name}》吗？`,
      '确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await updateBranch(row.id, {
      name: row.name,
      code: row.code,
      address: row.address,
      phone: row.phone,
      email: row.email,
      openingHours: row.openingHours,
      status: newStatus,
      parentId: row.parentId
    })
    if (res.code === 0) {
      ElMessage.success(`${action}成功`)
      refreshData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除分馆《${row.name}》吗？删除前请确保该分馆下无关联数据。`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteBranch(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      refreshData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除分馆失败:', error)
      ElMessage.error('删除分馆失败')
    }
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true
    const res = isEdit.value
      ? await updateBranch(formData.id, formData)
      : await createBranch(formData)
    if (res.code === 0) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      refreshData()
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
  formData.code = ''
  formData.address = ''
  formData.phone = ''
  formData.email = ''
  formData.openingHours = ''
  formData.status = 1
  formData.parentId = null
  formRef.value?.clearValidate()
}

function handleDialogClose() {
  resetForm()
}

function refreshData() {
  loadBranchList()
  loadBranchTree()
}

onMounted(() => {
  loadBranchList()
  loadBranchTree()
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

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  font-size: 14px;
}

.tree-node-label {
  font-weight: 500;
}

.tree-node-actions {
  margin-left: auto;
  display: none;
}

.el-tree-node__content:hover .tree-node-actions {
  display: inline-flex;
}
</style>
