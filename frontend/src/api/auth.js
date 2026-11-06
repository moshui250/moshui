import request from '@/utils/request'

export function registerApi(data) {
  return request.post('/auth/register', data, { silent: true })
}

export function loginApi(data) {
  return request.post('/auth/login', data, { silent: true })
}

export function logoutApi() {
  return request.post('/auth/logout', null, { silent: true })
}

export function getMeApi() {
  return request.get('/auth/me', { silent: true })
}

export function updateProfileApi(data) {
  return request.put('/auth/profile', data, { silent: true })
}
