<template>
  <header class="site-header">
    <div class="site-inner">
      <div class="brand" aria-label="CodeAgent">
        <span class="brand-mark">&lt;/&gt;</span>
        <span class="brand-copy">
          <strong>CodeAgent</strong>
          <em>AI 编程练习</em>
        </span>
      </div>

      <nav class="site-nav" aria-label="主导航">
        <router-link to="/">首页</router-link>
        <router-link to="/question">题库</router-link>
        <router-link to="/knowledge">知识点</router-link>
        <router-link to="/contests">竞赛</router-link>
        <router-link to="/rank">排名</router-link>
        <router-link to="/wrong-book">错题本</router-link>
        <router-link to="/dashboard">学习看板</router-link>
        <router-link to="/question/import">图片录题</router-link>
      </nav>

      <button
        class="menu-toggle"
        type="button"
        aria-label="打开导航"
        :aria-expanded="menuOpen"
        @click="menuOpen = !menuOpen"
      >
        <span></span><span></span><span></span>
      </button>

      <div class="header-right">
        <button class="command-trigger" type="button" aria-label="全局搜索" title="全局搜索 (Ctrl/Cmd+K)" @click="openCommand">
          <el-icon><Search /></el-icon>
        </button>
        <span class="health" :class="healthClass">
          <i></i>{{ healthText }}
        </span>
        <template v-if="userStore.user">
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-pill" type="button">
              <UserAvatar :name="userStore.displayName" :avatar="userStore.user?.avatar" size="sm" />
              <span class="user-name">{{ userStore.displayName }}</span>
              <span class="caret">⌄</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin-contest">创建竞赛</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <button v-else class="login-link" type="button" @click="openLogin">登录</button>
      </div>
    </div>

    <nav v-if="menuOpen" class="mobile-menu" aria-label="移动端导航">
      <button class="mobile-command" type="button" @click="openCommandFromMenu">
        <el-icon><Search /></el-icon>
        搜索题目、知识点或竞赛
      </button>
      <router-link to="/" @click="menuOpen = false">首页</router-link>
      <router-link to="/question" @click="menuOpen = false">题库</router-link>
      <router-link to="/knowledge" @click="menuOpen = false">知识点</router-link>
      <router-link to="/contests" @click="menuOpen = false">竞赛</router-link>
      <router-link to="/rank" @click="menuOpen = false">排名</router-link>
      <router-link to="/wrong-book" @click="menuOpen = false">错题本</router-link>
      <router-link to="/dashboard" @click="menuOpen = false">学习看板</router-link>
      <router-link to="/question/import" @click="menuOpen = false">图片录题</router-link>
    </nav>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getHealth } from '@/api/health'
import { useUserStore } from '@/store/user'
import { useAuthUiStore } from '@/store/auth-ui'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const authUi = useAuthUiStore()

function openLogin() {
  authUi.openAuth('login', route.name === 'home' ? '/question' : route.fullPath)
}
const health = ref(null)
const menuOpen = ref(false)
let timer = null

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  }
)

const healthClass = computed(() => {  if (!health.value) return 'is-checking'
  if (health.value.db === 'UP') return 'is-up'
  return 'is-warn'
})
const healthText = computed(() => {
  if (!health.value) return '检查服务'
  return health.value.db === 'UP' ? '服务正常' : '数据库未连接'
})
const isAdmin = computed(() => userStore.user?.role === 'ADMIN')

function openCommand() {
  window.dispatchEvent(new CustomEvent('command:open'))
}

function openCommandFromMenu() {
  menuOpen.value = false
  openCommand()
}

async function loadHealth() {
  try {
    health.value = await getHealth()
  } catch {
    health.value = null
  }
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'admin-contest') {
    router.push('/admin/contests/new')
    return
  }
  if (command === 'logout') {
    userStore.logout()
      .then(() => {
        ElMessage.success('已退出登录')
        router.replace('/')
      })
      .catch(() => {
        ElMessage.error('退出失败，请稍后再试')
      })
  }
}

onMounted(() => {
  if (userStore.token && !userStore.user) {
    userStore.fetchMe().catch(() => {})
  }
  loadHealth()
  timer = window.setInterval(loadHealth, 60000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #f5f6f7;
  border-bottom: 0;
}

.site-inner {
  width: min(1200px, calc(100% - 48px));
  height: 64px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-width: 140px;
  color: inherit;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: #fff;
  background: #1f3a5f;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -1px;
  box-shadow: none;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-copy strong {
  color: #111827;
  font-size: 16px;
  letter-spacing: -0.3px;
}

.brand-copy em {
  display: none;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.site-nav a {
  color: #6b7280;
  font-size: 13.5px;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.16s ease;
}

.site-nav a:hover,
.site-nav a.router-link-active {
  color: #111827;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.command-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

.command-trigger:hover {
  border-color: #e1e5ee;
  background: #f8fafc;
  color: #1f3a5f;
}

.health {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 11.5px;
  white-space: nowrap;
}

.health i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd5e1;
}

.health.is-up i {
  background: #2f7d5b;
  box-shadow: 0 0 0 4px rgba(47, 125, 91, 0.10);
}

.health.is-warn i {
  background: #a16207;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 10px 0 5px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  color: #111827;
  cursor: pointer;
  transition: border-color 0.16s ease;
}

.user-pill:hover {
  border-color: #d1d5db;
}

.avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 12px;
  font-weight: 700;
}

.user-name {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 650;
}

.caret {
  color: #94a3b8;
  font-size: 12px;
}

.login-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 16px;
  border: 1px solid #d1d5db;
  border-radius: 999px;
  background: #fff;
  color: #111827;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
}

.login-link:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.menu-toggle {
  display: none;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid #e1e5ee;
  border-radius: 9px;
  background: #fff;
  cursor: pointer;
  align-content: center;
  justify-items: center;
  gap: 4px;
}

.menu-toggle span {
  display: block;
  width: 15px;
  height: 1.5px;
  border-radius: 2px;
  background: #334155;
}

.mobile-menu {
  position: absolute;
  top: 64px;
  left: 16px;
  right: 16px;
  z-index: 21;
  display: grid;
  padding: 8px;
  border: 1px solid #e5e9f2;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.12);
}

.mobile-menu a {
  padding: 11px 12px;
  border-radius: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.mobile-command {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  padding: 11px 12px;
  border: 0;
  border-radius: 8px;
  background: #f4f6f8;
  color: #1f3a5f;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.mobile-menu a:hover,
.mobile-menu a.router-link-active {
  background: #f4f6f8;
  color: #1f3a5f;
}

@media (max-width: 900px) {
  .site-inner {
    width: calc(100% - 32px);
    gap: 10px;
  }

  .brand {
    min-width: 0;
  }

  .site-nav {
    display: none;
  }

  .command-trigger {
    display: none;
  }

  .header-right {
    order: 3;
    margin-left: 0;
    gap: 8px;
  }

  .menu-toggle {
    display: grid;
    order: 2;
    margin-left: auto;
    flex-shrink: 0;
  }

  .health {
    display: none;
  }
}
</style>
