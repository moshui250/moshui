import { defineStore } from 'pinia'
import { getMeApi, loginApi, logoutApi, registerApi, updateProfileApi } from '@/api/auth'
import { clearToken, getToken, saveToken } from '@/utils/auth-token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: null
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.nickname || '用户'
  },

  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user || null
      saveToken(token)
      window.dispatchEvent(new Event('auth:login'))
    },

    async login(payload) {
      const data = await loginApi(payload)
      if (!data?.token) {
        throw new Error('登录响应缺少 token')
      }
      this.setSession(data.token, data.user)
      return data
    },

    async register(payload) {
      return registerApi(payload)
    },

    async fetchMe() {
      if (!this.token) return null
      try {
        const user = await getMeApi()
        this.user = user
        return user
      } catch (error) {
        if (error?.code === 40100 || error?.status === 401) {
          this.clearAuth()
        }
        throw error
      }
    },

    async updateProfile(payload) {
      const user = await updateProfileApi(payload)
      this.user = user
      return user
    },

    async logout() {
      if (!this.token) {
        this.clearAuth()
        return
      }
      try {
        await logoutApi()
      } catch {
        // 无状态 JWT：服务端不可达时也必须完成本地退出。
      } finally {
        this.clearAuth()
      }
    },

    clearAuth() {
      this.token = ''
      this.user = null
      clearToken()
    }
  }
})
