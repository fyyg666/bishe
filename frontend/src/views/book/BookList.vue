<template>
  <div class="book-list">
    <!-- Search Bar — Apple-style single input + filters -->
    <div class="search-bar">
      <div class="search-main">
        <el-input
          v-model="searchForm.name"
          placeholder="搜索书名、作者、ISBN…"
          :prefix-icon="SearchIcon"
          clearable
          size="large"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-button
          type="primary"
          size="large"
          @click="handleSearch"
        >
          <el-icon><Search /></el-icon>
          <span v-if="!isMobile" class="search-btn-text">搜索</span>
        </el-button>
      </div>
      <div class="search-actions">
        <el-button
          text
          @click="showFilters = !showFilters"
        >
          <el-icon><Filter /></el-icon>
          {{ showFilters ? '收起筛选' : '高级筛选' }}
        </el-button>
      </div>
    </div>

    <!-- Collapsible Filters -->
    <el-collapse-transition>
      <div v-show="showFilters" class="filter-panel">
        <el-form :model="searchForm" label-width="60px" class="filter-form">
          <div class="filter-row">
            <el-form-item label="书名">
              <el-input v-model="searchForm.name" placeholder="书名关键词" clearable />
            </el-form-item>
            <el-form-item label="作者">
              <el-input v-model="searchForm.author" placeholder="作者姓名" clearable />
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="searchForm.category" placeholder="选择分类" clearable>
                <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
              </el-select>
            </el-form-item>
          </div>
          <div v-show="showAdvanced" class="filter-row advanced-fields">
            <el-form-item label="ISBN">
              <el-input v-model="advancedForm.isbn" placeholder="ISBN号" clearable />
            </el-form-item>
            <el-form-item label="出版社">
              <el-input v-model="advancedForm.publisher" placeholder="出版社名称" clearable />
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="advancedForm.category" placeholder="选择分类" clearable>
                <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
              </el-select>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button text type="primary" @click="showAdvanced = !showAdvanced">
              {{ showAdvanced ? '收起更多' : '更多条件' }}
            </el-button>
            <el-select v-model="advancedForm.orderBy" placeholder="排序方式" clearable class="order-select">
              <el-option label="默认" value="" />
              <el-option label="借阅次数" value="borrowCount" />
              <el-option label="出版日期" value="publishDate" />
            </el-select>
          </div>
        </el-form>
      </div>
    </el-collapse-transition>

    <!-- Toolbar -->
    <div class="table-toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="Plus" @click="$router.push('/books/add')">
          <span v-if="!isMobile">添加图书</span>
        </el-button>
        <el-button :icon="Download" @click="handleExport">
          <span v-if="!isMobile">导出</span>
        </el-button>
        <el-button :icon="Upload" @click="importDialogVisible = true">
          <span v-if="!isMobile">导入</span>
        </el-button>
      </div>
    </div>

    <!-- Main Content -->
    <div class="main-content">
      <!-- Facet Sidebar (Desktop) -->
      <div v-if="!isMobile" class="facet-sidebar">
        <div class="facet-section">
          <div class="facet-title">分类筛选</div>
          <div v-if="categoryFacets.length" class="facet-list">
            <div
              v-for="facet in categoryFacets"
              :key="facet.value"
              class="facet-item"
              :class="{ active: activeCategoryFacet === facet.value }"
              @click="toggleCategoryFacet(facet.value)"
            >
              <span class="facet-label">{{ facet.label }}</span>
              <span class="facet-count">{{ facet.count }}</span>
            </div>
          </div>
          <div v-else class="facet-empty">暂无数据</div>
        </div>
        <div class="facet-section">
          <div class="facet-title">热门作者</div>
          <div v-if="authorFacets.length" class="facet-list">
            <div
              v-for="facet in authorFacets"
              :key="facet.value"
              class="facet-item"
              :class="{ active: activeAuthorFacet === facet.value }"
              @click="toggleAuthorFacet(facet.value)"
            >
              <span class="facet-label">{{ facet.label }}</span>
              <span class="facet-count">{{ facet.count }}</span>
            </div>
          </div>
          <div v-else class="facet-empty">暂无数据</div>
        </div>
      </div>

      <!-- Table -->
      <div class="table-area">
        <el-table
          v-loading="loading"
          :data="books"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="书名" min-width="150" />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column v-if="!isMobile" prop="isbn" label="ISBN" width="150" />
          <el-table-column v-if="!isMobile" prop="category" label="分类" width="100" />
          <el-table-column v-if="!isMobile" prop="publisher" label="出版社" width="150" />
          <el-table-column prop="availableCount" label="库存" width="70" align="center" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.availableCount > 0 ? 'success' : 'danger'" size="small">
                {{ row.availableCount > 0 ? '在架' : '借出' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" :width="isMobile ? 56 : 200" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-tooltip content="查看详情" placement="top">
                  <el-button type="primary" link :icon="View" @click="handleDetail(row)" />
                </el-tooltip>
                <el-tooltip content="编辑" placement="top">
                  <el-button type="primary" link :icon="Edit" @click="handleEdit(row)" />
                </el-tooltip>
                <el-tooltip v-if="!isMobile" content="删除" placement="top">
                  <el-button type="danger" link :icon="Delete" @click="handleDelete(row)" />
                </el-tooltip>
                <el-tooltip v-if="!isMobile" content="荐购" placement="top">
                  <el-button type="warning" link :icon="ShoppingCart" @click="handleSuggest(row)" />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
            :size="isMobile ? 'small' : 'default'"
            @size-change="loadBooks"
            @current-change="loadBooks"
          />
        </div>
      </div>
    </div>

    <!-- Import Dialog -->
    <el-dialog v-model="importDialogVisible" title="批量导入图书" width="520px">
      <el-upload
        ref="uploadRef"
        :action="importAction"
        :headers="importHeaders"
        :on-success="handleImportSuccess"
        :on-error="handleImportError"
        :before-upload="beforeImportUpload"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将Excel文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx / .xls 格式，表头需包含：书名、作者、ISBN、分类、出版社、出版日期、价格、总库存、简介
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">确认导入</el-button>
      </template>
    </el-dialog>

    <!-- Import Result Dialog -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="总数">{{ importResult.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="成功">
          <el-tag type="success" size="small">{{ importResult.successCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败">
          <el-tag type="danger" size="small">{{ importResult.failCount }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="importResult.errors && importResult.errors.length" class="import-errors">
        <p class="import-error-title">错误详情：</p>
        <el-scrollbar max-height="200px">
          <ul class="import-error-list">
            <li v-for="(err, idx) in importResult.errors" :key="idx">{{ err }}</li>
          </ul>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- Suggest Dialog -->
    <el-dialog v-model="suggestDialogVisible" title="荐购图书" width="520px">
      <el-form :model="suggestForm" label-width="80px">
        <el-form-item label="书名">
          <el-input v-model="suggestForm.title" disabled />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="suggestForm.author" disabled />
        </el-form-item>
        <el-form-item label="ISBN">
          <el-input v-model="suggestForm.isbn" disabled />
        </el-form-item>
        <el-form-item label="荐购理由">
          <el-input v-model="suggestForm.reason" type="textarea" :rows="3" placeholder="请输入荐购理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="suggestDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="suggesting" @click="submitSuggestion">提交荐购</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'Books' })

import { ref, reactive, computed, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Upload, UploadFilled, Search, Filter, View, Edit, Delete, ShoppingCart } from '@element-plus/icons-vue'
import { useBookStore } from '@/stores/book'
import { useCategories } from '@/composables/useCategories'
import { useResponsive } from '@/composables/useResponsive'
import { getToken } from '@/utils/auth'
import { advancedSearchBooks, getCategoryFacet, getAuthorFacet } from '@/api/book'
import { createSuggestion } from '@/api/suggestion'

const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 10

const router = useRouter()
const bookStore = useBookStore()
const { isMobile } = useResponsive()

const loading = ref(false)
const books = ref([])
const total = ref(0)
const selectedBooks = ref([])
const showFilters = ref(false)
const showAdvanced = ref(false)

const SearchIcon = h(Search)

const { categoryOptions, loadCategories } = useCategories()

const searchForm = reactive({
  name: '',
  author: '',
  category: ''
})

const pagination = reactive({
  current: DEFAULT_PAGE,
  size: DEFAULT_PAGE_SIZE
})

const isAdvancedSearch = ref(false)

const advancedForm = reactive({
  isbn: '',
  publisher: '',
  category: '',
  orderBy: ''
})

const categoryFacets = ref([])
const authorFacets = ref([])
const activeCategoryFacet = ref('')
const activeAuthorFacet = ref('')

onMounted(() => {
  loadBooks()
  loadCategories()
  loadFacets()
})

async function loadFacets() {
  try {
    const [catRes, authRes] = await Promise.all([
      getCategoryFacet(),
      getAuthorFacet()
    ])
    categoryFacets.value = catRes.data || []
    authorFacets.value = authRes.data || []
  } catch {
    // silently ignore
  }
}

async function loadBooks() {
  loading.value = true
  try {
    if (isAdvancedSearch.value) {
      await loadAdvancedSearchResults()
    } else {
      await loadBasicSearchResults()
    }
  } catch {
    ElMessage.error('加载图书列表失败')
  } finally {
    loading.value = false
  }
}

async function loadBasicSearchResults() {
  const params = {
    keyword: searchForm.name || undefined,
    current: pagination.current,
    size: pagination.size
  }
  if (searchForm.author) params.author = searchForm.author
  if (searchForm.category) params.categoryId = searchForm.category
  if (activeCategoryFacet.value) params.categoryId = activeCategoryFacet.value
  if (activeAuthorFacet.value) params.author = activeAuthorFacet.value
  await bookStore.fetchBooks(params)
  books.value = bookStore.books
  total.value = bookStore.total
}

async function loadAdvancedSearchResults() {
  const params = {
    current: pagination.current,
    size: pagination.size
  }
  if (searchForm.name) params.title = searchForm.name
  if (searchForm.author) params.author = searchForm.author
  if (searchForm.category) params.categoryId = searchForm.category
  if (advancedForm.isbn) params.isbn = advancedForm.isbn
  if (advancedForm.publisher) params.publisher = advancedForm.publisher
  if (advancedForm.category) params.categoryId = advancedForm.category
  if (advancedForm.orderBy) params.orderBy = advancedForm.orderBy
  if (activeCategoryFacet.value) params.categoryId = activeCategoryFacet.value
  if (activeAuthorFacet.value) params.author = activeAuthorFacet.value
  const res = await advancedSearchBooks(params)
  books.value = res.data?.records || res.data?.list || []
  total.value = res.data?.total || 0
}

function handleSearch() {
  isAdvancedSearch.value = showAdvanced.value
  pagination.current = DEFAULT_PAGE
  loadBooks()
}

function handleReset() {
  Object.keys(searchForm).forEach(key => { searchForm[key] = '' })
  advancedForm.isbn = ''
  advancedForm.publisher = ''
  advancedForm.category = ''
  advancedForm.orderBy = ''
  activeCategoryFacet.value = ''
  activeAuthorFacet.value = ''
  isAdvancedSearch.value = false
  showAdvanced.value = false
  handleSearch()
}

function toggleCategoryFacet(value) {
  activeCategoryFacet.value = activeCategoryFacet.value === value ? '' : value
  pagination.current = DEFAULT_PAGE
  loadBooks()
}

function toggleAuthorFacet(value) {
  activeAuthorFacet.value = activeAuthorFacet.value === value ? '' : value
  pagination.current = DEFAULT_PAGE
  loadBooks()
}

function handleSelectionChange(selection) {
  selectedBooks.value = selection
}

function handleDetail(row) { router.push(`/books/${row.id}`) }
function handleEdit(row) { router.push(`/books/add?id=${row.id}`) }

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该图书吗？', '提示', { type: 'warning' })
    await bookStore.removeBook(row.id)
    ElMessage.success('删除成功')
    loadBooks()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}

const importDialogVisible = ref(false)
const importResultVisible = ref(false)
const importLoading = ref(false)
const uploadRef = ref(null)
const importResult = reactive({ totalCount: 0, successCount: 0, failCount: 0, errors: [] })
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const importAction = `${baseURL}/books/import`
const importHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))

function beforeImportUpload(file) {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('仅支持 Excel 文件（.xlsx / .xls）')
    return false
  }
  return true
}

function handleImportSuccess(response) {
  importLoading.value = false
  importDialogVisible.value = false
  if (response.code === 0) {
    Object.assign(importResult, response.data)
    importResultVisible.value = true
    loadBooks()
  } else {
    ElMessage.error(response.message || '导入失败')
  }
}

function handleImportError() {
  importLoading.value = false
  ElMessage.error('导入失败，请检查文件格式')
}

function submitImport() {
  uploadRef.value?.submit()
  importLoading.value = true
}

function handleExport() {
  const params = new URLSearchParams()
  if (searchForm.name) params.append('keyword', searchForm.name)
  if (searchForm.category) params.append('categoryId', searchForm.category)
  const token = getToken()
  const url = `${baseURL}/books/export?${params.toString()}`
  fetch(url, { headers: { 'Authorization': `Bearer ${token}` } })
    .then(res => res.blob())
    .then(blob => {
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `图书列表_${new Date().toISOString().slice(0, 10)}.xlsx`
      link.click()
      URL.revokeObjectURL(link.href)
      ElMessage.success('导出成功')
    })
    .catch(() => ElMessage.error('导出失败'))
}

const suggestDialogVisible = ref(false)
const suggesting = ref(false)
const suggestForm = ref({ title: '', author: '', isbn: '', reason: '' })

function handleSuggest(row) {
  suggestForm.value = { title: row.title, author: row.author, isbn: row.isbn, reason: '' }
  suggestDialogVisible.value = true
}

async function submitSuggestion() {
  suggesting.value = true
  try {
    await createSuggestion(suggestForm.value)
    ElMessage.success('荐购建议提交成功')
    suggestDialogVisible.value = false
  } catch (e) { ElMessage.error(e.message) } finally { suggesting.value = false }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.book-list {
  padding: 0;
}

/* ── Search Bar ─────────────────────────── */
.search-bar {
  margin-bottom: $space-5;

  .search-main {
    display: flex;
    gap: $space-3;

    @include mobile {
      flex-direction: column;
    }
  }

  .search-input {
    flex: 1;

    :deep(.el-input__wrapper) {
      background: $bg-input;
      border-radius: $radius-md;
      border: 1px solid transparent;
      box-shadow: none !important;
      height: 44px;

      &:hover {
        background: $gray-200;
      }

      &.is-focus {
        background: $bg-card;
        border-color: $primary;
        box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.12) !important;
      }
    }

    :deep(.el-input__inner) {
      font-size: $font-size-lg;

      &::placeholder {
        color: $text-placeholder;
      }
    }
  }

  .search-btn-text {
    margin-left: $space-1;
  }

  .search-actions {
    margin-top: $space-3;
    display: flex;
    gap: $space-3;
  }
}

/* ── Filter Panel ───────────────────────── */
.filter-panel {
  @include card($padding: $space-5, $radius: $radius-lg);
  margin-bottom: $space-5;

  .filter-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: $space-4;
    margin-bottom: $space-3;

    @include tablet {
      grid-template-columns: repeat(2, 1fr);
    }

    @include mobile {
      grid-template-columns: 1fr;
    }
  }

  .advanced-fields {
    padding-top: $space-3;
    border-top: 1px solid $border-light;
  }

  .filter-actions {
    display: flex;
    align-items: center;
    gap: $space-3;
    flex-wrap: wrap;
  }

  .order-select {
    width: 140px;
    margin-left: auto;

    @include mobile {
      width: 100%;
      margin-left: 0;
    }
  }
}

/* ── Toolbar ────────────────────────────── */
.table-toolbar {
  margin-bottom: $space-4;

  .toolbar-left {
    display: flex;
    gap: $space-3;
    flex-wrap: wrap;
  }
}

/* ── Main Content ───────────────────────── */
.main-content {
  display: flex;
  gap: $space-5;
  align-items: flex-start;

  @include mobile {
    flex-direction: column;
  }
}

/* ── Facet Sidebar ──────────────────────── */
.facet-sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.facet-section {
  @include card($padding: 0, $radius: $radius-lg);
  overflow: hidden;

  .facet-title {
    padding: $space-4 $space-5;
    font-size: $font-size-sm;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    border-bottom: 1px solid $border-light;
  }

  .facet-list {
    padding: $space-2;
  }

  .facet-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 10px;
    cursor: pointer;
    border-radius: $radius-sm;
    transition: background $transition-fast;

    &:hover {
      background: $gray-50;
    }

    &.active {
      background: $primary-lighter;

      .facet-label {
        color: $primary;
        font-weight: $font-weight-medium;
      }

      .facet-count {
        color: $primary;
      }
    }

    .facet-label {
      font-size: $font-size-sm;
      color: $text-regular;
      @include truncate;
      max-width: 130px;
    }

    .facet-count {
      font-size: 11px;
      color: $text-secondary;
      font-weight: $font-weight-medium;
    }
  }

  .facet-empty {
    padding: $space-6 $space-5;
    text-align: center;
    color: $text-secondary;
    font-size: $font-size-sm;
  }
}

/* ── Table Area ─────────────────────────── */
.table-area {
  flex: 1;
  min-width: 0;
  @include card($padding: 0, $radius: $radius-lg);
  overflow: hidden;

  .pagination {
    display: flex;
    justify-content: flex-end;
    padding: $space-4 $space-5;
    border-top: 1px solid $border-light;

    @include mobile {
      justify-content: center;
    }
  }
}

/* ── Table Actions ──────────────────────── */
.table-actions {
  display: flex;
  gap: $space-1;
  align-items: center;
}

/* ── Import Errors ──────────────────────── */
.import-errors {
  margin-top: $space-4;

  .import-error-title {
    font-weight: $font-weight-semibold;
    font-size: $font-size-sm;
    margin-bottom: $space-2;
    color: $text-primary;
  }

  .import-error-list {
    margin: 0;
    padding-left: 20px;
    font-size: $font-size-xs;
    color: $danger;
    line-height: 1.8;
  }
}
</style>
