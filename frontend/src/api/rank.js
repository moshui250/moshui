import request from '@/utils/request'

/**
 * 获取总刷题排行榜、个人排名与段位规则。
 */
export function getRanking() {
  return request.get('/rank', { silent: true })
}
