<template>
  <header class="site-header">
    <div class="site-inner">
      <router-link class="brand" to="/" aria-label="CodeAgent 首页" title="CodeAgent 首页">
        <img class="brand-mark" src="/brand-mark.svg" alt="" width="32" height="32" />
      </router-link>

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
        <el-popover placement="bottom-end" :width="202" trigger="click" popper-class="surface-popover">
          <template #reference>
            <button class="surface-trigger" type="button" aria-label="切换深浅色主题" title="切换深浅色主题">
              <el-icon><component :is="themeKey === 'dark' ? Sunny : Moon" /></el-icon>
            </button>
          </template>
          <div class="surface-panel">
            <div class="surface-head">
              <span>页面主题</span>
              <small>{{ activeTheme.label }}</small>
            </div>
            <div class="surface-options">
              <button
                v-for="item in themeOptions"
                :key="item.key"
                class="surface-option"
                :class="{ 'is-active': themeKey === item.key }"
                type="button"
                :aria-label="item.label"
                :title="item.label"
                @click="setTheme(item.key)"
              >
                <span class="surface-swatch" :style="{ background: item.color }"></span>
                <span>{{ item.label }}</span>
              </button>
            </div>
          </div>
        </el-popover>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Moon, Search, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useAuthUiStore } from '@/store/auth-ui'
import { useSurfaceTheme } from '@/store/theme'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const authUi = useAuthUiStore()
const { options: themeOptions, themeKey, activeTheme, setTheme } = useSurfaceTheme()

function openLogin() {
  authUi.openAuth('login', route.name === 'home' ? '/question' : route.fullPath)
}
const menuOpen = ref(false)

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  }
)

const isAdmin = computed(() => userStore.user?.role === 'ADMIN')

function openCommand() {
  window.dispatchEvent(new CustomEvent('command:open'))
}

function openCommandFromMenu() {
  menuOpen.value = false
  openCommand()
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
})
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--page-bg, #f5f6f7);
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
  gap: 0;
  min-width: 32px;
  color: inherit;
  text-decoration: none;
}

.brand-mark {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: block;
  object-fit: contain;
  border-radius: 0;
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

.surface-trigger {
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

.surface-trigger:hover {
  border-color: #e1e5ee;
  background: #f8fafc;
  color: #1f3a5f;
}

.surface-panel {
  padding: 2px;
}

.surface-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px 8px;
  color: #111827;
  font-size: 12px;
  font-weight: 600;
}

.surface-head small {
  color: #8b95a5;
  font-size: 11px;
  font-weight: 500;
}

.surface-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.surface-option {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 5px 7px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #4b5563;
  font-size: 11.5px;
  text-align: left;
  cursor: pointer;
}

.surface-option:hover {
  background: #f7f8fa;
}

.surface-option.is-active {
  border-color: #cfd8e4;
  background: #f7f9fb;
  color: #111827;
}

.surface-swatch {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 6px;
}

:global(.surface-popover.el-popover) {
  padding: 10px;
  border-color: #e4e8ef;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.10);
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
