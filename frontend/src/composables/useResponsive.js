import { ref, computed, onMounted, onUnmounted } from 'vue'

const BREAKPOINT_SM = 768
const BREAKPOINT_MD = 992

export function useResponsive() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

  function onResize() {
    width.value = window.innerWidth
  }

  onMounted(() => {
    window.addEventListener('resize', onResize)
    onResize()
  })

  onUnmounted(() => {
    window.removeEventListener('resize', onResize)
  })

  const isMobile = computed(() => width.value < BREAKPOINT_SM)
  const isTablet = computed(() => width.value >= BREAKPOINT_SM && width.value < BREAKPOINT_MD)
  const isDesktop = computed(() => width.value >= BREAKPOINT_MD)

  return { isMobile, isTablet, isDesktop, width }
}
