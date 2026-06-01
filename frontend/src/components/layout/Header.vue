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
                <el-tag
                  v-if="item.status === 'UNREAD'"
                  type="danger"
                  size="small"
                  effect="dark"
                  round
                >新</el-tag>
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
            :size="30"
            icon="UserFilled"
            class="user-avatar"
          />
          <span v-if="!isMobile" class="username">{{ userStore.username }}</span>
          <el-icon v-if="!isMobile" class="dropdown-arrow"><ArrowDown /></el-icon>
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
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})

async function fetchUnreadCount() {
  try {
    const res = await notificationApi.getUnreadCount()
    notificationCount.value = res.data || 0
  } catch {
    // silently ignore
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

.hamburger-btn {
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

<style lang="scss" scoped>
.notification-drawer {
  padding: 0 4px;
}

.notification-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.notification-empty {
  padding: 40px 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item {
  padding: 12px;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f0f0f0;
  }

  &.unread {
    background: #ecf5ff;
    border-left: 3px solid #409eff;

    &:hover {
      background: #d9ecff;
    }
  }
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.notification-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-more {
  text-align: center;
  padding: 12px 0;
}
</style>
