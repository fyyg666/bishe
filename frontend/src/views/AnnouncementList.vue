<template>
  <div class="page-container">
    <div class="page-header">
      <h2>公告管理</h2>
      <el-button
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>发布公告
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form
        :inline="true"
        :model="filterForm"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索标题/内容"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="filterForm.type"
            placeholder="选择类型"
            clearable
          >
            <el-option
              label="全部"
              value=""
            />
            <el-option
              label="通知"
              value="NOTICE"
            />
            <el-option
              label="活动"
              value="ACTIVITY"
            />
            <el-option
              label="系统"
              value="SYSTEM"
            />
          </el-select>
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
              label="草稿"
              value="DRAFT"
            />
            <el-option
              label="已发布"
              value="PUBLISHED"
            />
            <el-option
              label="已归档"
              value="ARCHIVED"
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

    <!-- 数据表格 -->
    <el-card class="table-card">
      <!-- 骨架屏加载状态 -->
      <el-skeleton
        v-if="loading && announcementList.length === 0"
        :rows="5"
        animated
      />
      
      <!-- 空状态 -->
      <EmptyState 
        v-else-if="!loading && announcementList.length === 0" 
        description="暂无公告数据"
        :show-button="true"
        action-text="刷新"
        @action="loadAnnouncementList"
      />
      
      <!-- 数据表格 -->
      <el-table
        v-else
        v-loading="loading"
        :data="announcementList"
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
          label="标题"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="type"
          label="类型"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="priority"
          label="优先级"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.priority > 0 ? 'danger' : 'info'"
              size="small"
            >
              {{ row.priority > 0 ? `P${row.priority}` : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
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
          prop="publisherName"
          label="发布人"
          width="100"
          align="center"
        />
        <el-table-column
          prop="publishTime"
          label="发布时间"
          width="160"
          align="center"
        >
          <template #default="{ row }">
            {{ row.publishTime || '-' }}
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
              type="primary"
              link
              @click="handleView(row)"
            >
              <el-icon><View /></el-icon>查看
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

      <!-- 分页 -->
      <div
        v-if="announcementList.length > 0"
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

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="标题"
          prop="title"
        >
          <el-input
            v-model="formData.title"
            placeholder="请输入公告标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          label="内容"
          prop="content"
        >
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          label="类型"
          prop="type"
        >
          <el-select
            v-model="formData.type"
            placeholder="选择类型"
          >
            <el-option
              label="通知"
              value="NOTICE"
            />
            <el-option
              label="活动"
              value="ACTIVITY"
            />
            <el-option
              label="系统"
              value="SYSTEM"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="优先级"
          prop="priority"
        >
          <el-input-number
            v-model="formData.priority"
            :min="0"
            :max="10"
          />
          <span class="form-tip">数值越大优先级越高</span>
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
              label="草稿"
              value="DRAFT"
            />
            <el-option
              label="已发布"
              value="PUBLISHED"
            />
            <el-option
              label="已归档"
              value="ARCHIVED"
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

    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="公告详情"
      width="700px"
    >
      <div
        v-if="currentAnnouncement"
        class="announcement-detail"
      >
        <h3 class="detail-title">
          {{ currentAnnouncement.title }}
        </h3>
        <div class="detail-meta">
          <el-tag
            :type="getTypeTagType(currentAnnouncement.type)"
            size="small"
          >
            {{ getTypeName(currentAnnouncement.type) }}
          </el-tag>
          <span class="meta-item">
            <el-icon><User /></el-icon>
            {{ currentAnnouncement.publisherName || '系统' }}
          </span>
          <span class="meta-item">
            <el-icon><Clock /></el-icon>
            {{ currentAnnouncement.publishTime || currentAnnouncement.createTime }}
          </span>
        </div>
        <el-divider />
        <div class="detail-content">
          {{ currentAnnouncement.content }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'Announcements' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAnnouncementList,
  getAnnouncementDetail,
  createAnnouncement,
  updateAnnouncement,
  publishAnnouncement,
  deleteAnnouncement
} from '@/api/announcement'
import EmptyState from '@/components/EmptyState.vue'

// 响应式数据
const loading = ref(false)
const submitLoading = ref(false)
const announcementList = ref([])
const filterForm = reactive({
  keyword: '',
  type: '',
  status: ''
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 对话框数据
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('发布公告')
const isEdit = ref(false)
const currentAnnouncement = ref(null)
const formRef = ref(null)

const formData = reactive({
  id: null,
  title: '',
  content: '',
  type: 'NOTICE',
  priority: 0,
  status: 'DRAFT'
})

const formRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 2, max: 200, message: '标题长度为2-200个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' }
  ]
}

// 获取类型标签颜色
function getTypeTagType(type) {
  const map = {
    NOTICE: 'primary',
    ACTIVITY: 'success',
    SYSTEM: 'warning'
  }
  return map[type] || 'info'
}

// 获取类型名称
function getTypeName(type) {
  const map = {
    NOTICE: '通知',
    ACTIVITY: '活动',
    SYSTEM: '系统'
  }
  return map[type] || type
}

// 获取状态标签颜色
function getStatusTagType(status) {
  const map = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ARCHIVED: ''
  }
  return map[status] || 'info'
}

// 获取状态名称
function getStatusName(status) {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

// 加载公告列表
async function loadAnnouncementList() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...filterForm
    }
    // 如果没有选择状态，默认查询所有状态（非管理员可能只需要已发布）
    if (!params.status && params.status !== '') {
      delete params.status
    }
    
    const res = await getAnnouncementList(params)
    if (res.code === 0) {
      announcementList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载公告列表失败:', error)
    ElMessage.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1
  loadAnnouncementList()
}

// 重置
function handleReset() {
  filterForm.keyword = ''
  filterForm.type = ''
  filterForm.status = ''
  handleSearch()
}

// 分页大小改变
function handleSizeChange() {
  loadAnnouncementList()
}

// 页码改变
function handleCurrentChange() {
  loadAnnouncementList()
}

// 创建
function handleCreate() {
  dialogTitle.value = '发布公告'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
function handleEdit(row) {
  dialogTitle.value = '编辑公告'
  isEdit.value = true
  formData.id = row.id
  formData.title = row.title
  formData.content = row.content
  formData.type = row.type
  formData.priority = row.priority
  formData.status = row.status
  dialogVisible.value = true
}

// 查看
async function handleView(row) {
  try {
    const res = await getAnnouncementDetail(row.id)
    if (res.code === 0) {
      currentAnnouncement.value = res.data
      viewDialogVisible.value = true
    }
  } catch (error) {
    console.error('加载公告详情失败:', error)
    ElMessage.error('加载公告详情失败')
  }
}

// 删除
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告《${row.title}》吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await deleteAnnouncement(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadAnnouncementList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除公告失败:', error)
      ElMessage.error('删除公告失败')
    }
  }
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    const res = isEdit.value
      ? await updateAnnouncement(formData.id, formData)
      : await createAnnouncement(formData)
    
    if (res.code === 0) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      
      // 如果是发布状态，自动发布
      if (formData.status === 'PUBLISHED' && !isEdit.value) {
        await publishAnnouncement(res.data.id)
      }
      
      dialogVisible.value = false
      loadAnnouncementList()
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
  formData.title = ''
  formData.content = ''
  formData.type = 'NOTICE'
  formData.priority = 0
  formData.status = 'DRAFT'
  formRef.value?.clearValidate()
}

// 对话框关闭
function handleDialogClose() {
  resetForm()
}

// 生命周期钩子
onMounted(() => {
  loadAnnouncementList()
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

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.announcement-detail {
  .detail-title {
    margin: 0 0 15px;
    font-size: 20px;
    font-weight: bold;
    color: #303133;
  }

  .detail-meta {
    display: flex;
    align-items: center;
    gap: 20px;
    color: #909399;
    font-size: 14px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 5px;
    }
  }

  .detail-content {
    line-height: 1.8;
    color: #606266;
    white-space: pre-wrap;
  }
}
</style>
