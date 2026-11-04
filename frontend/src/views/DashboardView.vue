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
  TrendCharts
} from '@element-plus/icons-vue'
import { getProfileStat } from '@/api/profile'
import { emptyProfileStat } from '@/data/demo'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const stat = reactive({ ...emptyProfileStat, knowledgeStats: [], recentSubmits: [] })
const loading = ref(true)
const statError = ref('')
const chartRef = ref(null)

let chartInstance = null

const accuracyText = computed(() => `${(Number(stat.accuracy || 0) * 100).toFixed(1)}%`)

const metrics = computed(() => [
  {
    label: '总提交',
    value: stat.totalSubmit,
    unit: '次',
    icon: DataAnalysis,
    tone: 'indigo',
    foot: '全部提交记录，按提交口径'
  },
  {
    label: '正确率',
    value: accuracyText.value,
    unit: '',
    icon: TrendCharts,
    tone: 'amber',
    foot: `AC ${stat.acSubmit} / 总提交 ${stat.totalSubmit}`
  },
  {
    label: '错题数',
    value: stat.wrongCount,
    unit: '道',
    icon: Collection,
    tone: 'rose',
    foot: '仅统计未掌握的错题'
  }
])

const statusMeta = {
  AC: { label: '通过', type: 'success' },
  WA: { label: '答案错误', type: 'danger' },
  TLE: { label: '超时', type: 'warning' },
  MLE: { label: '内存超限', type: 'warning' },
  RE: { label: '运行错误', type: 'danger' },
  CE: { label: '编译错误', type: 'danger' },
  SE: { label: '系统错误', type: 'info' },
  PENDING: { label: '排队中', type: 'info' },
  JUDGING: { label: '判题中', type: 'info' }
}

const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

async function loadStat() {
  loading.value = true
  statError.value = ''
  try {
    const data = await getProfileStat()
    Object.assign(stat, emptyProfileStat, data || {})
    stat.knowledgeStats = data?.knowledgeStats || []
    stat.recentSubmits = data?.recentSubmits || []
  } catch (error) {
    statError.value = error.message || '数据看板加载失败，请稍后重试'
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

  const items = stat.knowledgeStats || []
  const labels = items.map((item) => item.name)
  const values = items.map((item) => {
    if (!item.hasData) {
      return {
        value: 0,
        hasData: false,
        label: {
          show: true,
          position: 'top',
          formatter: '—',
          color: '#94a3b8',
          fontSize: 12
        }
      }
    }
    return { value: Number((Number(item.errorRate) * 100).toFixed(1)) }
  })

  chartInstance.setOption({
    animationDuration: 350,
    grid: { left: 42, right: 18, top: 28, bottom: 34 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params[0]
        if (item.data?.hasData === false) return `${item.name}<br/>暂无提交数据`
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
      name: '错误率',
      nameTextStyle: { color: '#a0a9ba', fontSize: 10, padding: [0, 0, 4, 0] },
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
        barWidth: 28,
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

function handleResize() {
  chartInstance?.resize()
}

function statusOf(row) {
  return statusMeta[row.status] || { label: row.status || '未知', type: 'info' }
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
          按提交口径还原学习轨迹：正确率、薄弱知识点和最近 5 次练习状态。
        </p>
      </div>
      <div class="date-chip">{{ today }}</div>
    </div>

    <div v-if="statError" class="demo-banner">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ statError }}</span>
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
        <div class="metric-foot">{{ metric.foot }}</div>
      </article>
    </section>

    <section class="panel chart-panel">
      <div class="panel-head">
        <div>
          <div class="panel-title">知识点错误率</div>
          <div class="panel-subtitle">错误率 = 1 - AC 提交数 / 总提交数；无提交的点显示“—”</div>
        </div>
        <el-tag type="info" effect="plain" round size="small">按提交口径</el-tag>
      </div>
      <div class="chart-wrap">
        <div ref="chartRef" class="chart"></div>
      </div>
    </section>

    <section class="panel recent-panel">
      <div class="panel-head">
        <div>
          <div class="panel-title">最近提交</div>
          <div class="panel-subtitle">最近 5 次判题记录</div>
        </div>
        <span class="recent-count">{{ stat.recentSubmits.length }} 条</span>
      </div>

      <el-table v-if="stat.recentSubmits.length" :data="stat.recentSubmits" row-key="id">
        <el-table-column label="题目" min-width="260">
          <template #default="{ row }">
            <span class="recent-title">{{ row.questionTitle || `题目 ${row.questionId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="90">
          <template #default="{ row }">{{ row.language === 'JAVA' ? 'Java' : 'C++' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusOf(row).type" effect="light" round size="small">
              {{ statusOf(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ row.runTimeMs == null ? '—' : `${row.runTimeMs} ms` }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
      </el-table>

      <el-empty
        v-else
        description="暂无提交记录，去题库开始第一道练习"
        :image-size="80"
      >
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
  grid-template-columns: repeat(3, 1fr);
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

.chart-panel {
  margin-bottom: 16px;
  overflow: hidden;
}

.chart-wrap {
  padding: 8px 8px 12px;
}

.chart {
  width: 100%;
  height: 300px;
}

.recent-panel {
  overflow: hidden;
  padding-bottom: 8px;
}

.recent-count {
  color: #94a3b8;
  font-size: 11px;
}

.recent-title {
  color: #334155;
  font-size: 12.5px;
  font-weight: 550;
}

@media (max-width: 1000px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
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
