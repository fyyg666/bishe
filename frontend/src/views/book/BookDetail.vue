<template>
  <div class="book-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>图书详情</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      
      <el-descriptions :column="2" border v-if="book">
        <el-descriptions-item label="书名">{{ book.name }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ book.author }}</el-descriptions-item>
        <el-descriptions-item label="ISBN">{{ book.isbn }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ book.category }}</el-descriptions-item>
        <el-descriptions-item label="出版社">{{ book.publisher }}</el-descriptions-item>
        <el-descriptions-item label="出版日期">{{ book.publishDate }}</el-descriptions-item>
        <el-descriptions-item label="库存">{{ book.stock }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥{{ book.price }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ book.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="book.stock > 0 ? 'success' : 'danger'">
            {{ book.stock > 0 ? '在架' : '借出' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ book.createTime }}</el-descriptions-item>
      </el-descriptions>
      
      <div class="action-bar">
        <el-button type="primary" @click="handleBorrow">借阅此书</el-button>
        <el-button @click="$router.push('/books')">返回列表</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBookStore } from '@/stores/book'

const route = useRoute()
const router = useRouter()
const bookStore = useBookStore()

const loading = ref(false)
const book = ref(null)

onMounted(() => {
  loadBookDetail()
})

async function loadBookDetail() {
  loading.value = true
  try {
    const id = route.params.id
    await bookStore.fetchBookDetail(id)
    book.value = bookStore.currentBook
  } catch (error) {
    ElMessage.error('加载图书详情失败')
  } finally {
    loading.value = false
  }
}

function handleBorrow() {
  if (book.value?.stock <= 0) {
    ElMessage.warning('该书目前无库存')
    return
  }
  router.push({ path: '/borrows/page', query: { bookId: book.value.id } })
}
</script>

<style lang="scss" scoped>
.book-detail {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .action-bar {
    margin-top: 20px;
    display: flex;
    gap: 10px;
  }
}
</style>
