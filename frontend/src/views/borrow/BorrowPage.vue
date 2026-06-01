<template>
  <div class="borrow-page" v-loading="loading">
    <el-card>
      <template #header>
        <span>借阅图书</span>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="图书ID"
          prop="bookId"
        >
          <el-input-number
            v-model="form.bookId"
            :min="1"
            :controls="false"
          />
          <span class="form-tip">输入要借阅的图书ID</span>
        </el-form-item>
        
        <el-form-item
          label="借阅天数"
          prop="borrowDays"
        >
          <el-input-number
            v-model="form.borrowDays"
            :min="1"
            :max="90"
            :controls="false"
          />
          <span class="form-tip">最长90天</span>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            {{ loading ? '提交中...' : '确认借阅' }}
          </el-button>
          <el-button @click="$router.push('/books')">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBorrowStore } from '@/stores/borrow'

const route = useRoute()
const router = useRouter()
const borrowStore = useBorrowStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  bookId: null,
  borrowDays: 30
})

const rules = {
  bookId: [{ required: true, message: '请输入图书ID', trigger: 'blur' }],
  borrowDays: [{ required: true, message: '请输入借阅天数', trigger: 'blur' }]
}

onMounted(() => {
  if (route.query.bookId) {
    form.bookId = parseInt(route.query.bookId)
  }
})

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      await borrowStore.borrow(form)
      ElMessage.success('借阅成功')
      router.push('/borrows')
    } catch (error) {
      ElMessage.error(error.message || '借阅失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.borrow-page {
  max-width: 600px;
  
  .form-tip {
    margin-left: 10px;
    color: #909399;
    font-size: 12px;
  }
  
  .el-input-number {
    width: 200px;
  }
}
</style>
