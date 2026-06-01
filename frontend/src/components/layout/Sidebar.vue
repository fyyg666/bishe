<template>
  <el-aside
    role="navigation"
    aria-label="主导航"
    :width="computedCollapsed ? '64px' : '240px'"
    class="sidebar"
  >
    <div class="sidebar-inner">
      <div
        class="logo-area"
        @click="toggleCollapse"
      >
        <div class="logo-icon">
          <el-icon :size="computedCollapsed ? 22 : 24">
            <Reading />
          </el-icon>
        </div>
        <transition name="fade">
          <span
            v-if="!computedCollapsed"
            class="logo-text"
          >图书馆系统</span>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="computedCollapsed"
        :router="true"
        class="nav-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>
            首页仪表盘
          </template>
        </el-menu-item>
        <el-menu-item index="/books">
          <el-icon><Reading /></el-icon>
          <template #title>
            图书管理
          </template>
        </el-menu-item>
        <el-menu-item index="/unified-search">
          <el-icon><Search /></el-icon>
          <template #title>
            统一检索
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/digital-resources"
        >
          <el-icon><Monitor /></el-icon>
          <template #title>
            数字资源管理
          </template>
        </el-menu-item>
        <el-menu-item index="/borrows">
          <el-icon><Tickets /></el-icon>
          <template #title>
            借阅管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/borrow-rules"
        >
          <el-icon><List /></el-icon>
          <template #title>
            借阅规则
          </template>
        </el-menu-item>
        <el-menu-item index="/seats">
          <el-icon><Grid /></el-icon>
          <template #title>
            座位预约
          </template>
        </el-menu-item>
        <el-menu-item index="/credit">
          <el-icon><Coin /></el-icon>
          <template #title>
            信用积分
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/readers"
        >
          <el-icon><UserFilled /></el-icon>
          <template #title>
            读者管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/announcements"
        >
          <el-icon><BellFilled /></el-icon>
          <template #title>
            公告管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/volunteers"
        >
          <el-icon><HelpFilled /></el-icon>
          <template #title>
            志愿服务
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/statistics"
        >
          <el-icon><DataAnalysis /></el-icon>
          <template #title>
            统计分析
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/reports"
        >
          <el-icon><DataLine /></el-icon>
          <template #title>
            自定义报表
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/compensations"
        >
          <el-icon><WarningFilled /></el-icon>
          <template #title>
            赔偿管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/budget-funds"
        >
          <el-icon><Wallet /></el-icon>
          <template #title>
            预算管理
          </template>
        </el-menu-item>
        <el-menu-item index="/suggestions">
          <el-icon><ShoppingCart /></el-icon>
          <template #title>
            荐购管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/marc"
        >
          <el-icon><Document /></el-icon>
          <template #title>
            MARC编目
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/z3950"
        >
          <el-icon><Connection /></el-icon>
          <template #title>
            Z39.50联机编目
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/purchase-orders"
        >
          <el-icon><ShoppingCart /></el-icon>
          <template #title>
            采购管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/vendors"
        >
          <el-icon><Shop /></el-icon>
          <template #title>
            供应商管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/serial/subscriptions"
        >
          <el-icon><Notebook /></el-icon>
          <template #title>
            期刊管理
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/serial/routings"
        >
          <el-icon><Guide /></el-icon>
          <template #title>
            期刊路由分发
          </template>
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/branches"
        >
          <el-icon><School /></el-icon>
          <template #title>
            分馆管理
          </template>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <template #title>
            个人信息
          </template>
        </el-menu-item>
      </el-menu>

      <div v-if="!forceExpanded" class="sidebar-footer">
        <div
          class="collapse-btn"
          @click="toggleCollapse"
        >
          <el-icon :size="18">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
      </div>
    </div>
  </el-aside>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  forceExpanded: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['navigate'])

const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(false)

const computedCollapsed = computed(() => {
  return props.forceExpanded ? false : isCollapsed.value
})

const activeMenu = computed(() => route.path)
const isAdmin = computed(() => userStore.role === 'ADMIN' || userStore.role === 'LIBRARIAN')

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function handleMenuSelect() {
  emit('navigate')
}
</script>

<style lang="scss" scoped>
@use '@/styles/mixins.scss' as *;

.sidebar {
  background: $gray-750;
  transition: width $transition-slow;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  z-index: 1;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-3;
  padding: 0 $space-4;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid oklch(1 0 0 / 0.06);
  flex-shrink: 0;
  transition: background $transition-base;

  &:hover {
    background: oklch(1 0 0 / 0.04);
  }

  .logo-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    background: $primary;
    border-radius: $radius-md;
    color: $bg-card;
    flex-shrink: 0;
  }

  .logo-text {
    color: oklch(0.92 0.008 50);
    font-size: $font-size-lg;
    font-weight: $font-weight-medium;
    letter-spacing: 0.5px;
    white-space: nowrap;
  }
}

.nav-menu {
  flex: 1;
  overflow-y: auto;
  padding: $space-3 0;
  background: transparent !important;

  :deep(.el-menu-item) {
    margin: 1px $space-2;
    border-radius: $radius-md;
    height: 40px;
    line-height: 40px;
    color: oklch(0.75 0.01 42) !important;
    transition: all $transition-fast;

    .el-icon {
      color: oklch(0.65 0.01 40);
      transition: color $transition-fast;
    }

    &:hover {
      background: oklch(1 0 0 / 0.05) !important;
      color: oklch(0.9 0.01 45) !important;

      .el-icon {
        color: oklch(0.85 0.01 45);
      }
    }

    &.is-active {
      background: $primary !important;
      color: #fff !important;

      .el-icon {
        color: #fff;
      }
    }
  }
}

.sidebar-footer {
  flex-shrink: 0;
  padding: $space-3 $space-4;
  border-top: 1px solid oklch(1 0 0 / 0.06);

  .collapse-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 34px;
    color: oklch(0.65 0.01 40);
    cursor: pointer;
    border-radius: $radius-md;
    transition: all $transition-fast;

    &:hover {
      background: oklch(1 0 0 / 0.05);
      color: oklch(0.85 0.01 45);
    }
  }
}
</style>
