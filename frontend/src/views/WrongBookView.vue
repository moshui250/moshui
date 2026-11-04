<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Collection,
  InfoFilled,
  RefreshRight,
  Reading,
  View
} from '@element-plus/icons-vue'
import hljs from 'highlight.js/lib/core'
import cpp from 'highlight.js/lib/languages/cpp'
import java from 'highlight.js/lib/languages/java'
import 'highlight.js/styles/github.css'
import {
  listWrongQuestions,
  markMastered
} from '@/api/profile'
import { recommendQuestions } from '@/api/question'

hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('java', java)

const router = useRouter()
const loading = ref(false)
const recLoading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const masteredTotal = ref(0)
const masteredFilter = ref(false)
const expanded = reactive({})
const recommendations = reactive({})

const difficultyMeta = {
  1: { label: '入门', type: 'success' },
  2: { label: '进阶', type: 'warning' },
  3: { label: '挑战', type: 'danger' }
}

const statusMeta = {
  AC: { label: '通过', type: 'success' },
  WA: { label: '答案错误', type: 'danger' },
  TLE: { label: '超时', type: 'warning' },
  MLE: { label: '内存超限', type: 'warning' },
  RE: { label: '运行错误', type: 'danger' },
  CE: { label: '编译错误', type: 'danger' },
  SE: { label: '系统错误', type: 'info' }
}

const emptyText = computed(() =>
  masteredFilter.value ? '还没有已掌握的错题' : '还没有错题，继续保持'
)

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
  const language = lastSubmitOf(row)?.language
  if (language === 'JAVA') return 'java'
  if (language === 'CPP') return 'cpp'
  return 'cpp'
}

function languageLabel(row) {
  return languageOf(row) === 'java' ? 'Java' : 'C++'
}

function highlightCode(row) {
  const code = codeOf(row)
  if (!code) {
    return '<span class="code-empty">这条错题没有保留提交代码</span>'
  }
  try {
    return hljs.highlight(code, { language: languageOf(row) }).value
  } catch {
    return hljs.highlightAuto(code).value
  }
}

function toggleCode(row) {
  expanded[row.id] = !expanded[row.id]
}

async function loadRows() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listWrongQuestions({
      page: 1,
      size: 20,
      mastered: masteredFilter.value
    })
    rows.value = data?.records || []
    total.value = data?.total || 0
    await Promise.all([loadMasteredTotal(), loadRecommendations()])
  } catch (error) {
    loadError.value = error.message || '错题本加载失败'
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadMasteredTotal() {
  try {
    const data = await listWrongQuestions({ page: 1, size: 1, mastered: true })
    masteredTotal.value = data?.total || 0
  } catch {
    masteredTotal.value = 0
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
    recommendations[row.id] = result.status === 'fulfilled' ? result.value || [] : []
  })
  recLoading.value = false
}

function goSolve(row) {
  router.push(`/question/${questionOf(row).id}/solve`)
}

async function handleMastered(row) {
  try {
    await markMastered(questionOf(row).id)
    ElMessage.success('已标记为掌握，已从待复习列表移除')
    rows.value = rows.value.filter((item) => item.id !== row.id)
    total.value = Math.max(0, total.value - 1)
    masteredTotal.value += 1
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function changeFilter(value) {
  masteredFilter.value = value === 'mastered'
  await loadRows()
}

onMounted(loadRows)
</script>

<template>
  <div class="wrong-page">
    <div class="page-head">
      <div>
        <div class="page-eyebrow">Review Space</div>
        <h1 class="page-title">错题本</h1>
        <p class="page-subtitle">
          做错的题不会消失。展开上次错误代码，按知识点复盘，再从同类题里把方法练熟。
        </p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="router.push('/question')">
          <el-icon><Reading /></el-icon>
          去题库练习
        </el-button>
      </div>
    </div>

    <section class="summary-strip">
      <div class="summary-item">
        <span>{{ masteredFilter ? '已掌握' : '待复习' }}</span>
        <strong>{{ total }}</strong>
      </div>
      <div class="summary-item">
        <span>已掌握</span>
        <strong>{{ masteredTotal }}</strong>
      </div>
      <div class="summary-note">
        <el-icon><Collection /></el-icon>
        <span>每条错题自动配 3 道同类题，点击即可继续练习</span>
      </div>
    </section>

    <div class="filter-row">
      <el-radio-group
        :model-value="masteredFilter ? 'mastered' : 'pending'"
        @change="changeFilter"
      >
        <el-radio-button value="pending">待复习</el-radio-button>
        <el-radio-button value="mastered">已掌握</el-radio-button>
      </el-radio-group>
      <span class="filter-hint">按最近答错时间倒序</span>
    </div>

    <div v-if="loadError" class="demo-banner">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ loadError }}</span>
    </div>

    <section v-loading="loading" class="wrong-list">
      <el-empty v-if="!loading && !rows.length" :description="emptyText" :image-size="96">
        <el-button type="primary" plain @click="router.push('/question')">
          去挑一道题
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-empty>

      <article v-for="row in rows" :key="row.id" class="wrong-card">
        <div class="wrong-main">
          <div class="wrong-heading">
            <button class="wrong-title" type="button" @click="goSolve(row)">
              {{ titleOf(row) }}
            </button>
            <el-tag :type="difficultyOf(row).type" effect="light" round size="small">
              {{ difficultyOf(row).label }}
            </el-tag>
            <el-tag :type="row.mastered ? 'success' : 'danger'" effect="plain" round size="small">
              {{ row.mastered ? '已掌握' : '未掌握' }}
            </el-tag>
          </div>

          <div class="wrong-meta">
            <span>{{ knowledgeOf(row) }}</span>
            <span>答错 {{ row.wrongCount }} 次</span>
            <span>最近 {{ row.lastWrongTime || '—' }}</span>
          </div>

          <div class="wrong-actions">
            <el-button type="primary" size="small" @click="goSolve(row)">
              <el-icon><RefreshRight /></el-icon>
              去重做
            </el-button>
            <el-button
              v-if="!row.mastered"
              size="small"
              plain
              @click="handleMastered(row)"
            >
              标记已掌握
            </el-button>
            <el-button link type="primary" @click="toggleCode(row)">
              <el-icon><View /></el-icon>
              {{ expanded[row.id] ? '收起上次代码' : '查看上次错误代码' }}
            </el-button>
          </div>

          <div v-if="expanded[row.id]" class="code-panel">
            <div class="code-head">
              <span>{{ languageLabel(row) }}</span>
              <span class="code-status">
                {{ statusMeta[lastSubmitOf(row)?.status]?.label || lastSubmitOf(row)?.status || '无提交记录' }}
              </span>
            </div>
            <pre class="code-block"><code v-html="highlightCode(row)"></code></pre>
          </div>

          <div class="similar-block">
            <div class="similar-head">
              <span>同类题推荐</span>
              <span class="similar-sub">同知识点 · 优先未做 · 难度接近</span>
            </div>
            <div v-if="recLoading" class="similar-loading">正在匹配同类题…</div>
            <div v-else-if="(recommendations[row.id] || []).length" class="similar-grid">
              <button
                v-for="item in recommendations[row.id]"
                :key="item.id"
                type="button"
                class="similar-card"
                @click="router.push(`/question/${item.id}/solve`)"
              >
                <span class="similar-title">{{ item.title }}</span>
                <span class="similar-meta">
                  {{ item.knowledgePointName || knowledgeOf(row) }} · {{ difficultyMeta[item.difficulty]?.label || '入门' }}
                  <em>{{ item.reason || '同类巩固' }}</em>
                </span>
              </button>
            </div>
            <div v-else class="similar-loading">暂无同类题，去题库看看其他知识点。</div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.summary-strip {
  display: flex;
  align-items: center;
  gap: 38px;
  margin-bottom: 16px;
  padding: 17px 22px;
  border: 1px solid var(--ca-line);
  border-radius: var(--ca-radius);
  background: #fff;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-item span {
  color: #94a3b8;
  font-size: 11px;
}

.summary-item strong {
  color: var(--ca-ink);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.summary-note {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-left: auto;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
  color: #7c869c;
  font-size: 11px;
}

.summary-note .el-icon {
  color: #6366f1;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.filter-hint {
  color: #a0a9ba;
  font-size: 11px;
}

.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 160px;
}

.wrong-card {
  border: 1px solid var(--ca-line);
  border-radius: var(--ca-radius);
  background: #fff;
  padding: 18px 20px;
}

.wrong-main {
  min-width: 0;
}

.wrong-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.wrong-title {
  padding: 0;
  border: 0;
  background: transparent;
  color: #1e293b;
  font-size: 15px;
  font-weight: 650;
  text-align: left;
  cursor: pointer;
}

.wrong-title:hover {
  color: var(--ca-primary);
}

.wrong-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 7px;
  color: #94a3b8;
  font-size: 11px;
}

.wrong-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.code-panel {
  margin-top: 14px;
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fbfcfe;
}

.code-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #eef2f7;
  color: #64748b;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.code-status {
  color: #b91c1c;
}

.code-block {
  max-height: 280px;
  margin: 0;
  padding: 13px 15px;
  overflow: auto;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11.5px;
  line-height: 1.7;
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
  margin-top: 16px;
  padding-top: 13px;
  border-top: 1px dashed #e8edf5;
}

.similar-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  color: #475569;
  font-size: 11.5px;
  font-weight: 600;
}

.similar-sub {
  color: #a0a9ba;
  font-size: 10px;
  font-weight: 400;
}

.similar-loading {
  margin-top: 9px;
  color: #a0a9ba;
  font-size: 11px;
}

.similar-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
  margin-top: 9px;
}

.similar-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 10px 11px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fcfdff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.similar-card:hover {
  border-color: #c7d2fe;
  background: #f5f7ff;
}

.similar-title {
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

.similar-meta em {
  margin-left: 4px;
  color: #818cf8;
  font-style: normal;
}

@media (max-width: 900px) {
  .summary-strip {
    flex-wrap: wrap;
    gap: 20px;
  }

  .summary-note {
    width: 100%;
    margin-left: 0;
  }

  .similar-grid {
    grid-template-columns: 1fr;
  }
}
</style>
