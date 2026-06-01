<template>
  <div class="digital-resource-list">
    <el-card class="search-card">
      <el-form
        :model="searchForm"
        inline
      >
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="题名/作者/ISBN"
            clearable
          />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select
            v-model="searchForm.resourceType"
            placeholder="全部类型"
            clearable
          >
            <el-option
              label="电子书"
              value="EBOOK"
            />
            <el-option
              label="音频"
              value="AUDIO"
            />
            <el-option
              label="视频"
              value="VIDEO"
            />
            <el-option
              label="数据库"
              value="DATABASE"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="table-toolbar">
      <el-button
        type="primary"
        :icon="Plus"
        @click="handleAdd"
      >
        添加资源
      </el-button>
    </div>

    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="resources"
        stripe
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="title"
          label="题名"
          min-width="150"
        />
        <el-table-column
          prop="author"
          label="作者"
          width="120"
        />
        <el-table-column
          prop="resourceType"
          label="资源类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="resourceTypeTag(row.resourceType)">
              {{ resourceTypeLabel(row.resourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="format"
          label="格式"
          width="80"
        />
        <el-table-column
          prop="provider"
          label="提供商"
          width="120"
        />
        <el-table-column
          prop="accessMode"
          label="访问方式"
          width="100"
        >
          <template #default="{ row }">
            {{ accessModeLabel(row.accessMode) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '可用' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="280"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.resourceType === 'EBOOK' && (row.format === 'PDF' || row.format === 'EPUB')"
              type="success"
              link
              @click="handleRead(row)"
            >
              在线阅读
            </el-button>
            <el-button
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="warning"
              link
              @click="handleDownload(row)"
            >
              下载
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

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadResources"
          @current-change="loadResources"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑数字资源' : '添加数字资源'"
      width="600px"
    >
      <el-form
        :model="form"
        label-width="100px"
      >
        <el-form-item label="题名">
          <el-input
            v-model="form.title"
            placeholder="请输入题名"
          />
        </el-form-item>
        <el-form-item label="作者">
          <el-input
            v-model="form.author"
            placeholder="请输入作者"
          />
        </el-form-item>
        <el-form-item label="ISBN">
          <el-input
            v-model="form.isbn"
            placeholder="请输入ISBN"
          />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select
            v-model="form.resourceType"
            placeholder="请选择资源类型"
          >
            <el-option
              label="电子书"
              value="EBOOK"
            />
            <el-option
              label="音频"
              value="AUDIO"
            />
            <el-option
              label="视频"
              value="VIDEO"
            />
            <el-option
              label="数据库"
              value="DATABASE"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="格式">
          <el-select
            v-model="form.format"
            placeholder="请选择格式"
          >
            <el-option
              label="PDF"
              value="PDF"
            />
            <el-option
              label="EPUB"
              value="EPUB"
            />
            <el-option
              label="MP3"
              value="MP3"
            />
            <el-option
              label="MP4"
              value="MP4"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="访问地址">
          <el-input
            v-model="form.accessUrl"
            placeholder="请输入访问地址"
          />
        </el-form-item>
        <el-form-item label="提供商">
          <el-input
            v-model="form.provider"
            placeholder="请输入提供商"
          />
        </el-form-item>
        <el-form-item label="访问方式">
          <el-select
            v-model="form.accessMode"
            placeholder="请选择访问方式"
          >
            <el-option
              label="在线"
              value="ONLINE"
            />
            <el-option
              label="下载"
              value="DOWNLOAD"
            />
            <el-option
              label="两者皆可"
              value="BOTH"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入简介"
          />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input
            v-model="form.coverUrl"
            placeholder="请输入封面图片URL"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option
              label="可用"
              :value="0"
            />
            <el-option
              label="下架"
              :value="1"
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
          :loading="submitting"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'DigitalResourceList' })

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getDigitalResourceList,
  createDigitalResource,
  updateDigitalResource,
  deleteDigitalResource,
  getDownloadUrl
} from '@/api/digitalResource'

const router = useRouter()

const loading = ref(false)
const resources = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)

const searchForm = reactive({
  keyword: '',
  resourceType: ''
})

const pagination = reactive({
  current: 1,
  size: 10
})

const defaultForm = {
  title: '',
  author: '',
  isbn: '',
  resourceType: 'EBOOK',
  format: 'PDF',
  accessUrl: '',
  provider: '',
  accessMode: 'ONLINE',
  description: '',
  coverUrl: '',
  status: 0
}

const form = reactive({ ...defaultForm })

onMounted(() => {
  loadResources()
})

async function loadResources() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.resourceType) params.resourceType = searchForm.resourceType
    const res = await getDigitalResourceList(params)
    resources.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载数字资源列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadResources()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.resourceType = ''
  pagination.current = 1
  loadResources()
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm)
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    title: row.title,
    author: row.author,
    isbn: row.isbn,
    resourceType: row.resourceType,
    format: row.format,
    accessUrl: row.accessUrl,
    provider: row.provider,
    accessMode: row.accessMode,
    description: row.description,
    coverUrl: row.coverUrl,
    status: row.status
  })
  form._editId = row.id
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.title) {
    ElMessage.warning('请输入题名')
    return
  }
  submitting.value = true
  try {
    const data = { ...form }
    delete data._editId
    if (isEdit.value) {
      await updateDigitalResource(form._editId, data)
      ElMessage.success('更新成功')
    } else {
      await createDigitalResource(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadResources()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该数字资源吗？', '提示', { type: 'warning' })
    await deleteDigitalResource(row.id)
    ElMessage.success('删除成功')
    loadResources()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}

function resourceTypeLabel(type) {
  const map = { EBOOK: '电子书', AUDIO: '音频', VIDEO: '视频', DATABASE: '数据库' }
  return map[type] || type
}

function resourceTypeTag(type) {
  const map = { EBOOK: '', AUDIO: 'success', VIDEO: 'warning', DATABASE: 'info' }
  return map[type] || ''
}

function accessModeLabel(mode) {
  const map = { ONLINE: '在线', DOWNLOAD: '下载', BOTH: '在线/下载' }
  return map[mode] || mode
}

function handleRead(row) {
  router.push(`/digital/read/${row.id}`)
}

async function handleDownload(row) {
  try {
    const res = await getDownloadUrl(row.id)
    const url = res.data?.url || res.data
    if (url) {
      window.open(url, '_blank')
    } else {
      ElMessage.warning('无法获取下载地址')
    }
  } catch {
    ElMessage.error('获取下载地址失败')
  }
}
</script>

<style lang="scss" scoped>
.digital-resource-list {
  .search-card {
    margin-bottom: 16px;
  }

  .table-toolbar {
    margin-bottom: 16px;
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
