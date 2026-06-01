<template>
  <a href="#main-content" class="skip-to-content">跳到主要内容</a>
  <router-view v-slot="{ Component }">
    <transition
      name="fade"
      mode="out-in"
    >
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
// App root component
</script>

<style lang="scss">
// Global styles are imported in main.js
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
