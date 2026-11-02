/**
 * Vite 配置
 * - dev 服务器：host 127.0.0.1（兼容 Windows 终端启动），port 5173，启动后自动打开浏览器
 * - 代理 /api → 后端 http://localhost:8080（后端 application.yml context-path=/api，不 rewrite）
 * - alias @ → src（组件导入用 @/xxx）
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // @ 别名映射到 src 目录
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    open: true,
    proxy: {
      // 前端请求 /api/v1/xxx → 代理到 http://localhost:8080/api/v1/xxx
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1000
  }
})
