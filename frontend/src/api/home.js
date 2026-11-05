import request from '@/utils/request'

/**
 * 查询首页公开统计：全站通过率、题目数与本周提交曲线。
 *
 * @returns {Promise<object>} 首页统计
 */
export function getHomeSummary() {
  return request.get('/home/summary', { silent: true })
}
