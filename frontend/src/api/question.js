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

/**
 * 上传并压缩题目图片。
 *
 * @param {File} file 原始图片
 * @param {(percent:number)=>void} onProgress 上传进度回调
 * @returns {Promise<{url:string}>} 可访问图片地址
 */
export function uploadQuestionImage(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/file/image', form, {
    silent: true,
    onUploadProgress: (event) => {
      if (event.total) onProgress?.(Math.round((event.loaded / event.total) * 100))
    }
  })
}

/**
 * 识别题目图片，返回草稿；不会直接入库。
 *
 * @param {File} file 原始图片
 * @param {string} imageUrl 已上传图片地址，可为空
 * @param {(percent:number)=>void} onProgress 上传进度回调
 * @returns {Promise<object>} parseId、draft、missingFields、warn、imageUrl
 */
export function parseQuestionImage(file, imageUrl, onProgress) {
  const form = new FormData()
  form.append('file', file)
  if (imageUrl) form.append('imageUrl', imageUrl)
  return request.post('/question/parse-image', form, {
    silent: true,
    onUploadProgress: (event) => {
      if (event.total) onProgress?.(Math.round((event.loaded / event.total) * 100))
    }
  })
}

/**
 * 用户确认草稿后入库。
 *
 * @param {object} data 包含 parseId 和修改后的题目字段
 * @returns {Promise<{questionId:number, needManualConfirm:boolean, message:string}>} 入库结果
 */
export function importParsedQuestion(data) {
  return request.post('/question/import-parsed', data, { silent: true })
}

/**
 * 手动创建个人题。
 *
 * @param {object} data 题目字段、样例和隐藏用例
 * @returns {Promise<number>} 新题 ID
 */
export function createQuestion(data) {
  return request.post('/question', data, { silent: true })
}
