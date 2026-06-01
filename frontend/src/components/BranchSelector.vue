<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    style="width: 100%"
    @update:model-value="handleChange"
  >
    <el-option
      v-for="item in branchList"
      :key="item.id"
      :label="item.name"
      :value="item.id"
    />
  </el-select>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listBranches } from '@/api/branch'

defineOptions({ name: 'BranchSelector' })

const props = defineProps({
  modelValue: {
    type: [Number, String],
    default: null
  },
  placeholder: {
    type: String,
    default: '选择分馆'
  },
  clearable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue'])

const branchList = ref([])

function handleChange(val) {
  emit('update:modelValue', val)
}

async function loadBranches() {
  try {
    const res = await listBranches({ status: '1' })
    if (res.code === 0) {
      branchList.value = res.data || []
    }
  } catch (error) {
    console.error('加载分馆列表失败:', error)
  }
}

onMounted(() => {
  loadBranches()
})
</script>
