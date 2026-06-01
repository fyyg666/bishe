<template>
  <el-container class="layout-container">
    <Sidebar v-if="!isMobile" />
    <el-drawer
      v-if="isMobile"
      v-model="sidebarVisible"
      direction="ltr"
      :size="'260px'"
      :show-close="false"
      :with-header="false"
      class="sidebar-drawer"
      @open="activateFocusTrap"
      @close="deactivateFocusTrap"
    >
      <Sidebar :force-expanded="true" @navigate="onNavigate" />
    </el-drawer>
    <el-container class="right-container">
      <Header
        :is-mobile="isMobile"
        @toggle-sidebar="sidebarVisible = !sidebarVisible"
      />
      <el-main id="main-content" role="main" class="main-content">
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
import { ref, onMounted, onUnmounted } from 'vue'
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import { useResponsive } from '@/composables/useResponsive'
import { useFocusTrap } from '@/composables/useFocusTrap'

const { isMobile } = useResponsive()
const sidebarVisible = ref(false)
const { activate: activateFocusTrap, deactivate: deactivateFocusTrap } = useFocusTrap()

function onNavigate() {
  sidebarVisible.value = false
  deactivateFocusTrap()
}

function onKeydown(e) {
  if (e.key === 'Escape' && sidebarVisible.value) {
    sidebarVisible.value = false
    deactivateFocusTrap()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  deactivateFocusTrap()
})
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
  padding: calc($header-height + $space-8) $space-10 $space-10;
  overflow-y: auto;
  height: 100vh;
  @include scrollbar;

  @include tablet {
    padding: calc($header-height + $space-6) $space-6 $space-6;
  }
}

@include mobile {
  .main-content {
    padding: calc($header-height + $space-3) $space-4 $space-4;
  }
}
</style>

<style lang="scss">
.sidebar-drawer {
  .el-drawer__body {
    padding: 0;
  }
}
</style>
