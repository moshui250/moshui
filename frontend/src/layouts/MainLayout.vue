<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  Collection,
  DataAnalysis,
  MagicStick,
  Menu,
  QuestionFilled,
  Reading,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { getHealth } from '@/api/health'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarOpen = ref(false)
const loggingOut = ref(false)
const health = reactive({
  status: 'CHECKING',
  db: 'UNKNOWN',
  time: ''
})

let healthTimer = null

const navItems = [
  { path: '/question', label: '题库', icon: Reading },
  { path: '/wrong-book', label: '错题本', icon: Collection },
  { path: '/dashboard', label: '数据看板', icon: DataAnalysis }
]

const activeMenu = computed(() => {
  if (route.path.startsWith('/question')) return '/question'
  return route.path
})

const section = computed(() => route.meta.section || 'CodeAgent')
const pageTitle = computed(() => route.meta.title || '')
const initial = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())

const healthClass = computed(() => {
  if (health.status === 'CHECKING') return 'is-checking'
  if (health.status === 'OFFLINE') return 'is-offline'
  return health.db === 'UP' ? 'is-ok' : 'is-warn'
})

const healthLabel = computed(() => {
  if (health.status === 'CHECKING') return '检查服务中'
  if (health.status === 'OFFLINE') return '服务未连接'
  return health.db === 'UP' ? '服务正常' : '数据库未连接'
})

const healthTip = computed(() => {
  if (health.status === 'OFFLINE') return '后端未启动，或 8080 端口不可达'
  if (health.db === 'UP') return `数据库连接正常${health.time ? ` · ${health.time}` : ''}`
  return `后端已启动，但 MySQL 未连接${health.time ? ` · ${health.time}` : ''}`
})

async function checkHealth() {
  try {
    const data = await getHealth()
    health.status = data?.status || 'UP'
    health.db = data?.db || 'DOWN'
    health.time = data?.time || ''
  } catch {
    health.status = 'OFFLINE'
    health.db = 'DOWN'
    health.time = ''
  }
}

function handleUserCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  }
}

async function handleLogout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await userStore.logout()
    ElMessage.success('已退出登录')
    await router.replace({ name: 'login' })
  } finally {
    loggingOut.value = false
  }
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

watch(
  () => route.fullPath,
  () => {
    sidebarOpen.value = false
  }
)

onMounted(() => {
  checkHealth()
  healthTimer = window.setInterval(checkHealth, 60000)
})

onBeforeUnmount(() => {
  if (healthTimer) window.clearInterval(healthTimer)
})
</script>

<template>
  <div class="app-shell" :class="{ 'sidebar-open': sidebarOpen }">
    <div class="sidebar-mask" @click="sidebarOpen = false"></div>

    <aside class="app-sidebar">
      <div class="brand" @click="router.push('/question')">
        <div class="brand-mark">&lt;/&gt;</div>
        <div class="brand-copy">
          <strong>CodeAgent</strong>
          <span>智码刷题</span>
        </div>
      </div>

      <div class="nav-label">学习空间</div>
      <el-menu class="side-menu" :default-active="activeMenu" router>
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-spacer"></div>

      <div class="coach-card">
        <div class="coach-icon">
          <el-icon><MagicStick /></el-icon>
        </div>
        <div class="coach-copy">
          <strong>AI 调试教练</strong>
          <p>提交失败后自动分析报错，分层给出提示，不直接给答案。</p>
        </div>
        <span class="coach-badge">S12 接入</span>
      </div>

      <div class="sidebar-footer">v0.1 · 工程骨架</div>
    </aside>

    <div class="app-main">
      <header class="topbar">
        <button class="icon-button mobile-only" aria-label="打开菜单" @click="sidebarOpen = true">
          <el-icon><Menu /></el-icon>
        </button>

        <div class="crumb">
          <span>{{ section }}</span>
          <el-icon><ArrowRight /></el-icon>
          <strong>{{ pageTitle }}</strong>
        </div>

        <div class="topbar-right">
          <el-tooltip :content="healthTip" placement="bottom">
            <button class="health-chip" :class="healthClass" @click="checkHealth">
              <span class="health-dot"></span>
              <span>{{ healthLabel }}</span>
            </button>
          </el-tooltip>

          <el-tooltip content="帮助与快捷键" placement="bottom">
            <button class="icon-button" aria-label="帮助">
              <el-icon><QuestionFilled /></el-icon>
            </button>
          </el-tooltip>

          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleUserCommand">
              <button class="user-chip">
                <span class="user-avatar">{{ initial }}</span>
                <span class="user-name">{{ userStore.displayName || '已登录' }}</span>
                <el-icon class="user-arrow"><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <el-button
              class="logout-btn"
              text
              :loading="loggingOut"
              @click="handleLogout"
            >
              <el-icon v-if="!loggingOut"><SwitchButton /></el-icon>
              退出
            </el-button>
          </template>

          <template v-else>
            <el-button class="login-btn" type="primary" plain size="small" @click="goLogin">
              登录
            </el-button>
          </template>
        </div>
      </header>

      <main class="page-wrap">
        <router-view />
      </main>

      <footer class="app-footer">
        <span>CodeAgent · 让每一次报错都有回应</span>
        <span class="footer-sep">/</span>
        <span>AI 原生编程练习平台</span>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--ca-bg);
}

.app-sidebar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  flex-direction: column;
  flex: 0 0 244px;
  width: 244px;
  height: 100vh;
  padding: 22px 14px 16px;
  background: #fff;
  border-right: 1px solid var(--ca-line);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 10px 22px;
  cursor: pointer;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #fff;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -1px;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.28);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-copy strong {
  font-size: 17px;
  letter-spacing: -0.3px;
  color: var(--ca-ink);
}

.brand-copy span {
  margin-top: 3px;
  font-size: 11px;
  letter-spacing: 1.5px;
  color: var(--ca-muted);
}

.nav-label {
  padding: 14px 14px 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1.2px;
  color: #94a3b8;
}

.side-menu {
  border-right: 0;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  height: 44px;
  margin: 3px 6px;
  padding: 0 13px !important;
  border-radius: 10px;
  color: #475569;
  font-size: 14px;
  font-weight: 500;
}

.side-menu :deep(.el-menu-item .el-icon) {
  margin-right: 11px;
  font-size: 18px;
}

.side-menu :deep(.el-menu-item:hover) {
  background: #f8fafc;
  color: var(--ca-ink);
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--ca-primary-soft);
  color: var(--ca-primary);
  font-weight: 600;
}

.sidebar-spacer {
  flex: 1;
}

.coach-card {
  position: relative;
  margin: 10px 4px 12px;
  padding: 15px 14px 13px;
  overflow: hidden;
  border: 1px solid #dfe3ff;
  border-radius: 14px;
  background:
    radial-gradient(circle at 100% 0%, rgba(99, 102, 241, 0.16), transparent 52%),
    linear-gradient(160deg, #f7f8ff 0%, #ffffff 100%);
}

.coach-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  margin-bottom: 9px;
  border-radius: 9px;
  background: #eef2ff;
  color: var(--ca-primary);
  font-size: 17px;
}

.coach-copy strong {
  font-size: 13px;
  color: var(--ca-ink);
}

.coach-copy p {
  margin: 5px 0 9px;
  font-size: 11.5px;
  line-height: 1.65;
  color: var(--ca-muted);
}

.coach-badge {
  display: inline-block;
  padding: 2px 7px;
  border-radius: 5px;
  background: #fff;
  border: 1px solid #e0e7ff;
  color: #6366f1;
  font-size: 10px;
  letter-spacing: 0.3px;
}

.sidebar-footer {
  padding: 4px 14px 0;
  font-size: 10.5px;
  color: #a3adc2;
}

.app-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16px;
  height: 64px;
  padding: 0 26px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--ca-line);
  backdrop-filter: blur(12px);
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 13px;
  color: #94a3b8;
}

.crumb .el-icon {
  font-size: 12px;
}

.crumb strong {
  color: var(--ca-ink);
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.health-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--ca-line);
  border-radius: 999px;
  background: #fff;
  color: var(--ca-muted);
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.health-chip:hover {
  border-color: #cbd5e1;
}

.health-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #94a3b8;
}

.health-chip.is-ok .health-dot {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.13);
}

.health-chip.is-warn .health-dot {
  background: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.13);
}

.health-chip.is-offline .health-dot {
  background: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.health-chip.is-checking .health-dot {
  background: #6366f1;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #64748b;
  font-size: 17px;
  cursor: pointer;
}

.icon-button:hover {
  background: #f1f5f9;
  color: var(--ca-ink);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 9px 0 4px;
  border: 1px solid var(--ca-line);
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #eef2ff;
  color: var(--ca-primary);
  font-size: 11px;
  font-weight: 700;
}

.user-name {
  max-width: 96px;
  overflow: hidden;
  color: #334155;
  font-size: 12.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  color: #94a3b8;
  font-size: 11px;
}

.login-btn {
  border-radius: 8px;
}

.logout-btn {
  height: 32px;
  padding: 0 6px;
  color: #64748b;
  font-size: 12.5px;
}

.logout-btn:hover {
  color: #dc2626;
}

.page-wrap {
  flex: 1;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 30px 34px;
}

.app-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 6px 20px 20px;
  color: #a3adc2;
  font-size: 11px;
}

.footer-sep {
  color: #cbd5e1;
}

.sidebar-mask {
  display: none;
}

.mobile-only {
  display: none;
}

@media (max-width: 980px) {
  .app-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
    box-shadow: 16px 0 40px rgba(15, 23, 42, 0.12);
  }

  .sidebar-open .app-sidebar {
    transform: translateX(0);
  }

  .sidebar-mask {
    position: fixed;
    inset: 0;
    z-index: 25;
    display: block;
    background: rgba(15, 23, 42, 0.3);
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease;
  }

  .sidebar-open .sidebar-mask {
    opacity: 1;
    pointer-events: auto;
  }

  .mobile-only {
    display: grid;
  }

  .topbar {
    padding: 0 18px;
  }

  .page-wrap {
    padding: 22px 18px 30px;
  }
}

@media (max-width: 640px) {
  .health-chip span:last-child {
    display: none;
  }

  .health-chip {
    width: 30px;
    justify-content: center;
    padding: 0;
  }

  .user-name,
  .user-arrow {
    display: none;
  }

  .user-chip {
    padding: 0 3px;
  }
}
</style>
