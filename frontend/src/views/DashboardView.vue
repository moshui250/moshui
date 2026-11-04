<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  Collection,
  DataAnalysis,
  InfoFilled,
  Medal,
  TrendCharts
} from '@element-plus/icons-vue'
import { getProfileStat } from '@/api/profile'
import { demoKnowledgePoints, emptyProfileStat } from '@/data/demo'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const stat = reactive({ ...emptyProfileStat })
const loading = ref(true)
const statError = ref('')
const chartRef = ref(null)

let chartInstance = null

const hasData = computed(() => Number(stat.totalSubmit) > 0)
const accuracyText = computed(() => `${Math.round((Number(stat.accuracy) || 0) * 100)}%`)

const knowledgeStats = computed(() => {
  if (Array.isArray(stat.knowledgeStats) && stat.knowledgeStats.length) {
    return stat.knowledgeStats
  }
  return demoKnowledgePoints.map((item) => ({
    knowledgePointId: item.id,
    name: item.name,
    submitCount: 0,
    acCount: 0,
    errorRate: 0,
    hasData: false
  }))
})

const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const metrics = computed(() => [
  {
    label: '累计提交',
    value: stat.totalSubmit,
    unit: '次',
    icon: DataAnalysis,
    tone: 'indigo'
  },
  {
    label: '已通过题目',
    value: stat.solvedCount,
    unit: '道',
    icon: Medal,
    tone: 'emerald'
  },
  {
    label: '提交正确率',
    value: accuracyText.value,
    unit: '',
    icon: TrendCharts,
    tone: 'amber'
  },
  {
    label: '错题数量',
    value: stat.wrongCount,
    unit: '道',
    icon: Collection,
    tone: 'rose'
  }
])

async function loadStat() {
  loading.value = true
  statError.value = ''
  try {
    const data = await getProfileStat()
    Object.assign(stat, emptyProfileStat, data || {})
  } catch (error) {
    statError.value = error.message || '数据看板接口暂未接入'
    Object.assign(stat, emptyProfileStat)
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

  const labels = knowledgeStats.value.map((item) => item.name)
  const values = knowledgeStats.value.map((item) =>
    Number(((Number(item.errorRate) || 0) * 100).toFixed(1))
  )

  chartInstance.setOption({
    animationDuration: 500,
    grid: { left: 36, right: 16, top: 20, bottom: 30 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params[0]
        return `${item.name}<br/>错误率 ${item.value}%`
      }
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e8edf5' } },
      axisLabel: { color: '#7c869c', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: {
        color: '#a0a9ba',
        fontSize: 10,
        formatter: '{value}%'
      },
      splitLine: { lineStyle: { color: '#f1f4f9' } }
    },
    series: [
      {
        name: '错误率',
        type: 'bar',
        data: values,
        barWidth: 26,
        showBackground: true,
        backgroundStyle: { color: '#f4f6fb', borderRadius: [6, 6, 0, 0] },
        itemStyle: {
          color: '#6366f1',
          borderRadius: [6, 6, 0, 0]
        }
      }
    ]
  })
  chartInstance.resize()
}

function pathWidth(item) {
  if (!item.hasData) return '0%'
  const completed = Math.max(8, 100 - (Number(item.errorRate) || 0) * 100)
  return `${Math.min(100, completed)}%`
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
    <div class="page-head">
      <div>
        <div class="page-eyebrow">Learning Analytics</div>
        <h1 class="page-title">数据看板</h1>
        <p class="page-subtitle">
          用提交记录还原学习轨迹：正确率、薄弱知识点和最近的练习状态。
        </p>
      </div>
      <div class="date-chip">{{ today }}</div>
    </div>

    <div v-if="statError" class="demo-banner">
      <el-icon><InfoFilled /></el-icon>
      <span>数据看板接口尚未接入（S14），当前展示零值空状态。</span>
    </div>

    <section v-loading="loading" class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="panel metric-card">
        <div class="metric-top">
          <span>{{ metric.label }}</span>
          <div class="metric-icon" :class="`tone-${metric.tone}`">
            <el-icon><component :is="metric.icon" /></el-icon>
          </div>
        </div>
        <div class="metric-value">
          {{ metric.value }}<span v-if="metric.unit">{{ metric.unit }}</span>
        </div>
        <div class="metric-foot">来自 submit_record 聚合</div>
      </article>
    </section>

    <div class="dashboard-grid">
      <section class="panel chart-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">知识点错误率</div>
            <div class="panel-subtitle">错误率越高，越值得优先复盘</div>
          </div>
          <el-tag type="info" effect="plain" round size="small">近 30 天</el-tag>
        </div>
        <div class="chart-wrap">
          <div ref="chartRef" class="chart"></div>
          <div v-if="!hasData && !loading" class="chart-empty">
            <div class="chart-empty-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <strong>还没有足够的数据</strong>
            <span>完成首次提交后，这里会生成知识点错误率</span>
          </div>
        </div>
      </section>

      <section class="panel path-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">学习路径</div>
            <div class="panel-subtitle">5 个核心知识点</div>
          </div>
          <span class="path-count">0 / 5</span>
        </div>
        <div class="path-list">
          <div v-for="(item, index) in knowledgeStats" :key="item.knowledgePointId || index" class="path-item">
            <div class="path-index">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="path-main">
              <div class="path-name">{{ item.name }}</div>
              <div class="path-bar">
                <span :style="{ width: pathWidth(item) }"></span>
              </div>
            </div>
            <span class="path-status">{{ item.hasData ? '练习中' : '未开始' }}</span>
          </div>
        </div>
      </section>
    </div>

    <section class="panel recent-panel">
      <div class="panel-head">
        <div>
          <div class="panel-title">最近提交</div>
          <div class="panel-subtitle">最近 7 天的判题记录</div>
        </div>
        <el-button link type="primary">查看全部</el-button>
      </div>
      <el-empty description="暂无提交记录，去题库开始第一道练习" :image-size="80">
        <el-button type="primary" plain @click="$router.push('/question')">去题库</el-button>
      </el-empty>
    </section>
  </div>
</template>

<style scoped>
.date-chip {
  padding: 8px 12px;
  border: 1px solid var(--ca-line);
  border-radius: 8px;
  background: #fff;
  color: #7c869c;
  font-size: 11.5px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.metric-card {
  padding: 17px 18px 15px;
}

.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7c869c;
  font-size: 11.5px;
}

.metric-icon {
  display: grid;
  place-items: center;
  width: 29px;
  height: 29px;
  border-radius: 8px;
  font-size: 15px;
}

.tone-indigo {
  background: #eef2ff;
  color: #4f46e5;
}

.tone-emerald {
  background: #ecfdf5;
  color: #059669;
}

.tone-amber {
  background: #fffbeb;
  color: #d97706;
}

.tone-rose {
  background: #fff1f2;
  color: #e11d48;
}

.metric-value {
  margin-top: 12px;
  color: var(--ca-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.9px;
  line-height: 1;
}

.metric-value span {
  margin-left: 3px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
}

.metric-foot {
  margin-top: 9px;
  color: #a3adc2;
  font-size: 10px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.chart-panel,
.path-panel {
  overflow: hidden;
}

.chart-wrap {
  position: relative;
  padding: 8px 8px 12px;
}

.chart {
  width: 100%;
  height: 278px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  pointer-events: none;
}

.chart-empty-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  margin-bottom: 4px;
  border-radius: 10px;
  background: #f4f6fb;
  color: #a5b4fc;
  font-size: 18px;
}

.chart-empty strong {
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
}

.chart-empty span {
  color: #a0a9ba;
  font-size: 11px;
}

.path-count {
  color: #94a3b8;
  font-size: 11px;
}

.path-list {
  padding: 8px 18px 12px;
}

.path-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f4f9;
}

.path-item:last-child {
  border-bottom: 0;
}

.path-index {
  color: #c2cad8;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
}

.path-main {
  flex: 1;
  min-width: 0;
}

.path-name {
  color: #334155;
  font-size: 12px;
  font-weight: 550;
}

.path-bar {
  height: 4px;
  margin-top: 7px;
  overflow: hidden;
  border-radius: 4px;
  background: #f1f4f9;
}

.path-bar span {
  display: block;
  height: 100%;
  border-radius: 4px;
  background: #818cf8;
}

.path-status {
  color: #a3adc2;
  font-size: 10px;
}

.recent-panel {
  overflow: hidden;
  padding-bottom: 8px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .date-chip {
    display: none;
  }
}
</style>
