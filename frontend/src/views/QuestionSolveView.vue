<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  CircleCheck,
  Clock,
  Delete,
  MagicStick,
  RefreshRight,
  VideoPlay,
  Warning
} from '@element-plus/icons-vue'
import AgentTimeline from '@/components/AgentTimeline.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import MarkdownBody from '@/components/MarkdownBody.vue'
import { getAgentHistory, streamAgent } from '@/api/agent'
import { getJudgeResult, getQuestion, runSampleCode, submitCode } from '@/api/question'

const route = useRoute()
const router = useRouter()

const LANGUAGES = [
  { value: 'CPP', label: 'C++', monaco: 'cpp' },
  { value: 'JAVA', label: 'Java', monaco: 'java' }
]
const TERMINAL_STATUSES = new Set(['AC', 'WA', 'TLE', 'MLE', 'RE', 'CE', 'SE'])
const STATUS_META = {
  AC: { label: '通过', type: 'success' },
  WA: { label: '答案错误', type: 'danger' },
  TLE: { label: '超出时间限制', type: 'warning' },
  MLE: { label: '超出内存限制', type: 'warning' },
  RE: { label: '运行错误', type: 'danger' },
  CE: { label: '编译错误', type: 'danger' },
  SE: { label: '系统错误', type: 'danger' },
  PENDING: { label: '排队中', type: 'info' },
  JUDGING: { label: '判题中', type: 'primary' }
}
const CODE_PREFIX = 'codeagent_code_'
const LANGUAGE_KEY = 'codeagent_last_language'

const contestId = computed(() => {
  const raw = Number(route.name === 'contest-solve' ? route.params.id : route.query.contestId)
  return Number.isFinite(raw) && raw > 0 ? raw : null
})

const questionId = computed(() => Number(route.params.questionId || route.params.id))

const loading = ref(true)
const loadError = ref('')
const question = ref(null)
const lowerPanel = ref(null)

const language = ref('CPP')
const code = ref('')
const baselineCode = ref('')
const judging = ref(false)
const judgeResult = ref(null)
const judgeError = ref('')
const usingDemoJudge = ref(false)
const sampleOnly = ref(false)
const activePane = ref('result')
let passNoticeShown = false

const agentEvents = ref([])
const agentAnswer = ref('')
const agentError = ref('')
const agentRunning = ref(false)
const agentCanReveal = ref(false)
const agentSessionId = ref(null)

let pollTimer = null
let demoTimer = null
let saveTimer = null
let disposed = false
let switchingLanguage = false
let agentController = null
let agentRunId = 0

const difficultyMap = {
  1: { label: '入门', type: 'success' },
  2: { label: '进阶', type: 'warning' },
  3: { label: '挑战', type: 'danger' }
}

const difficulty = computed(() => difficultyMap[question.value?.difficulty] || difficultyMap[1])
const knowledgeName = computed(
  () => question.value?.knowledgePoint?.name || question.value?.knowledgePointName || '未分类'
)
const samples = computed(() =>
  (question.value?.samples || []).map((sample, index) => ({
    index: index + 1,
    input: sample.inputData ?? sample.input ?? '',
    output: sample.expectedOutput ?? sample.output ?? ''
  }))
)

const customCases = ref([])
const CUSTOM_CASE_LIMIT = 5

function addCustomCase() {
  if (customCases.value.length >= CUSTOM_CASE_LIMIT) {
    ElMessage.warning(`最多添加 ${CUSTOM_CASE_LIMIT} 条自定义样例`)
    return
  }
  customCases.value.push({ input: '', expectedOutput: '' })
}

function removeCustomCase(index) {
  customCases.value.splice(index, 1)
}

function collectCustomCases() {
  const filled = customCases.value.filter(
    (item) => item.input.trim() || item.expectedOutput.trim()
  )
  if (filled.some((item) => !item.input.trim() || !item.expectedOutput.trim())) {
    ElMessage.warning('请补全自定义样例的输入和期望输出')
    return null
  }
  return filled.map((item) => ({
    input: item.input,
    expectedOutput: item.expectedOutput
  }))
}

// 题面描述通常已包含输入/输出小节；只补缺失部分，避免左栏出现重复标题。
const statementMarkdown = computed(() => {
  const description = question.value?.description || ''
  const missing = []

  if (!/^#{1,6}\s*输入格式/m.test(description)) {
    missing.push(`## 输入格式\n\n${question.value?.inputDesc || '暂无输入说明'}`)
  }
  if (!/^#{1,6}\s*输出格式/m.test(description)) {
    missing.push(`## 输出格式\n\n${question.value?.outputDesc || '暂无输出说明'}`)
  }

  return [description, ...missing].filter(Boolean).join('\n\n')
})
const currentLanguage = computed(
  () => LANGUAGES.find((item) => item.value === language.value) || LANGUAGES[0]
)
const canSubmit = computed(() => Boolean(question.value) && !judging.value && code.value.trim().length > 0)
const caseResults = computed(() => {
  const raw = judgeResult.value?.caseResults ?? judgeResult.value?.caseResultJson
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
})
const resultStatus = computed(() => String(judgeResult.value?.status || '').toUpperCase())
const resultMeta = computed(() => STATUS_META[resultStatus.value] || { label: resultStatus.value || '未知', type: 'info' })
const agentTabText = computed(() => {
  if (agentError.value) return '调试失败'
  if (agentRunning.value) return '思考中'
  if (agentAnswer.value) return '已完成'
  return '待调试'
})

function languageLabel(value) {
  return LANGUAGES.find((item) => item.value === value)?.label || value
}

function defaultTemplate(lang) {
  if (lang === 'JAVA') {
    return [
      'import java.util.*;',
      '',
      'public class Main {',
      '    public static void main(String[] args) {',
      '        Scanner sc = new Scanner(System.in);',
      '        // 在这里编写你的代码',
      '    }',
      '}',
      ''
    ].join('\n')
  }
  return [
    '#include <bits/stdc++.h>',
    'using namespace std;',
    '',
    'int main() {',
    '    ios::sync_with_stdio(false);',
    '    cin.tie(nullptr);',
    '    // 在这里编写你的代码',
    '    return 0;',
    '}',
    ''
  ].join('\n')
}

function templateMap() {
  const raw = question.value?.templateJson
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

/**
 * 读取题目模板，优先使用 templateJson 中对应语言的代码。
 * @param {'CPP'|'JAVA'} lang 语言代码
 * @returns {string} 对应语言的初始模板
 */
function templateFor(lang) {
  const map = templateMap()
  const keys = lang === 'CPP' ? ['cpp', 'CPP', 'c++', 'C++'] : ['java', 'JAVA', 'Java']
  for (const key of keys) {
    if (typeof map[key] === 'string' && map[key].trim()) return map[key]
  }
  return defaultTemplate(lang)
}

function codeStorageKey(id, lang) {
  return `${CODE_PREFIX}${id}_${lang}`
}

/**
 * 读取某道题、某种语言保存在本机的代码草稿。
 * @param {number|string} id 题目 ID
 * @param {'CPP'|'JAVA'} lang 语言代码
 * @returns {string|null} 已保存的代码，未保存时返回 null
 */
function readStoredCode(id, lang) {
  try {
    return localStorage.getItem(codeStorageKey(id, lang))
  } catch {
    return null
  }
}

function clearStoredCode(id) {
  if (id == null) return
  try {
    LANGUAGES.forEach(({ value }) => localStorage.removeItem(codeStorageKey(id, value)))
  } catch {
    // localStorage 不可用时没有草稿需要清理。
  }
}

function saveCurrentCode() {
  if (!question.value) return
  try {
    localStorage.setItem(codeStorageKey(question.value.id, language.value), code.value)
    localStorage.setItem(LANGUAGE_KEY, language.value)
  } catch {
    // 浏览器禁用 localStorage 时不影响答题，只是无法恢复草稿。
  }
}

function scheduleSave() {
  window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(saveCurrentCode, 250)
}

function handleCodeChange(value) {
  code.value = value
  if (!switchingLanguage) scheduleSave()
}

function readInitialLanguage() {
  try {
    const stored = localStorage.getItem(LANGUAGE_KEY)
    return stored === 'JAVA' ? 'JAVA' : 'CPP'
  } catch {
    return 'CPP'
  }
}

/**
 * 加载题目、恢复上次语言和本机代码草稿。
 * @returns {Promise<void>} 题目加载完成后结束
 */
async function loadQuestion() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getQuestion(questionId.value)
    if (!data) throw new Error('题目不存在')
    question.value = data
    customCases.value = []

    language.value = readInitialLanguage()
    baselineCode.value = templateFor(language.value)
    if (data.solved) {
      // 已通过的题重新进入时回到空白模板，不沿用旧代码。
      clearStoredCode(data.id)
      code.value = baselineCode.value
    } else {
      code.value = readStoredCode(data.id, language.value) ?? baselineCode.value
    }
    loadAgentHistory()
  } catch (error) {
    question.value = null
    loadError.value = error?.message || '题目加载失败'
  } finally {
    loading.value = false
  }
}

/**
 * 切换编辑器语言；若当前代码已修改，先确认再覆盖，并按语言保存草稿。
 * @param {'CPP'|'JAVA'} nextLanguage 目标语言
 * @returns {Promise<void>} 用户取消或切换完成后结束
 */
async function requestLanguage(nextLanguage) {
  if (nextLanguage === language.value || switchingLanguage) return

  saveCurrentCode()
  const currentDirty = code.value !== baselineCode.value

  if (currentDirty) {
    try {
      await ElMessageBox.confirm(
        `当前 ${languageLabel(language.value)} 代码已修改。切换后会载入 ${languageLabel(nextLanguage)} 模板，当前代码已按语言保存，不会丢失。`,
        '切换语言',
        {
          confirmButtonText: '切换语言',
          cancelButtonText: '留在当前语言',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }

  switchingLanguage = true
  try {
    language.value = nextLanguage
    baselineCode.value = templateFor(nextLanguage)
    code.value = readStoredCode(question.value.id, nextLanguage) ?? baselineCode.value
    try {
      localStorage.setItem(LANGUAGE_KEY, nextLanguage)
    } catch {
      // localStorage 不可用时仅影响下次进入的默认语言。
    }
  } finally {
    switchingLanguage = false
  }
}

function caseExpected(item) {
  return item?.expected ?? item?.expectedOutput ?? ''
}

function caseActual(item) {
  return item?.actual ?? item?.actualOutput ?? ''
}

function caseInput(item) {
  return item?.input ?? item?.inputData ?? ''
}

/**
 * 构造判题接口未接入时的演示结果，首条失败用例使用样例的期望输出。
 * @returns {object} 与真实判题结果结构一致的演示对象
 */
function buildDemoResult() {
  const first = samples.value[0] || { input: '5\n1 2 3 4 5', output: '15' }
  const expected = first.output || '15'
  return {
    submitId: 'DEMO',
    status: 'WA',
    passedCount: 3,
    totalCount: 4,
    runTimeMs: 118,
    errorMessage: '第 1 个用例输出不符',
    caseResults: [
      {
        index: 1,
        sample: true,
        passed: false,
        input: first.input,
        expected,
        actual: '14',
        stderr: ''
      },
      { index: 2, sample: true, passed: true },
      { index: 3, sample: false, passed: true },
      { index: 4, sample: false, passed: true }
    ],
    agentSuggest: false
  }
}

function runDemoJudge() {
  usingDemoJudge.value = true
  sampleOnly.value = false
  activePane.value = 'result'
  judging.value = true
  judgeError.value = ''
  judgeResult.value = null
  window.clearTimeout(demoTimer)
  demoTimer = window.setTimeout(() => {
    if (disposed) return
    judgeResult.value = buildDemoResult()
    judging.value = false
    scrollToResult()
    ElMessage.info('判题接口尚未接入，当前展示演示结果')
    startAgent('HINT')
  }, 900)
}

function sleep(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

/**
 * 轮询判题结果，直到终态、接口不存在或超时。
 * @param {number|string} submitId 提交记录 ID
 * @returns {Promise<void>} 轮询结束时完成
 */
async function pollJudge(submitId) {
  const startedAt = Date.now()
  while (!disposed && Date.now() - startedAt < 90000) {
    await sleep(900)
    if (disposed) return
    try {
      const data = await getJudgeResult(submitId)
      if (disposed) return
      judgeResult.value = data
      const status = String(data?.status || '').toUpperCase()
      if (TERMINAL_STATUSES.has(status)) {
        judging.value = false
        scrollToResult()
        return
      }
    } catch (error) {
      if (error?.status === 40100) return
      if (error?.status === 404) {
        judging.value = false
        judgeError.value = '判题结果接口尚未接入（S7）'
        return
      }
      // 单次网络抖动不终止轮询，继续等待下一次结果。
    }
  }

  if (!disposed) {
    judging.value = false
    judgeError.value = '判题超时，请稍后在提交记录中查看'
  }
}

function clearTimers() {
  window.clearTimeout(pollTimer)
  window.clearTimeout(demoTimer)
  window.clearTimeout(saveTimer)
}

/**
 * 提交当前代码：显示判题骨架屏，调用提交接口并轮询结果。
 * S7 判题接口尚未接入时，使用明确标注的本地演示结果。
 * @returns {Promise<void>} 提交和结果加载结束后完成
 */
function goBack() {
  if (contestId.value) {
    router.push({ name: 'contest-detail', params: { id: contestId.value } })
    return
  }
  router.push({ name: 'question-detail', params: { id: question.value.id } })
}

async function showPassNotice() {
  if (passNoticeShown || disposed) return
  passNoticeShown = true
  clearStoredCode(question.value?.id)
  const title = question.value?.title || '这道题'
  try {
    await ElMessageBox.alert(
      `恭喜你通过了《${title}》！这次提交已经记录，继续保持。`,
      '恭喜通过',
      {
        confirmButtonText: contestId.value ? '返回竞赛' : '返回题库',
        type: 'success',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false
      }
    )
  } catch {
    // 弹窗被关闭时也回到列表，避免停留在没有后续操作的答题页。
  }
  if (disposed) return
  if (contestId.value) {
    router.push({ name: 'contest-detail', params: { id: contestId.value } })
  } else {
    router.push({ name: 'question-list' })
  }
}

async function submit() {
  if (!question.value || judging.value) return
  if (!code.value.trim()) {
    ElMessage.warning('请先编写代码再提交')
    return
  }

  saveCurrentCode()
  clearTimers()
  passNoticeShown = false
  judging.value = true
  judgeResult.value = null
  judgeError.value = ''
  usingDemoJudge.value = false
  sampleOnly.value = false
  activePane.value = 'result'

  try {
    const payload = {
      questionId: question.value.id,
      language: language.value,
      code: code.value
    }
    if (contestId.value) payload.contestId = contestId.value
    const data = await submitCode(payload)
    const submitId = data?.submitId ?? data?.id
    if (!submitId) throw new Error('判题服务未返回 submitId')
    await pollJudge(submitId)
    const status = String(judgeResult.value?.status || '').toUpperCase()
    if (status === 'AC' && !sampleOnly.value && !usingDemoJudge.value) {
      await showPassNotice()
      return
    }
    if (status && status !== 'AC' && !contestId.value) {
      startAgent('HINT')
    }
  } catch (error) {
    if (error?.status === 404 || error?.status === 405 || error?.status === 501) {
      runDemoJudge()
      return
    }
    judging.value = false
    judgeError.value = error?.message || '提交失败，请稍后重试'
    ElMessage.error(judgeError.value)
  }
}

function resetJudge() {
  judgeResult.value = null
  judgeError.value = ''
  usingDemoJudge.value = false
  sampleOnly.value = false
}

function scrollToResult() {
  nextTick(() => {
    const el = lowerPanel.value
    if (!el) return
    const rect = el.getBoundingClientRect()
    if (rect.top >= 0 && rect.bottom <= window.innerHeight) return
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    el.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'nearest' })
  })
}

/**
 * 只运行公开样例：结果不会写入提交记录，失败也不会自动触发 AI 调试。
 * @returns {Promise<void>} 样例运行结束后完成
 */
async function runSample() {
  if (!question.value || judging.value) return
  if (!code.value.trim()) {
    ElMessage.warning('请先编写代码再运行样例')
    return
  }

  const customPayload = collectCustomCases()
  if (customPayload == null) return

  saveCurrentCode()
  clearTimers()
  judging.value = true
  judgeResult.value = null
  judgeError.value = ''
  usingDemoJudge.value = false
  sampleOnly.value = true
  activePane.value = 'result'

  try {
    const data = await runSampleCode({
      questionId: question.value.id,
      language: language.value,
      code: code.value,
      customCases: customPayload
    })
    judgeResult.value = data
  } catch (error) {
    judgeError.value = error?.message || '样例运行失败，请稍后重试'
    ElMessage.error(judgeError.value)
  } finally {
    judging.value = false
    scrollToResult()
  }
}

/**
 * 清空并取消当前 Agent 时间线。
 *
 * @returns {void}
 */
function resetAgent() {
  if (agentController) {
    agentController.abort()
    agentController = null
  }
  agentEvents.value = []
  agentAnswer.value = ''
  agentError.value = ''
  agentCanReveal.value = false
}

/**
 * 把一条 SSE 事件合并进时间线。
 *
 * @param {{type: string, data: object}} event SSE 事件
 * @param {number} runId 当前请求序号，用于丢弃旧请求
 * @returns {void}
 */
function handleAgentEvent(event, runId) {
  if (runId !== agentRunId || disposed) return
  const data = event.data || {}

  if (event.type === 'meta') {
    agentSessionId.value = data.sessionId ?? null
    return
  }

  if (event.type === 'thinking') {
    agentEvents.value.push({
      id: `thinking-${agentEvents.value.length}`,
      type: 'thinking',
      message: data.delta || '正在分析…'
    })
    return
  }

  if (event.type === 'tool_call') {
    agentEvents.value.push({
      id: data.id || `tool-call-${agentEvents.value.length}`,
      type: 'tool_call',
      name: data.name || 'tool',
      args: data.args
    })
    return
  }

  if (event.type === 'tool_result') {
    const item = agentEvents.value.find((entry) => entry.id === data.id)
    if (item) {
      item.type = 'tool_result'
      item.summary = data.summary || '工具执行完成'
      item.latencyMs = data.latencyMs
    } else {
      agentEvents.value.push({
        id: data.id || `tool-result-${agentEvents.value.length}`,
        type: 'tool_result',
        name: data.name || 'tool',
        summary: data.summary || '工具执行完成',
        latencyMs: data.latencyMs
      })
    }
    return
  }

  if (event.type === 'delta') {
    agentAnswer.value += data.delta || ''
    return
  }

  if (event.type === 'done') {
    agentCanReveal.value = Boolean(data.canReveal)
    return
  }

  if (event.type === 'error') {
    agentError.value = data.message || 'Agent 调试失败'
  }
}

/**
 * 启动一次流式调试；HINT 失败后会自动调用，也可由按钮触发。
 *
 * @param {'HINT'|'SOLUTION'} mode 提示层级
 * @returns {Promise<void>} 流结束或取消后完成
 */
async function startAgent(mode = 'HINT') {
  if (!question.value || agentRunning.value) return

  resetAgent()
  activePane.value = 'agent'
  const runId = ++agentRunId
  const controller = new AbortController()
  agentController = controller
  agentRunning.value = true

  const rawSubmitId = judgeResult.value?.submitId
  const payload = {
    questionId: question.value.id,
    message: mode === 'SOLUTION' ? '请给我这道题的参考代码' : '请帮我定位这段代码的错误，并给出修改方向',
    mode,
    language: language.value,
    code: code.value,
    submitId: typeof rawSubmitId === 'number' ? rawSubmitId : null,
    sessionId: agentSessionId.value
  }

  try {
    await streamAgent(payload, {
      signal: controller.signal,
      onEvent: (event) => handleAgentEvent(event, runId)
    })
  } catch (error) {
    if (error?.name === 'AbortError' || disposed) return
    if (!agentError.value) {
      agentError.value = error?.message || '调试连接中断，请稍后重试'
    }
  } finally {
    if (runId === agentRunId) {
      agentRunning.value = false
      agentController = null
    }
  }
}

/**
 * 刷新页面后从后端历史中恢复最近一次时间线。
 *
 * @returns {Promise<void>} 历史加载完成后结束
 */
async function loadAgentHistory() {
  if (!question.value) return
  try {
    const data = await getAgentHistory(question.value.id)
    const session = data?.sessions?.[0]
    const messages = session?.messages || []
    if (!session || !messages.length) return

    const events = []
    messages
      .filter((message) => message.role === 'tool')
      .forEach((message) => {
        events.push({
          id: `history-result-${message.id}`,
          type: 'tool_result',
          name: message.toolName || 'tool',
          args: message.toolInput,
          summary: message.content || '工具执行完成',
          latencyMs: null
        })
      })

    const assistant = [...messages].reverse().find((message) => message.role === 'assistant')
    if (!assistant?.content) return

    agentEvents.value = events
    agentAnswer.value = assistant.content
    agentCanReveal.value = assistant.hintLevel === 1
    agentSessionId.value = session.id
    activePane.value = 'agent'
  } catch {
    // 历史接口失败不阻断答题页。
  }
}

function handleBeforeUnload() {
  saveCurrentCode()
}

onMounted(() => {
  loadQuestion()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  disposed = true
  if (agentController) {
    agentController.abort()
    agentController = null
  }
  // 只有刷新页面（beforeunload）保留草稿；离开页面即恢复模板。
  clearStoredCode(question.value?.id)
  clearTimers()
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(() => {
  clearStoredCode(question.value?.id)
  return true
})
</script>

<template>
  <div class="solve-page">
    <div v-if="loading" class="solve-skeleton" aria-busy="true" aria-label="正在加载答题页面">
      <div class="skeleton-top">
        <span class="skeleton-block skeleton-back"></span>
        <span class="skeleton-block skeleton-title"></span>
      </div>
      <div class="skeleton-grid">
        <div class="skeleton-pane skeleton-statement">
          <span class="skeleton-block skeleton-line is-short"></span>
          <span class="skeleton-block skeleton-line"></span>
          <span class="skeleton-block skeleton-line"></span>
          <span class="skeleton-block skeleton-code"></span>
        </div>
        <div class="skeleton-pane skeleton-workspace">
          <span class="skeleton-block skeleton-toolbar"></span>
          <span class="skeleton-block skeleton-editor"></span>
        </div>
      </div>
    </div>

    <div v-else-if="!question" class="panel empty-panel">
      <el-empty :description="loadError || '题目不存在，或已被删除'" :image-size="100">
        <el-button type="primary" @click="router.push('/question')">回到题库</el-button>
        <el-button @click="loadQuestion">重新加载</el-button>
      </el-empty>
    </div>

    <template v-else>
      <header class="solve-top">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          {{ contestId ? '返回竞赛' : '返回题目' }}
        </button>

        <div class="head-main">
          <div class="page-eyebrow">
            <span>{{ contestId ? 'CONTEST' : 'PRACTICE' }}</span>
            <span class="eyebrow-sep">/</span>
            <span>PROBLEM {{ question.id }}</span>
          </div>
          <div class="head-title-line">
            <h1>{{ question.title }}</h1>
            <el-tag :type="difficulty.type" effect="light" round size="small">
              {{ difficulty.label }}
            </el-tag>
          </div>
        </div>

        <div class="top-meta">
          <span class="meta-chip">{{ knowledgeName }}</span>
          <el-tag v-if="contestId" type="warning" effect="light" round size="small">
            竞赛提交
          </el-tag>
          <span v-if="question.solved" class="meta-solved">
            <el-icon><CircleCheck /></el-icon>
            已通过
          </span>
        </div>
      </header>

      <div class="solve-grid">
        <aside class="problem-pane">
          <div class="pane-bar">
            <span>题目说明</span>
            <span class="pane-id">{{ question.id }}</span>
          </div>
          <div class="problem-scroll">
            <section class="problem-section problem-statement">
              <MarkdownBody :content="statementMarkdown" />
            </section>

            <section class="custom-samples">
              <div class="custom-samples-head">
                <div>
                  <strong>自定义样例</strong>
                </div>
                <button type="button" class="custom-add" @click="addCustomCase">+ 添加</button>
              </div>

              <p v-if="!customCases.length" class="custom-empty">
                可以给这道题补一条自己的输入和期望输出，运行样例时会一起执行。
              </p>

              <div
                v-for="(customCase, index) in customCases"
                :key="index"
                class="custom-case"
              >
                <div class="custom-case-head">
                  <span>样例 {{ index + 1 }}</span>
                  <button
                    type="button"
                    class="custom-remove"
                    aria-label="删除样例"
                    @click="removeCustomCase(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
                <div class="custom-case-fields">
                  <el-input
                    v-model="customCase.input"
                    type="textarea"
                    :rows="2"
                    resize="none"
                    placeholder="输入"
                  />
                  <el-input
                    v-model="customCase.expectedOutput"
                    type="textarea"
                    :rows="2"
                    resize="none"
                    placeholder="期望输出"
                  />
                </div>
              </div>
            </section>
          </div>
        </aside>

        <section class="workspace">
          <div class="panel editor-panel">
            <div class="editor-toolbar">
              <div class="editor-file">
                <span class="editor-dot"></span>
                <span class="file-name">{{ language === 'JAVA' ? 'Main.java' : 'main.cpp' }}</span>
                <span class="file-save">自动保存</span>
              </div>

              <el-radio-group
                :model-value="language"
                size="small"
                :disabled="judging"
                @change="requestLanguage"
              >
                <el-radio-button v-for="item in LANGUAGES" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </div>

            <CodeEditor
              :model-value="code"
              :language="currentLanguage.monaco"
              height="clamp(340px, 44vh, 500px)"
              @update:model-value="handleCodeChange"
            />

            <div class="action-bar">
              <span class="autosave-tip">
                <el-icon><CircleCheck /></el-icon>
                自动保存
              </span>

              <div class="actions">
                <el-button
                  :loading="judging && sampleOnly"
                  :disabled="judging || !code.trim()"
                  @click="runSample"
                >
                  <el-icon><VideoPlay /></el-icon>
                  运行样例
                </el-button>

                <el-button type="primary" :loading="judging" :disabled="!canSubmit" @click="submit">
                  {{ judging ? '判题中' : '提交' }}
                </el-button>

                <el-button
                  type="primary"
                  plain
                  :loading="agentRunning"
                  :disabled="!question || !!contestId"
                  :title="contestId ? '竞赛模式下不提供 AI 调试' : ''"
                  @click="startAgent('HINT')"
                >
                  <el-icon><MagicStick /></el-icon>
                  AI 调试
                </el-button>
              </div>
            </div>
          </div>

          <div ref="lowerPanel" class="panel workspace-lower">
            <div class="lower-tabs">
              <button
                class="lower-tab"
                :class="{ 'is-active': activePane === 'result' }"
                @click="activePane = 'result'"
              >
                判题结果
                <span v-if="judgeResult" class="tab-status" :class="`is-${resultMeta.type}`">
                  {{ resultMeta.label }}
                </span>
                <span v-else-if="judging" class="tab-status is-primary">运行中</span>
              </button>

              <button
                class="lower-tab"
                :class="{ 'is-active': activePane === 'agent' }"
                @click="activePane = 'agent'"
              >
                AI 调试
                <span
                  class="tab-status"
                  :class="{
                    'is-primary': agentRunning,
                    'is-success': !agentRunning && agentAnswer && !agentError,
                    'is-danger': agentError,
                    'is-muted': !agentRunning && !agentAnswer && !agentError
                  }"
                >
                  {{ agentTabText }}
                </span>
              </button>

              <span class="lower-tip">
                {{ activePane === 'result' ? (sampleOnly ? '仅运行样例，不写入提交记录' : '按用例逐项比对，失败时显示期望与实际输出') : '工具调用会实时显示在时间线上' }}
              </span>
            </div>

            <div v-show="activePane === 'result'" class="lower-body">
              <div v-if="judging" class="judging-state">
                <div class="judging-copy">
                  <el-icon class="is-loading"><Clock /></el-icon>
                  <div>
                    <strong>正在编译并运行用例</strong>
                    <p>通常需要几秒钟，请保持当前页面。</p>
                  </div>
                </div>
                <el-skeleton :rows="4" animated />
              </div>

              <div v-else-if="judgeError" class="judge-error">
                <el-alert :title="judgeError" type="error" :closable="false" show-icon />
                <el-button :icon="RefreshRight" @click="resetJudge">
                  {{ sampleOnly ? '重新运行' : '重新提交' }}
                </el-button>
              </div>

              <div v-else-if="judgeResult" class="judge-result">
                <div v-if="sampleOnly" class="demo-banner sample-banner">
                  <el-icon><Warning /></el-icon>
                  <span>仅运行样例（含自定义），不会写入提交记录，也不会自动触发 AI 调试。</span>
                </div>

                <div v-if="usingDemoJudge" class="demo-banner">
                  <el-icon><Warning /></el-icon>
                  <span>判题接口尚未接入，以下是界面演示结果，不会写入提交记录。</span>
                </div>

                <div class="result-summary">
                  <div class="summary-item">
                    <span>通过用例</span>
                    <strong>{{ judgeResult.passedCount ?? 0 }} / {{ judgeResult.totalCount ?? caseResults.length }}</strong>
                  </div>
                  <div class="summary-item">
                    <span>运行时间</span>
                    <strong>{{ judgeResult.runTimeMs != null ? `${judgeResult.runTimeMs} ms` : '—' }}</strong>
                  </div>
                  <div v-if="!sampleOnly" class="summary-item">
                    <span>提交编号</span>
                    <strong>{{ judgeResult.submitId ?? '—' }}</strong>
                  </div>
                </div>

                <div v-if="judgeResult.errorMessage" class="result-message">
                  {{ judgeResult.errorMessage }}
                </div>

                <div v-if="judgeResult.compileOutput" class="compile-output">
                  <div class="case-section-title">编译信息</div>
                  <pre>{{ judgeResult.compileOutput }}</pre>
                </div>

                <div v-if="caseResults.length" class="case-list">
                  <div
                    v-for="item in caseResults"
                    :key="item.index"
                    class="case-row"
                    :class="{ 'is-failed': !item.passed }"
                  >
                    <div class="case-row-head">
                      <div class="case-title">
                        <span class="case-index">用例 {{ item.index }}</span>
                        <el-tag v-if="item.custom" size="small" effect="plain">自定义</el-tag>
                        <el-tag v-else-if="item.sample" size="small" effect="plain">样例</el-tag>
                        <el-tag :type="item.passed ? 'success' : 'danger'" size="small" effect="light">
                          {{ item.passed ? '通过' : '未通过' }}
                        </el-tag>
                      </div>
                      <span v-if="item.runTimeMs != null" class="case-time">{{ item.runTimeMs }} ms</span>
                    </div>

                    <div v-if="!item.passed || item.custom" class="case-detail">
                      <div v-if="caseInput(item)" class="case-block">
                        <span>输入</span>
                        <pre>{{ caseInput(item) }}</pre>
                      </div>

                      <div class="compare-grid">
                        <div class="case-block">
                          <span>期望</span>
                          <pre>{{ caseExpected(item) || '(无)' }}</pre>
                        </div>
                        <div class="case-block">
                          <span>实际</span>
                          <pre>{{ caseActual(item) || '(无输出)' }}</pre>
                        </div>
                      </div>

                      <div v-if="item.stderr" class="case-block">
                        <span>错误输出</span>
                        <pre>{{ item.stderr }}</pre>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-else class="no-case-result">
                  {{ judgeResult.errorMessage || '暂无用例明细' }}
                </div>
              </div>

              <div v-else class="judge-empty">
                <span class="empty-icon">
                  <el-icon><VideoPlay /></el-icon>
                </span>
                <div>
                  <strong>还没有判题结果</strong>
                  <p>运行样例或提交代码后，这里会显示每个用例是否通过，以及失败用例的期望输出和实际输出。</p>
                </div>
              </div>
            </div>

            <AgentTimeline
              v-if="activePane === 'agent'"
              compact
              :events="agentEvents"
              :answer="agentAnswer"
              :running="agentRunning"
              :can-reveal="agentCanReveal"
              :error="agentError"
              @reveal="startAgent('SOLUTION')"
            />
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.solve-page {
  min-height: 300px;
}

.contest-solve {
  padding: 18px 24px 40px;
}

@media (max-width: 720px) {
  .contest-solve {
    padding: 14px 14px 28px;
  }
}

.empty-panel {
  padding: 34px 20px;
}

.solve-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  margin-top: 7px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
}

.back-link:hover {
  color: var(--ca-primary);
}

.head-main {
  min-width: 0;
}

.head-main h1 {
  color: var(--ca-ink);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.4px;
  line-height: 1.35;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 9px;
}

.meta-chip {
  padding: 2px 8px;
  border: 1px solid #e6ebf4;
  border-radius: 5px;
  background: #f8fafc;
  color: #64748b;
  font-size: 11px;
}

.meta-solved {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #2f7d5b;
  font-size: 11.5px;
  font-weight: 500;
}

.statement-link {
  flex-shrink: 0;
  margin-top: 3px;
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.solve-grid {
  display: grid;
  grid-template-columns: 350px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.problem-pane {
  position: sticky;
  top: 84px;
  overflow: hidden;
}

.problem-scroll {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  padding: 18px 20px 22px;
}

.problem-section + .problem-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #eef2f7;
}

.problem-section h3 {
  margin-bottom: 9px;
  color: #1e293b;
  font-size: 13px;
  font-weight: 650;
}

.custom-samples {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #eef2f7;
}

.custom-samples-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.custom-samples-head strong {
  display: block;
  color: #1e293b;
  font-size: 13px;
  font-weight: 650;
}

.custom-samples-head span {
  display: block;
  margin-top: 3px;
  color: #8a93a6;
  font-size: 11px;
}

.custom-add {
  flex: none;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: #1f3a5f;
  font-size: 12px;
  cursor: pointer;
}

.custom-empty {
  margin: 10px 0 0;
  color: #8a93a6;
  font-size: 11.5px;
  line-height: 1.7;
}

.custom-case {
  margin-top: 12px;
  padding: 10px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fbfcfe;
}

.custom-case-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #7c869c;
  font-size: 11px;
}

.custom-remove {
  display: inline-flex;
  align-items: center;
  padding: 2px;
  border: 0;
  background: transparent;
  color: #a0a9b8;
  cursor: pointer;
}

.custom-case-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.custom-case-fields :deep(.el-textarea__inner) {
  padding: 7px 9px;
  border-radius: 7px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
}

.sample-list {
  display: grid;
  gap: 9px;
}

.sample-card {
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fbfcfe;
}

.sample-label {
  padding: 6px 10px;
  border-bottom: 1px solid #eef2f7;
  color: #7c869c;
  font-size: 10px;
  font-weight: 600;
}

.sample-line {
  padding: 8px 10px;
}

.sample-line + .sample-line {
  border-top: 1px solid #eef2f7;
}

.sample-line > span {
  display: block;
  margin-bottom: 4px;
  color: #9aa4b6;
  font-size: 10px;
}

.sample-line pre {
  max-height: 120px;
  margin: 0;
  overflow: auto;
  color: #334155;
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
  font-size: 11px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.sample-empty {
  font-size: 12px;
}

.workspace {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.editor-panel {
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid #eef2f7;
  background: #fbfcfe;
}

.editor-label {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #334155;
  font-size: 12.5px;
  font-weight: 600;
}

.editor-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2f7d5b;
  box-shadow: 0 0 0 3px rgba(47, 125, 91, 0.10);
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.autosave-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 11px;
}

.autosave-tip .el-icon {
  color: #2f7d5b;
  font-size: 13px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.disabled-wrap {
  display: inline-flex;
}

.soon {
  margin-left: 4px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 9px;
}

.judge-panel {
  overflow: hidden;
}

.judge-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid #eef2f7;
}

.judging-state {
  padding: 20px 18px 18px;
}

.judging-copy {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}

.judging-copy > .el-icon {
  margin-top: 2px;
  color: var(--ca-primary);
  font-size: 18px;
}

.judging-copy strong {
  color: #334155;
  font-size: 13px;
}

.judging-copy p {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11.5px;
}

.judge-error {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
}

.judge-error .el-alert {
  flex: 1;
}

.judge-result {
  padding: 16px 18px 20px;
}

.result-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 30px;
  padding-bottom: 14px;
  border-bottom: 1px solid #eef2f7;
}

.summary-item span {
  display: block;
  margin-bottom: 3px;
  color: #94a3b8;
  font-size: 10.5px;
}

.summary-item strong {
  color: #334155;
  font-size: 15px;
  font-weight: 650;
}

.result-message {
  margin-top: 12px;
  padding: 9px 11px;
  border-radius: 7px;
  background: #f7efee;
  color: #b91c1c;
  font-size: 12px;
}

.compile-output {
  margin-top: 14px;
}

.case-section-title {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
}

.compile-output pre,
.case-block pre {
  margin: 0;
  overflow: auto;
  color: #334155;
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
  font-size: 11.5px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.compile-output pre {
  padding: 10px 12px;
  border: 1px solid #e8edf5;
  border-radius: 7px;
  background: #fbfcfe;
}

.case-list {
  display: grid;
  gap: 9px;
  margin-top: 15px;
}

.case-row {
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fff;
}

.case-row.is-failed {
  border-color: #eed6d4;
  background: #fbf9f9;
}

.case-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 12px;
}

.case-title {
  display: flex;
  align-items: center;
  gap: 7px;
}

.case-index {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.case-time {
  color: #94a3b8;
  font-size: 10.5px;
}

.case-detail {
  display: grid;
  gap: 8px;
  padding: 0 12px 11px;
}

.case-block > span {
  display: block;
  margin-bottom: 4px;
  color: #94a3b8;
  font-size: 10px;
}

.case-block pre {
  padding: 7px 9px;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #fff;
}

.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.no-case-result {
  margin-top: 14px;
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
}

.judge-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 34px 20px;
  text-align: center;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  margin-bottom: 10px;
  border-radius: 12px;
  background: #eef2f6;
  color: #3d5a78;
  font-size: 20px;
}

.judge-empty strong {
  color: #475569;
  font-size: 13px;
}

.judge-empty p {
  max-width: 360px;
  margin-top: 5px;
  color: #94a3b8;
  font-size: 11.5px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .solve-grid {
    grid-template-columns: 310px minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .solve-grid {
    grid-template-columns: 1fr;
  }

  .problem-pane {
    position: static;
  }

  .problem-scroll {
    max-height: none;
  }
}

@media (max-width: 640px) {
  .solve-head {
    flex-wrap: wrap;
  }

  .statement-link {
    display: none;
  }

  .action-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .actions {
    flex-wrap: wrap;
    width: 100%;
  }

  .compare-grid {
    grid-template-columns: 1fr;
  }
}

/* Public platform workspace */
.solve-skeleton {
  padding: 4px 0 18px;
}

.skeleton-top {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8edf5;
}

.skeleton-block {
  display: block;
  border-radius: 6px;
  background: #e8edf4;
}

.skeleton-back {
  width: 88px;
  height: 30px;
}

.skeleton-title {
  width: min(320px, 44vw);
  height: 30px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.82fr) minmax(0, 1.18fr);
  gap: 16px;
  margin-top: 18px;
}

.skeleton-pane {
  min-height: calc(100vh - 176px);
  padding: 18px;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: #fff;
}

.skeleton-statement {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.skeleton-line {
  width: 100%;
  height: 12px;
}

.skeleton-line.is-short {
  width: 42%;
  height: 18px;
}

.skeleton-code {
  width: 100%;
  height: 168px;
  margin-top: 8px;
}

.skeleton-workspace {
  padding: 14px;
}

.skeleton-toolbar {
  width: 100%;
  height: 32px;
}

.skeleton-editor {
  width: 100%;
  height: calc(100% - 48px);
  min-height: 330px;
  margin-top: 14px;
  background: repeating-linear-gradient(
    to bottom,
    #eef2f6 0,
    #eef2f6 10px,
    transparent 10px,
    transparent 26px
  );
}

.solve-page {
  width: min(1440px, 100%);
  margin: 0 auto;
  padding: 18px 24px 36px;
}

.solve-top {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8edf5;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  padding: 7px 10px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
}

.back-link:hover {
  border-color: #c8d2dc;
  background: #f7f9fb;
}

.solve-top .page-eyebrow {
  display: none;
  gap: 7px;
  color: #94a3b8;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.eyebrow-sep {
  color: #cbd5e1;
}

.head-title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
}

.head-title-line h1 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
  line-height: 1.3;
}

.top-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.solve-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.82fr) minmax(0, 1.18fr);
  gap: 18px;
  align-items: start;
  margin-top: 18px;
}

.problem-pane {
  position: sticky;
  top: 84px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 106px);
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: #fff;
}

.pane-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  height: 46px;
  padding: 0 20px;
  border-bottom: 1px solid #eef2f7;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 650;
  letter-spacing: 0.02em;
}

.pane-id {
  color: #a3aec2;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10.5px;
}

.problem-scroll {
  max-height: calc(100vh - 153px);
  padding: 22px 24px 30px;
  overflow-y: auto;
}

.workspace {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.editor-panel {
  overflow: hidden;
  border-radius: 12px;
}

.editor-toolbar {
  height: 50px;
  padding: 0 16px;
  background: #fbfcfe;
}

.editor-file {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.file-name {
  color: #334155;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
}

.file-save {
  padding-left: 9px;
  border-left: 1px solid #e2e8f0;
  color: #a3aec2;
  font-size: 10.5px;
}

.action-bar {
  padding: 11px 15px;
}

.workspace-lower {
  overflow: hidden;
  border-radius: 12px;
}

.lower-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid #eef2f7;
  background: #fbfcfe;
}

.lower-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 100%;
  padding: 0 10px;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 12.5px;
  font-weight: 650;
  cursor: pointer;
}

.lower-tab:hover {
  color: #334155;
}

.lower-tab.is-active {
  color: #1e293b;
}

.lower-tab.is-active::after {
  position: absolute;
  right: 9px;
  bottom: -1px;
  left: 9px;
  height: 2px;
  border-radius: 2px;
  background: var(--ca-primary);
  content: '';
}

.tab-status {
  padding: 1px 5px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 9.5px;
  font-weight: 600;
}

.tab-status.is-success {
  background: #edf5f1;
  color: #2f7d5b;
}

.tab-status.is-danger {
  background: #f7efee;
  color: #b42318;
}

.tab-status.is-warning {
  background: #f7f3ea;
  color: #a16207;
}

.tab-status.is-primary {
  background: #eef2f6;
  color: #1f3a5f;
}

.tab-status.is-muted {
  background: #f8fafc;
  color: #94a3b8;
}

.lower-tip {
  margin-left: auto;
  overflow: hidden;
  color: #94a3b8;
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lower-body {
  max-height: 390px;
  overflow-y: auto;
}

.lower-body .judge-result {
  padding: 16px 18px 20px;
}

.lower-body .judging-state {
  padding: 18px;
}

.lower-body .judge-empty {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 12px;
  padding: 28px 20px;
  text-align: left;
}

.lower-body .judge-empty .empty-icon {
  flex-shrink: 0;
  margin: 0;
}

.lower-body .judge-empty strong {
  display: block;
  margin-top: 2px;
}

.lower-body .judge-empty p {
  max-width: 520px;
  margin: 5px 0 0;
}

@media (max-width: 1080px) {
  .solve-top {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .top-meta {
    grid-column: 2;
    justify-content: flex-start;
  }

  .solve-grid {
    grid-template-columns: minmax(300px, 0.78fr) minmax(0, 1.22fr);
  }
}

@media (max-width: 860px) {
  .solve-page {
    padding: 14px 16px 28px;
  }

  .solve-top {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 12px;
  }

  .solve-grid {
    grid-template-columns: 1fr;
  }

  .skeleton-grid {
    grid-template-columns: 1fr;
  }

  .skeleton-pane {
    min-height: auto;
  }

  .skeleton-statement {
    min-height: 250px;
  }

  .skeleton-workspace {
    min-height: 430px;
  }

  .skeleton-editor {
    min-height: 330px;
  }

  .problem-pane {
    position: static;
    max-height: none;
  }

  .problem-scroll {
    max-height: 430px;
  }
}

@media (max-width: 640px) {
  .solve-page {
    padding: 12px 12px 24px;
  }

  .solve-top {
    grid-template-columns: 1fr;
    gap: 9px;
  }

  .back-link {
    justify-self: start;
    padding: 6px 8px;
    font-size: 11px;
  }

  .head-main {
    grid-column: 1;
  }

  .head-title-line h1 {
    font-size: 20px;
  }

  .top-meta {
    grid-column: 1;
  }

  .editor-toolbar {
    height: auto;
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px 12px;
  }

  .lower-tip {
    display: none;
  }

  .lower-body {
    max-height: 460px;
  }

  .custom-case-fields {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-direction: column;
  }

  .actions {
    width: 100%;
  }
}
</style>
