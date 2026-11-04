import request from '@/utils/request'
import { getToken } from '@/utils/auth-token'

/**
 * 读取某道题的 Agent 历史，按会话分组。
 *
 * @param {number|string} questionId 题目 ID
 * @returns {Promise<{sessions: Array}>} 历史会话
 */
export function getAgentHistory(questionId) {
  return request.get('/agent/history', {
    params: { questionId },
    silent: true
  })
}

/**
 * 用 fetch + ReadableStream 消费 SSE。
 * EventSource 只支持 GET，无法发送 POST 请求体。
 *
 * @param {object} payload 流式请求体
 * @param {(event: {type: string, data: object}) => void} onEvent 事件回调
 * @param {AbortSignal} signal 取消信号
 * @returns {Promise<void>} 流结束时完成
 */
export async function streamAgent(payload, { onEvent, signal } = {}) {
  const token = getToken()
  const response = await fetch('/api/agent/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal
  })

  if (!response.ok) {
    let message = `调试请求失败（${response.status}）`
    let code = response.status
    try {
      const body = await response.json()
      message = body?.message || message
      code = body?.code ?? code
    } catch {
      // 非 JSON 错误体保留 HTTP 状态
    }
    const error = new Error(message)
    error.status = code
    throw error
  }

  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const block = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      emitBlock(block, onEvent)
      boundary = buffer.indexOf('\n\n')
    }
  }

  if (buffer.trim()) {
    emitBlock(buffer, onEvent)
  }
}

/**
 * 解析一个 SSE 事件块。
 *
 * @param {string} block 事件块
 * @param {(event: {type: string, data: object}) => void} onEvent 回调
 * @returns {void}
 */
function emitBlock(block, onEvent) {
  let event = 'message'
  const dataLines = []

  for (const line of block.split('\n')) {
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  }

  if (!dataLines.length) return
  let data = {}
  try {
    data = JSON.parse(dataLines.join('\n'))
  } catch {
    data = { message: dataLines.join('\n') }
  }
  onEvent?.({ type: event, data })
}
