<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <Sidebar />
    
    <el-container>
      <!-- 顶部导航 -->
      <Header />
      
      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <!-- FIXED: P2-FE-07 - keep-alive添加include配置，只缓存列表页组件 -->
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
.layout-container {
  width: 100%;
  height: 100vh;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
