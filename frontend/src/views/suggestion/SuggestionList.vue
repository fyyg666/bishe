<template>
  <div class="suggestion-page">
    <el-card class="page-header">
      <div class="header-content">
        <h2>荐购管理</h2>
        <div>
          <el-select
            v-model="statusFilter"
            placeholder="状态筛选"
            clearable
            class="status-filter-select"
            @change="fetchList"
          >
            <el-option
              label="待审核"
              value="PENDING"
            />
            <el-option
              label="已批准"
              value="APPROVED"
            />
            <el-option
              label="已拒绝"
              value="REJECTED"
            />
          </el-select>
          <el-button
            type="primary"
            @click="showCreateDialog = true"
          >
            提交荐购
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="page-body">
      <el-table
        v-loading="loading"
        :data="suggestionList"
        stripe
      >
        <el-table-column
          prop="username"
          label="荐购人"
          width="120"
        />
        <el-table-column
          prop="title"
          label="书名"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="author"
          label="作者"
          width="120"
        />
        <el-table-column
          prop="isbn"
          label="ISBN"
          width="150"
        />
        <el-table-column
          prop="reason"
          label="荐购理由"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="statusDesc"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="reviewerName"
          label="审核人"
          width="100"
        />
        <el-table-column
          prop="reviewRemark"
          label="审核备注"
          min-width="120"
          show-overflow-tooltip
        />
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="success"
              @click="handleApprove(row)"
            >
              批准
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="danger"
              @click="handleReject(row)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        class="pagination"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 提交荐购对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="提交荐购建议"
      width="500px"
    >
      <el-form
        :model="createForm"
        label-width="100px"
      >
        <el-form-item label="书名">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="createForm.author" />
        </el-form-item>
        <el-form-item label="ISBN">
          <el-input v-model="createForm.isbn" />
        </el-form-item>
        <el-form-item label="荐购理由">
          <el-input
            v-model="createForm.reason"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="creating"
          @click="handleCreate"
        >
          提交
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="showReviewDialog"
      :title="reviewAction === 'approve' ? '批准荐购' : '拒绝荐购'"
      width="450px"
    >
      <el-form
        :model="reviewForm"
        label-width="100px"
      >
        <el-form-item label="书名">
          {{ currentSuggestion?.title }}
        </el-form-item>
        <el-form-item label="荐购人">
          {{ currentSuggestion?.username }}
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="reviewForm.remark"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">
          取消
        </el-button>
        <el-button
          :type="reviewAction === 'approve' ? 'success' : 'danger'"
          :loading="reviewing"
          @click="handleReview"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listSuggestions, createSuggestion, approveSuggestion, rejectSuggestion
} from '@/api/suggestion'

const loading = ref(false)
const creating = ref(false)
const reviewing = ref(false)
const suggestionList = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')
const showCreateDialog = ref(false)
const showReviewDialog = ref(false)
const reviewAction = ref('')
const currentSuggestion = ref(null)

const createForm = ref({ title: '', author: '', isbn: '', reason: '' })
const reviewForm = ref({ remark: '' })

async function fetchList() {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await listSuggestions(params)
    suggestionList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleCreate() {
  creating.value = true
  try {
    await createSuggestion(createForm.value)
    ElMessage.success('荐购建议提交成功')
    showCreateDialog.value = false
    createForm.value = { title: '', author: '', isbn: '', reason: '' }
    fetchList()
  } catch (e) { ElMessage.error(e.message) }
  finally { creating.value = false }
}

function handleApprove(row) {
  currentSuggestion.value = row
  reviewAction.value = 'approve'
  reviewForm.value = { remark: '' }
  showReviewDialog.value = true
}

function handleReject(row) {
  currentSuggestion.value = row
  reviewAction.value = 'reject'
  reviewForm.value = { remark: '' }
  showReviewDialog.value = true
}

async function handleReview() {
  if (!currentSuggestion.value) return
  reviewing.value = true
  try {
    const id = currentSuggestion.value.id
    if (reviewAction.value === 'approve') {
      await approveSuggestion(id, reviewForm.value.remark)
      ElMessage.success('已批准')
    } else {
      await rejectSuggestion(id, reviewForm.value.remark)
      ElMessage.success('已拒绝')
    }
    showReviewDialog.value = false
    fetchList()
  } catch (e) { ElMessage.error(e.message) }
  finally { reviewing.value = false }
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.suggestion-page {
  .page-header { margin-bottom: 16px; }
  .header-content {
    display: flex; justify-content: space-between; align-items: center;
    h2 { margin: 0; }
  }
  .status-filter-select { width: 120px; margin-right: 12px; }
  .pagination { margin-top: 16px; text-align: right; }
}

@include mobile {
  .suggestion-page {
    .header-content {
      flex-direction: column;
      gap: $space-3;
      align-items: flex-start;
    }
  }
  :deep(.el-table) {
    overflow-x: auto;
  }
}
</style>
