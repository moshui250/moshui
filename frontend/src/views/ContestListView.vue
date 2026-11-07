<template>
  <section class="contest-page">
    <div class="page-inner">
      <header class="page-head">
        <div>
          <h1>竞赛</h1>
          <p>在限定时间里完成一套题单，排名只看真实提交。</p>
        </div>
        <span class="page-count">共 {{ total }} 场</span>
      </header>

      <div class="contest-toolbar">
        <div class="filter-tabs" role="tablist" aria-label="竞赛状态">
          <button
            v-for="item in filters"
            :key="item.value"
            type="button"
            :class="{ active: status === item.value }"
            @click="changeStatus(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <el-input
          v-model="keyword"
          class="contest-search"
          placeholder="搜索竞赛"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div v-if="loading" class="contest-table-wrap">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="contests.length" class="contest-table-wrap">
        <table class="contest-table">
          <thead>
            <tr>
              <th class="cell-competition">竞赛</th>
              <th class="cell-status">状态</th>
              <th class="cell-number">题数</th>
              <th class="cell-number">报名</th>
              <th class="cell-time">时间</th>
              <th class="cell-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="contest in contests"
              :key="contest.id"
              tabindex="0"
              @click="openContest(contest)"
              @keydown.enter="openContest(contest)"
            >
              <td class="cell-competition">
                <strong>{{ contest.title }}</strong>
                <p>{{ contest.description || '暂无竞赛说明。' }}</p>
              </td>
              <td class="cell-status">
                <span class="status-pill" :class="'status-' + contest.status.toLowerCase()">
                  {{ statusLabel(contest.status) }}
                </span>
              </td>
              <td class="cell-number">{{ contest.problemCount }}</td>
              <td class="cell-number">{{ contest.participantCount }}</td>
              <td class="cell-time">
                <span>{{ formatDate(contest.startTime) }}</span>
                <em>— {{ formatDate(contest.endTime) }}</em>
              </td>
              <td class="cell-action">
                <span v-if="contest.registered" class="registered-mark">已报名</span>
                <span v-else class="muted">{{ contest.status === 'ENDED' ? '已结束' : '可报名' }}</span>
                <b>{{ contest.status === 'ENDED' ? '查看结果' : '进入竞赛' }} <span aria-hidden="true">→</span></b>
              </td>
            </tr>
          </tbody>
        </table>
        <ListPager
          v-if="total > size"
          :page="page"
          :page-size="size"
          :total="total"
          @change="changePage"
        />
      </div>

      <div v-else class="empty-state">
        <div class="empty-mark">◎</div>
        <h3>没有匹配的竞赛</h3>
        <p>换个状态或关键词，也可以从题库开始保持手感。</p>
        <router-link to="/question" class="empty-link">去题库练习</router-link>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listContests } from '@/api/contest'
import ListPager from '@/components/ListPager.vue'

const router = useRouter()
const filters = [
  { label: '全部', value: '' },
  { label: '进行中', value: 'RUNNING' },
  { label: '即将开始', value: 'UPCOMING' },
  { label: '已结束', value: 'ENDED' }
]

const contests = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(8)
const status = ref('')
const keyword = ref('')
const loading = ref(true)

function statusLabel(value) {
  return { RUNNING: '进行中', UPCOMING: '即将开始', ENDED: '已结束' }[value] || value
}

function formatDate(value) {
  if (!value) return '--'
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getMonth() + 1}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function load() {
  loading.value = true
  try {
    const data = await listContests({
      page: page.value,
      size: size.value,
      status: status.value || undefined,
      keyword: keyword.value.trim() || undefined
    })
    contests.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    contests.value = []
    total.value = 0
    ElMessage.error('竞赛列表加载失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

function changeStatus(value) {
  if (status.value === value) return
  status.value = value
  page.value = 1
  load()
}

function search() {
  page.value = 1
  load()
}

function changePage(value) {
  page.value = value
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openContest(contest) {
  router.push({ name: 'contest-detail', params: { id: contest.id } })
}

onMounted(load)
</script>

<style scoped>
.contest-page {
  padding: 48px 0 12px;
}

.page-inner {
  width: min(1240px, calc(100% - 48px));
  margin: 0 auto;
}

.contest-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 40px;
  padding: 18px 0 38px;
  border-bottom: 1px solid #e4e7ef;
}

.eyebrow {
  display: none;
  margin: 0 0 14px;
  color: #1f3a5f;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.18em;
}

h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(30px, 3.4vw, 40px);
  line-height: 1.16;
  letter-spacing: -0.03em;
}

.hero-desc {
  max-width: 560px;
  margin: 16px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.85;
}

.hero-stat {
  flex: 0 0 auto;
  display: flex;
  align-items: baseline;
  gap: 5px;
  color: #64748b;
  font-size: 12px;
}

.hero-stat strong {
  color: #1f3a5f;
  font-size: 42px;
  line-height: 1;
  font-weight: 760;
  letter-spacing: -0.05em;
}

.hero-stat em {
  font-style: normal;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin: 24px 0 20px;
}

.filter-tabs {
  display: inline-flex;
  padding: 4px;
  border: 1px solid #e5e8f0;
  border-radius: 12px;
  background: #fff;
}

.filter-tabs button {
  height: 32px;
  padding: 0 15px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.filter-tabs button.active {
  background: #eef2f6;
  color: #1f3a5f;
}

.filter-note {
  color: #94a3b8;
  font-size: 12px;
}

.contest-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

@media (min-width: 1120px) {
  .contest-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.contest-card {
  padding: 22px 24px 20px;
  border: 1px solid #e5e8f0;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.contest-card:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.contest-card.skeleton {
  cursor: default;
  min-height: 250px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-pill {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
}

.status-running {
  background: #edf5f1;
  color: #2f7d5b;
}

.status-upcoming {
  background: #eef2f6;
  color: #1f3a5f;
}

.status-ended {
  background: #f1f5f9;
  color: #64748b;
}

.contest-id {
  color: #cbd5e1;
  font-size: 12px;
  font-weight: 650;
  letter-spacing: 0.08em;
}

h2 {
  margin: 18px 0 8px;
  color: #0f172a;
  font-size: 21px;
  letter-spacing: -0.02em;
}

.contest-desc {
  min-height: 46px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.75;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.contest-meta {
  display: flex;
  gap: 18px;
  margin-top: 16px;
  color: #94a3b8;
  font-size: 12px;
}

.contest-meta b {
  color: #334155;
  font-weight: 700;
}

.time-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: #94a3b8;
  font-size: 11px;
}

.time-line i {
  width: 18px;
  height: 1px;
  background: #dbe1ea;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f7;
}

.registered-mark {
  color: #2f7d5b;
  font-size: 12px;
  font-weight: 650;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
}

.card-footer button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 0;
  background: transparent;
  color: #1f3a5f;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.card-footer button span {
  transition: transform 0.16s ease;
}

.contest-card:hover .card-footer button span {
  transform: translateX(3px);
}

.empty-state {
  padding: 68px 24px;
  border: 1px dashed #dbe1ea;
  border-radius: 12px;
  background: #fff;
  text-align: center;
}

.empty-mark {
  color: #c8d2dc;
  font-size: 34px;
}

.empty-state h3 {
  margin: 12px 0 6px;
  color: #1e293b;
  font-size: 17px;
}

.empty-state p {
  margin: 0 0 18px;
  color: #94a3b8;
  font-size: 13px;
}

.empty-link {
  color: #1f3a5f;
  font-size: 13px;
  font-weight: 650;
  text-decoration: none;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* 2026-11 table layout */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 8px 0 22px;
}

.page-head h1 {
  margin: 0;
  color: #111827;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.025em;
}

.page-head p {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.page-count {
  color: #9ca3af;
  font-size: 12px;
  white-space: nowrap;
}

.contest-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0 14px;
  border-bottom: 1px solid #e5e7eb;
}

.filter-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-tabs button {
  height: 30px;
  padding: 0 11px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.filter-tabs button:hover,
.filter-tabs button.active {
  background: #e9edf1;
  color: #111827;
}

.contest-search {
  width: 260px;
}

.contest-table-wrap {
  overflow-x: auto;
}

.contest-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.contest-table th {
  padding: 12px 10px;
  color: #9ca3af;
  font-size: 12px;
  font-weight: 500;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.contest-table td {
  padding: 16px 10px;
  border-bottom: 1px solid #e9ecef;
  color: #4b5563;
  font-size: 13px;
  vertical-align: middle;
}

.contest-table tbody tr {
  cursor: pointer;
}

.contest-table tbody tr:hover {
  background: #eef1f4;
}

.contest-table tbody tr:focus-visible {
  outline: 2px solid #1f3a5f;
  outline-offset: -2px;
}

.cell-competition {
  width: 42%;
}

.cell-competition strong {
  display: block;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.cell-competition p {
  max-width: 520px;
  margin: 5px 0 0;
  overflow: hidden;
  color: #9ca3af;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-status {
  width: 90px;
}

.cell-number {
  width: 64px;
  text-align: center !important;
  font-variant-numeric: tabular-nums;
}

.cell-time {
  width: 150px;
  color: #6b7280 !important;
  font-size: 12px !important;
  font-variant-numeric: tabular-nums;
}

.cell-time span,
.cell-time em {
  display: block;
  font-style: normal;
  white-space: nowrap;
}

.cell-time em {
  margin-top: 3px;
  color: #9ca3af;
}

.cell-action {
  width: 150px;
  text-align: right !important;
  white-space: nowrap;
}

.cell-action b {
  display: block;
  margin-top: 4px;
  color: #1f3a5f;
  font-size: 12px;
  font-weight: 600;
}

.contest-table-wrap :deep(.list-pager) {
  margin-top: 4px;
}

@media (max-width: 860px) {
  .page-inner {
    width: calc(100% - 32px);
  }

  .page-head h1 {
    font-size: 30px;
  }

  .contest-toolbar {
    flex-wrap: wrap;
  }

  .contest-search {
    width: 100%;
    order: 2;
  }

  .contest-table {
    min-width: 0;
  }

  .contest-table thead {
    display: none;
  }

  .contest-table,
  .contest-table tbody,
  .contest-table tr,
  .contest-table td {
    display: block;
  }

  .contest-table tr {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 5px 12px;
    padding: 16px 2px;
    border-bottom: 1px solid #e5e7eb;
  }

  .contest-table tr:hover {
    background: transparent;
  }

  .contest-table td {
    width: auto !important;
    padding: 0 !important;
    border: 0;
    text-align: left !important;
  }

  .cell-competition {
    grid-column: 1 / 3;
  }

  .cell-competition p {
    max-width: none;
  }

  .cell-status {
    grid-column: 1;
  }

  .cell-number {
    grid-column: 2;
    justify-self: end;
    color: #9ca3af !important;
    text-align: right !important;
  }

  .cell-number::before {
    content: '题数 ';
    font-size: 11px;
  }

  .cell-number:nth-of-type(4)::before {
    content: '报名 ';
  }

  .cell-time {
    grid-column: 1 / 3;
    margin-top: 3px;
  }

  .cell-time span,
  .cell-time em {
    display: inline;
  }

  .cell-time em {
    margin-top: 0;
  }

  .cell-action {
    grid-column: 1 / 3;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 4px;
  }

  .cell-action b {
    margin-top: 0;
  }
}
</style>
