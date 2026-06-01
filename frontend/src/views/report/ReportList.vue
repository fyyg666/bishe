<template>
  <div class="page-container">
    <div class="page-header">
      <h2>自定义报表</h2>
      <div class="header-actions">
        <el-select
          v-model="selectedCategory"
          placeholder="按分类筛选"
          clearable
          style="width: 160px; margin-right: 12px"
          @change="loadTemplates"
        >
          <el-option
            v-for="cat in categories"
            :key="cat"
            :label="cat"
            :value="cat"
          />
        </el-select>
        <el-button
          v-if="isAdmin"
          type="primary"
          @click="openCreateDialog"
        >
          <el-icon><Plus /></el-icon> 新建报表
        </el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="templates"
      stripe
    >
      <el-table-column
        prop="name"
        label="报表名称"
        width="180"
      />
      <el-table-column
        prop="description"
        label="描述"
        show-overflow-tooltip
      />
      <el-table-column
        prop="category"
        label="分类"
        width="120"
      />
      <el-table-column
        prop="createTime"
        label="创建时间"
        width="180"
      >
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="280"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            @click="openExecuteDialog(row)"
          >
            执行
          </el-button>
          <el-dropdown trigger="click" @command="(cmd) => handleExport(row, cmd)">
            <el-button type="success" size="small">
              导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="excel">Excel</el-dropdown-item>
                <el-dropdown-item command="csv">CSV</el-dropdown-item>
                <el-dropdown-item command="pdf">PDF</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button
            v-if="isAdmin"
            type="warning"
            size="small"
            @click="openEditDialog(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="isAdmin"
            type="danger"
            size="small"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="executeDialogVisible"
      :title="'执行报表 - ' + currentTemplate?.name"
      width="800px"
      destroy-on-close
    >
      <el-form
        v-if="paramDefs.length > 0"
        label-width="100px"
      >
        <el-form-item
          v-for="param in paramDefs"
          :key="param.name"
          :label="param.label"
        >
          <el-input-number
            v-if="param.type === 'number'"
            v-model="executeParams[param.name]"
            :placeholder="'请输入' + param.label"
            :min="1"
            controls-position="right"
          />
          <el-date-picker
            v-else-if="param.type === 'date'"
            v-model="executeParams[param.name]"
            type="date"
            :placeholder="'请选择' + param.label"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
          <el-input
            v-else
            v-model="executeParams[param.name]"
            :placeholder="'请输入' + param.label"
          />
        </el-form-item>
      </el-form>
      <el-empty
        v-else
        description="该报表无需参数"
        :image-size="60"
      />

      <div
        v-if="executeResults.length > 0"
        class="result-section"
      >
        <el-divider content-position="left">
          查询结果 ({{ executeResults.length }} 条)
        </el-divider>
        <el-table
          :data="executeResults"
          stripe
          max-height="400"
          style="width: 100%"
        >
          <el-table-column
            v-for="col in resultColumns"
            :key="col"
            :prop="col"
            :label="col"
            show-overflow-tooltip
          />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="executeDialogVisible = false">
          关闭
        </el-button>
        <el-button
          type="primary"
          :loading="executing"
          @click="handleExecute"
        >
          执行查询
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editDialogVisible"
      :title="isEditing ? '编辑报表' : '新建报表'"
      width="700px"
      destroy-on-close
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="100px"
      >
        <el-form-item
          label="报表名称"
          prop="name"
        >
          <el-input
            v-model="editForm.name"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item
          label="描述"
          prop="description"
        >
          <el-input
            v-model="editForm.description"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item
          label="分类"
          prop="category"
        >
          <el-input
            v-model="editForm.category"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item
          label="SQL模板"
          prop="sqlTemplate"
        >
          <el-input
            v-model="editForm.sqlTemplate"
            type="textarea"
            :rows="8"
            placeholder="SELECT ... FROM ... WHERE col = :paramName"
          />
        </el-form-item>
        <el-form-item
          label="参数定义"
          prop="parameters"
        >
          <el-input
            v-model="editForm.parameters"
            type="textarea"
            :rows="4"
            placeholder='[{"name":"startDate","label":"开始日期","type":"date"},{"name":"topN","label":"排行人数","type":"number","default":20}]'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  listTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  executeTemplate,
  getExportUrl
} from '@/api/report'
import { getToken } from '@/utils/auth'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const loading = ref(false)
const templates = ref([])
const selectedCategory = ref('')

const categories = computed(() => {
  const cats = new Set(templates.value.map(t => t.category).filter(Boolean))
  return [...cats]
})

const executeDialogVisible = ref(false)
const executing = ref(false)
const currentTemplate = ref(null)
const paramDefs = ref([])
const executeParams = ref({})
const executeResults = ref([])

const resultColumns = computed(() => {
  if (executeResults.value.length === 0) return []
  return Object.keys(executeResults.value[0])
})

const editDialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const saving = ref(false)
const editFormRef = ref(null)

const editForm = ref({
  name: '',
  description: '',
  category: '',
  sqlTemplate: '',
  parameters: '[]'
})

const editRules = {
  name: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  sqlTemplate: [{ required: true, message: '请输入SQL模板', trigger: 'blur' }]
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

async function loadTemplates() {
  loading.value = true
  try {
    const res = await listTemplates(selectedCategory.value || undefined)
    if (res.code === 0) {
      templates.value = res.data || []
    }
  } catch {
    console.error('加载报表模板失败')
  } finally {
    loading.value = false
  }
}

function openExecuteDialog(template) {
  currentTemplate.value = template
  executeResults.value = []
  try {
    paramDefs.value = JSON.parse(template.parameters || '[]')
  } catch {
    paramDefs.value = []
  }
  executeParams.value = {}
  for (const param of paramDefs.value) {
    if (param.default !== undefined) {
      executeParams.value[param.name] = param.default
    }
  }
  executeDialogVisible.value = true
}

async function handleExecute() {
  executing.value = true
  try {
    const res = await executeTemplate(currentTemplate.value.id, executeParams.value)
    if (res.code === 0) {
      executeResults.value = res.data || []
      if (executeResults.value.length === 0) {
        ElMessage.info('查询结果为空')
      }
    }
  } catch {
    console.error('执行报表失败')
  } finally {
    executing.value = false
  }
}

function handleExport(template, format = 'excel') {
  const token = getToken()
  const url = `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}${getExportUrl(template.id, format)}`
  const link = document.createElement('a')
  link.href = `${url}&access_token=${token}`
  link.target = '_blank'
  link.click()
}

function openCreateDialog() {
  isEditing.value = false
  editingId.value = null
  editForm.value = {
    name: '',
    description: '',
    category: '',
    sqlTemplate: '',
    parameters: '[]'
  }
  editDialogVisible.value = true
}

function openEditDialog(template) {
  isEditing.value = true
  editingId.value = template.id
  editForm.value = {
    name: template.name,
    description: template.description || '',
    category: template.category || '',
    sqlTemplate: template.sqlTemplate,
    parameters: template.parameters || '[]'
  }
  editDialogVisible.value = true
}

async function handleSave() {
  if (!editFormRef.value) return
  await editFormRef.value.validate()

  saving.value = true
  try {
    if (isEditing.value) {
      const res = await updateTemplate(editingId.value, editForm.value)
      if (res.code === 0) {
        ElMessage.success('报表模板更新成功')
        editDialogVisible.value = false
        loadTemplates()
      }
    } else {
      const res = await createTemplate(editForm.value)
      if (res.code === 0) {
        ElMessage.success('报表模板创建成功')
        editDialogVisible.value = false
        loadTemplates()
      }
    }
  } catch {
    console.error('保存报表模板失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(template) {
  try {
    await ElMessageBox.confirm(`确定要删除报表"${template.name}"吗？`, '删除确认', {
      type: 'warning'
    })
    const res = await deleteTemplate(template.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadTemplates()
    }
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadTemplates()
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
  }

  .header-actions {
    display: flex;
    align-items: center;
  }
}

.result-section {
  margin-top: 16px;
}
</style>
