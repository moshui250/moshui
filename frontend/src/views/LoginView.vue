<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import AuthBrandPanel from '@/components/AuthBrandPanel.vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{4,20}$/,
      message: '用户名为 4-20 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ]
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

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password
    })
    ElMessage.success('登录成功')
    await router.replace(resolveRedirect())
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <AuthBrandPanel />

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-kicker">WELCOME BACK</div>
        <h2>登录你的练习空间</h2>
        <p>继续上次的题目，或者从错题本里挑一道重新开始。</p>

        <el-form
          ref="formRef"
          class="auth-form"
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

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="6-32 位密码"
              autocomplete="current-password"
              show-password
            />
          </el-form-item>

          <el-button
            class="auth-submit"
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            登录
            <el-icon v-if="!loading" class="submit-arrow"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="auth-links">
          <span>还没有账号？</span>
          <router-link :to="{ name: 'register', query: route.query }">立即注册</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.submit-arrow {
  margin-left: 4px;
  font-size: 13px;
}
</style>
