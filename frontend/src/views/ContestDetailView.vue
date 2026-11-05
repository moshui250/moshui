<template>
  <section class="contest-detail-page">
    <div class="page-inner">
      <router-link class="back-link" to="/contests">
        <span aria-hidden="true">←</span> 全部竞赛
      </router-link>

      <template v-if="loading">
        <div class="detail-skeleton">
          <el-skeleton :rows="5" animated />
        </div>
      </template>

      <template v-else-if="contest">
        <header class="contest-banner" :class="'is-' + contest.status.toLowerCase()">
          <div class="banner-main">
            <div class="banner-top">
              <span class="status-pill" :class="'status-' + contest.status.toLowerCase()">
                {{ statusLabel(contest.status) }}
              </span>
              <span class="contest-no">CONTEST #{{ String(contest.id).padStart(2, '0') }}</span>
            </div>
            <h1>{{ contest.title }}</h1>
            <p>{{ contest.description || '这场竞赛还没有填写说明。' }}</p>
            <div class="banner-meta">
              <span><b>{{ contest.problemCount }}</b> 道题</span>
              <span><b>{{ contest.participantCount }}</b> 人报名</span>
              <span>{{ formatDate(contest.startTime) }} — {{ formatDate(contest.endTime) }}</span>
            </div>
          </div>

          <aside class="banner-side">
            <span class="countdown-label">{{ countdownLabel }}</span>
            <strong class="countdown">{{ countdownText }}</strong>
            <button
              v-if="contest.status !== 'ENDED'"
              class="register-button"
              type="button"
              :disabled="contest.registered || registering"
              @click="handleRegister"
            >
              {{ contest.registered ? '已报名' : registering ? '报名中…' : '报名参赛' }}
            </button>
            <span v-else class="ended-note">本场竞赛已结束</span>
          </aside>
        </header>

        <div class="tabs">
          <button type="button" :class="{ active: tab === 'problems' }" @click="tab = 'problems'">
            题单
            <span>{{ contest.problemCount }}</span>
          </button>
          <button type="button" :class="{ active: tab === 'rank' }" @click="switchRank">
            排名
            <span>{{ rankTotal }}</span>
          </button>
        </div>

        <div v-if="tab === 'problems'" class="problem-panel">
          <div class="panel-head">
            <div>
              <h2>本场题单</h2>
              <p>按顺序完成，提交后成绩实时进入排名。</p>
            </div>
            <span class="panel-note">总分 {{ totalScore }}</span>
          </div>

          <div v-if="contest.problems.length" class="problem-list">
            <div
              v-for="problem in contest.problems"
              :key="problem.label"
              class="problem-row"
              :class="{ locked: contest.status === 'ENDED' || !contest.registered }"
            >
              <span class="problem-label">{{ problem.label }}</span>
              <div class="problem-main">
                <strong>{{ problem.title }}</strong>
                <div class="problem-meta">
                  <span class="difficulty" :class="'level-' + problem.difficulty">
                    {{ difficultyLabel(problem.difficulty) }}
                  </span>
                  <span>{{ problem.knowledgePoint }}</span>
                </div>
              </div>
              <span class="problem-score">{{ problem.score }} 分</span>
              <button
                type="button"
                class="problem-action"
                :disabled="contest.status === 'ENDED' || !contest.registered"
                @click="goSolve(problem)"
              >
                {{ contest.status === 'ENDED' ? '已结束' : contest.registered ? '去做题' : '报名后解锁' }}
                <span aria-hidden="true">→</span>
              </button>
            </div>
          </div>
          <div v-else class="empty-block">题单还在准备中。</div>
        </div>

        <div v-else class="rank-panel">
          <div class="panel-head">
            <div>
              <h2>实时排名</h2>
              <p>先按总分，再按通过题数，最后按罚时升序。</p>
            </div>
            <button class="refresh-button" type="button" @click="loadRank">
              刷新
            </button>
          </div>

          <div v-loading="rankLoading" class="rank-table-wrap">
            <table v-if="rankRows.length" class="rank-table">
              <thead>
                <tr>
                  <th class="rank-col">排名</th>
                  <th>参赛者</th>
                  <th>通过</th>
                  <th>总分</th>
                  <th>罚时</th>
                  <th>最近通过</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in rankRows" :key="row.userId" :class="{ me: row.me }">
                  <td class="rank-col"><span class="rank-number">{{ row.rank }}</span></td>
                  <td>
                    <div class="rank-user">
                      <span class="rank-avatar">{{ (row.nickname || row.username || 'U').slice(0, 1) }}</span>
                      <span>{{ row.nickname || row.username }}</span>
                      <em v-if="row.me">我</em>
                    </div>
                  </td>
                  <td>{{ row.solvedCount }}</td>
                  <td class="score-cell">{{ row.score }}</td>
                  <td>{{ row.penaltyMinutes }} 分</td>
                  <td>{{ row.lastAcTime ? formatDate(row.lastAcTime) : '—' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-block">还没有人提交，第一名等你来拿。</div>
          </div>

          <div v-if="rankTotal > rankSize" class="rank-pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="rankPage"
              :page-size="rankSize"
              :total="rankTotal"
              @current-change="changeRankPage"
            />
          </div>
        </div>
      </template>

      <div v-else class="empty-state">
        <h2>竞赛不存在</h2>
        <router-link to="/contests">返回竞赛列表</router-link>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getContest, getContestRank, registerContest } from '@/api/contest'

const route = useRoute()
const router = useRouter()
const contest = ref(null)
const loading = ref(true)
const registering = ref(false)
const tab = ref('problems')
const rankRows = ref([])
const rankTotal = ref(0)
const rankPage = ref(1)
const rankSize = ref(20)
const rankLoading = ref(false)
const now = ref(Date.now())
let clock = null

const contestId = computed(() => route.params.id)

const totalScore = computed(() => {
  if (!contest.value?.problems?.length) return 0
  return contest.value.problems.reduce((sum, item) => sum + Number(item.score || 0), 0)
})

const countdownLabel = computed(() => {
  if (!contest.value) return ''
  if (contest.value.status === 'RUNNING') return '距结束'
  if (contest.value.status === 'UPCOMING') return '距开始'
  return '状态'
})

const countdownText = computed(() => {
  if (!contest.value) return '--:--:--'
  if (contest.value.status === 'ENDED') return '已结束'
  const target = contest.value.status === 'RUNNING'
    ? contest.value.endTime
    : contest.value.startTime
  const diff = Math.max(0, toTime(target) - now.value)
  const totalSeconds = Math.floor(diff / 1000)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (n) => String(n).padStart(2, '0')
  return days > 0
    ? `${days}天 ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
    : `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
})

function toTime(value) {
  return new Date(String(value).replace(' ', 'T')).getTime()
}

function statusLabel(value) {
  return { RUNNING: '进行中', UPCOMING: '即将开始', ENDED: '已结束' }[value] || value
}

function difficultyLabel(value) {
  return { 1: '入门', 2: '进阶', 3: '挑战' }[value] || '入门'
}

function formatDate(value) {
  if (!value) return '--'
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getMonth() + 1}月${pad(date.getDate())}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadContest() {
  loading.value = true
  try {
    contest.value = await getContest(contestId.value)
    rankTotal.value = Number(contest.value?.participantCount || 0)
  } catch {
    contest.value = null
    ElMessage.error('竞赛加载失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!contest.value || contest.value.registered || registering.value) return
  registering.value = true
  try {
    const data = await registerContest(contestId.value)
    contest.value.registered = true
    contest.value.participantCount = data.participantCount
    rankTotal.value = Number(data.participantCount || 0)
    ElMessage.success('报名成功，题单已解锁')
  } catch {
    ElMessage.error('报名失败，请稍后再试')
  } finally {
    registering.value = false
  }
}

async function loadRank() {
  rankLoading.value = true
  try {
    const data = await getContestRank(contestId.value, {
      page: rankPage.value,
      size: rankSize.value
    })
    rankRows.value = data.records || []
    rankTotal.value = Number(data.total || 0)
  } catch {
    rankRows.value = []
    rankTotal.value = 0
  } finally {
    rankLoading.value = false
  }
}

function switchRank() {
  tab.value = 'rank'
  loadRank()
}

function changeRankPage(value) {
  rankPage.value = value
  loadRank()
}

function goSolve(problem) {
  if (!contest.value?.registered || contest.value.status === 'ENDED') return
  router.push({
    name: 'contest-solve',
    params: { id: contest.value.id, questionId: problem.questionId }
  })
}

onMounted(async () => {
  await loadContest()
  clock = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (clock) window.clearInterval(clock)
})
</script>

<style scoped>
.contest-detail-page {
  padding: 28px 0 12px;
}

.page-inner {
  width: min(1240px, calc(100% - 48px));
  margin: 0 auto;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.back-link:hover {
  color: #4f46e5;
}

.detail-skeleton {
  padding: 32px;
  border: 1px solid #e5e8f0;
  border-radius: 20px;
  background: #fff;
}

.contest-banner {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 250px;
  gap: 36px;
  padding: 34px 36px;
  border: 1px solid #e3e7f1;
  border-radius: 22px;
  background:
    radial-gradient(circle at 92% 8%, rgba(79, 70, 229, 0.1), transparent 30%),
    #fff;
  overflow: hidden;
}

.contest-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 5px;
  height: 100%;
  background: #4f46e5;
}

.contest-banner.is-running::before {
  background: #10b981;
}

.contest-banner.is-ended::before {
  background: #cbd5e1;
}

.banner-top {
  display: flex;
  align-items: center;
  gap: 14px;
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
  background: #eafaf3;
  color: #059669;
}

.status-upcoming {
  background: #eef0ff;
  color: #4f46e5;
}

.status-ended {
  background: #f1f5f9;
  color: #64748b;
}

.contest-no {
  color: #cbd5e1;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

h1 {
  margin: 16px 0 10px;
  color: #0f172a;
  font-size: clamp(28px, 3.4vw, 40px);
  line-height: 1.2;
  letter-spacing: -0.035em;
}

.banner-main > p {
  max-width: 720px;
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.85;
}

.banner-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 22px;
  margin-top: 20px;
  color: #94a3b8;
  font-size: 12px;
}

.banner-meta b {
  color: #334155;
  font-weight: 700;
}

.banner-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-left: 24px;
  border-left: 1px solid #edf0f6;
}

.countdown-label {
  color: #94a3b8;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.countdown {
  margin-top: 6px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 760;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
}

.register-button {
  height: 40px;
  margin-top: 18px;
  border: 0;
  border-radius: 10px;
  background: #4f46e5;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.2);
}

.register-button:disabled {
  background: #eef0ff;
  color: #6366f1;
  box-shadow: none;
  cursor: default;
}

.ended-note {
  margin-top: 18px;
  color: #94a3b8;
  font-size: 13px;
}

.tabs {
  display: flex;
  gap: 6px;
  margin: 28px 0 16px;
  border-bottom: 1px solid #e5e8f0;
}

.tabs button {
  position: relative;
  height: 42px;
  padding: 0 16px;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
}

.tabs button.active {
  color: #4f46e5;
}

.tabs button.active::after {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: #4f46e5;
}

.tabs button span {
  margin-left: 5px;
  color: #94a3b8;
  font-size: 11px;
}

.problem-panel,
.rank-panel {
  padding: 26px 28px;
  border: 1px solid #e5e8f0;
  border-radius: 18px;
  background: #fff;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f0f2f7;
}

.panel-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  letter-spacing: -0.02em;
}

.panel-head p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.panel-note {
  color: #64748b;
  font-size: 12px;
}

.problem-list {
  display: flex;
  flex-direction: column;
}

.problem-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 78px 108px;
  align-items: center;
  gap: 16px;
  padding: 17px 2px;
  border-bottom: 1px solid #f2f4f8;
}

.problem-row:last-child {
  border-bottom: 0;
}

.problem-label {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: #f1f4ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 750;
}

.problem-main strong {
  display: block;
  color: #1e293b;
  font-size: 14px;
  font-weight: 650;
}

.problem-meta {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 7px;
  color: #94a3b8;
  font-size: 11px;
}

.difficulty {
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10px;
}

.level-1 {
  background: #eafaf3;
  color: #059669;
}

.level-2 {
  background: #fff7e6;
  color: #d97706;
}

.level-3 {
  background: #fff0f0;
  color: #dc2626;
}

.problem-score {
  color: #64748b;
  font-size: 12px;
  text-align: right;
}

.problem-action {
  border: 0;
  background: transparent;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  text-align: right;
}

.problem-action:disabled {
  color: #cbd5e1;
  cursor: default;
}

.problem-row.locked .problem-main strong {
  color: #64748b;
}

.rank-table-wrap {
  min-height: 190px;
  overflow-x: auto;
}

.rank-table {
  width: 100%;
  border-collapse: collapse;
}

.rank-table th {
  padding: 14px 10px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 600;
  text-align: left;
  border-bottom: 1px solid #f0f2f7;
}

.rank-table td {
  padding: 15px 10px;
  color: #475569;
  font-size: 13px;
  border-bottom: 1px solid #f5f6fa;
}

.rank-table tr.me {
  background: #f7f7ff;
}

.rank-table tr.me td:first-child {
  border-radius: 8px 0 0 8px;
}

.rank-table tr.me td:last-child {
  border-radius: 0 8px 8px 0;
}

.rank-col {
  width: 64px;
}

.rank-number {
  display: inline-grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 6px;
  background: #f4f6fb;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.rank-table tr:nth-child(1) .rank-number {
  background: #fff7e6;
  color: #d97706;
}

.rank-user {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #1e293b;
  font-weight: 600;
}

.rank-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #eef0ff;
  color: #4f46e5;
  font-size: 11px;
  font-weight: 700;
}

.rank-user em {
  padding: 1px 5px;
  border-radius: 4px;
  background: #eef0ff;
  color: #4f46e5;
  font-size: 10px;
  font-style: normal;
}

.score-cell {
  color: #0f172a;
  font-weight: 700;
}

.refresh-button {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #e1e5ee;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
}

.empty-block {
  padding: 46px 0;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}

.rank-pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.empty-state h2 {
  color: #334155;
}

.empty-state a {
  color: #4f46e5;
  font-size: 13px;
}

@media (max-width: 900px) {
  .page-inner {
    width: calc(100% - 32px);
  }

  .contest-banner {
    grid-template-columns: 1fr;
    padding: 26px 22px;
    gap: 24px;
  }

  .banner-side {
    padding-left: 0;
    padding-top: 20px;
    border-left: 0;
    border-top: 1px solid #edf0f6;
  }

  .countdown {
    font-size: 26px;
  }

  .problem-row {
    grid-template-columns: 34px minmax(0, 1fr) 60px;
    gap: 10px;
  }

  .problem-action {
    grid-column: 2 / -1;
    text-align: left;
    padding-bottom: 2px;
  }

  .problem-score {
    text-align: right;
  }

  .problem-panel,
  .rank-panel {
    padding: 20px 16px;
  }
}
</style>
