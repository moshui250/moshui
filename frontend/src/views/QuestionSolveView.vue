<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  CircleCheck,
  Clock,
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

const loading = ref(true)
const loadError = ref('')
const question = ref(null)

const language = ref('CPP')
const code = ref('')
const baselineCode = ref('')
const judging = ref(false)
const judgeResult = ref(null)
const judgeError = ref('')
const usingDemoJudge = ref(false)
const sampleOnly = ref(false)

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
    const data = await getQuestion(route.params.id)
    if (!data) throw new Error('题目不存在')
    question.value = data

    language.value = readInitialLanguage()
    baselineCode.value = templateFor(language.value)
    code.value = readStoredCode(data.id, language.value) ?? baselineCode.value
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
  judging.value = true
  judgeError.value = ''
  judgeResult.value = null
  window.clearTimeout(demoTimer)
  demoTimer = window.setTimeout(() => {
    if (disposed) return
    judgeResult.value = buildDemoResult()
    judging.value = false
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
async function submit() {
  if (!question.value || judging.value) return
  if (!code.value.trim()) {
    ElMessage.warning('请先编写代码再提交')
    return
  }

  saveCurrentCode()
  clearTimers()
  judging.value = true
  judgeResult.value = null
  judgeError.value = ''
  usingDemoJudge.value = false
  sampleOnly.value = false

  try {
    const data = await submitCode({
      questionId: question.value.id,
      language: language.value,
      code: code.value
    })
    const submitId = data?.submitId ?? data?.id
    if (!submitId) throw new Error('判题服务未返回 submitId')
    await pollJudge(submitId)
    const status = String(judgeResult.value?.status || '').toUpperCase()
    if (status && status !== 'AC') {
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

  saveCurrentCode()
  clearTimers()
  judging.value = true
  judgeResult.value = null
  judgeError.value = ''
  usingDemoJudge.value = false
  sampleOnly.value = true

  try {
    const data = await runSampleCode({
      questionId: question.value.id,
      language: language.value,
      code: code.value
    })
    judgeResult.value = data
  } catch (error) {
    judgeError.value = error?.message || '样例运行失败，请稍后重试'
    ElMessage.error(judgeError.value)
  } finally {
    judging.value = false
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
    agentEvents.value.push({
      id: `error-${agentEvents.value.length}`,
      type: 'error',
      message: agentError.value
    })
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
      agentEvents.value.push({
        id: `error-${agentEvents.value.length}`,
        type: 'error',
        message: agentError.value
      })
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
  saveCurrentCode()
  clearTimers()
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(() => {
  saveCurrentCode()
  return true
})
</script>

<template>
  <div v-loading="loading" class="solve-page">
    <div v-if="!loading && !question" class="panel empty-panel">
      <el-empty :description="loadError || '题目不存在，或已被删除'" :image-size="100">
        <el-button type="primary" @click="router.push('/question')">回到题库</el-button>
        <el-button @click="loadQuestion">重新加载</el-button>
      </el-empty>
    </div>

    <template v-else-if="question">
      <header class="solve-head">
        <button class="back-link" @click="router.push({ name: 'question-detail', params: { id: question.id } })">
          <el-icon><ArrowLeft /></el-icon>
          返回题面
        </button>

        <div class="head-main">
          <div class="page-eyebrow">Solve · Problem {{ question.id }}</div>
          <h1>{{ question.title }}</h1>
          <div class="meta-row">
            <el-tag :type="difficulty.type" effect="light" round size="small">
              {{ difficulty.label }}
            </el-tag>
            <span class="meta-chip">{{ knowledgeName }}</span>
            <span v-if="question.solved" class="meta-solved">
              <el-icon><CircleCheck /></el-icon>
              已通过
            </span>
          </div>
        </div>

        <el-button class="statement-link" text @click="router.push({ name: 'question-detail', params: { id: question.id } })">
          查看完整题面
        </el-button>
      </header>

      <div class="solve-grid">
        <aside class="problem-pane panel">
          <div class="problem-scroll">
            <section class="problem-section problem-statement">
              <MarkdownBody :content="statementMarkdown" />
            </section>
          </div>
        </aside>

        <section class="workspace">
          <div class="panel editor-panel">
            <div class="editor-toolbar">
              <div class="editor-label">
                <span class="editor-dot"></span>
                代码编辑器
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
              height="470px"
              @update:model-value="handleCodeChange"
            />

            <div class="action-bar">
              <span class="autosave-tip">
                <el-icon><CircleCheck /></el-icon>
                代码自动保存在本机
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
                  :disabled="!question"
                  @click="startAgent('HINT')"
                >
                  <el-icon><MagicStick /></el-icon>
                  AI 调试
                </el-button>
              </div>
            </div>
          </div>

          <div class="panel judge-panel">
            <div class="judge-head">
              <div>
                <div class="panel-title">{{ sampleOnly ? '样例运行' : '判题结果' }}</div>
                <div class="panel-subtitle">
                  {{ sampleOnly ? '仅运行公开样例，不写入提交记录' : '按用例逐项比对，失败时显示期望与实际输出' }}
                </div>
              </div>
              <el-tag v-if="judgeResult" :type="resultMeta.type" effect="light" round size="small">
                {{ resultMeta.label }}
              </el-tag>
            </div>

            <div v-if="judging" class="judging-state">
              <div class="judging-copy">
                <el-icon class="is-loading"><Clock /></el-icon>
                <div>
                  <strong>正在编译并运行用例</strong>
                  <p>通常需要几秒钟，请保持当前页面。</p>
                </div>
              </div>
              <el-skeleton :rows="5" animated />
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
                <span>仅运行公开样例，不会写入提交记录，也不会自动触发 AI 调试。</span>
              </div>

              <div v-if="usingDemoJudge" class="demo-banner">
                <el-icon><Warning /></el-icon>
                <span>判题接口尚未接入（S7），以下为界面演示结果，不会写入提交记录。</span>
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
                      <el-tag v-if="item.sample" size="small" effect="plain">样例</el-tag>
                      <el-tag :type="item.passed ? 'success' : 'danger'" size="small" effect="light">
                        {{ item.passed ? '通过' : '未通过' }}
                      </el-tag>
                    </div>
                    <span v-if="item.runTimeMs != null" class="case-time">{{ item.runTimeMs }} ms</span>
                  </div>

                  <div v-if="!item.passed" class="case-detail">
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
              <div class="empty-icon">
                <el-icon><VideoPlay /></el-icon>
              </div>
              <strong>提交代码后查看判题结果</strong>
              <p>这里会显示每个用例是否通过；失败用例会给出第几个、期望输出和实际输出。</p>
            </div>
          </div>

          <AgentTimeline
            v-if="question"
            :events="agentEvents"
            :answer="agentAnswer"
            :running="agentRunning"
            :can-reveal="agentCanReveal"
            :error="agentError"
            @reveal="startAgent('SOLUTION')"
          />
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.solve-page {
  min-height: 300px;
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
  color: #059669;
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
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
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
  color: #10b981;
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
  background: #fef2f2;
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
  border-color: #fecaca;
  background: #fffafa;
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
  background: #eef2ff;
  color: #6366f1;
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
</style>
