import request from '@/utils/request'

export function listKnowledgePoints() {
  return request.get('/knowledge-point/list', { silent: true })
}

export function listQuestions(params) {
  return request.get('/question/list', { params, silent: true })
}

export function getQuestion(id) {
  return request.get(`/question/${id}`, { silent: true })
}

export function recommendQuestions(questionId, limit = 3) {
  return request.get('/question/recommend', {
    params: { questionId, limit },
    silent: true
  })
}

/**
 * 提交判题。页面自行处理 404/405/501 的“接口未接入”兜底，因此这里关闭全局提示。
 *
 * @param {{ questionId: number, language: string, code: string }} data 提交题目、语言和代码
 * @returns {Promise<{ submitId: number|string, status: string }>} 判题任务
 */
export function submitCode(data) {
  return request.post('/judge/submit', data, { silent: true })
}

/**
 * 轮询判题结果。
 *
 * @param {number|string} submitId 提交记录 ID
 * @returns {Promise<object>} 含 status、passedCount、caseResults 等字段
 */
export function getJudgeResult(submitId) {
  return request.get(`/judge/result/${submitId}`, { silent: true })
}

/**
 * 只运行样例，不写提交记录。后端 S7 接入后启用。
 *
 * @param {{ questionId: number, language: string, code: string }} data 运行参数
 * @returns {Promise<object>} 样例运行结果
 */
export function runSampleCode(data) {
  return request.post('/judge/run-sample', data, { silent: true })
}
