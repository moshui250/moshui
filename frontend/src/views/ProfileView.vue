<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  InfoFilled,
  Lock,
  Monitor,
  Setting,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
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

const isLoggedIn = computed(() => userStore.isLoggedIn)
const displayName = computed(() => userStore.displayName || '未登录')
const initial = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())
const roleLabel = computed(() => {
  const role = userStore.user?.role
  if (role === 'ADMIN') return '管理员'
  if (role === 'USER') return '学习者'
  return '访客'
})

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

function goLogin() {
  router.push({ name: 'login', query: { redirect: '/profile' } })
}

async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出登录')
}
</script>

<template>
  <div class="profile-page">
    <div class="page-head">
      <div>
        <div class="page-eyebrow">Account</div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理账号信息与本地编辑器偏好。学习数据会在接入统计接口后同步展示。</p>
      </div>
    </div>

    <div class="profile-grid">
      <section class="panel identity-card">
        <div class="identity-main">
          <div class="profile-avatar">{{ initial }}</div>
          <div class="identity-copy">
            <div class="identity-name">
              {{ displayName }}
              <el-tag size="small" effect="plain" round>{{ roleLabel }}</el-tag>
            </div>
            <p v-if="isLoggedIn">
              {{ userStore.user?.username ? `@${userStore.user.username}` : '已登录用户' }}
            </p>
            <p v-else>登录后可保存刷题进度、错题与 AI 对话记录。</p>
          </div>
        </div>

        <div class="identity-meta">
          <div class="meta-item">
            <span>账号状态</span>
            <strong>{{ isLoggedIn ? '正常' : '未登录' }}</strong>
          </div>
          <div class="meta-item">
            <span>默认语言</span>
            <strong>{{ prefs.language === 'JAVA' ? 'Java' : 'C++' }}</strong>
          </div>
          <div class="meta-item">
            <span>编辑器字号</span>
            <strong>{{ prefs.fontSize }} px</strong>
          </div>
        </div>

        <div class="identity-actions">
          <el-button v-if="!isLoggedIn" type="primary" @click="goLogin">
            <el-icon><User /></el-icon>
            登录 / 注册
          </el-button>
          <el-button v-else type="danger" plain @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
          <span v-if="isLoggedIn" class="logout-note">JWT 无状态登出，由客户端清理 token</span>
        </div>
      </section>

      <section class="panel prefs-card">
        <div class="panel-head">
          <div>
            <div class="panel-title">编辑器偏好</div>
            <div class="panel-subtitle">保存在当前浏览器，不会上传</div>
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

          <div class="save-hint">
            <span>{{ savedHint || '修改后自动保存' }}</span>
          </div>
        </div>
      </section>
    </div>

    <div class="bottom-grid">
      <section class="panel info-card">
        <div class="info-icon">
          <el-icon><Lock /></el-icon>
        </div>
        <div>
          <h3>账号与安全</h3>
          <p>认证模块将在 S3 接入，包含注册、登录、JWT 鉴权与 /api/auth/me。当前页面为前端骨架。</p>
        </div>
        <el-tag type="info" effect="plain" round size="small">S3</el-tag>
      </section>

      <section class="panel info-card">
        <div class="info-icon">
          <el-icon><Monitor /></el-icon>
        </div>
        <div>
          <h3>关于 CodeAgent</h3>
          <p>面向编程新手的 AI 原生练习平台。技术栈：Vue 3、Vite、Element Plus、Pinia、Monaco、ECharts。</p>
        </div>
        <el-tag type="info" effect="plain" round size="small">v0.1</el-tag>
      </section>
    </div>

    <div class="version-note">
      <el-icon><InfoFilled /></el-icon>
      <span>当前为 S0 工程骨架，题库、判题、AI 调试等模块将按步骤接入。</span>
    </div>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.identity-card {
  padding: 24px;
}

.identity-main {
  display: flex;
  align-items: center;
  gap: 15px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 58px;
  height: 58px;
  border-radius: 16px;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.4), transparent 45%),
    linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-size: 23px;
  font-weight: 650;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.2);
}

.identity-name {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--ca-ink);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.identity-copy p {
  margin-top: 5px;
  color: #8a93a6;
  font-size: 11.5px;
}

.identity-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 22px;
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
  margin-top: 18px;
}

.logout-note {
  color: #a3adc2;
  font-size: 10.5px;
}

.head-icon {
  color: #a5b4fc;
  font-size: 17px;
}

.prefs-body {
  padding: 6px 20px 14px;
}

.pref-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 0;
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
  width: 168px;
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

.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px;
}

.info-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: #f8fafc;
  color: #64748b;
  font-size: 16px;
}

.info-card > div:nth-child(2) {
  flex: 1;
}

.info-card h3 {
  color: #334155;
  font-size: 12.5px;
  font-weight: 650;
}

.info-card p {
  margin-top: 5px;
  color: #7c869c;
  font-size: 11px;
  line-height: 1.65;
}

.version-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  color: #a3adc2;
  font-size: 10.5px;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .identity-meta {
    grid-template-columns: 1fr;
    gap: 10px;
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
