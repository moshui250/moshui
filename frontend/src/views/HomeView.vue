<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  Collection,
  DataAnalysis,
  MagicStick,
  Picture,
  Reading
} from '@element-plus/icons-vue'
import { getHealth } from '@/api/health'
import { getHomeSummary } from '@/api/home'
import { useUserStore } from '@/store/user'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const userStore = useUserStore()

const chartRef = ref(null)
const loading = ref(true)
const summaryError = ref('')
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

const navItems = [
  { label: '题库', path: '/question', icon: Reading },
  { label: 'AI 调试', action: 'ai', icon: MagicStick },
  { label: '错题本', path: '/wrong-book', icon: Collection },
  { label: '数据看板', path: '/dashboard', icon: DataAnalysis },
  { label: '图片录题', path: '/question/import', icon: Picture }
]

const announcements = [
  {
    tag: '进行中',
    tone: 'green',
    title: '自托管 Piston 判题服务已接入，C++ / Java 均可运行',
    date: '2026-11-05'
  },
  {
    tag: '置顶',
    tone: 'amber',
    title: '平台使用指南：从选题、提交到 AI 分层调试',
    date: '2026-11-03'
  },
  {
    tag: '重要',
    tone: 'red',
    title: '图片识别录题上线：识别结果需核对后才会入库',
    date: '2026-11-02'
  },
  {
    tag: '公告',
    tone: 'gray',
    title: '题库已覆盖 5 个知识点、15 道公共题',
    date: '2026-10-28'
  },
  {
    tag: '公告',
    tone: 'gray',
    title: 'AI 调试教练支持工具调用时间线回放',
    date: '2026-10-20'
  }
]

const initial = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())

const healthOk = computed(() => health.status === 'UP' && health.db === 'UP')
const healthText = computed(() => {
  if (health.status === 'CHECKING') return '正在检查服务状态'
  if (health.status === 'OFFLINE') return '服务连接异常'
  if (health.db !== 'UP') return '数据库暂未连接'
  return '系统运行正常 · 服务器响应良好'
})

const passRateText = computed(() => {
  if (!summary.hasData) return '—'
  return `${(Number(summary.passRate || 0) * 100).toFixed(1)}%`
})

const progressWidth = computed(() => {
  if (!summary.hasData) return '0%'
  return `${Math.min(100, Number(summary.passRate || 0) * 100)}%`
})

const weeklyTotal = computed(() =>
  (summary.weekly || []).reduce((sum, item) => sum + Number(item.submitCount || 0), 0)
)

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
    await nextTick()
    renderChart()
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const weekly = summary.weekly || []
  const labels = weekly.map((item) => item.label || item.date)
  const submitData = weekly.map((item) => Number(item.submitCount || 0))
  const acData = weekly.map((item) => Number(item.acCount || 0))

  chartInstance.setOption({
    animationDuration: 420,
    grid: { left: 48, right: 28, top: 28, bottom: 34 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const day = params[0]?.axisValue || ''
        const submit = params.find((item) => item.seriesName === '提交数')?.value ?? 0
        const ac = params.find((item) => item.seriesName === '通过数')?.value ?? 0
        return `${day}<br/>提交 ${submit} 次<br/>通过 ${ac} 次`
      }
    },
    legend: { show: false },
    xAxis: {
      type: 'category',
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e3ddd2' } },
      axisLabel: { color: '#6e6559', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      minInterval: 1,
      axisLabel: { color: '#9a9185', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0ece5' } }
    },
    series: [
      {
        name: '提交数',
        type: 'bar',
        data: submitData,
        barWidth: 26,
        itemStyle: { color: '#c58a3a', borderRadius: [3, 3, 0, 0] }
      },
      {
        name: '通过数',
        type: 'bar',
        data: acData,
        barWidth: 26,
        itemStyle: { color: '#4f8a63', borderRadius: [3, 3, 0, 0] }
      }
    ]
  })
  chartInstance.resize()
}

function handleResize() {
  chartInstance?.resize()
}

function go(path) {
  if (path) router.push(path)
}

function handleNav(item) {
  if (item.action === 'ai') {
    ElMessage.info('在答题页提交错误代码后，AI 调试教练会自动分析；也可以手动点「AI 调试」')
    router.push('/question')
    return
  }
  go(item.path)
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: '/question' } })
}

function goWorkspace() {
  router.push('/question')
}

onMounted(async () => {
  // 首页放行早于路由守卫，登录用户需要在这里补一次用户信息。
  if (userStore.token && !userStore.user) {
    userStore.fetchMe().catch(() => {})
  }
  await Promise.all([loadHealth(), loadSummary()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="home-page">
    <header class="oj-header">
      <button class="brand-block" type="button" @click="go('/')">
        <span class="brand-icon">&lt;/&gt;</span>
        <span class="brand-text">
          <strong>CodeAgent</strong>
          <small>OJ ONLINE JUDGE</small>
        </span>
      </button>

      <div class="title-block">
        <h1>在线评测平台</h1>
        <p>CODE · PRACTICE · GROW</p>
      </div>

      <div class="trophy" aria-hidden="true">🏆</div>
    </header>

    <nav class="oj-nav" aria-label="主导航">
      <div class="nav-inner">
        <button
          v-for="item in navItems"
          :key="item.label"
          class="nav-item"
          type="button"
          @click="handleNav(item)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>

        <div class="nav-spacer"></div>

        <button v-if="userStore.isLoggedIn" class="user-pill" type="button" @click="goWorkspace">
          <span class="user-avatar">{{ initial }}</span>
          <span class="user-label">{{ userStore.displayName || '已登录' }}</span>
          <span class="user-cta">进入题库 →</span>
        </button>
        <button v-else class="login-pill" type="button" @click="goLogin">登录</button>
      </div>
    </nav>

    <main class="oj-main">
      <section class="status-strip" :class="{ 'is-warn': !healthOk }">
        <div class="status-copy">
          <span class="status-dot"></span>
          <strong>{{ healthText }}</strong>
          <span class="status-divider">·</span>
          <span>{{ summary.hasData ? `近一周 ${weeklyTotal} 次提交` : '等待第一份提交' }}</span>
        </div>
        <div class="status-meter" aria-hidden="true">
          <span :style="{ width: progressWidth }"></span>
        </div>
        <strong class="status-rate">{{ passRateText }}</strong>
      </section>

      <section class="stats-strip" aria-label="平台概览">
        <span><strong>{{ summary.publicQuestions }}</strong> 道公共题</span>
        <span><strong>{{ summary.knowledgePoints }}</strong> 个知识点</span>
        <span><strong>{{ summary.totalSubmits }}</strong> 次全站提交</span>
        <span v-if="summary.hasData"><strong>{{ summary.acSubmits }}</strong> 次通过</span>
      </section>

      <section class="panel chart-panel">
        <div class="panel-title-row">
          <div class="panel-title">
            <span class="title-accent"></span>
            <div>
              <h2>评测速度统计</h2>
              <p>本周提交与通过数量 · 周一至周日</p>
            </div>
          </div>
          <div class="legend">
            <span><i class="legend-dot submit"></i>提交数</span>
            <span><i class="legend-dot ac"></i>通过数</span>
          </div>
        </div>

        <div v-if="loading" class="chart-loading">正在读取评测数据…</div>
        <div v-else-if="!summary.hasData" class="chart-empty">
          <strong>本周还没有提交</strong>
          <span>去题库完成第一道题，这里会出现真实的提交曲线。</span>
          <button type="button" class="text-link" @click="goWorkspace">去题库 →</button>
        </div>
        <div ref="chartRef" class="chart-canvas" :class="{ 'is-empty': !summary.hasData }"></div>

        <p v-if="summaryError" class="chart-error">{{ summaryError }}，当前展示空数据。</p>
      </section>

      <section class="panel notice-panel">
        <div class="panel-title-row">
          <div class="panel-title">
            <span class="title-accent"></span>
            <div>
              <h2>公告</h2>
              <p>平台更新与使用提示</p>
            </div>
          </div>
          <span class="notice-count">{{ announcements.length }} 条</span>
        </div>

        <ul class="notice-list">
          <li v-for="item in announcements" :key="item.title" class="notice-row">
            <span class="notice-tag" :class="`tone-${item.tone}`">{{ item.tag }}</span>
            <span class="notice-title">{{ item.title }}</span>
            <time>{{ item.date }}</time>
          </li>
        </ul>
      </section>

      <footer class="home-footer">
        <span>CodeAgent · 让每一次报错都有回应</span>
        <span class="footer-sep">/</span>
        <span>AI 原生编程练习平台</span>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f6f2ea;
  color: #292722;
}

.oj-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 24px;
  width: min(1280px, calc(100% - 48px));
  margin: 0 auto;
  padding: 26px 0 20px;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-self: start;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.brand-icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 4px;
  background: #1e2025;
  color: #f6f2ea;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -2px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-text strong {
  font-family: Georgia, 'Times New Roman', 'Songti SC', serif;
  font-size: 27px;
  font-weight: 700;
  letter-spacing: -0.8px;
}

.brand-text small {
  color: #786f63;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.title-block {
  text-align: center;
}

.title-block h1 {
  font-family: 'Songti SC', 'Noto Serif SC', 'STSong', serif;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 2px;
  line-height: 1.15;
}

.title-block p {
  margin-top: 7px;
  color: #b5532f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 3px;
}

.trophy {
  display: grid;
  place-items: center;
  justify-self: end;
  width: 64px;
  height: 64px;
  border: 2px solid #b5532f;
  border-radius: 50%;
  background: #fffaf2;
  font-size: 28px;
  box-shadow: 0 0 0 5px #f6f2ea, 0 0 0 7px #d8b7a2;
}

.oj-nav {
  background: #202228;
  color: #f2ede3;
}

.nav-inner {
  display: flex;
  align-items: center;
  gap: 4px;
  width: min(1220px, calc(100% - 48px));
  min-height: 68px;
  margin: 0 auto;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 17px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: #ddd7cc;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease;
}

.nav-item .el-icon {
  font-size: 19px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.nav-spacer {
  flex: 1;
}

.user-pill,
.login-pill {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  height: 38px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
}

.login-pill {
  padding: 0 27px;
  background: #c4552f;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.login-pill:hover {
  background: #aa4425;
}

.user-pill {
  padding: 0 13px 0 5px;
  background: #f7f2e8;
  color: #292722;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 29px;
  height: 29px;
  border-radius: 50%;
  background: #c4552f;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.user-label {
  max-width: 120px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cta {
  color: #9b4b2f;
  font-size: 11px;
}

.oj-main {
  width: min(1220px, calc(100% - 48px));
  margin: 22px auto 0;
  padding-bottom: 30px;
}

.status-strip {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 54px;
  padding: 0 20px;
  border-radius: 5px;
  background: #202228;
  color: #f1ece2;
}

.status-copy {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  font-size: 12px;
}

.status-copy strong {
  font-size: 13px;
  font-weight: 600;
}

.status-copy span:not(.status-dot):not(.status-divider) {
  color: #b4aea3;
}

.status-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #4b9b68;
  box-shadow: 0 0 0 3px rgba(75, 155, 104, 0.2);
}

.status-strip.is-warn .status-dot {
  background: #c58a3a;
}

.status-divider {
  color: #6f6a61;
}

.status-meter {
  flex: 1;
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #45464b;
}

.status-meter span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4b9b68, #5caa75);
  transition: width 0.5s ease;
}

.status-rate {
  min-width: 52px;
  font-size: 14px;
  text-align: right;
}

.stats-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 26px;
  padding: 15px 2px 13px;
  color: #857b6e;
  font-size: 12px;
}

.stats-strip strong {
  margin-right: 3px;
  color: #332f29;
  font-size: 15px;
  font-weight: 700;
}

.panel {
  border: 1px solid #e3ddd2;
  border-radius: 7px;
  background: #fffdf9;
  box-shadow: 0 2px 8px rgba(75, 59, 38, 0.04);
}

.chart-panel {
  position: relative;
  padding: 20px 22px 16px;
}

.panel-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.title-accent {
  width: 4px;
  height: 25px;
  margin-top: 1px;
  border-radius: 2px;
  background: #c4552f;
}

.panel-title h2 {
  font-family: 'Songti SC', 'Noto Serif SC', 'STSong', serif;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.4px;
}

.panel-title p {
  margin-top: 5px;
  color: #9a9185;
  font-size: 11px;
}

.legend {
  display: flex;
  gap: 18px;
  align-items: center;
  color: #756c60;
  font-size: 12px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-dot.submit {
  background: #c58a3a;
}

.legend-dot.ac {
  background: #4f8a63;
}

.chart-canvas {
  width: 100%;
  height: 336px;
  margin-top: 5px;
}

.chart-canvas.is-empty {
  opacity: 0.18;
}

.chart-loading,
.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 336px;
  color: #9a9185;
  font-size: 13px;
}

.chart-empty {
  flex-direction: column;
  gap: 7px;
  position: absolute;
  width: calc(100% - 44px);
  pointer-events: none;
}

.chart-empty strong {
  color: #514a40;
  font-size: 15px;
}

.chart-empty span {
  font-size: 12px;
}

.chart-empty .text-link {
  margin-top: 8px;
  border: 0;
  background: transparent;
  color: #b5532f;
  font-size: 12px;
  cursor: pointer;
  pointer-events: auto;
}

.chart-error {
  margin-top: 4px;
  color: #b4533a;
  font-size: 11px;
}

.notice-panel {
  margin-top: 16px;
  padding: 20px 22px 10px;
}

.notice-count {
  color: #a29a8e;
  font-size: 11px;
}

.notice-list {
  margin: 13px 0 0;
  padding: 0;
  list-style: none;
}

.notice-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  border-top: 1px solid #f0ece5;
  font-size: 13px;
}

.notice-tag {
  flex: 0 0 auto;
  min-width: 49px;
  padding: 2px 7px;
  border-radius: 2px;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.tone-green {
  background: #4f8a63;
  color: #fff;
}

.tone-amber {
  background: #d2a04a;
  color: #fff;
}

.tone-red {
  background: #b5532f;
  color: #fff;
}

.tone-gray {
  background: #e5e0d7;
  color: #71695e;
}

.notice-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #35312b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-row time {
  flex: 0 0 auto;
  color: #a29a8e;
  font-size: 12px;
}

.home-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 20px 0 4px;
  color: #a59c8e;
  font-size: 11px;
}

.footer-sep {
  color: #cfc7ba;
}

@media (max-width: 900px) {
  .oj-header {
    grid-template-columns: 1fr auto;
    gap: 14px;
  }

  .title-block {
    grid-column: 1 / -1;
    grid-row: 2;
    text-align: left;
  }

  .title-block h1 {
    font-size: 28px;
  }

  .trophy {
    grid-column: 2;
    grid-row: 1;
    width: 52px;
    height: 52px;
    font-size: 23px;
  }

  .brand-icon {
    width: 48px;
    height: 48px;
  }

  .brand-text strong {
    font-size: 23px;
  }

  .nav-inner {
    flex-wrap: wrap;
    width: calc(100% - 32px);
    padding: 9px 0;
  }

  .nav-item {
    padding: 0 10px;
    font-size: 13px;
  }

  .nav-spacer {
    display: none;
  }

  .oj-main {
    width: calc(100% - 32px);
    margin-top: 16px;
  }

  .status-strip {
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 10px;
    padding: 13px 15px;
  }

  .status-copy {
    flex: 1 1 100%;
  }

  .status-meter {
    flex: 1 1 60%;
  }

  .chart-panel,
  .notice-panel {
    padding: 16px 14px 10px;
  }

  .chart-canvas {
    height: 270px;
  }

  .legend {
    gap: 10px;
    font-size: 11px;
  }

  .notice-row {
    flex-wrap: wrap;
    gap: 5px 9px;
    padding: 9px 0;
  }

  .notice-title {
    flex: 1 1 calc(100% - 70px);
    white-space: normal;
  }

  .notice-row time {
    margin-left: 61px;
  }
}

@media (max-width: 520px) {
  .oj-header {
    width: calc(100% - 28px);
    padding-top: 16px;
  }

  .brand-text small {
    display: none;
  }

  .brand-text strong {
    font-size: 21px;
  }

  .title-block h1 {
    font-size: 24px;
    letter-spacing: 1px;
  }

  .title-block p {
    font-size: 10px;
    letter-spacing: 2px;
  }

  .nav-item span {
    display: none;
  }

  .nav-item {
    width: 38px;
    justify-content: center;
    padding: 0;
  }

  .user-cta,
  .user-label {
    display: none;
  }

  .user-pill {
    padding-right: 5px;
  }

  .panel-title-row {
    gap: 8px;
  }

  .panel-title h2 {
    font-size: 18px;
  }

  .legend {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
