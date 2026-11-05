<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Collection,
  DataAnalysis,
  EditPen,
  Lock,
  Picture,
  Setting,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { getProfileStat } from '@/api/profile'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const PREF_KEY = 'codeagent_preferences'
const stored = (() => {
  try {
    return JSON.parse(localStorage.getItem(PREF_KEY) || '{}')
  } catch {
    return {}
  }
})()

const prefs = reactive({
  language: stored.language || 'CPP',
  fontSize: Number(stored.fontSize) || 14,
  autoSave: stored.autoSave !== false
})

const savedHint = ref('')
const loadingStats = ref(false)
const stats = ref({
  totalSubmit: 0,
  accuracy: 0,
  wrongCount: 0
})

const displayName = computed(() => userStore.displayName || '未登录')
const initial = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())
const roleLabel = computed(() => {
  const role = userStore.user?.role
  if (role === 'ADMIN') return '管理员'
  if (role === 'USER') return '学习者'
  return '访客'
})
const accuracyText = computed(() => {
  if (!stats.value.totalSubmit) return '—'
  return `${(Number(stats.value.accuracy || 0) * 100).toFixed(1)}%`
})
const quickLinks = [
  { label: '我的错题', desc: '按知识点复盘', to: '/wrong-book', icon: Collection },
  { label: '学习看板', desc: '提交与正确率', to: '/dashboard', icon: DataAnalysis },
  { label: '图片录题', desc: '识别一张题目图', to: '/question/import', icon: Picture },
  { label: '去题库', desc: '继续下一道题', to: '/question', icon: User }
]

watch(
  prefs,
  (value) => {
    localStorage.setItem(PREF_KEY, JSON.stringify(value))
    savedHint.value = '已保存到本地'
    window.setTimeout(() => {
      savedHint.value = ''
    }, 1600)
  },
  { deep: true }
)

async function loadStats() {
  loadingStats.value = true
  try {
    const data = await getProfileStat()
    stats.value = {
      totalSubmit: Number(data?.totalSubmit || 0),
      accuracy: Number(data?.accuracy || 0),
      wrongCount: Number(data?.wrongCount || 0)
    }
  } catch {
    // 统计失败时保留 0，不影响个人中心其他操作。
  } finally {
    loadingStats.value = false
  }
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: '/profile' } })
}

async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出登录')
  await router.replace({ name: 'login' })
}

onMounted(loadStats)
</script>

<template>
  <div class="profile-page">
    <header class="page-head">
      <div>
        <div class="page-eyebrow">ACCOUNT / 学习账户</div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理账号、查看学习概览，并调整答题页的本地编辑器偏好。</p>
      </div>
      <el-button class="head-action" plain @click="router.push('/question')">
        去题库
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </header>

    <div class="profile-top">
      <section class="panel identity-card">
        <div class="identity-main">
          <div class="profile-avatar">{{ initial }}</div>
          <div class="identity-copy">
            <div class="identity-name">
              {{ displayName }}
              <el-tag size="small" effect="plain" round>{{ roleLabel }}</el-tag>
            </div>
            <p v-if="userStore.isLoggedIn">
              {{ userStore.user?.username ? `@${userStore.user.username}` : '已登录用户' }}
            </p>
            <p v-else>登录后可保存刷题进度、错题与 AI 对话记录。</p>
          </div>
          <span class="account-state">{{ userStore.isLoggedIn ? '正常' : '未登录' }}</span>
        </div>

        <div class="identity-meta">
          <div class="meta-item">
            <span>默认语言</span>
            <strong>{{ prefs.language === 'JAVA' ? 'Java' : 'C++' }}</strong>
          </div>
          <div class="meta-item">
            <span>编辑器字号</span>
            <strong>{{ prefs.fontSize }} px</strong>
          </div>
          <div class="meta-item">
            <span>登录方式</span>
            <strong>JWT</strong>
          </div>
        </div>

        <div class="identity-actions">
          <el-button v-if="!userStore.isLoggedIn" type="primary" @click="goLogin">
            <el-icon><User /></el-icon>
            登录 / 注册
          </el-button>
          <el-button v-else type="danger" plain @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
          <span class="logout-note">退出后本机 token 会立即清除</span>
        </div>
      </section>

      <section v-loading="loadingStats" class="panel stats-card">
        <div class="panel-head">
          <div>
            <div class="panel-title">学习概览</div>
            <div class="panel-subtitle">来自当前账号的真实提交</div>
          </div>
          <router-link class="text-link" to="/dashboard">
            完整看板
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>

        <div class="stat-grid">
          <div class="stat-item">
            <strong>{{ stats.totalSubmit }}</strong>
            <span>总提交</span>
          </div>
          <div class="stat-item">
            <strong>{{ accuracyText }}</strong>
            <span>正确率</span>
          </div>
          <div class="stat-item">
            <strong>{{ stats.wrongCount }}</strong>
            <span>未掌握错题</span>
          </div>
        </div>

        <div class="stat-note">
          <span class="note-dot"></span>
          这些数字与看板使用同一份统计口径
        </div>
      </section>
    </div>

    <div class="profile-main">
      <section class="panel prefs-card">
        <div class="panel-head">
          <div>
            <div class="panel-title">编辑器偏好</div>
            <div class="panel-subtitle">只保存在当前浏览器，不会上传</div>
          </div>
          <el-icon class="head-icon"><Setting /></el-icon>
        </div>

        <div class="prefs-body">
          <div class="pref-row">
            <div>
              <strong>默认语言</strong>
              <p>进入答题页时优先加载的模板</p>
            </div>
            <el-radio-group v-model="prefs.language" size="small">
              <el-radio-button label="CPP">C++</el-radio-button>
              <el-radio-button label="JAVA">Java</el-radio-button>
            </el-radio-group>
          </div>

          <div class="pref-row">
            <div>
              <strong>字号</strong>
              <p>Monaco 编辑器默认字体大小</p>
            </div>
            <div class="slider-wrap">
              <el-slider v-model="prefs.fontSize" :min="12" :max="20" :step="1" />
              <span class="slider-value">{{ prefs.fontSize }}px</span>
            </div>
          </div>

          <div class="pref-row">
            <div>
              <strong>自动保存代码</strong>
              <p>按题目与语言存入 localStorage</p>
            </div>
            <el-switch v-model="prefs.autoSave" />
          </div>

          <div class="save-hint">{{ savedHint || '修改后自动保存' }}</div>
        </div>
      </section>

      <aside class="profile-side">
        <section class="panel quick-card">
          <div class="panel-title">快捷入口</div>
          <div class="quick-list">
            <router-link
              v-for="item in quickLinks"
              :key="item.to"
              :to="item.to"
              class="quick-item"
            >
              <span class="quick-icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="quick-copy">
                <strong>{{ item.label }}</strong>
                <small>{{ item.desc }}</small>
              </span>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </router-link>
          </div>
        </section>

        <section class="panel security-card">
          <div class="security-icon">
            <el-icon><Lock /></el-icon>
          </div>
          <div>
            <h3>账号安全</h3>
            <p>登录由 JWT 保护，token 仅保存在当前浏览器。退出后需要重新验证。</p>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 36px 24px 64px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.page-eyebrow {
  display: none;
  margin-bottom: 9px;
  color: #1f3a5f;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.page-title {
  color: #0f172a;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -1.5px;
  line-height: 1.18;
}

.page-subtitle {
  max-width: 600px;
  margin-top: 9px;
  color: #7c869c;
  font-size: 13px;
  line-height: 1.7;
}

.head-action {
  flex-shrink: 0;
  border-radius: 9px;
  color: #1f3a5f;
  border-color: #dfe5f1;
}

.panel {
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 41, 59, 0.035);
}

.profile-top {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.9fr);
  gap: 16px;
}

.identity-card {
  padding: 24px;
}

.identity-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 58px;
  height: 58px;
  border-radius: 12px;
  background: #1f3a5f;
  color: #fff;
  font-size: 23px;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(15, 23, 42, 0.1);
}

.identity-copy {
  min-width: 0;
}

.identity-name {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.identity-copy p {
  margin-top: 4px;
  color: #8a93a6;
  font-size: 11.5px;
}

.account-state {
  margin-left: auto;
  padding: 4px 9px;
  border: 1px solid #d5e7dd;
  border-radius: 999px;
  background: #edf5f1;
  color: #2f7d5b;
  font-size: 10.5px;
  white-space: nowrap;
}

.identity-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 20px;
  padding: 14px 0;
  border-top: 1px solid #f1f4f9;
  border-bottom: 1px solid #f1f4f9;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item span {
  color: #a0a9ba;
  font-size: 10.5px;
}

.meta-item strong {
  color: #334155;
  font-size: 12.5px;
  font-weight: 600;
}

.identity-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 17px;
}

.logout-note {
  color: #a3adc2;
  font-size: 10.5px;
}

.stats-card {
  padding: 22px;
  background: #fff;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.panel-title {
  color: #1e293b;
  font-size: 14px;
  font-weight: 650;
}

.panel-subtitle {
  margin-top: 3px;
  color: #a0a9ba;
  font-size: 10.5px;
}

.text-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #1f3a5f;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 22px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding-right: 10px;
  border-right: 1px solid #f1f4f9;
}

.stat-item:last-child {
  border-right: 0;
}

.stat-item strong {
  color: #0f172a;
  font-size: 28px;
  font-weight: 750;
  letter-spacing: -1px;
  line-height: 1;
}

.stat-item span {
  color: #8a93a6;
  font-size: 10.5px;
}

.stat-note {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 22px;
  color: #9aa4b7;
  font-size: 10.5px;
}

.note-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #2f7d5b;
}

.profile-main {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
  gap: 16px;
  margin-top: 16px;
}

.prefs-card {
  padding: 22px;
}

.head-icon {
  color: #93a5b8;
  font-size: 17px;
}

.prefs-body {
  margin-top: 8px;
}

.pref-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 15px 0;
  border-bottom: 1px solid #f1f4f9;
}

.pref-row:last-of-type {
  border-bottom: 0;
}

.pref-row strong {
  color: #334155;
  font-size: 12.5px;
  font-weight: 600;
}

.pref-row p {
  margin-top: 3px;
  color: #a0a9ba;
  font-size: 10.5px;
}

.slider-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 180px;
}

.slider-wrap :deep(.el-slider) {
  flex: 1;
}

.slider-value {
  width: 34px;
  color: #64748b;
  font-size: 10.5px;
  text-align: right;
}

.save-hint {
  padding-top: 10px;
  color: #a3adc2;
  font-size: 10px;
  text-align: right;
}

.profile-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-card {
  padding: 20px;
}

.quick-list {
  display: grid;
  gap: 4px;
  margin-top: 12px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border-radius: 9px;
  color: inherit;
  transition: background 0.18s ease;
}

.quick-item:hover {
  background: #f7f9fb;
}

.quick-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 29px;
  height: 29px;
  border-radius: 8px;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 14px;
}

.quick-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.quick-copy strong {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.quick-copy small {
  color: #a0a9ba;
  font-size: 10px;
}

.quick-arrow {
  color: #b6c1d4;
  font-size: 12px;
}

.security-card {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 18px;
  background: #f7f9fb;
}

.security-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 31px;
  height: 31px;
  border-radius: 9px;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 15px;
}

.security-card h3 {
  color: #334155;
  font-size: 12.5px;
  font-weight: 650;
}

.security-card p {
  margin-top: 4px;
  color: #7c869c;
  font-size: 10.5px;
  line-height: 1.65;
}

@media (max-width: 960px) {
  .profile-top,
  .profile-main {
    grid-template-columns: 1fr;
  }

  .profile-side {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .profile-page {
    padding: 24px 14px 48px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .page-title {
    font-size: 30px;
  }

  .head-action {
    align-self: flex-start;
  }

  .identity-meta,
  .stat-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .meta-item {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .stat-item {
    flex-direction: row;
    align-items: baseline;
    justify-content: space-between;
    padding: 0 0 9px;
    border-right: 0;
    border-bottom: 1px solid #f1f4f9;
  }

  .stat-item:last-child {
    padding-bottom: 0;
    border-bottom: 0;
  }

  .stat-item strong {
    font-size: 25px;
  }

  .identity-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-side {
    grid-template-columns: 1fr;
  }

  .pref-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 9px;
  }

  .slider-wrap {
    width: 100%;
  }
}
</style>
