<template>
  <el-header role="banner" class="header">
    <div class="header-left">
      <el-icon
        v-if="isMobile"
        class="hamburger-btn"
        :size="22"
        @click="$emit('toggle-sidebar')"
      >
        <Operation />
      </el-icon>
      <Breadcrumb v-if="!isMobile" />
    </div>

    <div class="header-right">
      <div v-if="!isMobile" class="header-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索图书、作者、ISBN…"
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

      <el-drawer
        v-model="notificationDrawerVisible"
        title="通知中心"
        direction="rtl"
        size="380px"
      >
        <div class="notification-drawer">
          <div class="notification-header">
            <el-button
              v-if="notificationCount > 0"
              type="primary"
              link
              size="small"
              @click="handleMarkAllRead"
            >
              全部已读
            </el-button>
          </div>
          <div v-if="notifications.length === 0" class="notification-empty">
            <el-empty description="暂无通知" :image-size="80" />
          </div>
          <div v-else class="notification-list">
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notification-item"
              :class="{ unread: item.status === 'UNREAD' }"
              @click="handleNotificationClick(item)"
            >
              <div class="notification-title">
                <span class="unread-dot" v-if="item.status === 'UNREAD'"></span>
                <span>{{ item.title }}</span>
              </div>
              <div class="notification-content">{{ item.content }}</div>
              <div class="notification-time">{{ formatTime(item.createTime) }}</div>
            </div>
          </div>
          <div v-if="notificationTotal > notifications.length" class="notification-more">
            <el-button type="primary" link @click="loadMoreNotifications">加载更多</el-button>
          </div>
        </div>
      </el-drawer>

      <el-dropdown
        trigger="click"
        @command="handleUserCommand"
      >
        <span class="user-info">
          <el-avatar
            :size="28"
            icon="UserFilled"
            class="user-avatar"
          />
          <span v-if="!isMobile" class="username">{{ userStore.username }}</span>
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
import { ref, h, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import * as notificationApi from '@/api/notification'
import Breadcrumb from './Breadcrumb.vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})

defineEmits(['toggle-sidebar'])

const SearchIcon = h(Search)

const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')
const notificationCount = ref(0)
const notificationDrawerVisible = ref(false)
const notifications = ref([])
const notificationCurrent = ref(1)
const notificationTotal = ref(0)
let pollTimer = null

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30000)
  }
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

async function fetchUnreadCount() {
  try {
    const res = await notificationApi.getUnreadCount()
    notificationCount.value = res.data || 0
  } catch {
    // silently ignore
  }
}

function handleVisibilityChange() {
  if (!userStore.isLoggedIn) return
  if (document.hidden) {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  } else {
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30000)
  }
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/books', query: { keyword: searchKeyword.value } })
  }
}

async function showNotifications() {
  notificationDrawerVisible.value = true
  notificationCurrent.value = 1
  notifications.value = []
  await loadNotifications()
}

async function loadNotifications() {
  try {
    const res = await notificationApi.getNotifications({
      current: notificationCurrent.value,
      size: 10
    })
    const data = res.data
    if (notificationCurrent.value === 1) {
      notifications.value = data.records || []
    } else {
      notifications.value.push(...(data.records || []))
    }
    notificationTotal.value = data.total || 0
  } catch {
    // silently ignore
  }
}

async function loadMoreNotifications() {
  notificationCurrent.value++
  await loadNotifications()
}

async function handleMarkAllRead() {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => { n.status = 'READ' })
    notificationCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleNotificationClick(item) {
  if (item.status === 'UNREAD') {
    try {
      await notificationApi.markAsRead(item.id)
      item.status = 'READ'
      notificationCount.value = Math.max(0, notificationCount.value - 1)
    } catch {
      // silently ignore
    }
  }
}

function formatTime(time) {
  if (!time) return ''
  return dayjs(time).fromNow()
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
  @include glass(0.72, $glass-blur-header);
  border-bottom: 0.5px solid $border-light;
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.hamburger-btn {
  color: $text-regular;
  cursor: pointer;
  transition: color $transition-fast, transform $transition-fast;

  &:hover {
    color: $primary;
  }

  &:active {
    transform: scale(0.92);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.header-search {
  width: 240px;

  :deep(.el-input__wrapper) {
    background: $gray-100;
    box-shadow: none !important;
    border-radius: $radius-full;
    padding: 2px 16px;
    border: 1px solid transparent;
    transition: all $transition-fast;

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
    &::placeholder {
      color: $text-placeholder;
    }
  }
}

.icon-btn {
  color: $text-regular;
  cursor: pointer;
  padding: 6px;
  border-radius: $radius-full;
  transition: all $transition-fast;

  &:hover {
    color: $primary;
    background: rgba(0, 0, 0, 0.04);
  }

  &:active {
    transform: scale(0.92);
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
    background: rgba(0, 0, 0, 0.04);
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
}
</style>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.notification-drawer {
  padding: 0;
}

.notification-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: $space-4;
}

.notification-empty {
  padding: 60px 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.notification-item {
  padding: $space-4;
  border-radius: $radius-md;
  cursor: pointer;
  transition: background $transition-fast;
  border: 1px solid transparent;

  &:hover {
    background: $gray-50;
  }

    &.unread {
      background: $primary-lighter;
      border-color: rgba(0, 113, 227, 0.1);

      &:hover {
        background: #DAE9FF;
      }
  }
}

.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: $radius-full;
  background: $primary;
  flex-shrink: 0;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $text-primary;
  margin-bottom: 4px;
}

.notification-content {
  font-size: $font-size-xs;
  color: $text-regular;
  line-height: $line-height-base;
  margin-bottom: 6px;
  @include multi-truncate(2);
}

.notification-time {
  font-size: 11px;
  color: $text-secondary;
}

.notification-more {
  text-align: center;
  padding: $space-4 0;
}
</style>
