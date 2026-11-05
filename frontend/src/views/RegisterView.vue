<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, EditPen, Lock, User } from '@element-plus/icons-vue'
import AuthBrandPanel from '@/components/AuthBrandPanel.vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

function validateConfirm(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{4,20}$/,
      message: '用户名为 4-20 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  nickname: [{ max: 32, message: '昵称最多 32 个字符', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

function resolveRedirect() {
  const raw = route.query.redirect
  if (
    typeof raw === 'string' &&
    raw.startsWith('/') &&
    !raw.startsWith('//') &&
    raw !== '/login' &&
    !raw.startsWith('/login?') &&
    raw !== '/register' &&
    !raw.startsWith('/register?')
  ) {
    return raw
  }
  return '/question'
}

async function handleSubmit() {
  if (loading.value) return

  form.username = form.username.trim()
  form.nickname = form.nickname.trim()

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined
    })
    ElMessage.success('注册成功，请使用新账号登录')
    await router.replace({
      name: 'login',
      query: {
        username: form.username,
        redirect: resolveRedirect()
      }
    })
  } catch (error) {
    ElMessage.error(error.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <AuthBrandPanel />

    <section class="auth-panel">
      <router-link class="auth-back" to="/">
        <span aria-hidden="true">←</span>
        返回首页
      </router-link>
      <div class="auth-card">
        <div class="auth-kicker">CREATE ACCOUNT</div>
        <h2>创建你的练习账号</h2>
        <p>保存刷题进度、错题记录，以及每一次 AI 调试对话。</p>

        <el-form
          ref="formRef"
          class="auth-form register-form"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="handleSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              placeholder="4-20 位字母、数字或下划线"
              autocomplete="username"
              clearable
            />
          </el-form-item>

          <el-form-item label="昵称（选填）" prop="nickname">
            <el-input
              v-model="form.nickname"
              :prefix-icon="EditPen"
              placeholder="默认使用用户名"
              maxlength="32"
              clearable
            />
          </el-form-item>

          <div class="password-row">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                :prefix-icon="Lock"
                type="password"
                placeholder="6-32 位"
                autocomplete="new-password"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                :prefix-icon="Lock"
                type="password"
                placeholder="再次输入"
                autocomplete="new-password"
                show-password
              />
            </el-form-item>
          </div>

          <el-button
            class="auth-submit"
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            注册
            <el-icon v-if="!loading" class="submit-arrow"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="auth-links">
          <span>已经有账号？</span>
          <router-link :to="{ name: 'login', query: route.query }">去登录</router-link>
        </div>

        <p class="auth-tip">注册后请使用用户名登录；用户名注册后不可修改。</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.register-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.password-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.submit-arrow {
  margin-left: 4px;
  font-size: 13px;
}

.auth-tip {
  margin-top: 16px !important;
  color: #a0a9ba !important;
  font-size: 11px !important;
  line-height: 1.65;
}

@media (max-width: 480px) {
  .password-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
