<template>
  <div class="table-skeleton">
    <!-- 搜索区域骨架屏 -->
    <div v-if="showSearch" class="skeleton-search">
      <el-skeleton :rows="1" animated />
    </div>

    <!-- 表格骨架屏 -->
    <el-skeleton :rows="rows" animated :loading="true">
      <template #template>
        <div class="skeleton-table">
          <el-table :data="[]">
            <el-table-column v-for="i in columns" :key="i" :width="columnWidth">
              <template #header>
                <el-skeleton-item variant="text" style="width: 60px" />
              </template>
              <template #default>
                <el-skeleton-item variant="text" style="width: 100%" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-skeleton>

    <!-- 分页骨架屏 -->
    <div v-if="showPagination" class="skeleton-pagination">
      <el-skeleton :rows="1" animated />
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: {
    type: Number,
    default: 5
  },
  columns: {
    type: Number,
    default: 5
  },
  columnWidth: {
    type: String,
    default: '120'
  },
  showSearch: {
    type: Boolean,
    default: false
  },
  showPagination: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.table-skeleton {
  .skeleton-search {
    margin-bottom: 20px;
  }

  .skeleton-table {
    min-height: 300px;
  }

  .skeleton-pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
