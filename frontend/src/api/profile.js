import request from '@/utils/request'

export function getProfileStat() {
  return request.get('/profile/stat', { silent: true })
}

export function listWrongQuestions(params) {
  return request.get('/wrong/list', { params, silent: true })
}

export function markMastered(questionId) {
  return request.put(`/wrong/${questionId}/mastered`, { mastered: true })
}
