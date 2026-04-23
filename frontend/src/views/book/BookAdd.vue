<template>
  <div class="book-add">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑图书' : '添加图书' }}</span>
          <el-button @click="$router.back()">取消</el-button>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="书名" prop="name">
          <el-input v-model="form.name" placeholder="请输入书名" />
        </el-form-item>
        
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>
        
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" placeholder="请输入ISBN" />
        </el-form-item>
        
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="文学" value="文学" />
            <el-option label="科技" value="科技" />
            <el-option label="历史" value="历史" />
            <el-option label="艺术" value="艺术" />
            <el-option label="哲学" value="哲学" />
            <el-option label="经济" value="经济" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="form.publisher" placeholder="请输入出版社" />
        </el-form-item>
        
        <el-form-item label="出版日期" prop="publishDate">
          <el-date-picker
            v-model="form.publishDate"
            type="date"
            placeholder="选择出版日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :controls="false" />
        </el-form-item>
        
        <el-form-item label="简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入图书简介"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            {{ loading ? '提交中...' : '提 交' }}
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
import { useBookStore } from '@/stores/book'

const route = useRoute()
const router = useRouter()
const bookStore = useBookStore()

const formRef = ref(null)
const loading = ref(false)

const isEdit = computed(() => !!route.query.id)

const form = reactive({
  name: '',
  author: '',
  isbn: '',
  category: '',
  publisher: '',
  publishDate: '',
  price: 0,
  stock: 1,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

onMounted(async () => {
  if (isEdit.value) {
    const id = route.query.id
    await bookStore.fetchBookDetail(id)
    const book = bookStore.currentBook
    if (book) {
      Object.keys(form).forEach(key => {
        if (book[key] !== undefined) {
          form[key] = book[key]
        }
      })
    }
  }
})

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      if (isEdit.value) {
        form.id = parseInt(route.query.id)
        await bookStore.editBook(form)
        ElMessage.success('编辑成功')
      } else {
        await bookStore.createBook(form)
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
