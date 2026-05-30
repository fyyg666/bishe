<template>
  <div class="page-container">
    <div class="page-header">
      <h2>志愿服务</h2>
      <el-button
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>申请志愿服务
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row
      :gutter="20"
      class="stats-row"
    >
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon volunteer-icon">
            <el-icon><Medal /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ myStats.totalRecords || 0 }}
            </div>
            <div class="stat-label">
              累计服务次数
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon hours-icon">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ myStats.totalHours || 0 }}
            </div>
            <div class="stat-label">
              累计服务时长(h)
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon pending-icon">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ myStats.pendingCount || 0 }}
            </div>
            <div class="stat-label">
              待审核
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 标签页 -->
    <el-tabs
      v-model="activeTab"
      @tab-change="handleTabChange"
    >
      <el-tab-pane
        label="我的服务"
        name="my"
      >
        <!-- 筛选 -->
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
                  label="待审核"
                  value="PENDING"
                />
                <el-option
                  label="已通过"
                  value="APPROVED"
                />
                <el-option
                  label="已拒绝"
                  value="REJECTED"
                />
                <el-option
                  label="已取消"
                  value="CANCELLED"
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
      </el-tab-pane>
      
      <el-tab-pane
        v-if="isAdmin"
        label="待审核"
        name="pending"
      >
        <el-table
          v-loading="loading"
          :data="pendingList"
          stripe
        >
          <el-table-column
            prop="id"
            label="ID"
            width="80"
            align="center"
          />
          <el-table-column
            prop="realName"
            label="申请人"
            width="100"
            align="center"
          />
          <el-table-column
            prop="serviceDate"
            label="服务日期"
            width="120"
            align="center"
          />
          <el-table-column
            prop="startTime"
            label="开始时间"
            width="160"
            align="center"
          />
          <el-table-column
            prop="endTime"
            label="结束时间"
            width="160"
            align="center"
          />
          <el-table-column
            prop="serviceHours"
            label="服务时长"
            width="100"
            align="center"
          >
            <template #default="{ row }">
              {{ row.serviceHours }}h
            </template>
          </el-table-column>
          <el-table-column
            prop="serviceType"
            label="服务类型"
            width="120"
            align="center"
          />
          <el-table-column
            prop="description"
            label="服务描述"
            min-width="150"
            show-overflow-tooltip
          />
          <el-table-column
            label="操作"
            width="150"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="success"
                size="small"
                @click="handleReview(row, true)"
              >
                通过
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleReview(row, false)"
              >
                拒绝
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadPendingList"
            @current-change="loadPendingList"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 我的服务列表 -->
    <el-card
      v-show="activeTab === 'my'"
      class="table-card"
    >
      <!-- 骨架屏加载状态 -->
      <el-skeleton
        v-if="loading && volunteerList.length === 0"
        :rows="5"
        animated
      />
      
      <!-- 空状态 -->
      <EmptyState 
        v-else-if="!loading && volunteerList.length === 0" 
        description="暂无志愿服务记录"
        :show-button="true"
        action-text="申请服务"
        @action="handleCreate"
      />
      
      <!-- 数据表格 -->
      <el-table
        v-else
        v-loading="loading"
        :data="volunteerList"
        stripe
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
          align="center"
        />
        <el-table-column
          prop="serviceDate"
          label="服务日期"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            {{ formatDate(row.serviceDate) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="startTime"
          label="开始时间"
          width="160"
          align="center"
        />
        <el-table-column
          prop="endTime"
          label="结束时间"
          width="160"
          align="center"
        />
        <el-table-column
          prop="serviceHours"
          label="服务时长"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag type="success">
              {{ row.serviceHours }}h
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="serviceType"
          label="服务类型"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            {{ getServiceTypeName(row.serviceType) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          label="服务描述"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleView(row)"
            >
              <el-icon><View /></el-icon>详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="warning"
              link
              @click="handleCancel(row)"
            >
              <el-icon><Close /></el-icon>取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div
        v-if="volunteerList.length > 0"
        class="pagination"
      >
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadMyVolunteers"
          @current-change="loadMyVolunteers"
        />
      </div>
    </el-card>

    <!-- 创建申请对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="申请志愿服务"
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
          label="服务日期"
          prop="serviceDate"
        >
          <el-date-picker
            v-model="formData.serviceDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <el-form-item
          label="开始时间"
          prop="startTime"
        >
          <el-time-picker
            v-model="formData.startTime"
            placeholder="选择开始时间"
            format="HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item
          label="结束时间"
          prop="endTime"
        >
          <el-time-picker
            v-model="formData.endTime"
            placeholder="选择结束时间"
            format="HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item
          label="服务类型"
          prop="serviceType"
        >
          <el-select
            v-model="formData.serviceType"
            placeholder="选择服务类型"
          >
            <el-option
              label="图书整理"
              value="BOOK_SORTING"
            />
            <el-option
              label="阅读引导"
              value="READING_GUIDE"
            />
            <el-option
              label="场地维护"
              value="VENUE_MAINTENANCE"
            />
            <el-option
              label="活动协助"
              value="ACTIVITY_ASSIST"
            />
            <el-option
              label="咨询导引"
              value="INFO_GUIDE"
            />
            <el-option
              label="其他"
              value="OTHER"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="服务描述"
          prop="description"
        >
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请描述服务内容"
            maxlength="500"
            show-word-limit
          />
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
          提交申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="志愿服务详情"
      width="600px"
    >
      <el-descriptions
        v-if="currentVolunteer"
        :column="2"
        border
      >
        <el-descriptions-item label="服务日期">
          {{ formatDate(currentVolunteer.serviceDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="服务时长">
          {{ currentVolunteer.serviceHours }}h
        </el-descriptions-item>
        <el-descriptions-item
          label="开始时间"
          :span="2"
        >
          {{ currentVolunteer.startTime }}
        </el-descriptions-item>
        <el-descriptions-item
          label="结束时间"
          :span="2"
        >
          {{ currentVolunteer.endTime }}
        </el-descriptions-item>
        <el-descriptions-item label="服务类型">
          {{ getServiceTypeName(currentVolunteer.serviceType) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentVolunteer.status)">
            {{ getStatusName(currentVolunteer.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          label="服务描述"
          :span="2"
        >
          {{ currentVolunteer.description }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentVolunteer.reviewerName"
          label="审核人"
        >
          {{ currentVolunteer.reviewerName }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentVolunteer.reviewTime"
          label="审核时间"
        >
          {{ currentVolunteer.reviewTime }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentVolunteer.reviewRemark"
          label="审核备注"
          :span="2"
        >
          {{ currentVolunteer.reviewRemark }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="审核志愿服务"
      width="500px"
    >
      <el-form
        :model="reviewForm"
        label-width="100px"
      >
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :value="true">
              通过
            </el-radio>
            <el-radio :value="false">
              拒绝
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="reviewForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入审核备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleReviewSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getMyVolunteers,
  getPendingVolunteers,
  createVolunteer,
  cancelVolunteer,
  reviewVolunteer,
  getVolunteerStats
} from '@/api/volunteer'
import EmptyState from '@/components/EmptyState.vue'

// 状态
const activeTab = ref('my')
const loading = ref(false)
const submitLoading = ref(false)

// 列表数据
const volunteerList = ref([])
const pendingList = ref([])
const myStats = reactive({
  totalRecords: 0,
  totalHours: 0,
  pendingCount: 0
})

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 筛选
const filterForm = reactive({
  status: ''
})

// 对话框
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const currentVolunteer = ref(null)
const formRef = ref(null)

const formData = reactive({
  serviceDate: '',
  startTime: '',
  endTime: '',
  serviceType: '',
  description: ''
})

const reviewForm = reactive({
  approved: true,
  remark: ''
})

const formRules = {
  serviceDate: [{ required: true, message: '请选择服务日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }]
}

// 用户状态
const userStore = useUserStore()
const isAdmin = computed(() => {
  return userStore.userInfo?.role === 'ADMIN' || userStore.userInfo?.role === 'LIBRARIAN'
})

// 获取服务类型名称
function getServiceTypeName(type) {
  const map = {
    BOOK_SORTING: '图书整理',
    READING_GUIDE: '阅读引导',
    VENUE_MAINTENANCE: '场地维护',
    ACTIVITY_ASSIST: '活动协助',
    INFO_GUIDE: '咨询导引',
    OTHER: '其他'
  }
  return map[type] || type
}

// 获取状态名称
function getStatusName(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

// 获取状态标签类型
function getStatusTagType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

// 格式化日期
function formatDate(date) {
  if (!date) return '-'
  return date.substring(0, 10)
}

// 禁用过去的日期
function disabledDate(date) {
  return date.getTime() < Date.now() - 8.64e7
}

// 加载我的志愿服务
async function loadMyVolunteers() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (filterForm.status) {
      params.status = filterForm.status
    }
    const res = await getMyVolunteers(params)
    if (res.code === 0) {
      volunteerList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载志愿服务列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载待审核列表
async function loadPendingList() {
  loading.value = true
  try {
    const res = await getPendingVolunteers({
      current: pagination.current,
      size: pagination.size
    })
    if (res.code === 0) {
      pendingList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载待审核列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载我的统计
async function loadMyStats() {
  try {
    const res = await getVolunteerStats()
    if (res.code === 0) {
      Object.assign(myStats, res.data)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 标签页切换
function handleTabChange(tab) {
  if (tab === 'my') {
    pagination.current = 1
    loadMyVolunteers()
  } else if (tab === 'pending') {
    pagination.current = 1
    loadPendingList()
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1
  loadMyVolunteers()
}

// 重置
function handleReset() {
  filterForm.status = ''
  handleSearch()
}

// 创建
function handleCreate() {
  formData.serviceDate = ''
  formData.startTime = ''
  formData.endTime = ''
  formData.serviceType = ''
  formData.description = ''
  dialogVisible.value = true
}

// 提交申请
async function handleSubmit() {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    const res = await createVolunteer(formData)
    if (res.code === 0) {
      ElMessage.success('申请成功')
      dialogVisible.value = false
      loadMyVolunteers()
      loadMyStats()
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 查看详情
function handleView(row) {
  currentVolunteer.value = row
  viewDialogVisible.value = true
}

// 取消申请
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定要取消此志愿服务申请吗？', '取消确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await cancelVolunteer(row.id)
    if (res.code === 0) {
      ElMessage.success('已取消')
      loadMyVolunteers()
      loadMyStats()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消失败:', error)
    }
  }
}

// 打开审核对话框
function handleReview(row, approved) {
  reviewForm.approved = approved
  reviewForm.remark = ''
  currentVolunteer.value = row
  reviewDialogVisible.value = true
}

// 提交审核
async function handleReviewSubmit() {
  try {
    submitLoading.value = true
    const res = await reviewVolunteer(
      currentVolunteer.value.id,
      reviewForm.approved,
      reviewForm.remark
    )
    if (res.code === 0) {
      ElMessage.success('审核完成')
      reviewDialogVisible.value = false
      loadPendingList()
    }
  } catch (error) {
    console.error('审核失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 对话框关闭
function handleDialogClose() {
  formRef.value?.resetFields()
}

// 生命周期钩子
onMounted(() => {
  loadMyVolunteers()
  loadMyStats()
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 15px;

  .stat-icon {
    width: 50px;
    height: 50px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 15px;

    .el-icon {
      font-size: 24px;
      color: #fff;
    }

    &.volunteer-icon {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    &.hours-icon {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    }

    &.pending-icon {
      background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
    }
  }

  .stat-info {
    flex: 1;

    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 5px;
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
    }
  }
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-top: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
