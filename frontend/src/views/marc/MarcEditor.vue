<template>
  <div class="marc-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑MARC记录' : '新建MARC记录' }}</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="记录类型" prop="recordType">
              <el-select v-model="form.recordType" placeholder="请选择记录类型">
                <el-option label="图书" value="BOOK" />
                <el-option label="连续出版物" value="SERIAL" />
                <el-option label="地图" value="MAP" />
                <el-option label="视听资料" value="AV" />
                <el-option label="混合资料" value="MIXED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="控制号" prop="controlNumber">
              <el-input v-model="form.controlNumber" placeholder="如：001控制号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="待审核" value="PENDING_REVIEW" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">MARC字段</el-divider>

        <div class="fields-list">
          <MarcFieldEditor
            v-for="(field, idx) in form.fields"
            :key="idx"
            :field="field"
            @remove="removeField(idx)"
            @update="onFieldUpdate"
          />
        </div>

        <div class="add-field-area">
          <el-dropdown trigger="click" @command="addField">
            <el-button type="primary" :icon="Plus">添加字段</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="010">010 - ISBN</el-dropdown-item>
                <el-dropdown-item command="100">100 - 作者</el-dropdown-item>
                <el-dropdown-item command="245">245 - 题名</el-dropdown-item>
                <el-dropdown-item command="250">250 - 版本</el-dropdown-item>
                <el-dropdown-item command="260">260 - 出版</el-dropdown-item>
                <el-dropdown-item command="300">300 - 载体描述</el-dropdown-item>
                <el-dropdown-item command="650">650 - 主题</el-dropdown-item>
                <el-dropdown-item command="905">905 - 馆藏信息</el-dropdown-item>
                <el-dropdown-item command="custom" divided>自定义标签</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <el-form-item class="form-actions">
          <el-button type="primary" :loading="saving" @click="handleSave('DRAFT')">
            保存草稿
          </el-button>
          <el-button type="success" :loading="saving" @click="handleSave('PUBLISHED')">
            保存并发布
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import MarcFieldEditor from '@/components/marc/MarcFieldEditor.vue'
import { getMarcRecord, createMarcRecord, updateMarcRecord } from '@/api/marc'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const saving = ref(false)

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  recordType: 'BOOK',
  controlNumber: '',
  status: 'DRAFT',
  fields: []
})

const rules = {
  recordType: [{ required: true, message: '请选择记录类型', trigger: 'change' }],
  controlNumber: [{ required: true, message: '请输入控制号', trigger: 'blur' }]
}

const DEFAULT_SUBFIELDS = {
  '010': [{ code: 'a', value: '' }],
  '100': [{ code: 'a', value: '' }],
  '245': [{ code: 'a', value: '' }, { code: 'b', value: '' }, { code: 'c', value: '' }],
  '250': [{ code: 'a', value: '' }],
  '260': [{ code: 'a', value: '' }, { code: 'b', value: '' }, { code: 'c', value: '' }],
  '300': [{ code: 'a', value: '' }, { code: 'b', value: '' }, { code: 'c', value: '' }],
  '650': [{ code: 'a', value: '' }],
  '905': [{ code: 'a', value: '' }, { code: 'd', value: '' }]
}

function createField(tag, indicator1 = ' ', indicator2 = ' ') {
  const subfields = DEFAULT_SUBFIELDS[tag] || [{ code: 'a', value: '' }]
  return {
    tag,
    indicator1,
    indicator2,
    subfields: JSON.stringify(subfields),
    displayValue: ''
  }
}

function addField(tag) {
  if (tag === 'custom') {
    form.fields.push(createField(''))
  } else {
    form.fields.push(createField(tag))
  }
}

function removeField(idx) {
  form.fields.splice(idx, 1)
}

function onFieldUpdate() {
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getMarcRecord(route.params.id)
      const data = res.data
      if (data) {
        form.recordType = data.recordType || 'BOOK'
        form.controlNumber = data.controlNumber || ''
        form.status = data.status || 'DRAFT'
        if (data.fields && Array.isArray(data.fields)) {
          form.fields = data.fields.map(f => ({
            tag: f.tag || '',
            indicator1: f.indicator1 ?? ' ',
            indicator2: f.indicator2 ?? ' ',
            subfields: typeof f.subfields === 'string' ? f.subfields : JSON.stringify(f.subfields || []),
            displayValue: f.displayValue || ''
          }))
        }
      }
    } catch {
      ElMessage.error('加载MARC记录失败')
    }
  } else {
    form.fields = [
      createField('001'),
      createField('010'),
      createField('100'),
      createField('245'),
      createField('260')
    ]
  }
})

async function handleSave(status) {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const payload = {
      recordType: form.recordType,
      controlNumber: form.controlNumber,
      status,
      fields: form.fields.map(f => ({
        tag: f.tag,
        indicator1: f.indicator1 || ' ',
        indicator2: f.indicator2 || ' ',
        subfields: f.subfields,
        displayValue: f.displayValue || ''
      }))
    }

    if (isEdit.value) {
      await updateMarcRecord(route.params.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createMarcRecord(payload)
      ElMessage.success('创建成功')
    }
    router.push('/marc')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.marc-editor {
  :deep(.el-card) {
    border: none;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .fields-list {
    margin-bottom: 16px;
  }

  .add-field-area {
    margin-bottom: 24px;
  }

  .form-actions {
    margin-top: 16px;
  }
}
</style>
