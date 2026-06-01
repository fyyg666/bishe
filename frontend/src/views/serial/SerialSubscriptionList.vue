<template>
  <div class="page-container">
    <div class="page-header">
      <h2>期刊订阅管理</h2>
      <el-button
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>新增订阅
      </el-button>
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
              label="全部"
              value=""
            />
            <el-option
              label="订阅中"
              value="ACTIVE"
            />
            <el-option
              label="暂停"
              value="SUSPENDED"
            />
            <el-option
              label="终止"
              value="TERMINATED"
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
        v-if="loading && subscriptionList.length === 0"
        :rows="5"
        animated
      />

      <EmptyState
        v-else-if="!loading && subscriptionList.length === 0"
        description="暂无期刊订阅数据"
        :show-button="true"
        action-text="刷新"
        @action="loadList"
      />

      <el-table
        v-else
        v-loading="loading"
        :data="subscriptionList"
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
          prop="title"
          label="刊名"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="issn"
          label="ISSN"
          width="130"
          align="center"
        />
        <el-table-column
          prop="frequency"
          label="出版频率"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            {{ frequencyMap[row.frequency] || row.frequency }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status]">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="startDate"
          label="开始日期"
          width="110"
          align="center"
        />
        <el-table-column
          prop="endDate"
          label="结束日期"
          width="110"
          align="center"
        />
        <el-table-column
          label="操作"
          width="280"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleViewIssues(row)"
            >
              <el-icon><List /></el-icon>到刊
            </el-button>
            <el-button
              type="success"
              link
              @click="handleGenerate(row)"
            >
              <el-icon><MagicStick /></el-icon>生成预期
            </el-button>
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
        v-if="subscriptionList.length > 0"
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
          label="刊名"
          prop="title"
        >
          <el-input
            v-model="formData.title"
            placeholder="请输入刊名"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item
          label="ISSN"
          prop="issn"
        >
          <el-input
            v-model="formData.issn"
            placeholder="请输入ISSN"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item
          label="出版频率"
          prop="frequency"
        >
          <el-select
            v-model="formData.frequency"
            placeholder="选择出版频率"
          >
            <el-option
              label="日刊"
              value="DAILY"
            />
            <el-option
              label="周刊"
              value="WEEKLY"
            />
            <el-option
              label="双周刊"
              value="BIWEEKLY"
            />
            <el-option
              label="月刊"
              value="MONTHLY"
            />
            <el-option
              label="季刊"
              value="QUARTERLY"
            />
            <el-option
              label="年刊"
              value="YEARLY"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="开始日期"
          prop="startDate"
        >
          <el-date-picker
            v-model="formData.startDate"
            type="date"
            placeholder="选择开始日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item
          label="结束日期"
          prop="endDate"
        >
          <el-date-picker
            v-model="formData.endDate"
            type="date"
            placeholder="选择结束日期"
            value-format="YYYY-MM-DD"
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
              label="订阅中"
              value="ACTIVE"
            />
            <el-option
              label="暂停"
              value="SUSPENDED"
            />
            <el-option
              label="终止"
              value="TERMINATED"
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
defineOptions({ name: 'SerialSubscriptionList' })

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSubscriptionList,
  createSubscription,
  updateSubscription,
  deleteSubscription,
  generateExpectedIssues
} from '@/api/serial'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const subscriptionList = ref([])
const filterForm = reactive({
  status: ''
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增期刊订阅')
const isEdit = ref(false)
const formRef = ref(null)

const formData = reactive({
  id: null,
  title: '',
  issn: '',
  frequency: 'MONTHLY',
  startDate: '',
  endDate: '',
  status: 'ACTIVE'
})

const formRules = {
  title: [
    { required: true, message: '请输入刊名', trigger: 'blur' }
  ],
  frequency: [
    { required: true, message: '请选择出版频率', trigger: 'change' }
  ]
}

const frequencyMap = {
  DAILY: '日刊',
  WEEKLY: '周刊',
  BIWEEKLY: '双周刊',
  MONTHLY: '月刊',
  QUARTERLY: '季刊',
  YEARLY: '年刊'
}

const statusMap = {
  ACTIVE: '订阅中',
  SUSPENDED: '暂停',
  TERMINATED: '终止'
}

const statusTagType = {
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  TERMINATED: 'info'
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...filterForm
    }
    const res = await getSubscriptionList(params)
    if (res.code === 0) {
      subscriptionList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载期刊订阅列表失败:', error)
    ElMessage.error('加载期刊订阅列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadList()
}

function handleReset() {
  filterForm.status = ''
  handleSearch()
}

function handleSizeChange() {
  loadList()
}

function handleCurrentChange() {
  loadList()
}

function handleCreate() {
  dialogTitle.value = '新增期刊订阅'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑期刊订阅'
  isEdit.value = true
  formData.id = row.id
  formData.title = row.title
  formData.issn = row.issn
  formData.frequency = row.frequency
  formData.startDate = row.startDate
  formData.endDate = row.endDate
  formData.status = row.status
  dialogVisible.value = true
}

function handleViewIssues(row) {
  router.push({ path: '/serial/issues', query: { subscriptionId: row.id } })
}

async function handleGenerate(row) {
  try {
    await ElMessageBox.confirm(
      `确定为《${row.title}》生成预期到刊记录吗？将清除已有的"预期"状态记录并重新生成。`,
      '生成确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await generateExpectedIssues(row.id)
    if (res.code === 0) {
      ElMessage.success('预期到刊生成成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('生成预期到刊失败:', error)
      ElMessage.error('生成预期到刊失败')
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除期刊订阅《${row.title}》吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await deleteSubscription(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除期刊订阅失败:', error)
      ElMessage.error('删除期刊订阅失败')
    }
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const res = isEdit.value
      ? await updateSubscription(formData.id, formData)
      : await createSubscription(formData)

    if (res.code === 0) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadList()
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

function resetForm() {
  formData.id = null
  formData.title = ''
  formData.issn = ''
  formData.frequency = 'MONTHLY'
  formData.startDate = ''
  formData.endDate = ''
  formData.status = 'ACTIVE'
  formRef.value?.clearValidate()
}

function handleDialogClose() {
  resetForm()
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
