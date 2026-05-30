<template>
  <el-container class="layout-container">
    <Sidebar />
    <el-container class="right-container">
      <Header />
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['Books', 'Borrows', 'Announcements', 'SeatList']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

.layout-container {
  width: 100%;
  height: 100vh;
}

.right-container {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  background: $bg-page;
}

.main-content {
  padding: calc($header-height + $space-5) $space-6 $space-6;
  overflow-y: auto;
  height: 100vh;
  @include scrollbar;
}

@include mobile {
  .main-content {
    padding: calc($header-height + $space-3) $space-3 $space-3;
  }
}
</style>
