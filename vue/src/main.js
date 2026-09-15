import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/index.scss'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)

// 从后端获取用户信息（仅刷新页面token时请求）
const userStore = useUserStore()
userStore.fetchUserInfo()

app.use(ElementPlus)
app.use(router)

app.mount('#app')

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
