<template>
  <div class="page-container">
    <div class="page-header">
      <h2>借阅规则管理</h2>
      <el-button
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>新增规则
      </el-button>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="rules"
        stripe
      >
        <el-table-column
          prop="readerType"
          label="读者类型"
          width="120"
        >
          <template #default="{ row }">
            <el-tag :type="getReaderTypeTag(row.readerType)">
              {{ getReaderTypeLabel(row.readerType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="bookType"
          label="图书类型"
          width="120"
        >
          <template #default="{ row }">
            <el-tag :type="row.bookType === 'NORMAL' ? '' : 'warning'">
              {{ row.bookType === 'NORMAL' ? '普通' : '参考' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="maxBorrow"
          label="最大借阅数"
          width="110"
        />
        <el-table-column
          prop="maxDays"
          label="最大天数"
          width="100"
        />
        <el-table-column
          prop="maxRenew"
          label="最大续借次数"
          width="120"
        />
        <el-table-column
          prop="renewDays"
          label="续借天数"
          width="100"
        />
        <el-table-column
          prop="finePerDay"
          label="每日罚款"
          width="100"
        >
          <template #default="{ row }">
            ¥{{ row.finePerDay }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑规则' : '新增规则'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item
          label="读者类型"
          prop="readerType"
        >
          <el-select
            v-model="form.readerType"
            placeholder="请选择读者类型"
            :disabled="isEdit"
          >
            <el-option
              label="普通读者"
              value="READER"
            />
            <el-option
              label="图书管理员"
              value="LIBRARIAN"
            />
            <el-option
              label="系统管理员"
              value="ADMIN"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="图书类型"
          prop="bookType"
        >
          <el-select
            v-model="form.bookType"
            placeholder="请选择图书类型"
            :disabled="isEdit"
          >
            <el-option
              label="普通"
              value="NORMAL"
            />
            <el-option
              label="参考"
              value="REFERENCE"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="最大借阅数"
          prop="maxBorrow"
        >
          <el-input-number
            v-model="form.maxBorrow"
            :min="1"
            :max="50"
          />
        </el-form-item>
        <el-form-item
          label="最大借阅天数"
          prop="maxDays"
        >
          <el-input-number
            v-model="form.maxDays"
            :min="1"
            :max="365"
          />
        </el-form-item>
        <el-form-item
          label="最大续借次数"
          prop="maxRenew"
        >
          <el-input-number
            v-model="form.maxRenew"
            :min="0"
            :max="10"
          />
        </el-form-item>
        <el-form-item
          label="续借天数"
          prop="renewDays"
        >
          <el-input-number
            v-model="form.renewDays"
            :min="0"
            :max="180"
          />
        </el-form-item>
        <el-form-item
          label="每日罚款(元)"
          prop="finePerDay"
        >
          <el-input-number
            v-model="form.finePerDay"
            :min="0"
            :max="10"
            :step="0.1"
            :precision="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'BorrowRuleList' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getBorrowRules,
  createBorrowRule,
  updateBorrowRule,
  deleteBorrowRule
} from '@/api/borrowRule'

const loading = ref(false)
const rules = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  readerType: '',
  bookType: '',
  maxBorrow: 5,
  maxDays: 30,
  maxRenew: 1,
  renewDays: 15,
  finePerDay: 0.10
})

const formRules = {
  readerType: [{ required: true, message: '请选择读者类型', trigger: 'change' }],
  bookType: [{ required: true, message: '请选择图书类型', trigger: 'change' }],
  maxBorrow: [{ required: true, message: '请输入最大借阅数', trigger: 'blur' }],
  maxDays: [{ required: true, message: '请输入最大借阅天数', trigger: 'blur' }],
  maxRenew: [{ required: true, message: '请输入最大续借次数', trigger: 'blur' }],
  renewDays: [{ required: true, message: '请输入续借天数', trigger: 'blur' }],
  finePerDay: [{ required: true, message: '请输入每日罚款金额', trigger: 'blur' }]
}

onMounted(() => {
  loadRules()
})

async function loadRules() {
  loading.value = true
  try {
    const res = await getBorrowRules()
    rules.value = res.data || []
  } catch {
    ElMessage.error('加载借阅规则失败')
  } finally {
    loading.value = false
  }
}

function getReaderTypeLabel(type) {
  const map = { READER: '普通读者', LIBRARIAN: '图书管理员', ADMIN: '系统管理员' }
  return map[type] || type
}

function getReaderTypeTag(type) {
  const map = { READER: '', LIBRARIAN: 'success', ADMIN: 'danger' }
  return map[type] || 'info'
}

function handleCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, {
    readerType: '', bookType: '', maxBorrow: 5, maxDays: 30,
    maxRenew: 1, renewDays: 15, finePerDay: 0.10
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, {
    readerType: row.readerType, bookType: row.bookType,
    maxBorrow: row.maxBorrow, maxDays: row.maxDays,
    maxRenew: row.maxRenew, renewDays: row.renewDays,
    finePerDay: row.finePerDay
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (isEdit.value) {
      await updateBorrowRule(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createBorrowRule(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRules()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除该规则（${getReaderTypeLabel(row.readerType)} - ${row.bookType === 'NORMAL' ? '普通' : '参考'}）？`,
      '提示',
      { type: 'warning' }
    )
    await deleteBorrowRule(row.id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
    }
  }
}
</style>
