<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  CircleCheck,
  InfoFilled,
  RefreshRight,
  View
} from '@element-plus/icons-vue'
import hljs from 'highlight.js/lib/core'
import cpp from 'highlight.js/lib/languages/cpp'
import java from 'highlight.js/lib/languages/java'
import DOMPurify from 'dompurify'
import 'highlight.js/styles/github.css'
import { listWrongQuestions, markMastered } from '@/api/profile'
import { recommendQuestions } from '@/api/question'

hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('java', java)

const router = useRouter()
const loading = ref(false)
const recLoading = ref(false)
const loadError = ref('')
const rows = ref([])
const pendingTotal = ref(0)
const masteredTotal = ref(0)
const activeFilter = ref('pending')
const expanded = reactive({})
const recommendations = reactive({})

const difficultyMeta = {
  1: { label: '入门', className: 'easy' },
  2: { label: '进阶', className: 'medium' },
  3: { label: '挑战', className: 'hard' }
}

const statusMeta = {
  AC: '通过',
  WA: '答案错误',
  TLE: '运行超时',
  MLE: '内存超限',
  RE: '运行错误',
  CE: '编译错误',
  SE: '系统错误'
}

const masteryRate = computed(() => {
  const total = pendingTotal.value + masteredTotal.value
  return total ? Math.round((masteredTotal.value / total) * 100) : 0
})

const currentEmptyText = computed(() => {
  if (activeFilter.value === 'mastered') return '还没有已掌握的错题'
  return '待复习的错题已经清空'
})

function questionOf(row) {
  return row.question || {}
}

function titleOf(row) {
  return questionOf(row).title || `题目 ${questionOf(row).id || row.questionId}`
}

function knowledgeOf(row) {
  return questionOf(row).knowledgePoint?.name || '未分类'
}

function difficultyOf(row) {
  return difficultyMeta[questionOf(row).difficulty] || difficultyMeta[1]
}

function lastSubmitOf(row) {
  return row.lastSubmit || null
}

function codeOf(row) {
  const submit = lastSubmitOf(row)
  return submit?.code || submit?.codeSnippet || ''
}

function languageOf(row) {
  return lastSubmitOf(row)?.language === 'JAVA' ? 'java' : 'cpp'
}

function languageLabel(row) {
  return languageOf(row) === 'java' ? 'Java' : 'C++'
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).slice(5, 16)
}

function highlightCode(row) {
  const code = codeOf(row)
  if (!code) {
    return '<span class="code-empty">这条错题没有保留提交代码</span>'
  }
  try {
    return DOMPurify.sanitize(
      hljs.highlight(code, { language: languageOf(row) }).value
    )
  } catch {
    return DOMPurify.sanitize(hljs.highlightAuto(code).value)
  }
}

function toggleCode(row) {
  expanded[row.id] = !expanded[row.id]
}

async function loadCounts() {
  const results = await Promise.allSettled([
    listWrongQuestions({ page: 1, size: 1, mastered: false }),
    listWrongQuestions({ page: 1, size: 1, mastered: true })
  ])
  if (results[0].status === 'fulfilled') {
    pendingTotal.value = results[0].value?.total || 0
  }
  if (results[1].status === 'fulfilled') {
    masteredTotal.value = results[1].value?.total || 0
  }
}

async function loadRecommendations() {
  if (!rows.value.length) {
    recLoading.value = false
    return
  }
  recLoading.value = true
  const results = await Promise.allSettled(
    rows.value.map((row) => recommendQuestions(questionOf(row).id, 3))
  )
  results.forEach((result, index) => {
    const row = rows.value[index]
    recommendations[row.id] =
      result.status === 'fulfilled' ? result.value || [] : []
  })
  recLoading.value = false
}

async function loadRows() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listWrongQuestions({
      page: 1,
      size: 20,
      mastered: activeFilter.value === 'mastered'
    })
    rows.value = data?.records || []
    if (activeFilter.value === 'mastered') {
      masteredTotal.value = data?.total || 0
    } else {
      pendingTotal.value = data?.total || 0
    }
    await Promise.all([loadCounts(), loadRecommendations()])
  } catch (error) {
    loadError.value = error.message || '错题本加载失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

function changeFilter(value) {
  if (activeFilter.value === value) return
  activeFilter.value = value
  Object.keys(expanded).forEach((key) => delete expanded[key])
  loadRows()
}

function goSolve(row) {
  router.push(`/question/${questionOf(row).id}/solve`)
}

function goRecommend(item) {
  router.push(`/question/${item.id}/solve`)
}

async function handleMastered(row) {
  try {
    await markMastered(questionOf(row).id)
    ElMessage.success('已标记为掌握，移入已掌握列表')
    if (activeFilter.value === 'pending') {
      rows.value = rows.value.filter((item) => item.id !== row.id)
      pendingTotal.value = Math.max(0, pendingTotal.value - 1)
    }
    masteredTotal.value += 1
  } catch (error) {
    ElMessage.error(error.message || '操作失败，请稍后再试')
  }
}

onMounted(loadRows)
</script>

<template>
  <div class="wrong-page">
    <header class="page-head">
      <div class="head-copy">
        <h1 class="page-title">错题本</h1>
      </div>
      <button class="btn-primary" type="button" @click="router.push('/question')">
        去题库练习
        <el-icon><ArrowRight /></el-icon>
      </button>
    </header>

    <section class="review-strip" aria-label="错题概览">
      <div class="strip-metric">
        <span class="metric-label">待复习</span>
        <strong class="metric-value">{{ pendingTotal }}</strong>
      </div>
      <div class="strip-divider" />
      <div class="strip-metric">
        <span class="metric-label">已掌握</span>
        <strong class="metric-value">{{ masteredTotal }}</strong>
      </div>
      <div class="strip-divider" />
      <div class="strip-metric">
        <span class="metric-label">复习进度</span>
        <strong class="metric-value">{{ masteryRate }}<small>%</small></strong>
      </div>
      <div class="strip-note">
        <el-icon><CircleCheck /></el-icon>
        <span>每道错题配 3 道同类题</span>
      </div>
    </section>

    <div class="filter-bar">
      <div class="filter-tabs" role="tablist" aria-label="错题状态">
        <button
          class="filter-tab"
          :class="{ active: activeFilter === 'pending' }"
          type="button"
          role="tab"
          :aria-selected="activeFilter === 'pending'"
          @click="changeFilter('pending')"
        >
          待复习
          <span>{{ pendingTotal }}</span>
        </button>
        <button
          class="filter-tab"
          :class="{ active: activeFilter === 'mastered' }"
          type="button"
          role="tab"
          :aria-selected="activeFilter === 'mastered'"
          @click="changeFilter('mastered')"
        >
          已掌握
          <span>{{ masteredTotal }}</span>
        </button>
      </div>
    </div>

    <div v-if="loadError" class="error-banner">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ loadError }}</span>
      <button type="button" @click="loadRows">重试</button>
    </div>

    <section v-loading="loading" class="wrong-list">
      <div v-if="!loading && !rows.length" class="empty-card">
        <div class="empty-mark">✓</div>
        <h2>{{ currentEmptyText }}</h2>
        <button class="btn-primary" type="button" @click="router.push('/question')">
          去题库
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <article
        v-for="(row, index) in rows"
        :key="row.id"
        v-reveal="{ delay: index * 35 }"
        class="wrong-card"
      >
        <div class="card-top">
          <div class="card-copy">
            <div class="card-kicker">{{ knowledgeOf(row) }}</div>
            <button class="card-title" type="button" @click="goSolve(row)">
              {{ titleOf(row) }}
            </button>
            <div class="card-meta">
              <span class="difficulty" :class="difficultyOf(row).className">
                <i />
                {{ difficultyOf(row).label }}
              </span>
              <span>答错 {{ row.wrongCount }} 次</span>
              <span>最近 {{ formatTime(row.lastWrongTime) }}</span>
            </div>
          </div>
          <div class="card-actions">
            <button class="btn-small" type="button" @click="goSolve(row)">
              <el-icon><RefreshRight /></el-icon>
              去重做
            </button>
            <button
              v-if="!row.mastered"
              class="btn-text"
              type="button"
              @click="handleMastered(row)"
            >
              标记已掌握
            </button>
          </div>
        </div>

        <button
          class="code-toggle"
          type="button"
          :aria-expanded="!!expanded[row.id]"
          @click="toggleCode(row)"
        >
          <el-icon><View /></el-icon>
          {{ expanded[row.id] ? '收起错误代码' : '查看上次错误代码' }}
          <span v-if="lastSubmitOf(row)?.status" class="code-status">
            {{ statusMeta[lastSubmitOf(row).status] || lastSubmitOf(row).status }}
          </span>
        </button>

        <div v-if="expanded[row.id]" class="code-panel">
          <div class="code-head">
            <span>{{ languageLabel(row) }}</span>
            <span>上次提交</span>
          </div>
          <pre class="code-block"><code v-html="highlightCode(row)"></code></pre>
        </div>

        <div class="similar-block">
          <div class="similar-head">
            <div>
              <span class="similar-title">同类练习</span>
            </div>
            <span class="similar-count">
              {{ (recommendations[row.id] || []).length }} / 3
            </span>
          </div>

          <div v-if="recLoading" class="similar-loading">正在匹配同类题…</div>
          <div v-else-if="(recommendations[row.id] || []).length" class="similar-grid">
            <button
              v-for="(item, index) in recommendations[row.id]"
              :key="item.id"
              class="similar-card"
              type="button"
              @click="goRecommend(item)"
            >
              <span class="similar-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="similar-name">{{ item.title }}</span>
              <span class="similar-meta">
                {{ item.knowledgePointName || knowledgeOf(row) }} ·
                {{ difficultyMeta[item.difficulty]?.label || '入门' }}
              </span>
              <span class="similar-reason">{{ item.reason || '同类巩固' }}</span>
              <span class="similar-arrow">→</span>
            </button>
          </div>
          <div v-else class="similar-loading">暂无同类题。</div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.wrong-page {
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

.btn-primary,
.btn-small,
.btn-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 0;
  font-family: inherit;
  cursor: pointer;
}

.btn-primary {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 8px;
  background: #1f3a5f;
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  transition: background 0.16s;
}

.btn-primary:hover {
  background: #152a45;
}

.review-strip {
  display: flex;
  align-items: center;
  gap: 28px;
  min-height: 84px;
  padding: 18px 24px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fff;
}

.strip-metric {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 76px;
}

.metric-label {
  color: #94a3b8;
  font-size: 11px;
}

.metric-value {
  color: #0f172a;
  font-size: 26px;
  font-weight: 720;
  letter-spacing: -0.04em;
  line-height: 1.1;
}

.metric-value small {
  margin-left: 1px;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}

.strip-divider {
  width: 1px;
  height: 30px;
  background: #eef2f7;
}

.strip-note {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-left: auto;
  padding: 8px 11px;
  border-radius: 7px;
  background: #f7f9fb;
  color: #7c869c;
  font-size: 11px;
}

.strip-note .el-icon {
  color: #2f7d5b;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 24px 0 12px;
}

.filter-tabs {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid #e8edf5;
  border-radius: 9px;
  background: #fff;
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.filter-tab span {
  min-width: 17px;
  padding: 1px 5px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 10px;
  line-height: 1.5;
  text-align: center;
}

.filter-tab.active {
  background: #eef2f6;
  color: #152a45;
  font-weight: 600;
}

.filter-tab.active span {
  background: #dfe6ed;
  color: #1f3a5f;
}

.sort-hint {
  color: #a0a9ba;
  font-size: 11px;
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

.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wrong-card {
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fff;
  padding: 18px 20px 16px;
  transition: border-color 0.16s, box-shadow 0.16s;
}

.wrong-card:hover {
  border-color: #d9e0ec;
  box-shadow: 0 8px 24px rgb(15 23 42 / 4%);
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.card-copy {
  min-width: 0;
}

.card-kicker {
  color: #94a3b8;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.card-title {
  display: block;
  margin: 5px 0 7px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1e293b;
  font-family: inherit;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.35;
  text-align: left;
  cursor: pointer;
}

.card-title:hover {
  color: #1f3a5f;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 13px;
  color: #94a3b8;
  font-size: 11px;
}

.difficulty {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.difficulty i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.difficulty.easy {
  color: #2f7d5b;
}

.difficulty.medium {
  color: #a16207;
}

.difficulty.hard {
  color: #b42318;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.btn-small {
  min-height: 31px;
  padding: 0 11px;
  border: 1px solid #dbe3ef;
  border-radius: 7px;
  background: #fff;
  color: #152a45;
  font-size: 11.5px;
  font-weight: 600;
}

.btn-small:hover {
  border-color: #c8d2dc;
  background: #f7f9fb;
}

.btn-text {
  min-height: 31px;
  padding: 0 7px;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  font-size: 11.5px;
}

.btn-text:hover {
  background: #f8fafc;
  color: #334155;
}

.code-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 13px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #6e8299;
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
}

.code-toggle:hover {
  color: #1f3a5f;
}

.code-status {
  margin-left: 3px;
  padding: 1px 6px;
  border-radius: 999px;
  background: #f7efee;
  color: #9f2d2d;
  font-size: 10px;
}

.code-panel {
  margin-top: 10px;
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 9px;
  background: #fbfcfe;
}

.code-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 12px;
  border-bottom: 1px solid #eef2f7;
  color: #64748b;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.code-head span:last-child {
  color: #a0a9ba;
  font-weight: 400;
  letter-spacing: 0;
}

.code-block {
  max-height: 260px;
  margin: 0;
  padding: 13px 15px;
  overflow: auto;
  color: #334155;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11.5px;
  line-height: 1.72;
  tab-size: 4;
}

.code-block :deep(code) {
  font-family: inherit;
  background: transparent;
}

.code-empty {
  color: #94a3b8;
}

.similar-block {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e8edf5;
}

.similar-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.similar-title {
  color: #475569;
  font-size: 11.5px;
  font-weight: 600;
}

.similar-sub {
  margin-left: 7px;
  color: #a0a9ba;
  font-size: 10px;
}

.similar-count {
  color: #a0a9ba;
  font-size: 10px;
}

.similar-loading {
  margin-top: 9px;
  color: #a0a9ba;
  font-size: 11px;
}

.similar-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 8px;
}

.similar-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-height: 72px;
  padding: 9px 11px 9px 30px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fcfdff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.16s, background 0.16s;
}

.similar-card:hover {
  border-color: #c8d2dc;
  background: #f5f7f8;
}

.similar-index {
  position: absolute;
  top: 10px;
  left: 10px;
  color: #c8d2dc;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 9px;
  letter-spacing: 0.02em;
}

.similar-name {
  overflow: hidden;
  color: #334155;
  font-size: 11.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.similar-meta {
  overflow: hidden;
  color: #94a3b8;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.similar-reason {
  overflow: hidden;
  color: #6e8299;
  font-size: 9.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.similar-arrow {
  position: absolute;
  right: 10px;
  bottom: 8px;
  color: #c8d2dc;
  font-size: 12px;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 24px 58px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fff;
  text-align: center;
}

.empty-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  background: #edf5f1;
  color: #2f7d5b;
  font-size: 18px;
  font-weight: 700;
}

.empty-card h2 {
  margin: 14px 0 7px;
  color: #1e293b;
  font-size: 16px;
  font-weight: 650;
}

.empty-card p {
  max-width: 420px;
  margin: 0 0 18px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .review-strip {
    flex-wrap: wrap;
    gap: 18px;
  }

  .strip-note {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 760px) {
  .wrong-page {
    padding: 24px 14px 52px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
    padding-bottom: 20px;
  }

  .page-title {
    font-size: 36px;
  }

  .page-subtitle {
    font-size: 12.5px;
  }

  .review-strip {
    gap: 14px;
    padding: 15px 16px;
  }

  .strip-metric {
    min-width: 62px;
  }

  .metric-value {
    font-size: 22px;
  }

  .strip-divider {
    height: 26px;
  }

  .filter-bar {
    margin-top: 18px;
  }

  .sort-hint {
    display: none;
  }

  .card-top {
    flex-direction: column;
    gap: 10px;
  }

  .card-actions {
    width: 100%;
  }

  .btn-small {
    flex: 1;
  }

  .similar-grid {
    grid-template-columns: 1fr;
  }
}
</style>
