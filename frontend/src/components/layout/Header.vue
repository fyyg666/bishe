<template>
  <el-header class="header">
    <div class="header-left">
      <breadcrumb />
    </div>
    
    <div class="header-right">
      <!-- 搜索 -->
      <div class="header-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索图书..."
          prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
        />
      </div>
      
      <!-- 通知 -->
      <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
        <el-icon class="notification-icon" @click="showNotifications">
          <Bell />
        </el-icon>
      </el-badge>
      
      <!-- 用户信息 -->
      <el-dropdown @command="handleUserCommand">
        <span class="user-info">
          <el-avatar :size="32" icon="UserFilled" />
          <span class="username">{{ userStore.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人信息
            </el-dropdown-item>
            <el-dropdown-item command="credit">
              <el-icon><Coin /></el-icon>我的积分
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import Breadcrumb from './Breadcrumb.vue'

const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')
const notificationCount = ref(0)

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/books', query: { keyword: searchKeyword.value } })
  }
}

function showNotifications() {
  // TODO: 显示通知列表
}

function handleUserCommand(command) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'credit':
      router.push('/credit')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout()
        router.push('/login')
      }).catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-search {
  width: 250px;
}

.notification-icon {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  
  &:hover {
    color: #409eff;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  
  .username {
    color: #606266;
  }
}
</style>
