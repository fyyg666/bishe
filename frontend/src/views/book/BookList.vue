<template>
  <div class="book-list">
    <el-card class="search-card">
      <div class="search-header" @click="isMobile && (searchCollapsed = !searchCollapsed)">
        <span v-if="isMobile" class="search-toggle">
          <el-icon><Search /></el-icon>
          <span>搜索筛选</span>
          <el-icon class="toggle-arrow" :class="{ expanded: !searchCollapsed }"><ArrowDown /></el-icon>
        </span>
      </div>
      <el-form
        v-show="!isMobile || !searchCollapsed"
        :model="searchForm"
        inline
        :class="{ 'mobile-form': isMobile }"
      >
        <el-form-item label="书名">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入书名"
            clearable
          />
        </el-form-item>
        <el-form-item label="作者">
          <el-input
            v-model="searchForm.author"
            placeholder="请输入作者"
            clearable
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="searchForm.category"
            placeholder="请选择分类"
            clearable
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
          <el-button @click="showAdvanced = !showAdvanced">
            {{ showAdvanced ? '收起高级搜索' : '高级搜索' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-show="showAdvanced" class="advanced-search-card">
      <el-form
        :model="advancedForm"
        label-width="90px"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="书名">
              <el-input
                v-model="advancedForm.title"
                placeholder="请输入书名"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="作者">
              <el-input
                v-model="advancedForm.author"
                placeholder="请输入作者"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="ISBN">
              <el-input
                v-model="advancedForm.isbn"
                placeholder="请输入ISBN"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="出版社">
              <el-input
                v-model="advancedForm.publisher"
                placeholder="请输入出版社"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="分类">
              <el-select
                v-model="advancedForm.category"
                placeholder="请选择分类"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="cat in categoryOptions"
                  :key="cat.value"
                  :label="cat.label"
                  :value="cat.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="出版日期">
              <el-date-picker
                v-model="advancedForm.publishDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="排序方式">
              <el-select
                v-model="advancedForm.orderBy"
                placeholder="请选择排序方式"
                style="width: 100%"
              >
                <el-option
                  label="默认"
                  value=""
                />
                <el-option
                  label="借阅次数"
                  value="borrowCount"
                />
                <el-option
                  label="出版日期"
                  value="publishDate"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="16">
            <el-form-item label=" ">
              <el-button
                type="primary"
                @click="handleAdvancedSearch"
              >
                搜索
              </el-button>
              <el-button @click="handleAdvancedReset">
                重置
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <div class="table-toolbar">
      <el-button
        type="primary"
        :icon="Plus"
        @click="$router.push('/books/add')"
      >
        <span v-if="!isMobile">添加图书</span>
      </el-button>
      <el-button
        :icon="Download"
        @click="handleExport"
      >
        <span v-if="!isMobile">导出</span>
      </el-button>
      <el-button
        :icon="Upload"
        @click="importDialogVisible = true"
      >
        <span v-if="!isMobile">导入</span>
      </el-button>
    </div>

    <div class="main-content">
      <div v-if="!isMobile" class="facet-sidebar">
        <el-card class="facet-card">
          <template #header>
            <span>分类筛选</span>
          </template>
          <div
            v-if="categoryFacets.length"
            class="facet-list"
          >
            <div
              v-for="facet in categoryFacets"
              :key="facet.value"
              class="facet-item"
              :class="{ active: activeCategoryFacet === facet.value }"
              @click="toggleCategoryFacet(facet.value)"
            >
              <span class="facet-label">{{ facet.label }}</span>
              <el-tag
                size="small"
                type="info"
              >
                {{ facet.count }}
              </el-tag>
            </div>
          </div>
          <el-empty
            v-else
            description="暂无数据"
            :image-size="40"
          />
        </el-card>

        <el-card class="facet-card">
          <template #header>
            <span>热门作者</span>
          </template>
          <div
            v-if="authorFacets.length"
            class="facet-list"
          >
            <div
              v-for="facet in authorFacets"
              :key="facet.value"
              class="facet-item"
              :class="{ active: activeAuthorFacet === facet.value }"
              @click="toggleAuthorFacet(facet.value)"
            >
              <span class="facet-label">{{ facet.label }}</span>
              <el-tag
                size="small"
                type="info"
              >
                {{ facet.count }}
              </el-tag>
            </div>
          </div>
          <el-empty
            v-else
            description="暂无数据"
            :image-size="40"
          />
        </el-card>
      </div>

      <el-card class="table-card">
        <div class="table-wrapper">
          <el-table
            v-loading="loading"
            :data="books"
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column
              type="selection"
              width="55"
            />
            <el-table-column
              prop="id"
              label="ID"
              width="80"
            />
            <el-table-column
              prop="title"
              label="书名"
              min-width="150"
            />
            <el-table-column
              prop="author"
              label="作者"
              width="120"
            />
            <el-table-column
              v-if="!isMobile"
              prop="isbn"
              label="ISBN"
              width="150"
            />
            <el-table-column
              v-if="!isMobile"
              prop="category"
              label="分类"
              width="100"
            />
            <el-table-column
              v-if="!isMobile"
              prop="publisher"
              label="出版社"
              width="150"
            />
            <el-table-column
              prop="availableCount"
              label="库存"
              width="80"
            />
            <el-table-column
              prop="status"
              label="状态"
              width="100"
            >
              <template #default="{ row }">
                <el-tag :type="row.availableCount > 0 ? 'success' : 'danger'">
                  {{ row.availableCount > 0 ? '在架' : '借出' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              :width="isMobile ? '120' : '260'"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  @click="handleDetail(row)"
                >
                  {{ isMobile ? '' : '详情' }}
                  <el-icon v-if="isMobile"><View /></el-icon>
                </el-button>
                <el-button
                  type="primary"
                  link
                  @click="handleEdit(row)"
                >
                  {{ isMobile ? '' : '编辑' }}
                  <el-icon v-if="isMobile"><Edit /></el-icon>
                </el-button>
                <el-button
                  v-if="!isMobile"
                  type="danger"
                  link
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
                <el-button
                  v-if="!isMobile"
                  type="warning"
                  link
                  @click="handleSuggest(row)"
                >
                  荐购
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="total"
            :page-sizes="PAGE_SIZE_OPTIONS"
            :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
            :small="isMobile"
            @size-change="loadBooks"
            @current-change="loadBooks"
          />
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="importDialogVisible"
      title="批量导入图书"
      width="500px"
    >
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
        <div class="el-upload__text">
          将Excel文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx / .xls 格式，表头需包含：书名、作者、ISBN、分类、出版社、出版日期、价格、总库存、简介
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="importLoading"
          @click="submitImport"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importResultVisible"
      title="导入结果"
      width="500px"
    >
      <el-descriptions
        :column="1"
        border
      >
        <el-descriptions-item label="总数">
          {{ importResult.totalCount }}
        </el-descriptions-item>
        <el-descriptions-item label="成功">
          <el-tag type="success">{{ importResult.successCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败">
          <el-tag type="danger">{{ importResult.failCount }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div
        v-if="importResult.errors && importResult.errors.length"
        style="margin-top: 16px"
      >
        <p style="font-weight: bold; margin-bottom: 8px">
          错误详情：
        </p>
        <el-scrollbar max-height="200px">
          <ul style="margin: 0; padding-left: 20px; color: #f56c6c; font-size: 13px">
            <li
              v-for="(err, idx) in importResult.errors"
              :key="idx"
            >
              {{ err }}
            </li>
          </ul>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button
          type="primary"
          @click="importResultVisible = false"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="suggestDialogVisible"
      title="荐购图书"
      width="500px"
    >
      <el-form
        :model="suggestForm"
        label-width="100px"
      >
        <el-form-item label="书名">
          <el-input
            v-model="suggestForm.title"
            disabled
          />
        </el-form-item>
        <el-form-item label="作者">
          <el-input
            v-model="suggestForm.author"
            disabled
          />
        </el-form-item>
        <el-form-item label="ISBN">
          <el-input
            v-model="suggestForm.isbn"
            disabled
          />
        </el-form-item>
        <el-form-item label="荐购理由">
          <el-input
            v-model="suggestForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入荐购理由"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="suggestDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="suggesting"
          @click="submitSuggestion"
        >
          提交荐购
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'Books' })

import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Upload, UploadFilled } from '@element-plus/icons-vue'
import { useBookStore } from '@/stores/book'
import { useCategories } from '@/composables/useCategories'
import { useResponsive } from '@/composables/useResponsive'
import { getToken } from '@/utils/auth'
import { advancedSearchBooks, getCategoryFacet, getAuthorFacet } from '@/api/book'
import { createSuggestion } from '@/api/suggestion'

const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 10
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

const router = useRouter()
const bookStore = useBookStore()
const { isMobile } = useResponsive()

const loading = ref(false)
const books = ref([])
const total = ref(0)
const selectedBooks = ref([])
const searchCollapsed = ref(true)

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

const showAdvanced = ref(false)
const isAdvancedSearch = ref(false)

const advancedForm = reactive({
  title: '',
  author: '',
  isbn: '',
  publisher: '',
  category: '',
  publishDateRange: null,
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
    // 分面数据加载失败不影响主流程
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
  if (searchForm.author) {
    params.author = searchForm.author
  }
  if (searchForm.category) {
    params.categoryId = searchForm.category
  }
  if (activeCategoryFacet.value) {
    params.categoryId = activeCategoryFacet.value
  }
  if (activeAuthorFacet.value) {
    params.author = activeAuthorFacet.value
  }
  await bookStore.fetchBooks(params)
  books.value = bookStore.books
  total.value = bookStore.total
}

async function loadAdvancedSearchResults() {
  const params = {
    current: pagination.current,
    size: pagination.size
  }
  if (advancedForm.title) params.title = advancedForm.title
  if (advancedForm.author) params.author = advancedForm.author
  if (advancedForm.isbn) params.isbn = advancedForm.isbn
  if (advancedForm.publisher) params.publisher = advancedForm.publisher
  if (advancedForm.category) params.categoryId = advancedForm.category
  if (advancedForm.orderBy) params.orderBy = advancedForm.orderBy
  if (activeCategoryFacet.value) params.categoryId = activeCategoryFacet.value
  if (activeAuthorFacet.value) params.author = activeAuthorFacet.value
  if (advancedForm.publishDateRange && advancedForm.publishDateRange.length === 2) {
    params.publishDateStart = advancedForm.publishDateRange[0]
    params.publishDateEnd = advancedForm.publishDateRange[1]
  }
  const res = await advancedSearchBooks(params)
  books.value = res.data?.records || res.data?.list || []
  total.value = res.data?.total || 0
}

function handleSearch() {
  isAdvancedSearch.value = false
  pagination.current = DEFAULT_PAGE
  loadBooks()
}

function handleReset() {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  activeCategoryFacet.value = ''
  activeAuthorFacet.value = ''
  isAdvancedSearch.value = false
  handleSearch()
}

function handleAdvancedSearch() {
  isAdvancedSearch.value = true
  pagination.current = DEFAULT_PAGE
  loadBooks()
}

function handleAdvancedReset() {
  advancedForm.title = ''
  advancedForm.author = ''
  advancedForm.isbn = ''
  advancedForm.publisher = ''
  advancedForm.category = ''
  advancedForm.publishDateRange = null
  advancedForm.orderBy = ''
  activeCategoryFacet.value = ''
  activeAuthorFacet.value = ''
  isAdvancedSearch.value = false
  pagination.current = DEFAULT_PAGE
  loadBooks()
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

function handleDetail(row) {
  router.push(`/books/${row.id}`)
}

function handleEdit(row) {
  router.push(`/books/add?id=${row.id}`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该图书吗？', '提示', {
      type: 'warning'
    })
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
const importHeaders = computed(() => ({
  Authorization: `Bearer ${getToken()}`
}))

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
  fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => res.blob()).then(blob => {
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `图书列表_${new Date().toISOString().slice(0,10)}.xlsx`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  }).catch(() => ElMessage.error('导出失败'))
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
  } catch (e) { ElMessage.error(e.message) }
  finally { suggesting.value = false }
}
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

.book-list {
  .search-card {
    margin-bottom: 16px;
  }

  .search-header {
    display: none;
  }

  .search-toggle {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 14px;
    color: $text-regular;

    .toggle-arrow {
      transition: transform 0.2s;
      &.expanded {
        transform: rotate(180deg);
      }
    }
  }

  .mobile-form {
    :deep(.el-form-item) {
      margin-right: 0;
      width: 100%;
    }
  }

  .advanced-search-card {
    margin-bottom: 16px;
  }

  .table-toolbar {
    margin-bottom: 16px;
  }

  .main-content {
    display: flex;
    gap: 16px;
    align-items: flex-start;
  }

  .facet-sidebar {
    width: 220px;
    flex-shrink: 0;

    .facet-card {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .facet-list {
      .facet-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 6px 8px;
        cursor: pointer;
        border-radius: 4px;
        transition: background-color 0.2s;

        &:hover {
          background-color: #f5f7fa;
        }

        &.active {
          background-color: #ecf5ff;

          .facet-label {
            color: #409eff;
            font-weight: 500;
          }
        }

        .facet-label {
          font-size: 13px;
          color: #606266;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 130px;
        }
      }
    }
  }

  .table-card {
    flex: 1;
    min-width: 0;
  }

  .table-wrapper {
    overflow-x: auto;
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}

@include mobile {
  .book-list {
    .search-header {
      display: block;
    }

    .main-content {
      flex-direction: column;
    }

    .pagination {
      justify-content: center;
    }
  }
}
</style>
