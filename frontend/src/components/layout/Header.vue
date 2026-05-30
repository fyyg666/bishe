<template>
  <el-header class="header">
    <div class="header-left">
      <Breadcrumb />
    </div>

    <div class="header-right">
      <div class="header-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索图书…"
          :prefix-icon="SearchIcon"
          clearable
          size="default"
          @keyup.enter="handleSearch"
        />
      </div>

      <el-badge
        :value="notificationCount"
        :hidden="notificationCount === 0"
        class="notification-badge"
      >
        <el-icon
          class="icon-btn"
          :size="20"
          @click="showNotifications"
        >
          <Bell />
        </el-icon>
      </el-badge>

      <el-dropdown
        trigger="click"
        @command="handleUserCommand"
      >
        <span class="user-info">
          <el-avatar
            :size="30"
            icon="UserFilled"
            class="user-avatar"
          />
          <span class="username">{{ userStore.username }}</span>
          <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人信息
            </el-dropdown-item>
            <el-dropdown-item command="credit">
              <el-icon><Coin /></el-icon>我的积分
            </el-dropdown-item>
            <el-dropdown-item
              divided
              command="logout"
            >
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import { ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import Breadcrumb from './Breadcrumb.vue'

const SearchIcon = h(Search)

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
  ElMessage.info('通知功能开发中')
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
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning',
        buttonSize: 'small'
      }).then(() => {
        userStore.logout()
        router.push('/login')
      }).catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

.header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: $z-sticky;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $header-height !important;
  padding: 0 $space-5;
  background: oklch(1 0 0 / 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid $border-light;
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $space-4;
}

.header-search {
  width: 220px;

  :deep(.el-input__wrapper) {
    background: $gray-50;
    box-shadow: none !important;
    border-radius: 20px;
    padding: 2px 16px;

    &:hover {
      background: $gray-100;
    }

    &.is-focus {
      background: $bg-card;
      box-shadow: 0 0 0 2px $primary-lighter inset !important;
    }
  }

  :deep(.el-input__inner) {
    &::placeholder {
      color: $text-placeholder;
    }
  }
}

.icon-btn {
  color: $text-regular;
  cursor: pointer;
  transition: color $transition-fast, transform $transition-fast;

  &:hover {
    color: $primary;
    transform: scale(1.1);
  }

  &:active {
    transform: scale(0.95);
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: $space-2;
  cursor: pointer;
  padding: $space-1 $space-2;
  border-radius: $radius-md;
  transition: background $transition-fast;

  &:hover {
    background: $gray-50;
  }

  .user-avatar {
    background: $primary-lighter;
    color: $primary;
    flex-shrink: 0;
  }

  .username {
    color: $text-primary;
    font-size: $font-size-sm;
    font-weight: $font-weight-medium;
    max-width: 100px;
    @include truncate;
  }

  .dropdown-arrow {
    color: $text-secondary;
    font-size: 12px;
    transition: transform $transition-fast;
  }

  &:hover .dropdown-arrow {
    transform: rotate(180deg);
  }
}
</style>
