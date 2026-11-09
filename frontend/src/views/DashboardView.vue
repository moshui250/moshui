<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ArrowRight, InfoFilled } from '@element-plus/icons-vue'
import { getProfileStat } from '@/api/profile'
import { emptyProfileStat } from '@/data/demo'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const stat = reactive({ ...emptyProfileStat, knowledgeStats: [], recentSubmits: [], weekly: [] })
const loading = ref(true)
const statError = ref('')
const chartRef = ref(null)
let chartInstance = null

const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const hasSubmit = computed(() => Number(stat.totalSubmit || 0) > 0)
const accuracyText = computed(() =>
  hasSubmit.value ? `${(Number(stat.accuracy || 0) * 100).toFixed(1)}%` : '—'
)

const metrics = computed(() => [
  {
    label: '总提交',
    value: stat.totalSubmit,
    unit: '次',
    foot: '全部判题记录',
    tone: 'indigo'
  },
  {
    label: '正确率',
    value: accuracyText.value,
    unit: '',
    foot: `AC ${stat.acSubmit} / 提交 ${stat.totalSubmit}`,
    tone: 'mint'
  },
  {
    label: '未掌握错题',
    value: stat.wrongCount,
    unit: '道',
    foot: '仅统计待复习',
    tone: 'rose'
  },
  {
    label: '累计 AC',
    value: stat.totalAc,
    unit: '道',
    foot: '含图片录题，不计段位',
    tone: 'indigo'
  }
])

const activeKnowledge = computed(() =>
  (stat.knowledgeStats || [])
    .filter((item) => item.hasData && Number(item.errorRate) > 0)
    .sort((a, b) => Number(b.errorRate) - Number(a.errorRate))
)

const weakPoints = computed(() => activeKnowledge.value.slice(0, 3))
const weakest = computed(() => activeKnowledge.value[0] || null)

const week = computed(() => (Array.isArray(stat.weekly) ? stat.weekly : []))
const weekTotal = computed(() => week.value.reduce((sum, day) => sum + Number(day.submitCount || 0), 0))
const weekAc = computed(() => week.value.reduce((sum, day) => sum + Number(day.acCount || 0), 0))
const weekRate = computed(() => (weekTotal.value ? Math.round((weekAc.value / weekTotal.value) * 1000) / 10 : 0))
const activeDays = computed(() => week.value.filter((day) => Number(day.submitCount || 0) > 0).length)
const bestDay = computed(() => {
  return [...week.value].sort((a, b) => Number(b.submitCount || 0) - Number(a.submitCount || 0))[0] || null
})
const maxDaySubmit = computed(() => Math.max(1, ...week.value.map((day) => Number(day.submitCount || 0))))

const statusMeta = {
  AC: { label: '通过', className: 'ok' },
  WA: { label: '答案错误', className: 'danger' },
  TLE: { label: '运行超时', className: 'warn' },
  MLE: { label: '内存超限', className: 'warn' },
  RE: { label: '运行错误', className: 'danger' },
  CE: { label: '编译错误', className: 'danger' },
  SE: { label: '系统错误', className: 'muted' },
  PENDING: { label: '排队中', className: 'muted' },
  JUDGING: { label: '判题中', className: 'muted' }
}

function statusOf(row) {
  return statusMeta[row.status] || { label: row.status || '未知', className: 'muted' }
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).slice(5, 16)
}

function goRecent(row) {
  router.push(`/question/${row.questionId}`)
}

async function loadStat() {
  loading.value = true
  statError.value = ''
  try {
    const data = await getProfileStat()
    Object.assign(stat, emptyProfileStat, data || {})
    stat.knowledgeStats = Array.isArray(data?.knowledgeStats) ? data.knowledgeStats : []
    stat.recentSubmits = Array.isArray(data?.recentSubmits) ? data.recentSubmits : []
    stat.weekly = Array.isArray(data?.weekly) ? data.weekly : []
  } catch (error) {
    statError.value = error.message || '学习看板加载失败，请稍后重试'
    Object.assign(stat, emptyProfileStat)
  } finally {
    loading.value = false
    await nextTick()
    renderChart()
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  chartInstance = echarts.init(chartRef.value)
  const labels = week.value.map((day) => day.label)
  const submitData = week.value.map((day) => Number(day.submitCount || 0))
  const acData = week.value.map((day) => Number(day.acCount || 0))
  const gridColor = '#eef2f7'
  const textColor = '#7c869c'
  const submitColor = '#557596'
  const acColor = '#2f7d5b'
  const submitFill = 'rgba(85, 117, 150, 0.14)'
  const acFill = 'rgba(47, 125, 91, 0.10)'

  chartInstance.setOption({
    animationDuration: 460,
    grid: { left: 34, right: 18, top: 18, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: '#dbe2ec' } },
      backgroundColor: '#fff',
      borderColor: '#e4e8ef',
      textStyle: { color: '#111827', fontSize: 12 },
      formatter: (params) => {
        const rows = params.map((item) => `${item.marker}${item.seriesName}：${item.value}`)
        return `${params[0]?.axisValue || ''}<br/>${rows.join('<br/>')}`
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: gridColor } },
      axisLabel: { color: textColor, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: textColor, fontSize: 10 },
      splitLine: { lineStyle: { color: gridColor, type: 'dashed' } }
    },
    series: [
      {
        name: '提交',
        type: 'line',
        smooth: 0.36,
        symbol: 'circle',
        symbolSize: 6,
        data: submitData,
        lineStyle: { width: 2.2, color: submitColor },
        itemStyle: { color: submitColor, borderColor: '#fff', borderWidth: 2 },
        areaStyle: { color: submitFill }
      },
      {
        name: '通过',
        type: 'line',
        smooth: 0.36,
        symbol: 'circle',
        symbolSize: 6,
        data: acData,
        lineStyle: { width: 2.2, color: acColor },
        itemStyle: { color: acColor, borderColor: '#fff', borderWidth: 2 },
        areaStyle: { color: acFill }
      }
    ]
  })
  chartInstance.resize()
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await loadStat()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<template>
  <div class="dashboard-page">
    <header class="page-head compact-head">
      <h1 class="compact-page-title">学习看板</h1>
      <div class="head-actions">
        <span class="date-text">{{ today }}</span>
        <button class="btn-primary" type="button" @click="router.push('/question')">
          去题库
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </header>

    <div v-if="statError" class="error-banner">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ statError }}</span>
      <button type="button" @click="loadStat">重试</button>
    </div>

    <section v-loading="loading" class="metric-grid" aria-label="学习概览">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-label">
          <span class="metric-dot" :class="`tone-${metric.tone}`" />
          {{ metric.label }}
        </div>
        <div class="metric-number">
          {{ metric.value }}<small v-if="metric.unit">{{ metric.unit }}</small>
        </div>
        <div class="metric-foot">{{ metric.foot }}</div>
      </article>
    </section>

    <section class="analytics-surface">
      <article class="chart-section">
        <div class="section-head chart-head">
          <div>
            <h2 class="section-title">本周提交趋势</h2>
            <p class="section-caption">最近 7 天 · 提交与通过</p>
          </div>
          <div class="chart-legend">
            <span><i class="legend-dot legend-submit" />提交</span>
            <span><i class="legend-dot legend-ac" />通过</span>
          </div>
        </div>
        <div class="week-highlights">
          <article class="highlight-card">
            <div class="highlight-top"><span>本周提交</span><small>最近 7 天</small></div>
            <strong>{{ weekTotal }}<small>次</small></strong>
            <p>{{ activeDays }} / 7 天有练习</p>
            <div class="spark-bars" aria-hidden="true">
              <i
                v-for="day in week"
                :key="`submit-${day.date}`"
                :style="{ height: `${Math.max(4, (Number(day.submitCount || 0) / maxDaySubmit) * 100)}%` }"
              />
            </div>
          </article>
          <article class="highlight-card">
            <div class="highlight-top"><span>本周通过</span><small>AC</small></div>
            <strong>{{ weekAc }}<small>次</small></strong>
            <p>通过率 {{ weekRate }}%</p>
            <div class="spark-bars is-ac" aria-hidden="true">
              <i
                v-for="day in week"
                :key="`ac-${day.date}`"
                :style="{ height: `${Math.max(4, (Number(day.acCount || 0) / maxDaySubmit) * 100)}%` }"
              />
            </div>
          </article>
          <article class="highlight-card">
            <div class="highlight-top"><span>学习节奏</span><small>活跃度</small></div>
            <strong>{{ activeDays }}<small>天</small></strong>
            <p v-if="bestDay && bestDay.submitCount">最佳 {{ bestDay.label }} · {{ bestDay.submitCount }} 次</p>
            <p v-else>本周还没有提交</p>
            <div class="rhythm-dots" aria-hidden="true">
              <i
                v-for="day in week"
                :key="`day-${day.date}`"
                :class="{ 'is-active': Number(day.submitCount || 0) > 0 }"
                :title="day.label"
              />
            </div>
          </article>
        </div>
        <div class="chart-wrap">
          <div ref="chartRef" class="chart" />
        </div>
      </article>

      <aside class="insight-section">
        <div class="section-head">
          <div>
            <h2 class="section-title">薄弱知识点</h2>
            <p class="section-caption">错误率从高到低</p>
          </div>
        </div>

        <div v-if="weakPoints.length" class="weak-list">
          <div v-for="item in weakPoints" :key="item.name" class="weak-row">
            <div class="weak-line">
              <span class="weak-name">{{ item.name }}</span>
              <span class="weak-value">{{ Math.round(Number(item.errorRate) * 100) }}%</span>
            </div>
            <div class="weak-track">
              <span :style="{ width: `${Number(item.errorRate) * 100}%` }" />
            </div>
            <span class="weak-foot">{{ item.acCount }} / {{ item.submitCount }} 次通过</span>
          </div>
        </div>
        <div v-else class="weak-empty">
          <span>暂无明显薄弱点</span>
        </div>

        <div class="insight-note">
          <span v-if="weakest">
            优先复习「{{ weakest.name }}」，再去错题本做同类题。
          </span>
          <span v-else-if="stat.totalSubmit">当前没有明显薄弱点，保持这个正确率。</span>
          <span v-else>先完成一次判题，再看复盘建议。</span>
        </div>

        <button class="text-link" type="button" @click="router.push('/wrong-book')">
          去错题本复盘
          <span>→</span>
        </button>
      </aside>
    </section>

    <section class="recent-section">
      <div class="recent-section-head">
        <div>
          <h2 class="section-title">最近提交</h2>
          <p class="section-caption">最近 5 次判题记录</p>
        </div>
        <span class="recent-count">{{ stat.recentSubmits.length }} 条</span>
      </div>

      <div v-if="stat.recentSubmits.length" class="recent-table">
        <div class="recent-head">
          <span>题目</span>
          <span>语言</span>
          <span>状态</span>
          <span>耗时</span>
          <span>提交时间</span>
        </div>
        <button
          v-for="row in stat.recentSubmits"
          :key="row.id"
          class="recent-row"
          type="button"
          @click="goRecent(row)"
        >
          <span class="recent-title">{{ row.questionTitle || `题目 ${row.questionId}` }}</span>
          <span class="recent-lang">{{ row.language === 'JAVA' ? 'Java' : 'C++' }}</span>
          <span>
            <span class="status-pill" :class="statusOf(row).className">
              {{ statusOf(row).label }}
            </span>
          </span>
          <span class="recent-time">
            {{ row.runTimeMs == null ? '—' : `${row.runTimeMs} ms` }}
          </span>
          <span class="recent-date">{{ formatTime(row.createTime) }}</span>
        </button>
      </div>

      <div v-else class="recent-empty">
        <span class="empty-mark">◎</span>
        <h3>还没有提交记录</h3>
        <p>提交后这里会显示判题结果。</p>
        <button class="btn-primary" type="button" @click="router.push('/question')">
          去挑一道题
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: 34px 24px 76px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 26px;
}

.page-eyebrow {
  display: none;
  color: #3d5a78;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.page-title {
  margin: 8px 0 10px;
  color: #0f172a;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.12;
}

.page-subtitle {
  max-width: 620px;
  margin: 0;
  color: #64748b;
  font-size: 13.5px;
  line-height: 1.8;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.date-chip {
  padding: 8px 11px;
  border: 1px solid #e8edf5;
  border-radius: 7px;
  background: #fff;
  color: #7c869c;
  font-size: 11.5px;
  white-space: nowrap;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 15px;
  border: 0;
  border-radius: 8px;
  background: #1f3a5f;
  color: #fff;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.btn-primary:hover {
  background: #152a45;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #f1dddb;
  border-radius: 8px;
  background: #fbf7f7;
  color: #b91c1c;
  font-size: 12px;
}

.error-banner button {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: #b91c1c;
  font-family: inherit;
  font-size: 12px;
  text-decoration: underline;
  cursor: pointer;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.metric-card {
  padding: 17px 18px 14px;
  border: 1px solid #e8edf5;
  border-radius: 13px;
  background: #fff;
}

.metric-label {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #7c869c;
  font-size: 11.5px;
}

.metric-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.tone-indigo {
  background: #3d5a78;
}

.tone-mint {
  background: #2f7d5b;
}

.tone-rose {
  background: #9f2d2d;
}

.metric-number {
  margin-top: 11px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 740;
  letter-spacing: -0.04em;
  line-height: 1;
}

.metric-number small {
  margin-left: 3px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
}

.metric-foot {
  margin-top: 8px;
  color: #a0a9ba;
  font-size: 10px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(280px, 0.7fr);
  gap: 14px;
  margin-bottom: 14px;
}

.panel {
  border: 1px solid #e8edf5;
  border-radius: 13px;
  background: #fff;
}

.chart-card {
  min-width: 0;
  padding: 17px 18px 10px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  margin: 0;
  color: #1e293b;
  font-size: 14px;
  font-weight: 650;
}

.panel-subtitle {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 10.5px;
}

.panel-chip {
  padding: 3px 8px;
  border: 1px solid #eef2f7;
  border-radius: 999px;
  color: #a0a9ba;
  font-size: 10px;
  white-space: nowrap;
}

.chart-wrap {
  padding-top: 4px;
}

.chart {
  width: 100%;
  height: 292px;
}

.insight-card {
  display: flex;
  flex-direction: column;
  padding: 17px 18px 16px;
}

.weak-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
  margin-top: 18px;
}

.weak-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.weak-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.weak-name {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.weak-value {
  color: #1f3a5f;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
}

.weak-track {
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: #f1f4f9;
}

.weak-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #6e8299;
}

.weak-foot {
  color: #a0a9ba;
  font-size: 9.5px;
}

.weak-empty {
  margin-top: 18px;
  padding: 16px 0;
  color: #a0a9ba;
  font-size: 11px;
  line-height: 1.7;
}

.insight-note {
  margin-top: auto;
  padding-top: 18px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.7;
}

.panel-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 0 0;
  border: 0;
  border-top: 1px solid #f1f4f9;
  background: transparent;
  color: #1f3a5f;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.panel-link:hover {
  color: #152a45;
}

.recent-card {
  overflow: hidden;
  padding: 17px 18px 8px;
}

.recent-table {
  margin-top: 14px;
}

.recent-head,
.recent-row {
  display: grid;
  grid-template-columns: minmax(200px, 1.8fr) 70px 100px 80px 110px;
  align-items: center;
  gap: 12px;
}

.recent-head {
  padding: 0 10px 8px;
  color: #a0a9ba;
  font-size: 10px;
}

.recent-row {
  width: 100%;
  min-height: 42px;
  padding: 0 10px;
  border: 0;
  border-top: 1px solid #f4f6fa;
  background: transparent;
  color: #475569;
  font-family: inherit;
  font-size: 11.5px;
  text-align: left;
  cursor: pointer;
}

.recent-row:hover {
  background: #fbfcfd;
}

.recent-title {
  overflow: hidden;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-lang,
.recent-time,
.recent-date {
  color: #94a3b8;
  font-size: 10.5px;
}

.status-pill {
  display: inline-flex;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 10px;
  line-height: 1.5;
}

.status-pill.ok {
  background: #edf5f1;
  color: #2f7d5b;
}

.status-pill.danger {
  background: #f7efee;
  color: #9f2d2d;
}

.status-pill.warn {
  background: #f7f3ea;
  color: #b45309;
}

.status-pill.muted {
  background: #f8fafc;
  color: #64748b;
}

.recent-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 42px 20px 38px;
  text-align: center;
}

.empty-mark {
  color: #c8d2dc;
  font-size: 28px;
}

.recent-empty h3 {
  margin: 9px 0 5px;
  color: #334155;
  font-size: 14px;
  font-weight: 650;
}

.recent-empty p {
  margin: 0 0 14px;
  color: #94a3b8;
  font-size: 11px;
}

@media (max-width: 1000px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .insight-card {
    min-height: 230px;
  }
}

@media (max-width: 760px) {
  .dashboard-page {
    padding: 24px 14px 52px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
    padding-bottom: 20px;
  }

  .page-title {
    font-size: 36px;
  }

  .page-subtitle {
    font-size: 12.5px;
  }

  .head-actions {
    width: 100%;
  }

  .date-chip {
    flex: 1;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .metric-card {
    padding: 14px 12px 12px;
  }

  .metric-number {
    font-size: 23px;
  }

  .metric-foot {
    line-height: 1.5;
  }

  .chart-card,
  .insight-card,
  .recent-card {
    padding-right: 14px;
    padding-left: 14px;
  }

  .chart {
    height: 250px;
  }

  .recent-head {
    display: none;
  }

  .recent-row {
    grid-template-columns: minmax(0, 1fr) 58px 92px;
    gap: 8px;
    padding: 10px 0;
  }

  .recent-row > span:nth-child(4),
  .recent-row > span:nth-child(5) {
    display: none;
  }
}

/* 2026-11 layout pass: fewer surfaces, larger type, one analytics surface. */
.dashboard-page {
  max-width: 1200px;
  padding: 36px 24px 72px;
}

.page-head {
  align-items: flex-end;
  padding-bottom: 28px;
}

.page-title {
  margin: 0 0 8px;
  color: #111827;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.15;
}

.page-subtitle {
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
}

.head-actions {
  gap: 16px;
}

.date-text {
  color: #6b7280;
  font-size: 13px;
  white-space: nowrap;
}

.btn-primary {
  min-height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 13px;
}

.metric-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 24px 0 22px;
}

.metric-card {
  padding: 22px 24px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: none;
}

.metric-label {
  gap: 8px;
  color: #4b5563;
  font-size: 13px;
}

.metric-number {
  margin-top: 14px;
  color: #111827;
  font-size: 40px;
  font-weight: 700;
  letter-spacing: -0.035em;
}

.metric-number small {
  margin-left: 4px;
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
}

.metric-foot {
  margin-top: 10px;
  color: #9ca3af;
  font-size: 12px;
}

.analytics-surface {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.72fr);
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.chart-section {
  min-width: 0;
  padding: 24px;
  border-right: 1px solid #e5e7eb;
}

.insight-section {
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.section-head {
  margin-bottom: 18px;
}

.section-title {
  margin: 0;
  color: #111827;
  font-size: 16px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.section-caption {
  margin: 5px 0 0;
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.5;
}

.chart {
  height: 300px;
}

.weak-list {
  gap: 18px;
  margin-top: 2px;
}

.weak-name {
  color: #374151;
  font-size: 14px;
}

.weak-value {
  color: #1f3a5f;
  font-size: 12px;
}

.weak-track {
  height: 5px;
  background: #eef0f3;
}

.weak-foot {
  color: #9ca3af;
  font-size: 12px;
}

.weak-empty {
  color: #9ca3af;
  font-size: 13px;
}

.insight-note {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #eef0f3;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.6;
}

.text-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  margin-top: 10px;
  padding: 10px 0 0;
  border: 0;
  background: transparent;
  color: #1f3a5f;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.text-link:hover {
  color: #152a45;
}

.recent-section {
  margin-top: 30px;
  padding-top: 22px;
  border-top: 1px solid #e5e7eb;
}

.recent-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.recent-count {
  color: #9ca3af;
  font-size: 12px;
  white-space: nowrap;
}

.recent-table {
  margin-top: 14px;
}

.recent-head {
  padding: 0 10px 10px;
  border-bottom: 1px solid #e5e7eb;
  color: #9ca3af;
  font-size: 12px;
}

.recent-row {
  min-height: 48px;
  padding: 0 10px;
  border: 0;
  border-bottom: 1px solid #eef0f3;
  background: transparent;
  color: #4b5563;
  font-size: 13px;
}

.recent-row:hover {
  background: #fff;
}

.recent-title {
  color: #111827;
  font-size: 14px;
}

.recent-lang,
.recent-time,
.recent-date {
  color: #6b7280;
  font-size: 12px;
}

.status-pill {
  padding: 2px 8px;
  font-size: 11px;
}

.recent-empty {
  padding: 38px 20px 30px;
}

.recent-empty h3 {
  font-size: 15px;
}

.recent-empty p {
  font-size: 12px;
}

@media (max-width: 1000px) {
  .analytics-surface {
    grid-template-columns: 1fr;
  }

  .chart-section {
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }
}

@media (max-width: 760px) {
  .dashboard-page {
    padding: 24px 14px 52px;
  }

  .page-head {
    align-items: flex-start;
  }

  .page-title {
    font-size: 30px;
  }

  .page-subtitle {
    font-size: 13px;
  }

  .metric-grid {
    gap: 12px;
    margin-top: 20px;
  }

  .metric-card {
    padding: 18px 16px 16px;
  }

  .metric-label {
    font-size: 12px;
  }

  .metric-number {
    font-size: 32px;
  }

  .metric-foot {
    font-size: 11px;
  }

  .chart-section,
  .insight-section {
    padding: 18px;
  }

  .chart {
    height: 250px;
  }

  .recent-section {
    margin-top: 24px;
  }

  .recent-row {
    grid-template-columns: minmax(0, 1fr) 58px 92px;
    gap: 8px;
    padding: 10px 0;
  }
}

@media (min-width: 761px) {
  .page-head {
    justify-content: flex-end;
  }
}

.chart-head {
  align-items: center;
}

.chart-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #8b95a5;
  font-size: 11px;
}

.chart-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.legend-submit {
  background: #557596;
}

.legend-ac {
  background: #2f7d5b;
}

.week-highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0 4px;
}

.highlight-card {
  position: relative;
  min-height: 106px;
  padding: 12px 13px 10px;
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fbfcfd;
}

.highlight-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7c869c;
  font-size: 11px;
}

.highlight-top small {
  color: #a0a9ba;
  font-size: 10px;
}

.highlight-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1;
}

.highlight-card strong small {
  margin-left: 3px;
  color: #8b95a5;
  font-size: 11px;
  font-weight: 500;
}

.highlight-card p {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 10.5px;
}

.spark-bars {
  position: absolute;
  right: 12px;
  bottom: 10px;
  display: flex;
  align-items: flex-end;
  gap: 3px;
  width: 84px;
  height: 34px;
  opacity: 0.8;
}

.spark-bars i {
  flex: 1;
  min-height: 3px;
  border-radius: 2px 2px 0 0;
  background: #8aa4bd;
}

.spark-bars.is-ac i {
  background: #6f9a84;
}

.rhythm-dots {
  position: absolute;
  right: 12px;
  bottom: 14px;
  display: flex;
  gap: 4px;
}

.rhythm-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #dce3ec;
}

.rhythm-dots i.is-active {
  background: #557596;
}

@media (max-width: 760px) {
  .week-highlights {
    grid-template-columns: 1fr;
  }

  .highlight-card {
    min-height: 92px;
  }
}
</style>
