import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import monacoEditorPluginModule from 'vite-plugin-monaco-editor'

// 该插件是 CommonJS 包，Vite 加载后 default 可能再包一层。
const monacoEditorPlugin = monacoEditorPluginModule.default || monacoEditorPluginModule

export default defineConfig({
  plugins: [
    vue(),
    // 由插件生成并挂载 Monaco 的 editor worker，避免 worker 路径 404。
    monacoEditorPlugin({
      languageWorkers: ['editorWorkerService']
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 后端接口本身带 /api 前缀，这里不能 rewrite
      },
      // 上传后的图片由后端 /uploads/** 提供，开发环境也要代理。
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 2000
  }
})
