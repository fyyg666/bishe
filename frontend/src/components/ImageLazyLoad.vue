<template>
  <div class="lazy-image" :class="{ 'is-loaded': isLoaded }">
    <img
      v-if="isVisible"
      ref="imgRef"
      :src="src"
      :alt="alt"
      :class="imageClass"
      :style="imageStyle"
      loading="lazy"
      @load="onImageLoad"
      @error="onImageError"
    />
    <div v-else class="lazy-placeholder" :style="placeholderStyle">
      <el-icon v-if="loading" class="is-loading">
        <Loading />
      </el-icon>
    </div>
  </div>
</template>

<script setup>
/**
 * 图片懒加载组件
 * 使用 IntersectionObserver API 实现图片懒加载
 * 
 * @author library-system-team
 * @date 2026-04-24
 */
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  // 图片地址
  src: {
    type: String,
    required: true
  },
  // 图片描述
  alt: {
    type: String,
    default: ''
  },
  // 自定义类名
  imageClass: {
    type: [String, Object, Array],
    default: ''
  },
  // 自定义样式
  imageStyle: {
    type: [String, Object],
    default: ''
  },
  // 占位符高度
  placeholderHeight: {
    type: String,
    default: '200px'
  },
  // 占位符背景色
  placeholderBg: {
    type: String,
    default: '#f5f7fa'
  }
})

const emit = defineEmits(['load', 'error'])

const imgRef = ref(null)
const isVisible = ref(false)
const isLoaded = ref(false)
const loading = ref(true)

let observer = null

// 占位符样式
const placeholderStyle = computed(() => ({
  height: props.placeholderHeight,
  backgroundColor: props.placeholderBg
}))

onMounted(() => {
  // 使用 IntersectionObserver 实现懒加载
  if ('IntersectionObserver' in window) {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            isVisible.value = true
            // 图片开始加载后停止观察
            observer?.unobserve(entry.target)
          }
        })
      },
      {
        rootMargin: '50px 0px', // 提前 50px 开始加载
        threshold: 0.01
      }
    )

    if (imgRef.value) {
      observer.observe(imgRef.value)
    }
  } else {
    // 不支持 IntersectionObserver 时，直接显示图片
    isVisible.value = true
  }
})

onUnmounted(() => {
  // 清理 observer
  if (observer) {
    observer.disconnect()
  }
})

function onImageLoad() {
  isLoaded.value = true
  loading.value = false
  emit('load')
}

function onImageError(e) {
  loading.value = false
  emit('error', e)
}
</script>

<style lang="scss" scoped>
.lazy-image {
  width: 100%;
  position: relative;
  overflow: hidden;
  
  .lazy-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    border-radius: inherit;
    
    .el-icon {
      font-size: 24px;
      color: #c0c4cc;
    }
  }
  
  img {
    width: 100%;
    height: auto;
    display: block;
    opacity: 0;
    transition: opacity 0.3s ease-in-out;
  }
  
  &.is-loaded img {
    opacity: 1;
  }
}
</style>
