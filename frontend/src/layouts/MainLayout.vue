<template>
  <el-container class="main-layout">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '210px'" class="sidebar">
      <div class="logo">
        <img src="/logo.svg" alt="logo" v-if="!isCollapse">
        <span v-if="!isCollapse">图书馆系统</span>
        <el-icon v-else><Collection /></el-icon>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- Header -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import Breadcrumb from '@/components/Breadcrumb.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
  { path: '/books', title: '图书管理', icon: 'Collection' },
  { path: '/readers', title: '读者管理', icon: 'UserFilled' },
  { path: '/borrows', title: '借阅管理', icon: 'Reading' },
  { path: '/seats', title: '座位预约', icon: 'OfficeBuilding' },
  { path: '/announcements', title: '公告管理', icon: 'BellFilled' },
  { path: '/volunteers', title: '志愿服务', icon: 'Service' },
  { path: '/statistics', title: '统计分析', icon: 'TrendCharts' },
]

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      // TODO: Open settings dialog
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style lang="scss" scoped>
// FIXED: FE-P3-04 - 响应式布局适配移动端
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;

  .logo {
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid #1f2d3d;

    img {
      width: 32px;
      height: 32px;
      margin-right: 10px;
    }

    .el-icon {
      font-size: 24px;
      color: #409eff;
    }
  }

  .el-menu {
    border-right: none;
  }
  
  // 移动端适配
  @media screen and (max-width: 768px) {
    position: fixed;
    left: -210px;
    z-index: 1000;
    height: 100vh;
    
    &.is-collapse {
      left: -64px;
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      margin-right: 15px;

      &:hover {
        color: #409eff;
      }
    }
    
    // 移动端显示菜单按钮
    @media screen and (max-width: 768px) {
      .collapse-btn {
        display: flex;
      }
    }
  }

  .header-right {
    .user-info {
      cursor: pointer;
      display: flex;
      align-items: center;
      
      // 移动端只显示首字母
      @media screen and (max-width: 576px) {
        font-size: 14px;
      }

      .el-icon {
        margin-left: 5px;
      }
    }
  }
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
  
  // 移动端适配
  @media screen and (max-width: 768px) {
    padding: 12px;
    margin-left: 0;
  }
}

// Transition animations
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
