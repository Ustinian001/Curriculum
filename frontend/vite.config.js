import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后端接口前缀（去掉 /api，直接映射业务路径）
const API_PREFIXES = ['/auth', '/student', '/teacher', '/admin']

// 单条接口代理配置：浏览器直接访问页面（Accept: text/html）时跳过代理，
// 交由 Vite 返回 SPA；axios 请求（Accept: application/json）走代理转发到后端
const proxyTarget = {
  target: 'http://localhost:8081',
  changeOrigin: true,
  bypass(req) {
    if (req.headers.accept && req.headers.accept.includes('text/html')) {
      return false
    }
  }
}

const proxy = {}
for (const p of API_PREFIXES) {
  proxy[p] = proxyTarget
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: true,
    port: 5173,
    proxy
  }
})
