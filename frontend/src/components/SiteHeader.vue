<template>
  <header class="site-header">
    <div class="site-inner">
      <router-link class="brand" to="/" aria-label="CodeAgent 首页">
        <span class="brand-mark">&lt;/&gt;</span>
        <span class="brand-copy">
          <strong>CodeAgent</strong>
          <em>AI 编程练习</em>
        </span>
      </router-link>

      <nav class="site-nav" aria-label="主导航">
        <router-link to="/question">题库</router-link>
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
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <router-link v-else class="login-link" :to="{ name: 'login', query: { redirect: $route.fullPath } }">
          登录
        </router-link>
      </div>
    </div>

    <nav v-if="menuOpen" class="mobile-menu" aria-label="移动端导航">
      <router-link to="/question" @click="menuOpen = false">题库</router-link>
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
import { getHealth } from '@/api/health'
import { useUserStore } from '@/store/user'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const health = ref(null)
const menuOpen = ref(false)
let timer = null

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  }
)

const healthClass = computed(() => {
  if (!health.value) return 'is-checking'
  if (health.value.db === 'UP') return 'is-up'
  return 'is-warn'
})
const healthText = computed(() => {
  if (!health.value) return '检查服务'
  return health.value.db === 'UP' ? '服务正常' : '数据库未连接'
})

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
  background: #ffffff;
  border-bottom: 1px solid #e8eaf0;
}

.site-inner {
  width: min(1240px, calc(100% - 48px));
  height: 68px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 36px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  min-width: 180px;
  color: inherit;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  color: #fff;
  background: #1f3a5f;
  font-size: 15px;
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
  color: #0f172a;
  font-size: 17px;
  letter-spacing: -0.3px;
}

.brand-copy em {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 10px;
  font-style: normal;
  letter-spacing: 0.12em;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 26px;
  flex: 1;
}

.site-nav a {
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.16s ease;
}

.site-nav a:hover,
.site-nav a.router-link-active {
  color: #1f3a5f;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.health {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #64748b;
  font-size: 12px;
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
  height: 36px;
  padding: 0 10px 0 5px;
  border: 1px solid #e1e5ee;
  border-radius: 999px;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.user-pill:hover {
  border-color: #c8d2dc;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
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
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  background: #1f3a5f;
  color: #fff;
  font-size: 13px;
  font-weight: 650;
  text-decoration: none;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.10);
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
  top: 67px;
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

  .header-right {
    order: 2;
    margin-left: auto;
    gap: 8px;
  }

  .menu-toggle {
    display: grid;
    order: 3;
    flex-shrink: 0;
  }

  .health {
    display: none;
  }
}
</style>
