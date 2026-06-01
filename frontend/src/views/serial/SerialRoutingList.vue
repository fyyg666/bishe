<template>
  <div class="page-container">
    <div class="page-header">
      <h2>期刊路由分发</h2>
      <div>
        <el-button
          type="primary"
          @click="handleCreateRouting"
        >
          <el-icon><Plus /></el-icon>新建分发
        </el-button>
        <el-button
          type="success"
          @click="handleBatchCreate"
        >
          <el-icon><CopyDocument /></el-icon>批量分发
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="分发记录" name="routings">
        <el-card class="filter-card">
          <el-form
            :inline="true"
            :model="filterForm"
          >
            <el-form-item label="分发状态">
              <el-select
                v-model="filterForm.routingStatus"
                placeholder="选择状态"
                clearable
              >
                <el-option
                  label="全部"
                  value=""
                />
                <el-option
                  label="待分发"
                  value="PENDING"
                />
                <el-option
                  label="运送中"
                  value="IN_TRANSIT"
                />
                <el-option
                  label="已送达"
                  value="DELIVERED"
                />
                <el-option
                  label="已退回"
                  value="RETURNED"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="分发目标">
              <el-input
                v-model="filterForm.destination"
                placeholder="搜索分发目标"
                clearable
              />
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
            v-if="loading && routingList.length === 0"
            :rows="5"
            animated
          />

          <EmptyState
            v-else-if="!loading && routingList.length === 0"
            description="暂无路由分发记录"
            :show-button="true"
            action-text="刷新"
            @action="loadRoutings"
          />

          <el-table
            v-else
            v-loading="loading"
            :data="routingList"
            stripe
            style="width: 100%"
          >
            <el-table-column
              prop="subscriptionTitle"
              label="刊名"
              min-width="150"
              show-overflow-tooltip
            />
            <el-table-column
              label="卷/期"
              width="120"
              align="center"
            >
              <template #default="{ row }">
                {{ row.issueVolume || '-' }}/{{ row.issueIssue || '-' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="destination"
              label="分发目标"
              min-width="120"
              show-overflow-tooltip
            />
            <el-table-column
              prop="copies"
              label="份数"
              width="80"
              align="center"
            />
            <el-table-column
              prop="routingOrder"
              label="顺序"
              width="80"
              align="center"
            />
            <el-table-column
              prop="routingStatus"
              label="分发状态"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag :type="statusTagType[row.routingStatus]">
                  {{ statusMap[row.routingStatus] || row.routingStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="sentDate"
              label="发出日期"
              width="110"
              align="center"
            />
            <el-table-column
              prop="receivedBy"
              label="签收人"
              width="100"
              align="center"
            />
            <el-table-column
              prop="receivedDate"
              label="签收日期"
              width="110"
              align="center"
            />
            <el-table-column
              label="操作"
              width="220"
              align="center"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  v-if="row.routingStatus === 'PENDING'"
                  type="primary"
                  link
                  @click="handleSend(row)"
                >
                  <el-icon><Van /></el-icon>发出
                </el-button>
                <el-button
                  v-if="row.routingStatus === 'IN_TRANSIT'"
                  type="success"
                  link
                  @click="handleDeliver(row)"
                >
                  <el-icon><Check /></el-icon>签收
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
            v-if="routingList.length > 0"
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
      </el-tab-pane>

      <el-tab-pane label="分发模板" name="templates">
        <el-card class="table-card">
          <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
            <el-select
              v-model="templateSubscriptionId"
              placeholder="选择订阅以查看模板"
              clearable
              filterable
              @change="loadTemplates"
              style="width: 300px;"
            >
              <el-option
                v-for="sub in subscriptionOptions"
                :key="sub.id"
                :label="sub.title"
                :value="sub.id"
              />
            </el-select>
            <el-button
              type="primary"
              :disabled="!templateSubscriptionId"
              @click="handleCreateTemplate"
            >
              <el-icon><Plus /></el-icon>新增模板
            </el-button>
          </div>

          <el-table
            v-loading="templateLoading"
            :data="templateList"
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
              prop="destination"
              label="分发目标"
              min-width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="copies"
              label="份数"
              width="80"
              align="center"
            />
            <el-table-column
              prop="routingOrder"
              label="顺序"
              width="80"
              align="center"
            />
            <el-table-column
              label="操作"
              width="120"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  type="danger"
                  link
                  @click="handleDeleteTemplate(row)"
                >
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="createDialogVisible"
      title="新建分发"
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
          label="订阅"
          prop="subscriptionId"
        >
          <el-select
            v-model="createForm.subscriptionId"
            placeholder="选择订阅"
            filterable
            style="width: 100%;"
          >
            <el-option
              v-for="sub in subscriptionOptions"
              :key="sub.id"
              :label="sub.title"
              :value="sub.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="到刊记录"
          prop="issueId"
        >
          <el-select
            v-model="createForm.issueId"
            placeholder="选择到刊记录"
            filterable
            style="width: 100%;"
          >
            <el-option
              v-for="issue in issueOptions"
              :key="issue.id"
              :label="`卷${issue.volume || '-'} 期${issue.issue || '-'}`"
              :value="issue.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="分发目标"
          prop="destination"
        >
          <el-input
            v-model="createForm.destination"
            placeholder="请输入分发目标"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item
          label="份数"
          prop="copies"
        >
          <el-input-number
            v-model="createForm.copies"
            :min="1"
            :max="100"
          />
        </el-form-item>
        <el-form-item
          label="顺序"
          prop="routingOrder"
        >
          <el-input-number
            v-model="createForm.routingOrder"
            :min="1"
            :max="100"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createForm.notes"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
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
          @click="handleSubmitCreate"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="batchDialogVisible"
      title="批量分发"
      width="500px"
      @close="handleBatchDialogClose"
    >
      <el-form
        ref="batchFormRef"
        :model="batchForm"
        :rules="batchFormRules"
        label-width="100px"
      >
        <el-form-item
          label="订阅"
          prop="subscriptionId"
        >
          <el-select
            v-model="batchForm.subscriptionId"
            placeholder="选择订阅"
            filterable
            style="width: 100%;"
            @change="handleBatchSubscriptionChange"
          >
            <el-option
              v-for="sub in subscriptionOptions"
              :key="sub.id"
              :label="sub.title"
              :value="sub.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="到刊记录"
          prop="issueId"
        >
          <el-select
            v-model="batchForm.issueId"
            placeholder="选择到刊记录"
            filterable
            style="width: 100%;"
          >
            <el-option
              v-for="issue in batchIssueOptions"
              :key="issue.id"
              :label="`卷${issue.volume || '-'} 期${issue.issue || '-'}`"
              :value="issue.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmitBatch"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="deliverDialogVisible"
      title="签收确认"
      width="400px"
    >
      <el-form
        label-width="80px"
      >
        <el-form-item label="签收人">
          <el-input
            v-model="deliverReceivedBy"
            placeholder="请输入签收人姓名"
            maxlength="100"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmitDeliver"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="templateDialogVisible"
      title="新增分发模板"
      width="500px"
      @close="handleTemplateDialogClose"
    >
      <el-form
        ref="templateFormRef"
        :model="templateForm"
        :rules="templateFormRules"
        label-width="100px"
      >
        <el-form-item
          label="分发目标"
          prop="destination"
        >
          <el-input
            v-model="templateForm.destination"
            placeholder="请输入分发目标"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item
          label="份数"
          prop="copies"
        >
          <el-input-number
            v-model="templateForm.copies"
            :min="1"
            :max="100"
          />
        </el-form-item>
        <el-form-item
          label="顺序"
          prop="routingOrder"
        >
          <el-input-number
            v-model="templateForm.routingOrder"
            :min="1"
            :max="100"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmitTemplate"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'SerialRoutingList' })

import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRoutings,
  createRouting,
  batchCreateRoutings,
  sendRouting,
  deliverRouting,
  deleteRouting,
  listRoutingTemplates,
  createRoutingTemplate,
  deleteRoutingTemplate,
  getSubscriptionList,
  getIssueList
} from '@/api/serial'
import EmptyState from '@/components/EmptyState.vue'

const activeTab = ref('routings')
const loading = ref(false)
const submitLoading = ref(false)
const routingList = ref([])
const filterForm = reactive({
  routingStatus: '',
  destination: ''
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const statusMap = {
  PENDING: '待分发',
  IN_TRANSIT: '运送中',
  DELIVERED: '已送达',
  RETURNED: '已退回'
}

const statusTagType = {
  PENDING: 'warning',
  IN_TRANSIT: 'primary',
  DELIVERED: 'success',
  RETURNED: 'info'
}

const subscriptionOptions = ref([])
const issueOptions = ref([])
const batchIssueOptions = ref([])

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  subscriptionId: null,
  issueId: null,
  destination: '',
  copies: 1,
  routingOrder: 1,
  notes: ''
})
const createFormRules = {
  subscriptionId: [
    { required: true, message: '请选择订阅', trigger: 'change' }
  ],
  destination: [
    { required: true, message: '请输入分发目标', trigger: 'blur' }
  ]
}

const batchDialogVisible = ref(false)
const batchFormRef = ref(null)
const batchForm = reactive({
  subscriptionId: null,
  issueId: null
})
const batchFormRules = {
  subscriptionId: [
    { required: true, message: '请选择订阅', trigger: 'change' }
  ],
  issueId: [
    { required: true, message: '请选择到刊记录', trigger: 'change' }
  ]
}

const deliverDialogVisible = ref(false)
const deliverRoutingId = ref(null)
const deliverReceivedBy = ref('')

const templateDialogVisible = ref(false)
const templateFormRef = ref(null)
const templateForm = reactive({
  destination: '',
  copies: 1,
  routingOrder: 1
})
const templateFormRules = {
  destination: [
    { required: true, message: '请输入分发目标', trigger: 'blur' }
  ]
}

const templateLoading = ref(false)
const templateList = ref([])
const templateSubscriptionId = ref(null)

async function loadSubscriptions() {
  try {
    const res = await getSubscriptionList({ current: 1, size: 1000 })
    if (res.code === 0) {
      subscriptionOptions.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载订阅列表失败:', error)
    ElMessage.error('加载订阅列表失败')
  }
}

async function loadIssues(subscriptionId, targetRef) {
  if (!subscriptionId) {
    targetRef.value = []
    return
  }
  try {
    const res = await getIssueList({ subscriptionId, current: 1, size: 1000 })
    if (res.code === 0) {
      targetRef.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载到刊列表失败:', error)
    ElMessage.error('加载到刊列表失败')
  }
}

async function loadRoutings() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      routingStatus: filterForm.routingStatus || undefined,
      destination: filterForm.destination || undefined
    }
    const res = await listRoutings(params)
    if (res.code === 0) {
      routingList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载路由分发列表失败:', error)
    ElMessage.error('加载路由分发列表失败')
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  if (!templateSubscriptionId.value) {
    templateList.value = []
    return
  }
  templateLoading.value = true
  try {
    const res = await listRoutingTemplates({ subscriptionId: templateSubscriptionId.value })
    if (res.code === 0) {
      templateList.value = res.data || []
    }
  } catch (error) {
    console.error('加载模板列表失败:', error)
    ElMessage.error('加载模板列表失败')
  } finally {
    templateLoading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadRoutings()
}

function handleReset() {
  filterForm.routingStatus = ''
  filterForm.destination = ''
  handleSearch()
}

function handleSizeChange() {
  loadRoutings()
}

function handleCurrentChange() {
  loadRoutings()
}

function handleCreateRouting() {
  resetCreateForm()
  createDialogVisible.value = true
}

function handleBatchCreate() {
  batchForm.subscriptionId = null
  batchForm.issueId = null
  batchIssueOptions.value = []
  batchDialogVisible.value = true
}

async function handleBatchSubscriptionChange(val) {
  batchForm.issueId = null
  await loadIssues(val, batchIssueOptions)
}

watch(() => createForm.subscriptionId, async (val) => {
  createForm.issueId = null
  await loadIssues(val, issueOptions)
})

async function handleSend(row) {
  try {
    await ElMessageBox.confirm(
      '确定发出该分发记录吗？',
      '发出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await sendRouting(row.id)
    if (res.code === 0) {
      ElMessage.success('发出成功')
      loadRoutings()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发出失败:', error)
      ElMessage.error('发出失败')
    }
  }
}

function handleDeliver(row) {
  deliverRoutingId.value = row.id
  deliverReceivedBy.value = ''
  deliverDialogVisible.value = true
}

async function handleSubmitDeliver() {
  if (!deliverReceivedBy.value.trim()) {
    ElMessage.warning('请输入签收人')
    return
  }
  submitLoading.value = true
  try {
    const res = await deliverRouting(deliverRoutingId.value, { receivedBy: deliverReceivedBy.value })
    if (res.code === 0) {
      ElMessage.success('签收成功')
      deliverDialogVisible.value = false
      loadRoutings()
    }
  } catch (error) {
    console.error('签收失败:', error)
    ElMessage.error('签收失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      '确定要删除该分发记录吗？',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await deleteRouting(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadRoutings()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

async function handleSubmitCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
    submitLoading.value = true

    const res = await createRouting(createForm)
    if (res.code === 0) {
      ElMessage.success('创建成功')
      createDialogVisible.value = false
      loadRoutings()
    }
  } catch (error) {
    console.error('创建失败:', error)
    ElMessage.error('创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleSubmitBatch() {
  if (!batchFormRef.value) return
  try {
    await batchFormRef.value.validate()
    submitLoading.value = true

    const res = await batchCreateRoutings({
      issueId: batchForm.issueId,
      subscriptionId: batchForm.subscriptionId
    })
    if (res.code === 0) {
      ElMessage.success('批量分发成功')
      batchDialogVisible.value = false
      loadRoutings()
    }
  } catch (error) {
    console.error('批量分发失败:', error)
    ElMessage.error('批量分发失败')
  } finally {
    submitLoading.value = false
  }
}

function handleCreateTemplate() {
  templateForm.destination = ''
  templateForm.copies = 1
  templateForm.routingOrder = 1
  templateDialogVisible.value = true
}

async function handleSubmitTemplate() {
  if (!templateFormRef.value) return
  try {
    await templateFormRef.value.validate()
    submitLoading.value = true

    const res = await createRoutingTemplate({
      subscriptionId: templateSubscriptionId.value,
      destination: templateForm.destination,
      copies: templateForm.copies,
      routingOrder: templateForm.routingOrder
    })
    if (res.code === 0) {
      ElMessage.success('模板创建成功')
      templateDialogVisible.value = false
      loadTemplates()
    }
  } catch (error) {
    console.error('模板创建失败:', error)
    ElMessage.error('模板创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteTemplate(row) {
  try {
    await ElMessageBox.confirm(
      '确定要删除该模板吗？',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await deleteRoutingTemplate(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadTemplates()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
      ElMessage.error('删除模板失败')
    }
  }
}

function resetCreateForm() {
  createForm.subscriptionId = null
  createForm.issueId = null
  createForm.destination = ''
  createForm.copies = 1
  createForm.routingOrder = 1
  createForm.notes = ''
  issueOptions.value = []
  createFormRef.value?.clearValidate()
}

function handleCreateDialogClose() {
  resetCreateForm()
}

function handleBatchDialogClose() {
  batchForm.subscriptionId = null
  batchForm.issueId = null
  batchIssueOptions.value = []
  batchFormRef.value?.clearValidate()
}

function handleTemplateDialogClose() {
  templateForm.destination = ''
  templateForm.copies = 1
  templateForm.routingOrder = 1
  templateFormRef.value?.clearValidate()
}

onMounted(() => {
  loadSubscriptions()
  loadRoutings()
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
