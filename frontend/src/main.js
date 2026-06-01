import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { AlarmClock, ArrowDown, ArrowLeft, ArrowRight, Bell, BellFilled, Calendar, Check, CircleCheck, CircleCloseFilled, Clock, Close, Coin, Collection, Connection, CopyDocument, DataAnalysis, DataLine, Delete, Document, Download, Edit, Expand, Fold, FullScreen, Grid, Guide, HelpFilled, HomeFilled, Key, List, Loading, Lock, MagicStick, Medal, Monitor, Notebook, Operation, Picture, Plus, Reading, Refresh, RefreshRight, School, Search, Shop, ShoppingCart, SwitchButton, Tickets, Timer, Upload, UploadFilled, User, UserFilled, Van, View, Wallet, WarningFilled, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import { piniaPersistedstatePlugin } from './utils/pinia-persistence'

// Import styles
import 'element-plus/dist/index.css'
import './styles/index.scss'
import './styles/responsive.scss'
import './styles/accessibility.scss'

const app = createApp(App)

const icons = { AlarmClock, ArrowDown, ArrowLeft, ArrowRight, Bell, BellFilled, Calendar, Check, CircleCheck, CircleCloseFilled, Clock, Close, Coin, Collection, Connection, CopyDocument, DataAnalysis, DataLine, Delete, Document, Download, Edit, Expand, Fold, FullScreen, Grid, Guide, HelpFilled, HomeFilled, Key, List, Loading, Lock, MagicStick, Medal, Monitor, Notebook, Operation, Picture, Plus, Reading, Refresh, RefreshRight, School, Search, Shop, ShoppingCart, SwitchButton, Tickets, Timer, Upload, UploadFilled, User, UserFilled, Van, View, Wallet, WarningFilled, ZoomIn, ZoomOut }
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}

// 创建 Pinia 实例并应用持久化插件
const pinia = createPinia()
pinia.use(piniaPersistedstatePlugin)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn, size: 'default', zIndex: 3000 })

app.mount('#app')
