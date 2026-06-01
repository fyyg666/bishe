<template>
  <el-card class="marc-field-editor" shadow="never">
    <div class="field-header">
      <el-input v-model="field.tag" placeholder="标签" style="width:80px" maxlength="3" @change="onTagChange" />
      <el-input v-model="field.indicator1" placeholder="I1" style="width:50px" maxlength="1" />
      <el-input v-model="field.indicator2" placeholder="I2" style="width:50px" maxlength="1" />
      <span class="tag-label">{{ tagLabel }}</span>
      <el-button type="danger" :icon="Delete" circle size="small" @click="$emit('remove')" />
    </div>
    <div class="subfields">
      <div v-for="(sf, idx) in subfieldList" :key="idx" class="subfield-row">
        <el-input v-model="sf.code" placeholder="代码" style="width:60px" maxlength="1" />
        <el-input v-model="sf.value" placeholder="值" style="flex:1" @input="updateDisplayValue" />
        <el-button type="danger" :icon="Delete" circle size="small" @click="removeSubfield(idx)" />
      </div>
      <el-button type="primary" :icon="Plus" size="small" @click="addSubfield">添加子字段</el-button>
    </div>
    <div v-if="field.displayValue" class="display-value">
      <el-text type="info" size="small">{{ field.displayValue }}</el-text>
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

const props = defineProps({
  field: { type: Object, required: true }
})
const emit = defineEmits(['remove', 'update'])

const TAG_LABELS = {
  '001': '控制号', '008': '定长字段', '010': 'ISBN', '020': 'ISBN',
  '100': '作者', '245': '题名', '250': '版本', '260': '出版',
  '300': '载体描述', '490': '丛编', '500': '附注', '650': '主题',
  '700': '附加作者', '905': '馆藏信息'
}

const tagLabel = computed(() => TAG_LABELS[props.field.tag] || '')

const subfieldList = ref([])

watch(() => props.field.subfields, (val) => {
  if (val) {
    try {
      subfieldList.value = typeof val === 'string' ? JSON.parse(val) : val
    } catch {
      subfieldList.value = []
    }
  } else {
    subfieldList.value = []
  }
}, { immediate: true })

const addSubfield = () => {
  subfieldList.value.push({ code: 'a', value: '' })
  syncSubfields()
}

const removeSubfield = (idx) => {
  subfieldList.value.splice(idx, 1)
  syncSubfields()
}

const syncSubfields = () => {
  props.field.subfields = JSON.stringify(subfieldList.value)
  emit('update', props.field)
}

const updateDisplayValue = () => {
  props.field.displayValue = subfieldList.value
    .filter(sf => sf.value)
    .map(sf => `$${sf.code}${sf.value}`)
    .join(' ')
  syncSubfields()
}

const onTagChange = () => {
  emit('update', props.field)
}
</script>

<style scoped>
.marc-field-editor { margin-bottom: 8px; }
.field-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.tag-label { color: #909399; font-size: 12px; min-width: 60px; }
.subfields { padding-left: 12px; }
.subfield-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.display-value { margin-top: 4px; padding-left: 12px; }
</style>
