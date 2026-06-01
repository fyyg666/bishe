<template>
  <div class="book-add" v-loading="loading">
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
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input
              v-model="form.isbn"
              placeholder="请输入ISBN"
              @blur="checkIsbnDuplicate"
            />
            <el-button
              type="primary"
              :loading="isbnLookupLoading"
              @click="handleIsbnLookup"
            >
              ISBN查询
            </el-button>
          </div>
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
          label="封面图片"
          prop="coverImage"
        >
          <el-upload
            class="cover-uploader"
            action="/api/v1/files/upload"
            :headers="uploadHeaders"
            :data="{ directory: 'covers' }"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
            accept="image/jpeg,image/png,image/gif"
          >
            <img
              v-if="form.coverImage"
              :src="form.coverImage"
              class="cover-preview"
            >
            <el-icon
              v-else
              class="cover-uploader-icon"
            >
              <Plus />
            </el-icon>
          </el-upload>
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
import { Plus } from '@element-plus/icons-vue'
import { useBookStore } from '@/stores/book'
import { useCategories } from '@/composables/useCategories'
import { getToken } from '@/utils/auth'
import { checkIsbn, lookupIsbn } from '@/api/book'

const route = useRoute()
const router = useRouter()
const bookStore = useBookStore()

const formRef = ref(null)
const loading = ref(false)
const isbnLookupLoading = ref(false)

const { categoryOptions, loadCategories } = useCategories()

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
  category: '',
  publisher: '',
  publishDate: '',
  price: 0,
  totalCount: 1,
  coverImage: '',
  description: ''
})

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${getToken()}`
}))

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
    if (isNaN(id)) {
      router.push('/books')
      return
    }
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
      form.coverImage = book.coverImage || ''
      form.description = book.description || ''
    }
  }
})

function handleUploadSuccess(response) {
  if (response.data && response.data.url) {
    form.coverImage = response.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error('上传失败，未获取到图片地址')
  }
}

function beforeUpload(file) {
  const isImage = ['image/jpeg', 'image/png', 'image/gif'].includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('封面图片仅支持 JPG/PNG/GIF 格式')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('封面图片大小不能超过 10MB')
    return false
  }
  return true
}

async function handleIsbnLookup() {
  if (!form.isbn || form.isbn.trim() === '') {
    ElMessage.warning('请先输入ISBN号')
    return
  }
  isbnLookupLoading.value = true
  try {
    const res = await lookupIsbn(form.isbn)
    const data = res.data
    if (!data) {
      ElMessage.info('未查询到该ISBN的图书信息')
      return
    }
    if (data.title) form.title = data.title
    if (data.author) form.author = data.author
    if (data.publisher) form.publisher = data.publisher
    if (data.publishDate) form.publishDate = data.publishDate
    if (data.description) form.description = data.description
    if (data.coverUrl) form.coverImage = data.coverUrl
    ElMessage.success(`查询成功（来源：${data.source || '未知'}）`)
  } catch (error) {
    ElMessage.error(error.message || 'ISBN查询失败')
  } finally {
    isbnLookupLoading.value = false
  }
}

async function checkIsbnDuplicate() {
  if (!form.isbn || form.isbn.trim() === '') return
  try {
    const res = await checkIsbn(form.isbn)
    if (res.data && res.data.exists) {
      ElMessage.warning('该ISBN已存在，请确认是否重复录入')
    }
  } catch {
    // API might not exist yet, ignore
  }
}

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
        coverImage: form.coverImage || null,
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

  .cover-uploader :deep(.el-upload) {
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
    width: 178px;
    height: 178px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .cover-uploader :deep(.el-upload:hover) {
    border-color: var(--el-color-primary);
  }

  .cover-uploader-icon {
    font-size: 28px;
    color: #8c939d;
  }

  .cover-preview {
    width: 178px;
    height: 178px;
    object-fit: cover;
    display: block;
  }
}
</style>
