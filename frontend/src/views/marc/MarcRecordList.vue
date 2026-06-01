<template>
  <div class="marc-record-list">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="控制号/题名" clearable />
        </el-form-item>
        <el-form-item label="记录类型">
          <el-select v-model="searchForm.recordType" placeholder="全部类型" clearable>
            <el-option label="图书" value="BOOK" />
            <el-option label="连续出版物" value="SERIAL" />
            <el-option label="地图" value="MAP" />
            <el-option label="视听资料" value="AV" />
            <el-option label="混合资料" value="MIXED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="table-toolbar">
      <el-button type="primary" :icon="Plus" @click="$router.push('/marc/create')">新建记录</el-button>
      <el-button :icon="Upload" @click="importDialogVisible = true">导入MARC</el-button>
      <el-button :icon="Download" :disabled="!selectedIds.length" @click="handleExport">导出选中</el-button>
    </div>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="records" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="controlNumber" label="控制号" width="160" />
        <el-table-column prop="recordType" label="记录类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ RECORD_TYPE_MAP[row.recordType] || row.recordType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TYPE_MAP[row.status] || 'info'">
              {{ STATUS_MAP[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </div>
    </el-card>

    <el-dialog v-model="importDialogVisible" title="导入MARC文件" width="500px">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".mrc,.xml"
        drag
        :on-change="handleFileChange"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将MARC文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .mrc (ISO 2709) / .xml (MARCXML) 格式</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">确认导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="MARC记录详情" width="700px">
      <el-descriptions :column="2" border v-if="viewRecord">
        <el-descriptions-item label="控制号">{{ viewRecord.controlNumber }}</el-descriptions-item>
        <el-descriptions-item label="记录类型">{{ RECORD_TYPE_MAP[viewRecord.recordType] || viewRecord.recordType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ STATUS_MAP[viewRecord.status] || viewRecord.status }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewRecord.createTime }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="viewRecord && viewRecord.fields && viewRecord.fields.length" style="margin-top: 16px">
        <h4 style="margin-bottom: 8px">字段列表</h4>
        <el-table :data="viewRecord.fields" size="small" border>
          <el-table-column prop="tag" label="标签" width="80" />
          <el-table-column prop="indicator1" label="I1" width="50" />
          <el-table-column prop="indicator2" label="I2" width="50" />
          <el-table-column prop="displayValue" label="显示值" min-width="200" />
        </el-table>
      </div>
      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, UploadFilled } from '@element-plus/icons-vue'
import { getMarcRecords, deleteMarcRecord, importMarcFile, exportMarcRecords } from '@/api/marc'

const RECORD_TYPE_MAP = { BOOK: '图书', SERIAL: '连续出版物', MAP: '地图', AV: '视听资料', MIXED: '混合资料' }
const STATUS_MAP = { DRAFT: '草稿', PUBLISHED: '已发布', PENDING_REVIEW: '待审核' }
const STATUS_TYPE_MAP = { DRAFT: 'info', PUBLISHED: 'success', PENDING_REVIEW: 'warning' }

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const selectedIds = ref([])

const searchForm = reactive({ keyword: '', recordType: '' })
const pagination = reactive({ current: 1, size: 10 })

const importDialogVisible = ref(false)
const importLoading = ref(false)
const uploadRef = ref(null)
const importFile = ref(null)

const viewDialogVisible = ref(false)
const viewRecord = ref(null)

onMounted(() => {
  loadRecords()
})

async function loadRecords() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.recordType) params.recordType = searchForm.recordType
    const res = await getMarcRecords(params)
    records.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载MARC记录列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadRecords()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.recordType = ''
  pagination.current = 1
  loadRecords()
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(s => s.id)
}

async function handleView(row) {
  viewRecord.value = row
  viewDialogVisible.value = true
}

function handleEdit(row) {
  router.push(`/marc/${row.id}/edit`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该MARC记录吗？', '提示', { type: 'warning' })
    await deleteMarcRecord(row.id)
    ElMessage.success('删除成功')
    loadRecords()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}

function handleFileChange(file) {
  importFile.value = file.raw
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    const res = await importMarcFile(formData)
    ElMessage.success(`导入成功，共 ${res.data?.successCount ?? 0} 条`)
    importDialogVisible.value = false
    importFile.value = null
    loadRecords()
  } catch (e) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

async function handleExport() {
  try {
    const res = await exportMarcRecords(selectedIds.value)
    const blob = new Blob([res.data], { type: 'application/octet-stream' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `marc_export_${new Date().toISOString().slice(0, 10)}.mrc`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}
</script>

<style lang="scss" scoped>
.marc-record-list {
  .search-card {
    margin-bottom: 16px;
  }

  .table-toolbar {
    margin-bottom: 16px;
  }

  .table-card {
    border: none;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
