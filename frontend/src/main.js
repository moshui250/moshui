import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { useUserStore } from './store/user'
import { resetAuthExpired } from './utils/request'
import reveal from './directives/reveal'
import './styles/main.css'
import './styles/reveal.css'

const app = createApp(App)
const pinia = createPinia()

for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('reveal', reveal)

// axios 层只发事件，不直接依赖 router，避免循环引用。
// 多层拦截：请求层只提示一次，这里再保证只导航一次。
let authRedirecting = false

window.addEventListener('auth:expired', () => {
  const userStore = useUserStore(pinia)
  userStore.clearAuth()

  if (authRedirecting) return
  authRedirecting = true

  const current = router.currentRoute.value
  const finish = () => {
    window.setTimeout(() => {
      authRedirecting = false
      resetAuthExpired()
    }, 300)
  }

  if (current.name === 'login') {
    finish()
    return
  }

  router
    .replace({
      name: 'login',
      query: { redirect: current.fullPath }
    })
    .finally(finish)
})

app.mount('#app')
