import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearToken, getToken } from '@/utils/auth-token'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 多个请求同时返回 40100 时，只提示并跳转一次。
let authExpiredLocked = false

export function resetAuthExpired() {
  authExpiredLocked = false
}

function handleAuthExpired(message) {
  clearToken()

  if (authExpiredLocked) return
  authExpiredLocked = true

  ElMessage.warning(message || '登录状态已失效，请重新登录')
  window.dispatchEvent(new Event('auth:expired'))

  // 页面已经跳转后解锁；即使解锁，重复跳转仍由 main.js 的导航锁兜底。
  window.setTimeout(() => {
    authExpiredLocked = false
  }, 2000)
}

function makeError(message, body, status) {
  const error = new Error(message)
  error.code = body?.code
  error.status = status
  return error
}

function resolveHttpMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  if (body?.message) return body.message
  if (status === 401) return '请先登录'
  if (status === 403) return '没有操作权限'
  if (status === 404) return '接口暂未实现'
  if (status === 429) return '请求过于频繁，请稍后再试'
  if (status >= 500) return '服务暂时不可用'
  if (error.code === 'ECONNABORTED') return '请求超时，请检查后端服务'
  return error.message || '网络异常'
}

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data

      if (body.code === 40100) {
        handleAuthExpired(body.message)
        return Promise.reject(makeError(body.message || '登录状态已失效', body, response.status))
      }

      if (!response.config.silent) {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(makeError(body.message || '请求失败', body, response.status))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    const isUnauthorized = body?.code === 40100 || (status === 401 && !body?.code)

    if (isUnauthorized) {
      handleAuthExpired(body?.message)
      return Promise.reject(makeError(body?.message || '登录状态已失效', body, status))
    }

    const message = resolveHttpMessage(error)
    if (!error.config?.silent) {
      ElMessage.error(message)
    }
    return Promise.reject(makeError(message, body, status))
  }
)

export default request
