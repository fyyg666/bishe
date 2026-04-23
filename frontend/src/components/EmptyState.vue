<template>
  <div class="empty-state">
    <el-empty :image-size="imageSize" :description="description">
      <slot>
        <el-button v-if="showButton" type="primary" @click="$emit('action')">
          {{ actionText }}
        </el-button>
      </slot>
    </el-empty>
  </div>
</template>

<script setup>
defineProps({
  description: {
    type: String,
    default: '暂无数据'
  },
  imageSize: {
    type: Number,
    default: 100
  },
  showButton: {
    type: Boolean,
    default: false
  },
  actionText: {
    type: String,
    default: '刷新'
  }
})

defineEmits(['action'])
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 20px;
}
</style>
