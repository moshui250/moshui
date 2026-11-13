<template>
  <el-dialog
    v-model="authUi.open"
    :width="420"
    class="auth-dialog"
    :show-close="true"
    append-to-body
    destroy-on-close
    @closed="handleClosed"
  >
    <div class="auth-head">
      <p class="auth-kicker">{{ authUi.mode === 'login' ? 'WELCOME BACK' : 'CREATE ACCOUNT' }}</p>
      <h2>{{ authUi.mode === 'login' ? '登录练习空间' : '创建练习账号' }}</h2>
      <p class="auth-note">
        {{ authUi.mode === 'login' ? '继续上次的进度，或者从错题本里挑一道重新开始。' : '保存刷题进度、错题记录和 AI 调试对话。' }}
      </p>
    </div>

    <div class="auth-tabs" role="tablist">
      <button
        type="button"
        :class="{ active: authUi.mode === 'login' }"
        role="tab"
        :aria-selected="authUi.mode === 'login'"
        @click="switchMode('login')"
      >
        登录
      </button>
      <button
        type="button"
        :class="{ active: authUi.mode === 'register' }"
        role="tab"
        :aria-selected="authUi.mode === 'register'"
        @click="switchMode('register')"
      >
        注册
      </button>
    </div>

    <el-form
      ref="formRef"
      :key="authUi.mode"
      :model="form"
      :rules="rules"
      label-position="top"
      class="auth-form"
      @submit.prevent="submit"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model.trim="form.username"
          placeholder="4-20 位字母、数字或下划线"
          maxlength="20"
          autocomplete="username"
        />
      </el-form-item>

      <el-form-item v-if="authUi.mode === 'register'" label="昵称（选填）" prop="nickname">
        <el-input v-model.trim="form.nickname" placeholder="默认使用用户名" maxlength="32" />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          maxlength="32"
          :placeholder="authUi.mode === 'login' ? '输入密码' : '6-32 位密码'"
          :autocomplete="authUi.mode === 'login' ? 'current-password' : 'new-password'"
        />
      </el-form-item>

      <el-form-item v-if="authUi.mode === 'register'" label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password maxlength="32" placeholder="再次输入" />
      </el-form-item>

      <button class="auth-submit" type="button" :disabled="loading" @click="submit">
        <span v-if="loading" class="submit-loading">处理中…</span>
        <span v-else>{{ authUi.mode === 'login' ? '登录' : '注册' }}</span>
        <el-icon v-if="!loading"><ArrowRight /></el-icon>
      </button>
    </el-form>

    <button class="auth-switch" type="button" @click="switchMode(authUi.mode === 'login' ? 'register' : 'login')">
      {{ authUi.mode === 'login' ? '还没有账号？立即注册' : '已经有账号？去登录' }}
    </button>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useAuthUiStore } from '@/store/auth-ui'
import { useUserStore } from '@/store/user'

const authUi = useAuthUiStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const rules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword:
    authUi.mode === 'register'
      ? [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          {
            validator: (_rule, value, callback) => {
              if (value !== form.password) callback(new Error('两次输入的密码不一致'))
              else callback()
            },
            trigger: 'blur'
          }
        ]
      : []
}))

watch(
  () => route.query.auth,
  (mode) => {
    if (mode !== 'login' && mode !== 'register') return
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    authUi.openAuth(mode, redirect)
    const query = { ...route.query }
    delete query.auth
    delete query.redirect
    router.replace({ path: route.path, query })
  },
  { immediate: true }
)

watch(
  () => authUi.mode,
  async () => {
    await nextTick()
    formRef.value?.clearValidate()
  }
)

function resetForm() {
  form.username = ''
  form.nickname = ''
  form.password = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

function switchMode(mode) {
  if (mode === authUi.mode) return
  authUi.mode = mode
  // 登录与注册是两套账号场景，切换时不能沿用上一套表单内容。
  resetForm()
}

function safeRedirect() {
  const raw = authUi.redirect
  if (raw.startsWith('/') && !raw.startsWith('//') && !raw.startsWith('/login') && !raw.startsWith('/register')) {
    return raw
  }
  return '/question'
}

async function submit() {
  if (loading.value) return
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (authUi.mode === 'register') {
      await userStore.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined
      })
      ElMessage.success('注册成功，请登录')
      authUi.mode = 'login'
      form.password = ''
      form.confirmPassword = ''
      return
    }

    await userStore.login({ username: form.username, password: form.password })
    authUi.closeAuth()
    const target = safeRedirect()
    if (target !== route.fullPath) router.push(target)
  } catch (error) {
    // loginApi/registerApi 使用 silent 请求，错误提示必须由弹窗自己给出。
    ElMessage.error(error?.message || (authUi.mode === 'register' ? '注册失败，请稍后重试' : '账号或密码错误'))
  } finally {
    loading.value = false
  }
}

function handleClosed() {
  resetForm()
}
</script>

<style scoped>
.auth-head {
  padding-right: 24px;
}

.auth-kicker {
  margin: 0 0 8px;
  color: #6b7280;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.auth-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 23px;
  letter-spacing: -0.03em;
}

.auth-note {
  margin: 7px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.auth-tabs {
  display: flex;
  gap: 18px;
  margin: 22px 0 18px;
  border-bottom: 1px solid #edf0f5;
}

.auth-tabs button {
  position: relative;
  padding: 0 0 9px;
  border: 0;
  background: transparent;
  color: #8a93a3;
  font-size: 13px;
  cursor: pointer;
}

.auth-tabs button.active {
  color: #0f172a;
  font-weight: 650;
}

.auth-tabs button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: #1f3a5f;
  content: '';
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 15px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 5px;
  color: #475569;
  font-size: 12px;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 40px;
  border-radius: 9px;
  box-shadow: 0 0 0 1px #e5e9ef inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1f3a5f inset, 0 0 0 3px rgba(31, 58, 95, 0.08);
}

.auth-submit {
  display: flex;
  width: 100%;
  height: 42px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
  border: 0;
  border-radius: 9px;
  background: #1f3a5f;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.auth-submit:disabled {
  cursor: default;
  opacity: 0.6;
}

.auth-switch {
  display: block;
  margin: 14px auto 2px;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
}

.auth-switch:hover {
  color: #1f3a5f;
}

.auth-dialog :deep(.el-dialog__body) {
  padding-top: 4px;
}

@media (max-width: 640px) {
  :global(.auth-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
