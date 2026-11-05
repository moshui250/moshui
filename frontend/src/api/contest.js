import request from '@/utils/request'

/**
 * 分页查询竞赛。
 *
 * @param {{page?:number,size?:number,status?:string}} params 查询条件
 * @returns {Promise<{records:Array,total:number,page:number,size:number}>}
 */
export function listContests(params = {}) {
  return request.get('/contest', { params })
}

/**
 * 查询竞赛详情和题单。
 *
 * @param {number|string} contestId 竞赛 ID
 * @returns {Promise<object>}
 */
export function getContest(contestId) {
  return request.get(`/contest/${contestId}`)
}

/**
 * 报名竞赛。
 *
 * @param {number|string} contestId 竞赛 ID
 * @returns {Promise<{registered:boolean,participantCount:number}>}
 */
export function registerContest(contestId) {
  return request.post(`/contest/${contestId}/register`)
}

/**
 * 分页查询竞赛排名。
 *
 * @param {number|string} contestId 竞赛 ID
 * @param {{page?:number,size?:number}} params 分页参数
 * @returns {Promise<{records:Array,total:number,page:number,size:number}>}
 */
export function getContestRank(contestId, params = {}) {
  return request.get(`/contest/${contestId}/rank`, { params })
}
