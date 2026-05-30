<template>
  <div class="book-add">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑图书' : '添加图书' }}</span>
          <el-button @click="$router.back()">
            取消
          </el-button>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="书名"
          prop="title"
        >
          <el-input
            v-model="form.title"
            placeholder="请输入书名"
          />
        </el-form-item>
        
        <el-form-item
          label="作者"
          prop="author"
        >
          <el-input
            v-model="form.author"
            placeholder="请输入作者"
          />
        </el-form-item>
        
        <el-form-item
          label="ISBN"
          prop="isbn"
        >
          <el-input
            v-model="form.isbn"
            placeholder="请输入ISBN"
          />
        </el-form-item>
        
        <el-form-item
          label="分类"
          prop="category"
        >
          <el-select
            v-model="form.category"
            placeholder="请选择分类"
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item
          label="出版社"
          prop="publisher"
        >
          <el-input
            v-model="form.publisher"
            placeholder="请输入出版社"
          />
        </el-form-item>
        
        <el-form-item
          label="出版日期"
          prop="publishDate"
        >
          <el-date-picker
            v-model="form.publishDate"
            type="date"
            placeholder="选择出版日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item
          label="价格"
          prop="price"
        >
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :controls="false"
          />
        </el-form-item>
        
        <el-form-item
          label="库存"
          prop="totalCount"
        >
          <el-input-number
            v-model="form.totalCount"
            :min="0"
            :controls="false"
          />
        </el-form-item>
        
        <el-form-item
          label="简介"
          prop="description"
        >
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入图书简介"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            {{ loading ? '提交中...' : '提 交' }}
          </el-button>
          <el-button @click="$router.back()">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBookStore } from '@/stores/book'
import { getCategories } from '@/api/category'

const route = useRoute()
const router = useRouter()
const bookStore = useBookStore()

const formRef = ref(null)
const loading = ref(false)

// 分类选项：优先从API加载，API不可用时使用硬编码兜底
const categoryOptions = ref([
  { label: '文学', value: '文学' },
  { label: '科技', value: '科技' },
  { label: '历史', value: '历史' },
  { label: '艺术', value: '艺术' },
  { label: '哲学', value: '哲学' },
  { label: '经济', value: '经济' }
])
const HARDCODED_CATEGORIES = ['文学', '科技', '历史', '艺术', '哲学', '经济']

async function loadCategories() {
  try {
    const res = await getCategories()
    const list = res.data || []
    if (Array.isArray(list) && list.length > 0) {
      categoryOptions.value = list.map(c => ({
        label: c.name || c,
        value: c.name || c
      }))
    }
  } catch {
    // API不可用，使用硬编码兜底
    categoryOptions.value = HARDCODED_CATEGORIES.map(c => ({ label: c, value: c }))
  }
}

const isEdit = computed(() => !!route.query.id)

// 分类名→ID映射（与后端 book_category 表一致）
const categoryNameToId = {
  '文学': 1, '科技': 2, '历史': 3,
  '艺术': 4, '哲学': 5, '经济': 6
}

const form = reactive({
  title: '',
  author: '',
  isbn: '',
  category: '',       // UI显示用（中文名）
  publisher: '',
  publishDate: '',
  price: 0,
  totalCount: 1,
  description: ''
})

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

onMounted(async () => {
  await loadCategories()
  if (isEdit.value) {
    const id = parseInt(route.query.id)
    await bookStore.fetchBookDetail(id)
    const book = bookStore.currentBook
    if (book) {
      form.title = book.title || ''
      form.author = book.author || ''
      form.isbn = book.isbn || ''
      form.publisher = book.publisher || ''
      form.publishDate = book.publishDate || ''
      form.price = book.price || 0
      form.totalCount = book.totalCount || 1
      form.description = book.description || ''
    }
  }
})

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      // 构建后端BookRequest格式
      const requestData = {
        title: form.title,
        author: form.author,
        isbn: form.isbn,
        categoryId: categoryNameToId[form.category] || 1,
        publisher: form.publisher,
        publishDate: form.publishDate ? form.publishDate.substring(0, 7) : null,
        price: form.price,
        totalCount: form.totalCount,
        description: form.description
      }

      if (isEdit.value) {
        const id = parseInt(route.query.id)
        await bookStore.editBook(id, requestData)
        ElMessage.success('编辑成功')
      } else {
        await bookStore.createBook(requestData)
        ElMessage.success('添加成功')
      }
      router.push('/books')
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.book-add {
  :deep(.el-card) {
    border: none;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }

  :deep(.el-card__body) {
    padding: 32px 40px;
  }

  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .el-input-number {
    width: 200px;
  }
}
</style>
