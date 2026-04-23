<template>
  <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
    <div class="logo">
      <img src="@/assets/logo.png" alt="logo" v-if="!isCollapsed" @error="handleImgError" />
      <span v-if="!isCollapsed" class="logo-text">图书馆系统</span>
      <span v-else class="logo-icon">图</span>
    </div>
    
    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapsed"
      :router="true"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <template #title>首页仪表盘</template>
      </el-menu-item>
      
      <el-menu-item index="/books">
        <el-icon><Reading /></el-icon>
        <template #title>图书管理</template>
      </el-menu-item>
      
      <el-menu-item index="/borrows">
        <el-icon><Tickets /></el-icon>
        <template #title>借阅管理</template>
      </el-menu-item>
      
      <el-menu-item index="/seats">
        <el-icon><Grid /></el-icon>
        <template #title>座位预约</template>
      </el-menu-item>
      
      <el-menu-item index="/credit">
        <el-icon><Coin /></el-icon>
        <template #title>信用积分</template>
      </el-menu-item>
      
      <el-menu-item index="/profile">
        <el-icon><User /></el-icon>
        <template #title>个人信息</template>
      </el-menu-item>
    </el-menu>
    
    <div class="collapse-btn" @click="toggleCollapse">
      <el-icon>
        <Fold v-if="!isCollapsed" />
        <Expand v-else />
      </el-icon>
    </div>
  </el-aside>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function handleImgError(e) {
  e.target.style.display = 'none'
}
</script>

<style lang="scss" scoped>
.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  position: relative;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  background-color: #2b3a4a;
  
  img {
    width: 32px;
    height: 32px;
    margin-right: 10px;
  }
  
  .logo-text {
    color: #fff;
    font-size: 16px;
    font-weight: bold;
  }
  
  .logo-icon {
    color: #409eff;
    font-size: 24px;
    font-weight: bold;
  }
}

.collapse-btn {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  color: #bfcbd9;
  cursor: pointer;
  font-size: 20px;
  
  &:hover {
    color: #409eff;
  }
}

:deep(.el-menu) {
  border-right: none;
  
  .el-menu-item {
    &:hover {
      background-color: #263445 !important;
    }
    
    &.is-active {
      background-color: #263445 !important;
    }
  }
}
</style>
