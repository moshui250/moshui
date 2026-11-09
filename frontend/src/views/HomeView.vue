<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  ArrowRight,
  Collection,
  DataAnalysis,
  MagicStick,
  Picture,
  Reading,
  Trophy
} from '@element-plus/icons-vue'
import { getHealth } from '@/api/health'
import { getHomeSummary } from '@/api/home'
import { listKnowledgePoints } from '@/api/question'
import { useUserStore } from '@/store/user'
import SiteHeader from '@/components/SiteHeader.vue'

echarts.use([BarChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const userStore = useUserStore()

const chartRef = ref(null)
const loading = ref(true)
const summaryError = ref('')
const knowledgePoints = ref([])
let chartInstance = null

const summary = reactive({
  totalSubmits: 0,
  acSubmits: 0,
  passRate: 0,
  publicQuestions: 0,
  knowledgePoints: 0,
  hasData: false,
  weekly: []
})

const health = reactive({
  status: 'CHECKING',
  db: 'UNKNOWN'
})

const counts = reactive({
  publicQuestions: 0,
  knowledgePoints: 0,
  totalSubmits: 0
})

const coachStates = [
  {
    text: '第 3 行：循环从 i = 1 开始，条件写到 i <= n，会访问 a[n]，导致数组越界。',
    tools: ['locate_error'],
    foot: '先定位错误，不直接给代码。'
  },
  {
    text: '错误集中在数组边界。先确认下标范围，再看第一个失败用例的输入和期望输出。',
    tools: ['retrieve_knowledge'],
    foot: '补一条边界知识，再改代码。'
  },
  {
    text: '先检查循环起点和终止条件。改成 i = 0 且 i < n，再重新运行样例。',
    tools: ['hint_level_1'],
    foot: '需要时再看参考代码。'
  }
]

const coachIndex = ref(0)
const coachPaused = ref(false)
const reducedMotion = ref(false)
const tilt = reactive({ x: 0, y: 0 })
const tiltVisual = ref(null)
const tiltActive = ref(false)
const coachCount = computed(() => `${String(coachIndex.value + 1).padStart(2, '0')} / 03`)
const tiltStyle = computed(() => ({
  '--tilt-x': `${tilt.x}deg`,
  '--tilt-y': `${tilt.y}deg`
}))

let coachTimer = null
let countRaf = 0
let tiltRaf = 0
let tiltRect = null
let tiltEnabled = false
let tiltPointer = { x: 0, y: 0 }


const navItems = [
  { label: '题库', path: '/question', icon: Reading },
  { label: '竞赛', path: '/contests', icon: Trophy },
  { label: '错题本', path: '/wrong-book', icon: Collection },
  { label: '数据看板', path: '/dashboard', icon: DataAnalysis },
  { label: '图片录题', path: '/question/import', icon: Picture }
]

const steps = [
  {
    index: '01',
    title: '选一道刚好不会的题',
    body: '按知识点挑一道难度合适的题，卡住就是练习的开始。'
  },
  {
    index: '02',
    title: '写下第一版代码',
    body: '用 C++ 或 Java 提交，先看第一个失败的用例。'
  },
  {
    index: '03',
    title: '让 AI 指出下一步',
    body: 'AI 先定位错误行和原因，再给分层提示。'
  }
]

const initial = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())

const healthOk = computed(() => health.status === 'UP' && health.db === 'UP')
const healthText = computed(() => {
  if (health.status === 'CHECKING') return '正在检查服务'
  if (health.status === 'OFFLINE') return '服务连接异常'
  if (health.db !== 'UP') return '数据库暂未连接'
  return '服务正常'
})

const passRateText = computed(() => {
  if (!summary.hasData) return '—'
  return `${(Number(summary.passRate || 0) * 100).toFixed(1)}%`
})

const weeklyTotal = computed(() =>
  (summary.weekly || []).reduce((sum, item) => sum + Number(item.submitCount || 0), 0)
)

function statDisplay(key) {
  return loading.value ? '—' : counts[key]
}

function animateCounts() {
  const targets = {
    publicQuestions: Number(summary.publicQuestions || 0),
    knowledgePoints: Number(summary.knowledgePoints || 0),
    totalSubmits: Number(summary.totalSubmits || 0)
  }
  cancelAnimationFrame(countRaf)
  if (reducedMotion.value) {
    Object.assign(counts, targets)
    return
  }

  const from = { ...counts }
  const startedAt = performance.now()
  const duration = 700
  const tick = (now) => {
    const progress = Math.min(1, (now - startedAt) / duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    counts.publicQuestions = Math.round(from.publicQuestions + (targets.publicQuestions - from.publicQuestions) * eased)
    counts.knowledgePoints = Math.round(from.knowledgePoints + (targets.knowledgePoints - from.knowledgePoints) * eased)
    counts.totalSubmits = Math.round(from.totalSubmits + (targets.totalSubmits - from.totalSubmits) * eased)
    if (progress < 1) countRaf = requestAnimationFrame(tick)
  }
  countRaf = requestAnimationFrame(tick)
}

function stopCoachAuto() {
  if (coachTimer) {
    window.clearInterval(coachTimer)
    coachTimer = null
  }
}

function startCoachAuto() {
  if (reducedMotion.value || coachPaused.value || coachTimer) return
  coachTimer = window.setInterval(() => {
    coachIndex.value = (coachIndex.value + 1) % coachStates.length
  }, 4200)
}

function pauseCoach() {
  coachPaused.value = true
  stopCoachAuto()
}

function resumeCoach() {
  coachPaused.value = false
  startCoachAuto()
}

function selectCoach(index) {
  coachIndex.value = index
  stopCoachAuto()
  if (!coachPaused.value) startCoachAuto()
}

function handleTiltEnter() {
  if (!tiltEnabled) return
  tiltRect = tiltVisual.value?.getBoundingClientRect() || null
  tiltActive.value = true
}

function handleTiltMove(event) {
  if (!tiltEnabled || !tiltRect || tiltRaf) return
  tiltPointer = { x: event.clientX, y: event.clientY }
  tiltRaf = requestAnimationFrame(() => {
    tiltRaf = 0
    if (!tiltRect) return
    const px = (tiltPointer.x - tiltRect.left) / tiltRect.width
    const py = (tiltPointer.y - tiltRect.top) / tiltRect.height
    tilt.y = (px - 0.5) * 12
    tilt.x = -(py - 0.5) * 12
  })
}

function handleTiltLeave() {
  tiltActive.value = false
  tiltRect = null
  if (tiltRaf) {
    cancelAnimationFrame(tiltRaf)
    tiltRaf = 0
  }
  tilt.x = 0
  tilt.y = 0
}


async function loadHealth() {
  try {
    const data = await getHealth()
    health.status = data?.status || 'UP'
    health.db = data?.db || 'DOWN'
  } catch {
    health.status = 'OFFLINE'
    health.db = 'DOWN'
  }
}

async function loadSummary() {
  loading.value = true
  summaryError.value = ''
  try {
    const data = await getHomeSummary()
    Object.assign(summary, {
      totalSubmits: Number(data?.totalSubmits || 0),
      acSubmits: Number(data?.acSubmits || 0),
      passRate: Number(data?.passRate || 0),
      publicQuestions: Number(data?.publicQuestions || 0),
      knowledgePoints: Number(data?.knowledgePoints || 0),
      hasData: Boolean(data?.hasData),
      weekly: Array.isArray(data?.weekly) ? data.weekly : []
    })
  } catch (error) {
    summaryError.value = error.message || '首页统计加载失败'
  } finally {
    loading.value = false
    animateCounts()
    await nextTick()
    renderChart()
  }
}

async function loadKnowledgePoints() {
  try {
    const data = await listKnowledgePoints()
    knowledgePoints.value = Array.isArray(data) ? data.slice(0, 5) : []
  } catch {
    knowledgePoints.value = []
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  chartInstance = echarts.init(chartRef.value)

  const axisLine = '#e2e8f0'
  const axisText = '#94a3b8'
  const splitLine = '#f1f5f9'
  const submitColor = '#1f3a5f'
  const acColor = '#2f7d5b'
  const weekly = summary.weekly || []
  const labels = weekly.map((item) => item.label || item.date)
  const submitData = weekly.map((item) => Number(item.submitCount || 0))
  const acData = weekly.map((item) => Number(item.acCount || 0))

  chartInstance.setOption({
    animationDuration: 320,
    grid: { left: 38, right: 16, top: 18, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const day = params[0]?.axisValue || ''
        const submit = params.find((item) => item.seriesName === '提交')?.value ?? 0
        const ac = params.find((item) => item.seriesName === '通过')?.value ?? 0
        return `${day}<br/>提交 ${submit} 次<br/>通过 ${ac} 次`
      }
    },
    legend: { show: false },
    xAxis: {
      type: 'category',
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: axisLine } },
      axisLabel: { color: axisText, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      minInterval: 1,
      axisLabel: { color: axisText, fontSize: 11 },
      splitLine: { lineStyle: { color: splitLine } }
    },
    series: [
      {
        name: '提交',
        type: 'bar',
        data: submitData,
        barWidth: 14,
        itemStyle: { color: submitColor, borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '通过',
        type: 'bar',
        data: acData,
        barWidth: 14,
        itemStyle: { color: acColor, borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
  chartInstance.resize()
}

function handleResize() {
  chartInstance?.resize()
  tiltRect = null
}

function go(path) {
  if (path) router.push(path)
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: '/question' } })
}

function goWorkspace() {
  router.push('/question')
}

function goKnowledge() {
  router.push('/knowledge')
}

function goKnowledgePoint(item) {
  router.push({ name: 'question-list', query: { kp: String(item.id) } })
}

function goWrongBook() {
  router.push('/wrong-book')
}

onMounted(async () => {
  // 首页早于路由守卫放行，登录用户需要在这里补一次用户信息。
  if (userStore.token && !userStore.user) {
    userStore.fetchMe().catch(() => {})
  }
  reducedMotion.value = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  tiltEnabled = !reducedMotion.value && (window.matchMedia?.('(hover: hover) and (pointer: fine)').matches ?? false)
  startCoachAuto()
  await Promise.all([loadHealth(), loadSummary(), loadKnowledgePoints()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  stopCoachAuto()
  cancelAnimationFrame(countRaf)
  if (tiltRaf) cancelAnimationFrame(tiltRaf)
})
</script>

<template>
  <div class="home-page">
    <SiteHeader />

    <main class="page">
      <section class="hero">
        <div class="hero-copy">
          <h1 class="hero-enter hero-enter-1">把“不会做”<br />拆成<span class="mark">下一步</span>。</h1>
          <p class="lede hero-enter hero-enter-2">
            从第一道题开始，先写，再看判题结果，最后由 AI 指出错在哪一行。
          </p>
          <div class="hero-actions hero-enter hero-enter-3">
            <button class="btn-primary" type="button" @click="goWorkspace">
              从题库开始 <ArrowRight />
            </button>
            <button class="btn-ghost" type="button" @click="goWrongBook">
              看看我的错题
            </button>
          </div>
          <dl class="hero-stats hero-enter hero-enter-4">
            <div>
              <dt>{{ statDisplay('publicQuestions') }}</dt>
              <dd>道公共题</dd>
            </div>
            <div>
              <dt>{{ statDisplay('knowledgePoints') }}</dt>
              <dd>个知识点</dd>
            </div>
            <div>
              <dt>{{ statDisplay('totalSubmits') }}</dt>
              <dd>次全站提交</dd>
            </div>
          </dl>
        </div>

        <div
          ref="tiltVisual"
          class="hero-visual"
          :class="{ 'is-tilting': tiltActive }"
          :style="tiltStyle"
          @mouseenter="handleTiltEnter"
          @mousemove="handleTiltMove"
          @mouseleave="handleTiltLeave"
        >
          <div class="visual-stack">
            <div class="preview-card">
              <div class="preview-head">
                <span class="window-dots"><i></i><i></i><i></i></span>
                <span class="file-name">Main.java</span>
                <span class="preview-label">界面预览</span>
              </div>
              <div class="code-block">
                <div class="code-line"><span>1</span><code>int sum = 0;</code></div>
                <div class="code-line"><span>2</span><code>for (int i = 1; i &lt;= n; i++) {</code></div>
                <div class="code-line is-error"><span>3</span><code>&nbsp;&nbsp;sum += a[i];</code></div>
                <div class="code-line"><span>4</span><code>}</code></div>
                <div class="code-line"><span>5</span><code>System.out.println(sum);</code></div>
              </div>
              <div class="fail-line">
                <span class="fail-dot"></span>
                用例 1 未通过 · 期望 15，实际 14
              </div>
            </div>

            <div
              class="coach-card"
              @mouseenter="pauseCoach"
              @mouseleave="resumeCoach"
              @focusin="pauseCoach"
              @focusout="resumeCoach"
            >
              <div class="coach-head">
                <span class="coach-icon"><MagicStick /></span>
                <strong>AI 调试教练</strong>
                <span class="coach-count">{{ coachCount }}</span>
              </div>
              <Transition name="coach-swap" mode="out-in">
                <div :key="coachIndex" class="coach-body">
                  <p>{{ coachStates[coachIndex].text }}</p>
                  <div class="coach-tools">
                    <span v-for="tool in coachStates[coachIndex].tools" :key="tool">{{ tool }}</span>
                  </div>
                  <div class="coach-foot">{{ coachStates[coachIndex].foot }}</div>
                </div>
              </Transition>
              <div class="coach-dots" role="tablist" aria-label="AI 调试状态">
                <button
                  v-for="(state, index) in coachStates"
                  :key="state.text"
                  class="coach-dot"
                  :class="{ active: index === coachIndex }"
                  type="button"
                  role="tab"
                  :aria-selected="index === coachIndex"
                  :aria-label="`切换到第 ${index + 1} 条`"
                  @click.stop="selectCoach(index)"
                ></button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="loop">
        <div v-reveal class="section-head">
          <div>
            <h2>从一道题，走完一次闭环</h2>
          </div>
        </div>

        <div class="step-list">
          <article
            v-for="(step, index) in steps"
            :key="step.index"
            v-reveal="{ delay: index * 45 }"
            class="step"
          >
            <span class="step-index">{{ step.index }}</span>
            <h3>{{ step.title }}</h3>
            <p>{{ step.body }}</p>
          </article>
        </div>
      </section>

      <section class="insight-grid">
        <article v-reveal class="panel chart-panel">
          <div class="panel-head">
            <div>
              <h2>本周练习</h2>
            </div>
            <div class="chart-total">
              <strong>{{ summary.hasData ? weeklyTotal : '—' }}</strong>
              <span>次提交</span>
            </div>
          </div>

          <div v-if="loading" class="chart-state">正在读取本周数据…</div>
          <div v-else-if="!summary.hasData" class="chart-state">
            <strong>这一周还没有提交</strong>
          </div>
          <div ref="chartRef" class="chart-canvas" :class="{ 'is-empty': !summary.hasData || loading }"></div>

          <div class="chart-foot">
            <div class="legend">
              <span><i class="legend-dot submit"></i>提交</span>
              <span><i class="legend-dot ac"></i>通过</span>
            </div>
            <span v-if="summary.hasData" class="pass-copy">全站通过率 {{ passRateText }}</span>
          </div>
          <p v-if="summaryError" class="chart-error">{{ summaryError }}，当前展示空数据。</p>
        </article>

        <aside v-reveal="{ delay: 70 }" class="panel knowledge-panel">
          <div class="panel-head">
            <div>
              <h2>知识地图</h2>
            </div>
            <span class="count-badge">{{ summary.knowledgePoints || knowledgePoints.length }}</span>
          </div>

          <div v-if="knowledgePoints.length" class="knowledge-list">
            <button
              v-for="(item, index) in knowledgePoints"
              :key="item.id || item.code"
              class="knowledge-row"
              type="button"
              @click="goKnowledgePoint(item)"
            >
              <span class="knowledge-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="knowledge-copy">
                <strong>{{ item.name }}</strong>
                <small>{{ item.description || '从一个知识点开始练习' }}</small>
              </span>
              <span class="knowledge-count">{{ item.questionCount ?? 0 }} 题</span>
            </button>
          </div>
          <div v-else class="knowledge-empty">知识点暂时未加载。</div>

          <button class="panel-link" type="button" @click="goKnowledge">
            查看全部知识点 <ArrowRight />
          </button>
        </aside>
      </section>

      <section class="closing">
        <div>
          <h2>选一道题，先写下第一版。</h2>
          <p>卡住的地方，就是练习开始的地方。</p>
        </div>
        <button class="btn-light" type="button" @click="goWorkspace">
          进入题库 <ArrowRight />
        </button>
      </section>

      <footer class="site-foot">
        <span class="foot-brand">CodeAgent <em>智码刷题</em></span>
        <span class="foot-copy">让每一次报错都有回应。</span>
        <span class="foot-status" :class="{ warn: !healthOk }"><i></i>{{ healthText }}</span>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.home-page {
  --h-ink: #0f172a;
  --h-text: #475569;
  --h-muted: #64748b;
  --h-line: #e2e8f0;
  --h-primary: #1f3a5f;
  --h-primary-soft: #eef2f6;
  --h-mint: #2f7d5b;
  --h-surface: #ffffff;
  min-height: 100vh;
  background: #f5f6f7;
  color: var(--h-ink);
}

.site-head {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 32px;
  min-height: 72px;
  padding: 0 max(24px, calc((100vw - 1200px) / 2));
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(247, 248, 252, 0.94);
  backdrop-filter: blur(12px);
}

.brand {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--h-primary);
  color: #fff;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -1px;
}

.brand-name {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.brand-name strong {
  font-size: 16px;
  font-weight: 750;
  letter-spacing: -0.3px;
}

.brand-name small {
  color: #94a3b8;
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.8px;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-link {
  height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
}

.nav-link:hover {
  background: #eef2f6;
  color: var(--h-primary);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.health-pill,
.foot-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 11px;
  white-space: nowrap;
}

.health-pill i,
.foot-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--h-mint);
  box-shadow: 0 0 0 3px rgba(47, 125, 91, 0.10);
}

.health-pill.warn i,
.foot-status.warn i {
  background: #a16207;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.12);
}

.login-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 34px;
  padding: 0 13px;
  border: 1px solid #c8d2dc;
  border-radius: 8px;
  background: #fff;
  color: var(--h-primary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.login-link svg {
  width: 14px;
  height: 14px;
}

.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 10px 0 3px;
  border: 1px solid var(--h-line);
  border-radius: 999px;
  background: #fff;
  color: var(--h-ink);
  cursor: pointer;
}

.avatar {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--h-primary-soft);
  color: var(--h-primary);
  font-size: 11px;
  font-weight: 700;
}

.user-name {
  max-width: 110px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page {
  width: min(1200px, calc(100% - 48px));
  margin: 0 auto;
  padding: 66px 0 26px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  gap: 64px;
  align-items: center;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #3d5a78;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 1.7px;
}

.eyebrow span {
  width: 18px;
  height: 1px;
  background: currentColor;
}

.hero h1 {
  margin-top: 18px;
  color: var(--h-ink);
  font-size: clamp(42px, 5.2vw, 64px);
  font-weight: 750;
  letter-spacing: -3px;
  line-height: 1.08;
}

.hero-enter {
  opacity: 0;
  animation: hero-enter 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-enter-1 {
  animation-delay: 0.03s;
}

.hero-enter-2 {
  animation-delay: 0.16s;
}

.hero-enter-3 {
  animation-delay: 0.29s;
}

.hero-enter-4 {
  animation-delay: 0.42s;
}

@keyframes hero-enter {
  from {
    opacity: 0;
    transform: translateY(24px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero h1 .mark {
  color: var(--h-primary);
  white-space: nowrap;
}

.eyebrow,
.panel-kicker,
.closing-kicker {
  display: none;
}

.lede {
  max-width: 540px;
  margin-top: 22px;
  color: var(--h-text);
  font-size: 15px;
  line-height: 1.85;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 28px;
}

.btn-primary,
.btn-ghost,
.btn-light {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 42px;
  padding: 0 17px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary {
  border: 1px solid var(--h-primary);
  background: var(--h-primary);
  color: #fff;
}

.btn-primary:hover {
  background: #152a45;
  border-color: #152a45;
}

.btn-primary svg,
.btn-light svg {
  width: 15px;
  height: 15px;
}

.btn-ghost {
  border: 1px solid var(--h-line);
  background: #fff;
  color: var(--h-text);
}

.btn-ghost:hover {
  border-color: #c8d2dc;
  color: var(--h-primary);
}

.hero-stats {
  display: flex;
  gap: 0;
  margin: 34px 0 0;
}

.hero-stats > div {
  min-width: 104px;
  padding-right: 24px;
}

.hero-stats > div + div {
  padding-left: 24px;
  border-left: 1px solid var(--h-line);
}

.hero-stats dt {
  color: var(--h-ink);
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.8px;
}

.hero-stats dd {
  margin: 3px 0 0;
  color: #94a3b8;
  font-size: 11px;
}

.hero-visual {
  position: relative;
  padding: 4px 0 32px 18px;
  perspective: 1000px;
  --tilt-lift: 0px;
}

.hero-visual:hover {
  --tilt-lift: -6px;
}

.visual-stack {
  transform: perspective(1000px) rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg))
    translateY(var(--tilt-lift));
  transform-style: preserve-3d;
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.hero-visual.is-tilting .visual-stack {
  transition-duration: 0.3s;
}

.preview-card {
  position: relative;
  z-index: 1;
  overflow: hidden;
  border: 1px solid #dfe5ee;
  border-radius: 10px;
  background: #fff;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 8px 24px rgba(15, 23, 42, 0.08);
  transition: box-shadow 0.4s ease;
}

.hero-visual:hover .preview-card,
.hero-visual:hover .coach-card {
  box-shadow:
    0 2px 4px rgba(15, 23, 42, 0.06),
    0 20px 48px rgba(15, 23, 42, 0.16);
}

.preview-head {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 14px;
  border-bottom: 1px solid #eef2f7;
  background: #fbfcfe;
}

.window-dots {
  display: inline-flex;
  gap: 4px;
}

.window-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #dbe3ee;
}

.window-dots i:first-child {
  background: #d8b4b6;
}

.file-name {
  color: #94a3b8;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
}

.preview-label {
  margin-left: auto;
  padding: 2px 7px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 9px;
}

.code-block {
  padding: 17px 16px 14px;
  background: #fff;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  line-height: 2.05;
}

.code-line {
  display: flex;
  gap: 15px;
  padding: 0 5px;
  border-radius: 3px;
  color: #475569;
  white-space: pre;
}

.code-line span {
  width: 14px;
  color: #cbd5e1;
  font-size: 9px;
  text-align: right;
  user-select: none;
}

.code-line code {
  padding: 0;
  background: transparent;
  color: inherit;
  font-family: inherit;
}

.code-line.is-error {
  background: #f7efee;
  color: #9f2d2d;
}

.code-line.is-error span {
  color: #d8b4b6;
}

.fail-line {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 14px 14px;
  padding: 8px 10px;
  border: 1px solid #fecdd3;
  border-radius: 6px;
  background: #fff7f8;
  color: #9f1239;
  font-size: 10px;
}

.fail-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #9f2d2d;
}

.coach-card {
  position: relative;
  z-index: 2;
  width: calc(100% - 28px);
  min-height: 184px;
  margin: -6px 0 0 28px;
  padding: 15px 16px 12px;
  border: 1px solid #c8d2dc;
  border-radius: 12px;
  background: #f7f9fb;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 8px 24px rgba(15, 23, 42, 0.08);
  transition: box-shadow 0.4s ease;
}

.coach-head {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #1f2937;
  font-size: 11px;
}

.coach-icon {
  display: grid;
  place-items: center;
  width: 21px;
  height: 21px;
  border-radius: 6px;
  background: #dfe6ed;
  color: var(--h-primary);
}

.coach-icon svg {
  width: 13px;
  height: 13px;
}

.coach-count {
  margin-left: auto;
  color: #93a5b8;
  font-size: 9px;
}

.coach-card p {
  margin-top: 9px;
  color: #152a45;
  font-size: 10.5px;
  line-height: 1.75;
}

.coach-card p code {
  padding: 1px 3px;
  border-radius: 3px;
  background: #eef2f6;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 9.5px;
}

.coach-tools {
  display: flex;
  gap: 5px;
  margin-top: 10px;
}

.coach-tools span {
  padding: 2px 6px;
  border: 1px solid #dfe6ed;
  border-radius: 4px;
  background: #fff;
  color: #64748b;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 8.5px;
}

.coach-foot {
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px dashed #dfe6ed;
  color: #94a3b8;
  font-size: 9px;
}

.coach-body {
  min-height: 100px;
}

.coach-dots {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}

.coach-dot {
  width: 5px;
  height: 5px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: #cbd5e1;
  cursor: pointer;
  opacity: 0.8;
  transform: scaleX(1);
  transition:
    transform 0.3s ease,
    opacity 0.3s ease;
}

.coach-dot:hover {
  opacity: 1;
}

.coach-dot.active {
  background: var(--h-primary);
  opacity: 1;
  transform: scaleX(1.9);
}

.coach-swap-enter-active,
.coach-swap-leave-active {
  transition:
    opacity 0.35s ease,
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.coach-swap-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.coach-swap-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.loop {
  margin-top: 78px;
  padding-top: 30px;
  border-top: 1px solid var(--h-line);
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
}

.section-head h2 {
  margin-top: 9px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.8px;
}

.section-note {
  max-width: 330px;
  color: #94a3b8;
  font-size: 11.5px;
  line-height: 1.7;
  text-align: right;
}

.step-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 28px;
  border-top: 1px solid var(--h-line);
}

.step {
  padding: 22px 26px 8px 0;
}

.step + .step {
  padding-left: 26px;
  border-left: 1px solid var(--h-line);
}

.step-index {
  color: #c8d2dc;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  font-weight: 700;
}

.step h3 {
  margin-top: 9px;
  font-size: 15px;
  font-weight: 650;
  letter-spacing: -0.2px;
}

.step p {
  max-width: 280px;
  margin-top: 8px;
  color: var(--h-muted);
  font-size: 12px;
  line-height: 1.75;
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: 18px;
  margin-top: 54px;
}

.panel {
  border: 1px solid var(--h-line);
  border-radius: 10px;
  background: var(--h-surface);
}

.chart-panel {
  position: relative;
  padding: 22px 22px 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.panel-kicker {
  color: #93a5b8;
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 1.4px;
}

.panel-head h2 {
  margin-top: 6px;
  color: var(--h-ink);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.4px;
}

.panel-desc {
  margin-top: 5px;
  color: #94a3b8;
  font-size: 10.5px;
  line-height: 1.5;
}

.chart-total {
  display: flex;
  align-items: baseline;
  gap: 5px;
  color: #94a3b8;
  font-size: 10px;
}

.chart-total strong {
  color: var(--h-ink);
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -1px;
}

.chart-canvas {
  height: 238px;
  margin-top: 12px;
}

.chart-canvas.is-empty {
  opacity: 0.35;
}

.chart-state {
  position: absolute;
  top: 128px;
  left: 50%;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  width: min(300px, 80%);
  color: #94a3b8;
  font-size: 10.5px;
  line-height: 1.6;
  text-align: center;
  transform: translateX(-50%);
}

.chart-state strong {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.chart-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.legend {
  display: flex;
  gap: 14px;
  color: #94a3b8;
  font-size: 10px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend-dot {
  width: 6px;
  height: 6px;
  border-radius: 2px;
}

.legend-dot.submit {
  background: var(--h-primary);
}

.legend-dot.ac {
  background: var(--h-mint);
}

.pass-copy {
  color: #94a3b8;
  font-size: 10px;
}

.chart-error {
  margin-top: 8px;
  color: #b45309;
  font-size: 10px;
}

.knowledge-panel {
  display: flex;
  flex-direction: column;
  padding: 22px 0 14px;
}

.knowledge-panel .panel-head {
  padding: 0 20px 16px;
}

.count-badge {
  display: grid;
  place-items: center;
  min-width: 28px;
  height: 28px;
  padding: 0 7px;
  border-radius: 8px;
  background: var(--h-primary-soft);
  color: var(--h-primary);
  font-size: 11px;
  font-weight: 700;
}

.knowledge-list {
  flex: 1;
}

.knowledge-row {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 11px 20px;
  border: 0;
  border-top: 1px solid #f1f5f9;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.knowledge-row:hover {
  background: #fbfcfd;
}

.knowledge-index {
  color: #c8d2dc;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 9px;
}

.knowledge-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.knowledge-copy strong {
  color: var(--h-ink);
  font-size: 12px;
  font-weight: 600;
}

.knowledge-copy small {
  overflow: hidden;
  color: #94a3b8;
  font-size: 9.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-count {
  color: #94a3b8;
  font-size: 9.5px;
  white-space: nowrap;
}

.knowledge-empty {
  padding: 30px 20px;
  color: #94a3b8;
  font-size: 11px;
}

.panel-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 12px 20px 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--h-primary);
  font-size: 10.5px;
  cursor: pointer;
}

.panel-link svg {
  width: 12px;
  height: 12px;
}

.closing {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-top: 30px;
  padding: 34px 36px;
  border-radius: 12px;
  background: var(--h-ink);
  color: #fff;
}

.closing-kicker {
  color: #6e8299;
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 1.6px;
}

.closing h2 {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.6px;
}

.closing p:not(.closing-kicker) {
  max-width: 470px;
  margin-top: 7px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.7;
}

.btn-light {
  flex-shrink: 0;
  border: 1px solid #fff;
  background: #fff;
  color: var(--h-ink);
}

.btn-light:hover {
  background: #eef2f6;
  border-color: #eef2f6;
}

.site-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 26px 2px 4px;
  color: #94a3b8;
  font-size: 10.5px;
}

.foot-brand {
  color: #475569;
  font-weight: 700;
}

.foot-brand em {
  margin-left: 4px;
  color: #94a3b8;
  font-style: normal;
  font-weight: 400;
}

.foot-copy {
  flex: 1;
}

.foot-status {
  font-size: 10px;
}

@media (prefers-reduced-motion: reduce) {
  .hero-enter {
    opacity: 1;
    animation: none;
    transform: none;
  }

  .visual-stack,
  .hero-visual:hover {
    transition: none;
    transform: none;
    --tilt-lift: 0px;
  }

  .preview-card,
  .coach-card,
  .coach-dot,
  .coach-swap-enter-active,
  .coach-swap-leave-active {
    transition: none;
  }
}

@media (max-width: 1000px) {
  .site-nav {
    display: none;
  }

  .hero {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .hero-copy {
    max-width: 700px;
  }

  .hero-visual {
    max-width: 620px;
    padding-left: 0;
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .site-head {
    gap: 12px;
    min-height: 64px;
    padding: 0 18px;
  }

  .health-pill {
    display: none;
  }

  .brand-name small {
    display: none;
  }

  .page {
    width: calc(100% - 36px);
    padding-top: 42px;
  }

  .hero h1 {
    font-size: 42px;
    letter-spacing: -2px;
  }

  .hero-stats {
    gap: 0;
  }

  .hero-stats > div {
    min-width: 0;
    flex: 1;
    padding-right: 12px;
  }

  .hero-stats > div + div {
    padding-left: 12px;
  }

  .step-list {
    grid-template-columns: 1fr;
  }

  .step {
    padding: 18px 0;
  }

  .step + .step {
    padding-left: 0;
    border-top: 1px solid var(--h-line);
    border-left: 0;
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .section-note {
    text-align: left;
  }

  .closing {
    align-items: flex-start;
    flex-direction: column;
    padding: 26px 22px;
  }

  .site-foot {
    flex-wrap: wrap;
  }
}
</style>
