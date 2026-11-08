import { defineStore } from 'pinia'

export const useAuthUiStore = defineStore('authUi', {
  state: () => ({
    open: false,
    mode: 'login',
    redirect: ''
  }),

  actions: {
    openAuth(mode = 'login', redirect = '') {
      this.mode = mode === 'register' ? 'register' : 'login'
      this.redirect = typeof redirect === 'string' ? redirect : ''
      this.open = true
    },

    closeAuth() {
      this.open = false
    }
  }
})
