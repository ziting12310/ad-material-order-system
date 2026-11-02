/**
 * 应用入口 — 组装 Vue / Pinia / Router / Tailwind
 * 顺序：createApp → use(Pinia) → use(Router) → mount
 * Pinia 必须先于 Router 注册（router 守卫内使用 useUserStore）
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
