<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, InfoFilled, Search } from '@element-plus/icons-vue'
import { listKnowledgePoints, listQuestions } from '@/api/question'
import { demoKnowledgePoints, demoQuestions } from '@/data/demo'

const router = useRouter()

const filters = reactive({
  keyword: '',
  difficulty: null,
  knowledgePointId: null
})

const page = ref(1)
const size = ref(8)
const total = ref(0)
const rows = ref([])
const knowledgePoints = ref([])
const loading = ref(false)
const usingDemo = ref(false)
const loadError = ref('')

const hasFilters = computed(
  () => Boolean(filters.keyword) || filters.difficulty !== null || filters.knowledgePointId !== null
)

const difficultyMap = {
  1: { label: '入门', type: 'success' },
  2: { label: '进阶', type: 'warning' },
  3: { label: '挑战', type: 'danger' }
}

function difficultyInfo(level) {
  return difficultyMap[level] || { label: '未分级', type: 'info' }
}

function knowledgeName(row) {
  return (
    row.knowledgePointName ||
    knowledgePoints.value.find((item) => item.id === row.knowledgePointId)?.name ||
    '未分类'
  )
}

async function loadKnowledgeOptions() {
  try {
    const data = await listKnowledgePoints()
    knowledgePoints.value = Array.isArray(data) && data.length ? data : demoKnowledgePoints
  } catch {
    knowledgePoints.value = demoKnowledgePoints
  }
}

function applyDemoData() {
  let list = [...demoQuestions]
  const keyword = filters.keyword.trim().toLowerCase()

  if (keyword) {
    list = list.filter((item) => item.title.toLowerCase().includes(keyword))
  }
  if (filters.difficulty !== null) {
    list = list.filter((item) => item.difficulty === filters.difficulty)
  }
  if (filters.knowledgePointId !== null) {
    list = list.filter((item) => item.knowledgePointId === filters.knowledgePointId)
  }

  total.value = list.length
  const start = (page.value - 1) * size.value
  rows.value = list.slice(start, start + size.value)
}

async function loadQuestions() {
  loading.value = true
  loadError.value = ''

  try {
    const data = await listQuestions({
      page: page.value,
      size: size.value,
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.difficulty !== null ? { difficulty: filters.difficulty } : {}),
      ...(filters.knowledgePointId !== null
        ? { knowledgePointId: filters.knowledgePointId }
        : {})
    })
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
    usingDemo.value = false
  } catch (error) {
    usingDemo.value = true
    loadError.value = error.message || '题库接口暂未接入'
    applyDemoData()
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  if (usingDemo.value) {
    applyDemoData()
  } else {
    loadQuestions()
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.difficulty = null
  filters.knowledgePointId = null
  search()
}

function changePage(nextPage) {
  page.value = nextPage
  if (usingDemo.value) {
    applyDemoData()
  } else {
    loadQuestions()
  }
}

function openQuestion(row) {
  router.push(`/question/${row.id}`)
}

function randomQuestion() {
  if (!rows.value.length) {
    ElMessage.info('当前没有可练习的题目')
    return
  }
  const pick = rows.value[Math.floor(Math.random() * rows.value.length)]
  router.push(`/question/${pick.id}`)
}

onMounted(async () => {
  await loadKnowledgeOptions()
  await loadQuestions()
})
</script>

<template>
  <div class="question-page">
    <section class="hero">
      <div class="hero-copy">
        <div class="page-eyebrow">Practice Library</div>
        <h1 class="page-title">题库</h1>
        <p class="page-subtitle">
          从数组、字符串到链表、递归与排序，按知识点循序练习。提交失败时，AI 教练会分层提示，而不是直接给答案。
        </p>
        <div class="hero-meta">
          <span><b>5</b> 个知识点</span>
          <span class="meta-divider"></span>
          <span><b>3</b> 个难度梯度</span>
          <span class="meta-divider"></span>
          <span>AI 调试待接入</span>
        </div>
      </div>

      <div class="hero-side">
        <div class="suggest-card">
          <div class="suggest-top">
            <span class="suggest-label">今日建议</span>
            <span class="suggest-step">STEP 01</span>
          </div>
          <strong>从一道数组题开始</strong>
          <p>先读懂输入输出，再动手写。不会做也没关系，提交后可以进入 AI 调试。</p>
          <el-button class="suggest-btn" type="primary" @click="randomQuestion">
            随机一题
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </section>

    <div v-if="usingDemo" class="demo-banner">
      <el-icon><InfoFilled /></el-icon>
      <span>
        当前展示前端骨架演示数据。后端题库接口接入后会自动切换为真实数据
        <template v-if="loadError">（{{ loadError }}）</template>。
      </span>
    </div>

    <section class="panel filter-panel">
      <el-input
        v-model="filters.keyword"
        class="search-input"
        placeholder="搜索题目，例如：两数之和"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select
        v-model="filters.knowledgePointId"
        class="filter-select"
        placeholder="全部知识点"
        clearable
        @change="search"
      >
        <el-option
          v-for="item in knowledgePoints"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>

      <el-select
        v-model="filters.difficulty"
        class="filter-select filter-select-sm"
        placeholder="全部难度"
        clearable
        @change="search"
      >
        <el-option label="入门" :value="1" />
        <el-option label="进阶" :value="2" />
        <el-option label="挑战" :value="3" />
      </el-select>

      <el-button type="primary" @click="search">筛选</el-button>
      <el-button text :disabled="!hasFilters" @click="resetFilters">重置</el-button>

      <div class="filter-total">共 {{ total }} 道题</div>
    </section>

    <section class="panel table-panel">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        class="question-table"
        @row-click="openQuestion"
      >
        <el-table-column label="题目" min-width="330">
          <template #default="{ row }">
            <div class="question-main">
              <div class="question-title">{{ row.title }}</div>
              <div class="question-sub">
                <span>{{ row.type === 'PERSONAL' ? '个人题' : '公共题' }}</span>
                <span class="dot">·</span>
                <span>题号 {{ row.id }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="知识点" width="130">
          <template #default="{ row }">
            <span class="knowledge-text">{{ knowledgeName(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="难度" width="110">
          <template #default="{ row }">
            <el-tag :type="difficultyInfo(row.difficulty).type" effect="light" round size="small">
              {{ difficultyInfo(row.difficulty).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.solved" type="success" effect="plain" round size="small">
              <span class="solved-tag">
                <el-icon><CircleCheck /></el-icon>
                已通过
              </span>
            </el-tag>
            <span v-else class="status-todo">
              <span class="status-dot"></span>
              未作答
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openQuestion(row)">
              开始练习
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            :description="loadError ? '题库接口尚未接入，暂无真实数据' : '没有找到匹配的题目'"
            :image-size="92"
          >
            <el-button v-if="hasFilters" type="primary" plain @click="resetFilters">
              清空筛选
            </el-button>
          </el-empty>
        </template>
      </el-table>

      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="changePage"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 36px;
  margin-bottom: 24px;
}

.hero-copy {
  flex: 1;
  padding: 8px 0 4px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  color: #7c869c;
  font-size: 12px;
}

.hero-meta b {
  color: #334155;
  font-weight: 700;
}

.meta-divider {
  width: 1px;
  height: 11px;
  background: #d9dfeb;
}

.hero-side {
  flex: 0 0 300px;
}

.suggest-card {
  height: 100%;
  padding: 18px 19px 17px;
  border: 1px solid #dfe3ff;
  border-radius: 15px;
  background:
    radial-gradient(circle at 100% 0%, rgba(99, 102, 241, 0.16), transparent 55%),
    linear-gradient(160deg, #f8f9ff 0%, #fff 100%);
}

.suggest-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.suggest-label {
  color: #6366f1;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.suggest-step {
  color: #a7b0c3;
  font-size: 9.5px;
  letter-spacing: 1px;
}

.suggest-card strong {
  display: block;
  color: #1e293b;
  font-size: 15px;
  font-weight: 650;
}

.suggest-card p {
  margin: 7px 0 14px;
  color: #6b7588;
  font-size: 11.5px;
  line-height: 1.7;
}

.suggest-btn {
  height: 34px;
  border-radius: 8px;
  font-size: 12px;
}

.filter-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 13px 15px;
}

.search-input {
  width: 280px;
}

.filter-select {
  width: 156px;
}

.filter-select-sm {
  width: 124px;
}

.filter-total {
  margin-left: auto;
  padding-right: 3px;
  color: #94a3b8;
  font-size: 11.5px;
  white-space: nowrap;
}

.table-panel {
  overflow: hidden;
}

.question-table {
  width: 100%;
}

.question-table :deep(.el-table__row) {
  cursor: pointer;
}

.question-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 3px;
}

.question-title {
  color: #1e293b;
  font-size: 13.5px;
  font-weight: 600;
}

.question-sub {
  color: #a0a9ba;
  font-size: 10.5px;
}

.dot {
  margin: 0 5px;
}

.knowledge-text {
  color: #526078;
  font-size: 12.5px;
}

.solved-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-todo {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
}

.action-arrow {
  margin-left: 2px;
  font-size: 11px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 17px 16px;
  border-top: 1px solid #f1f4f9;
}

@media (max-width: 1100px) {
  .hero-side {
    flex-basis: 265px;
  }

  .filter-panel {
    flex-wrap: wrap;
  }

  .filter-total {
    width: 100%;
    margin-left: 0;
    padding-left: 3px;
  }
}

@media (max-width: 760px) {
  .hero {
    flex-direction: column;
    gap: 20px;
  }

  .hero-side {
    flex-basis: auto;
  }

  .search-input {
    width: 100%;
  }

  .filter-select,
  .filter-select-sm {
    flex: 1;
    width: auto;
  }
}
</style>
