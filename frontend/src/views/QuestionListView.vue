<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, CircleCheck, Picture, Refresh, Search } from '@element-plus/icons-vue'
import { listKnowledgePoints, listQuestions } from '@/api/question'

const route = useRoute()
const router = useRouter()

const filters = reactive({
  keyword: '',
  difficulty: null,
  knowledgePointId: null
})

const scope = ref('PUBLIC')

const knowledgePoints = ref([])
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const loadError = ref('')

const difficultyMap = {
  1: { label: '入门', className: 'is-easy' },
  2: { label: '进阶', className: 'is-medium' },
  3: { label: '挑战', className: 'is-hard' }
}

const hasFilters = computed(
  () => Boolean(filters.keyword.trim()) || filters.difficulty !== null || filters.knowledgePointId !== null
)

const pageCount = computed(() => Math.ceil(total.value / size.value))

function queryValue(value) {
  return Array.isArray(value) ? value[0] : value
}

// 从 URL 还原筛选条件，保证刷新、分享和浏览器后退都能恢复。
function readQuery() {
  const keyword = queryValue(route.query.keyword)
  const knowledgePointId = Number(queryValue(route.query.kp))
  const difficulty = Number(queryValue(route.query.difficulty))
  const pageNumber = Number(queryValue(route.query.page))
  const scopeValue = queryValue(route.query.scope)

  scope.value = scopeValue === 'PERSONAL' ? 'PERSONAL' : 'PUBLIC'
  filters.keyword = typeof keyword === 'string' ? keyword : ''
  filters.knowledgePointId =
    Number.isInteger(knowledgePointId) && knowledgePointId > 0 ? knowledgePointId : null
  filters.difficulty = [1, 2, 3].includes(difficulty) ? difficulty : null
  page.value = Number.isInteger(pageNumber) && pageNumber > 0 ? pageNumber : 1
}

function buildQuery() {
  const query = {}
  if (scope.value === 'PERSONAL') query.scope = 'PERSONAL'
  if (filters.keyword.trim()) query.keyword = filters.keyword.trim()
  if (filters.knowledgePointId !== null) query.kp = String(filters.knowledgePointId)
  if (filters.difficulty !== null) query.difficulty = String(filters.difficulty)
  if (page.value > 1) query.page = String(page.value)
  return query
}

function syncQuery() {
  const target = router.resolve({ name: 'question-list', query: buildQuery() })
  if (target.fullPath === route.fullPath) {
    loadQuestions()
    return
  }
  router.replace({ name: 'question-list', query: buildQuery() })
}

watch(
  () => route.query,
  () => {
    if (route.name !== 'question-list') return
    readQuery()
    loadQuestions()
  }
)

function difficultyInfo(level) {
  return difficultyMap[level] || { label: '未分级', className: 'is-unknown' }
}

function knowledgeName(row) {
  return (
    row.knowledgePointName ||
    knowledgePoints.value.find((item) => item.id === row.knowledgePointId)?.name ||
    '未分类'
  )
}

function typeLabel(row) {
  if (row.type === 'PUBLIC') return '公共题'
  return row.visibility === 'SHARED' ? '共享题' : '私有题'
}

function switchScope(value) {
  if (scope.value === value) return
  scope.value = value
  page.value = 1
  syncQuery()
}

async function loadKnowledgeOptions() {
  try {
    const data = await listKnowledgePoints()
    knowledgePoints.value = Array.isArray(data) ? data : []
  } catch {
    knowledgePoints.value = []
  }
}

async function loadQuestions() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listQuestions({
      page: page.value,
      size: size.value,
      type: scope.value,
      ...(filters.keyword.trim() ? { keyword: filters.keyword.trim() } : {}),
      ...(filters.difficulty !== null ? { difficulty: filters.difficulty } : {}),
      ...(filters.knowledgePointId !== null
        ? { knowledgePointId: filters.knowledgePointId }
        : {})
    })
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
  } catch (error) {
    rows.value = []
    total.value = 0
    loadError.value = error?.message || '题库加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  syncQuery()
}

function resetFilters() {
  filters.keyword = ''
  filters.difficulty = null
  filters.knowledgePointId = null
  page.value = 1
  syncQuery()
}

function changePage(nextPage) {
  page.value = nextPage
  syncQuery()
}

function openQuestion(row) {
  router.push({
    name: 'question-detail',
    params: { id: row.id },
    query: { from: route.fullPath }
  })
}

function randomQuestion() {
  if (!rows.value.length) {
    search()
    return
  }
  const pick = rows.value[Math.floor(Math.random() * rows.value.length)]
  openQuestion(pick)
}

onMounted(async () => {
  readQuery()
  await Promise.all([loadKnowledgeOptions(), loadQuestions()])
})
</script>

<template>
  <div class="catalog-page">
    <section class="catalog-head">
      <div class="head-copy">
        <h1>题库</h1>
        <p>按知识点练习，提交后看判题结果。</p>
      </div>

      <div class="head-side">
        <div class="head-stats">
          <div>
            <strong>{{ total || '—' }}</strong>
            <span>道练习题</span>
          </div>
          <div>
            <strong>{{ knowledgePoints.length || '—' }}</strong>
            <span>个知识点</span>
          </div>
          <div>
            <strong>2</strong>
            <span>种语言</span>
          </div>
        </div>
        <div class="head-actions">
          <el-button :icon="Refresh" @click="randomQuestion">随机一题</el-button>
          <el-button type="primary" :icon="Picture" @click="router.push('/question/import')">
            图片录题
          </el-button>
        </div>
      </div>
    </section>

    <section class="scope-bar">
      <button
        type="button"
        class="scope-tab"
        :class="{ active: scope === 'PUBLIC' }"
        @click="switchScope('PUBLIC')"
      >
        公共题库
      </button>
      <button
        type="button"
        class="scope-tab"
        :class="{ active: scope === 'PERSONAL' }"
        @click="switchScope('PERSONAL')"
      >
        我的题目
      </button>
      <span class="scope-note">共享题计入累计 AC，不计入段位</span>
    </section>

    <section class="filter-bar">
      <el-input
        v-model="filters.keyword"
        class="search-input"
        placeholder="搜索题目"
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
        class="filter-select level-select"
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

      <span class="filter-count">共 {{ total }} 道</span>
    </section>

    <el-alert
      v-if="loadError"
      class="load-alert"
      type="error"
      :closable="false"
      show-icon
      :title="loadError"
    />

    <section class="problem-board">
      <div class="board-head">
        <span class="col-status">状态</span>
        <span class="col-title">题目</span>
        <span class="col-kp">知识点</span>
        <span class="col-level">难度</span>
        <span class="col-action">操作</span>
      </div>

      <div v-if="loading" class="board-loading">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="rows.length" class="board-body">
        <button
          v-for="row in rows"
          :key="row.id"
          type="button"
          class="problem-row"
          :class="{ 'is-solved': row.solved }"
          @click="openQuestion(row)"
        >
          <span class="status-cell">
            <span v-if="row.solved" class="status-icon is-done">
              <el-icon><CircleCheck /></el-icon>
            </span>
            <span v-else class="status-icon"></span>
          </span>

          <span class="title-cell">
            <strong>{{ row.title }}</strong>
            <em>
              #{{ row.id }}
              <span>·</span>
              {{ typeLabel(row) }}
            </em>
          </span>

          <span class="kp-cell">{{ knowledgeName(row) }}</span>

          <span class="level-cell">
            <span class="level-dot" :class="difficultyInfo(row.difficulty).className"></span>
            {{ difficultyInfo(row.difficulty).label }}
          </span>

          <span class="action-cell">
            开始练习
            <el-icon><ArrowRight /></el-icon>
          </span>
        </button>
      </div>

      <el-empty
        v-else
        class="board-empty"
        :description="hasFilters ? '没有匹配的题目，换个条件试试' : '题库暂时还没有题目'"
        :image-size="88"
      >
        <el-button v-if="hasFilters" type="primary" plain @click="resetFilters">
          清空筛选
        </el-button>
      </el-empty>

      <div v-if="total > 0" class="board-foot">
        <span class="foot-count">第 {{ page }} / {{ pageCount }} 页</span>
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
.catalog-page {
  width: min(1240px, calc(100% - 48px));
  margin: 0 auto;
  padding: 38px 0 64px;
}

.catalog-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 36px;
  padding: 6px 0 30px;
  border-bottom: 1px solid #e6e9f1;
}

.eyebrow {
  display: none;
  margin-bottom: 10px;
  color: #1f3a5f;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 2.2px;
}

.head-copy h1 {
  color: #0f172a;
  font-size: 42px;
  font-weight: 700;
  letter-spacing: -1.2px;
  line-height: 1.1;
}

.head-copy p {
  max-width: 610px;
  margin-top: 18px;
  color: #64748b;
  font-size: 15px;
  line-height: 1.8;
}

.head-side {
  flex: 0 0 370px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 20px;
}

.head-stats {
  display: flex;
  align-items: flex-start;
  gap: 30px;
}

.head-stats div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.head-stats strong {
  color: #0f172a;
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -0.6px;
}

.head-stats span {
  color: #94a3b8;
  font-size: 11px;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.scope-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 24px 0 12px;
}

.scope-tab {
  padding: 8px 14px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.scope-tab.active {
  border-color: #1f3a5f;
  background: #1f3a5f;
  color: #fff;
}

.scope-note {
  margin-left: auto;
  color: #8a93a6;
  font-size: 11px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 22px 0 16px;
  padding: 13px 14px;
  border: 1px solid #e5e9f2;
  border-radius: 10px;
  background: #fff;
}

.search-input {
  width: 300px;
}

.filter-select {
  width: 160px;
}

.level-select {
  width: 128px;
}

.filter-count {
  margin-left: auto;
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

.load-alert {
  margin-bottom: 14px;
}

.problem-board {
  overflow: hidden;
  border: 1px solid #e5e9f2;
  border-radius: 12px;
  background: #fff;
}

.board-head,
.problem-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 132px 100px 112px;
  grid-template-areas: 'status title kp level action';
  align-items: center;
  gap: 12px;
}

.board-head {
  min-height: 44px;
  padding: 0 22px;
  border-bottom: 1px solid #eef1f6;
  background: #fafbfe;
  color: #94a3b8;
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 0.6px;
}

.col-status { grid-area: status; }
.col-title { grid-area: title; }
.col-kp { grid-area: kp; }
.col-level { grid-area: level; }
.col-action { grid-area: action; text-align: right; }

.problem-row {
  width: 100%;
  min-height: 72px;
  padding: 0 22px;
  border: 0;
  border-bottom: 1px solid #f1f4f9;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.problem-row:last-child {
  border-bottom: 0;
}

.problem-row:hover {
  background: #f7f9fb;
}

.problem-row.is-solved .title-cell strong {
  color: #334155;
}

.status-cell {
  grid-area: status;
}

.status-icon {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border: 1.5px solid #dbe1eb;
  border-radius: 50%;
  color: #2f7d5b;
  font-size: 13px;
}

.status-icon.is-done {
  border-color: #cfe3d9;
  background: #edf5f1;
}

.title-cell {
  grid-area: title;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.title-cell strong {
  overflow: hidden;
  color: #1e293b;
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-cell em {
  color: #a0a9ba;
  font-size: 10.5px;
  font-style: normal;
}

.title-cell em span {
  margin: 0 4px;
}

.kp-cell {
  grid-area: kp;
  color: #526078;
  font-size: 12px;
}

.level-cell {
  grid-area: level;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #526078;
  font-size: 11.5px;
}

.level-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd5e1;
}

.level-dot.is-easy { background: #2f7d5b; }
.level-dot.is-medium { background: #a16207; }
.level-dot.is-hard { background: #b42318; }

.action-cell {
  grid-area: action;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  color: #1f3a5f;
  font-size: 11.5px;
  font-weight: 600;
}

.action-cell .el-icon {
  font-size: 11px;
}

.board-loading {
  padding: 26px 22px;
}

.board-empty {
  padding: 30px 0 26px;
}

.board-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 15px 22px;
  border-top: 1px solid #f1f4f9;
}

.foot-count {
  color: #94a3b8;
  font-size: 11.5px;
}

@media (max-width: 1000px) {
  .catalog-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 22px;
  }

  .head-side {
    flex: none;
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .head-copy h1 {
    font-size: 48px;
  }

  .filter-bar {
    flex-wrap: wrap;
  }

  .filter-count {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 760px) {
  .catalog-page {
    width: calc(100% - 28px);
    padding: 24px 0 44px;
  }

  .catalog-head {
    padding-bottom: 22px;
  }

  .head-copy h1 {
    font-size: 42px;
    letter-spacing: -1.6px;
  }

  .head-copy p {
    margin-top: 12px;
    font-size: 13.5px;
  }

  .head-side {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .head-stats {
    width: 100%;
    justify-content: space-between;
    gap: 12px;
  }

  .head-actions {
    width: 100%;
  }

  .head-actions .el-button {
    flex: 1;
  }

  .scope-bar {
    flex-wrap: wrap;
    margin-top: 18px;
  }

  .scope-note {
    width: 100%;
    margin-left: 0;
  }

  .filter-bar {
    padding: 11px;
  }

  .search-input,
  .filter-select,
  .level-select {
    width: 100%;
  }

  .filter-bar > .el-button {
    flex: 1;
  }

  .board-head {
    display: none;
  }

  .problem-row {
    grid-template-columns: 28px minmax(0, 1fr) auto;
    grid-template-areas:
      'status title action'
      '. level level';
    gap: 4px 10px;
    min-height: 86px;
    padding: 14px 14px;
  }

  .title-cell strong {
    font-size: 13.5px;
  }

  .kp-cell {
    display: none;
  }

  .level-cell {
    font-size: 10.5px;
  }

  .action-cell {
    font-size: 10.5px;
  }

  .board-foot {
    padding: 12px 14px;
  }

  .foot-count {
    display: none;
  }

  .board-foot .el-pagination {
    margin: 0 auto;
  }
}
</style>
