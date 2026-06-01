<template>
  <div class="page-container">
    <div class="page-header">
      <h2>期刊到刊管理</h2>
      <div>
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>返回订阅列表
        </el-button>
        <el-button
          type="warning"
          @click="handleCheckOverdue"
        >
          <el-icon><AlarmClock /></el-icon>检查逾期
        </el-button>
      </div>
    </div>

    <el-card class="table-card">
      <el-skeleton
        v-if="loading && issueList.length === 0"
        :rows="5"
        animated
      />

      <EmptyState
        v-else-if="!loading && issueList.length === 0"
        description="暂无到刊记录，请先生成预期到刊"
        :show-button="true"
        action-text="刷新"
        @action="loadList"
      />

      <el-table
        v-else
        v-loading="loading"
        :data="issueList"
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
          prop="volume"
          label="卷"
          width="100"
          align="center"
        />
        <el-table-column
          prop="issue"
          label="期"
          width="100"
          align="center"
        />
        <el-table-column
          prop="expectedDate"
          label="预期到刊日期"
          width="130"
          align="center"
        />
        <el-table-column
          prop="receivedDate"
          label="实际到刊日期"
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
            <el-tag :type="statusTagType[row.status]">
              {{ statusMap[row.status] || row.status }}
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
              v-if="row.status === 'EXPECTED'"
              type="success"
              link
              @click="handleReceive(row)"
            >
              <el-icon><Check /></el-icon>到刊登记
            </el-button>
            <el-button
              v-if="row.status === 'EXPECTED'"
              type="danger"
              link
              @click="handleMarkMissing(row)"
            >
              <el-icon><Close /></el-icon>标记缺刊
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="issueList.length > 0"
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
  </div>
</template>

<script setup>
defineOptions({ name: 'SerialIssueList' })

import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getIssueList,
  receiveIssue,
  markIssueMissing,
  checkOverdueIssues
} from '@/api/serial'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const issueList = ref([])
const subscriptionId = ref(route.query.subscriptionId || '')
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const statusMap = {
  EXPECTED: '预期',
  RECEIVED: '已到刊',
  MISSING: '缺刊'
}

const statusTagType = {
  EXPECTED: 'warning',
  RECEIVED: 'success',
  MISSING: 'danger'
}

async function loadList() {
  if (!subscriptionId.value) {
    ElMessage.warning('缺少订阅ID参数')
    return
  }

  loading.value = true
  try {
    const params = {
      subscriptionId: subscriptionId.value,
      current: pagination.current,
      size: pagination.size
    }
    const res = await getIssueList(params)
    if (res.code === 0) {
      issueList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载到刊列表失败:', error)
    ElMessage.error('加载到刊列表失败')
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  loadList()
}

function handleCurrentChange() {
  loadList()
}

function goBack() {
  router.push('/serial/subscriptions')
}

async function handleReceive(row) {
  try {
    const res = await receiveIssue(row.id)
    if (res.code === 0) {
      ElMessage.success('到刊登记成功')
      loadList()
    }
  } catch (error) {
    console.error('到刊登记失败:', error)
    ElMessage.error('到刊登记失败')
  }
}

async function handleMarkMissing(row) {
  try {
    await ElMessageBox.confirm(
      '确定将该期标记为缺刊吗？',
      '缺刊确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await markIssueMissing(row.id)
    if (res.code === 0) {
      ElMessage.success('缺刊标记成功')
      loadList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('标记缺刊失败:', error)
      ElMessage.error('标记缺刊失败')
    }
  }
}

async function handleCheckOverdue() {
  try {
    const res = await checkOverdueIssues()
    if (res.code === 0) {
      ElMessage.success(`检查完成，共标记${res.data}条缺刊`)
      loadList()
    }
  } catch (error) {
    console.error('检查逾期失败:', error)
    ElMessage.error('检查逾期失败')
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

.table-card {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
