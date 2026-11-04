import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// Vite 配置 — 广告物料订单管理系统前端
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    open: true,
    proxy: {
      // 后端接口代理：/api → http://localhost:8080/api
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
