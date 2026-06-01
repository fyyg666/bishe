<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item :to="{ path: '/' }">
      首页
    </el-breadcrumb-item>
    <el-breadcrumb-item
      v-for="item in breadcrumbs"
      :key="item.path"
    >
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const breadcrumbs = computed(() => {
  return route.matched
    .filter(item => item.meta && item.meta.title && item.path !== '/')
    .map(item => ({
      path: item.path,
      title: item.meta.title
    }))
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

:deep(.el-breadcrumb__inner) {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
}

@include mobile {
  :deep(.el-breadcrumb__inner) {
    max-width: 120px;
  }
}
</style>
