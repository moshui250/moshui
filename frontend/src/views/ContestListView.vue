<template>
  <section class="contest-page">
    <div class="page-inner">
      <div class="contest-hero">
        <div class="hero-copy">
          <p class="eyebrow">CONTEST ARENA</p>
          <h1>把练习，变成一次短跑。</h1>
          <p class="hero-desc">
            在限定时间里完成一套题单。排名只看真实提交，
            每一次通过都会留下时间与罚时记录。
          </p>
        </div>
        <div class="hero-stat">
          <span>全部竞赛</span>
          <strong>{{ total }}</strong>
          <em>场</em>
        </div>
      </div>

      <div class="filter-row">
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
        <span class="filter-note">按开始时间排序</span>
      </div>

      <div v-if="loading" class="contest-grid">
        <div v-for="i in 4" :key="i" class="contest-card skeleton">
          <el-skeleton :rows="3" animated />
        </div>
      </div>

      <div v-else-if="contests.length" class="contest-grid">
        <article
          v-for="contest in contests"
          :key="contest.id"
          class="contest-card"
          :class="'is-' + contest.status.toLowerCase()"
          @click="openContest(contest)"
        >
          <div class="card-top">
            <span class="status-pill" :class="'status-' + contest.status.toLowerCase()">
              {{ statusLabel(contest.status) }}
            </span>
            <span class="contest-id">#{{ String(contest.id).padStart(2, '0') }}</span>
          </div>

          <h2>{{ contest.title }}</h2>
          <p class="contest-desc">{{ contest.description || '暂无竞赛说明。' }}</p>

          <div class="contest-meta">
            <span><b>{{ contest.problemCount }}</b> 道题</span>
            <span><b>{{ contest.participantCount }}</b> 人报名</span>
          </div>

          <div class="time-line">
            <span>{{ formatDate(contest.startTime) }}</span>
            <i></i>
            <span>{{ formatDate(contest.endTime) }}</span>
          </div>

          <div class="card-footer">
            <span v-if="contest.registered" class="registered-mark">已报名</span>
            <span v-else-if="contest.status === 'ENDED'" class="muted">已结束</span>
            <span v-else class="muted">进入后可报名</span>
            <button type="button">
              {{ contest.status === 'ENDED' ? '查看结果' : '进入竞赛' }}
              <span aria-hidden="true">→</span>
            </button>
          </div>
        </article>
      </div>

      <div v-else class="empty-state">
        <div class="empty-mark">◎</div>
        <h3>这个状态下还没有竞赛</h3>
        <p>可以先看看其他场次，或者从题库开始保持手感。</p>
        <router-link to="/question" class="empty-link">去题库练习</router-link>
      </div>

      <div v-if="total > size" class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="changePage"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listContests } from '@/api/contest'

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
const size = ref(6)
const status = ref('')
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
      status: status.value || undefined
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
  border-color: #c8cdfa;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.07);
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

@media (max-width: 860px) {
  .page-inner {
    width: calc(100% - 32px);
  }

  .contest-hero {
    align-items: flex-start;
    flex-direction: column;
    gap: 22px;
  }

  .hero-stat strong {
    font-size: 34px;
  }

  .contest-grid {
    grid-template-columns: 1fr;
  }

  .filter-note {
    display: none;
  }
}
</style>
