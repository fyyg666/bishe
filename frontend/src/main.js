import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import { piniaPersistedstatePlugin } from './utils/pinia-persistence'

// Import styles
import 'element-plus/dist/index.css'
import './styles/index.scss'

const app = createApp(App)

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 创建 Pinia 实例并应用持久化插件
const pinia = createPinia()
pinia.use(piniaPersistedstatePlugin)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn, size: 'default', zIndex: 3000 })

app.mount('#app')
