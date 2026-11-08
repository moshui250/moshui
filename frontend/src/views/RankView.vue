<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh, QuestionFilled } from '@element-plus/icons-vue'
import { getRanking } from '@/api/rank'
import TierBadge from '@/components/TierBadge.vue'
import TierHelpDialog from '@/components/TierHelpDialog.vue'

const loading = ref(false)
const loadError = ref('')
const helpVisible = ref(false)
const data = ref({
  top: [],
  me: null,
  totalUsers: 0,
  refreshedAt: null,
  tiers: []
})
let timer = null

const me = computed(() => data.value.me)
const progress = computed(() => me.value?.progress)
const progressPercent = computed(() => {
  const value = Number(progress.value?.percent || 0)
  return Math.max(0, Math.min(100, value * 100))
})
const progressHint = computed(() => {
  if (!progress.value) return ''
  if (progress.value.maxTier) return '已达到最高段位'
  const remain = Math.max(0, Number(progress.value.targetAc || 0) - Number(progress.value.currentAc || 0))
  return `距离下一段「${progress.value.nextTierName}」还差 ${remain} 题`
})
const refreshText = computed(() => {
  if (!data.value.refreshedAt) return '等待刷新'
  return `更新于 ${formatTime(data.value.refreshedAt)} · 每 60 秒自动刷新`
})

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

function displayName(row) {
  return row.nickname || '匿名用户'
}

function initial(row) {
  return displayName(row).slice(0, 1).toUpperCase()
}

async function loadRanking() {
  loading.value = true
  loadError.value = ''
  try {
    const result = await getRanking()
    data.value = {
      top: result?.top || [],
      me: result?.me || null,
      totalUsers: Number(result?.totalUsers || 0),
      refreshedAt: result?.refreshedAt || null,
      tiers: result?.tiers || []
    }
  } catch (error) {
    loadError.value = error?.message || '排行榜加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRanking()
  timer = window.setInterval(loadRanking, 60000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="rank-page">
    <header class="page-head compact-head">
      <h1 class="compact-page-title">排名</h1>
      <div class="refresh-block">
        <span>{{ refreshText }}</span>
        <el-button :icon="Refresh" :loading="loading" plain @click="loadRanking">刷新</el-button>
      </div>
    </header>

    <section v-if="me" class="my-card">
      <div class="my-rank">
        <span class="my-label">我的排名</span>
        <div class="my-number">
          <strong v-if="me.onBoard">第 {{ me.rank }} 名</strong>
          <strong v-else>未上榜</strong>
          <span v-if="me.onBoard">全站共 {{ data.totalUsers }} 人</span>
          <span v-else>全站第 {{ me.rank }} 名，仅本人可见</span>
        </div>
      </div>

      <div class="my-tier">
        <TierBadge :tier="me.tier" size="lg" />
        <div class="tier-copy">
          <strong>{{ me.acCount }} 题 AC</strong>
          <span>{{ me.onBoard ? '已进入前 100 名' : '继续做题进入总榜' }}</span>
        </div>
        <el-button
          class="help-button"
          circle
          text
          :icon="QuestionFilled"
          aria-label="查看段位规则"
          @click="helpVisible = true"
        />
      </div>

      <div class="my-progress">
        <div class="progress-head">
          <span>段位进度</span>
          <strong>
            {{ me.progress.currentAc }} / {{ me.progress.maxTier ? '∞' : me.progress.targetAc }}
          </strong>
        </div>
        <div class="progress-track">
          <i :style="{ width: `${progressPercent}%` }"></i>
        </div>
        <p>{{ progressHint }}</p>
      </div>
    </section>

    <section class="board-card">
      <div class="board-head">
        <div>
          <h2>总榜</h2>
          <p>最多展示前 100 名</p>
        </div>
        <el-button
          class="help-button"
          circle
          text
          :icon="QuestionFilled"
          aria-label="查看段位规则"
          @click="helpVisible = true"
        />
      </div>

      <div v-if="loadError" class="load-error">
        <p>{{ loadError }}</p>
        <el-button size="small" plain @click="loadRanking">重试</el-button>
      </div>

      <template v-else>
        <div class="rank-head">
          <span>名次</span>
          <span>用户</span>
          <span>AC 题数</span>
          <span>段位</span>
        </div>

        <div v-if="loading && !data.top.length" class="rank-loading">
          <el-skeleton :rows="8" animated />
        </div>

        <el-empty
          v-else-if="!data.top.length"
          :image-size="72"
          description="还没有人登上总榜"
        />

        <div v-else class="rank-body">
          <div
            v-for="row in data.top"
            :key="row.userId"
            class="rank-row"
            :class="{ 'is-me': row.me }"
          >
            <span class="rank-no" :class="{ 'is-top': row.rank <= 3 }">{{ row.rank }}</span>
            <div class="rank-user">
              <span class="rank-avatar">{{ initial(row) }}</span>
              <span class="rank-name">
                <strong>{{ displayName(row) }}</strong>
              </span>
              <span v-if="row.me" class="me-tag">我</span>
            </div>
            <strong class="rank-ac">{{ row.acCount }}</strong>
            <TierBadge :tier="row.tier" size="sm" />
          </div>
        </div>
      </template>
    </section>

    <TierHelpDialog
      v-model="helpVisible"
      :tiers="data.tiers"
      :current-code="me?.tier?.code"
    />
  </div>
</template>

<style scoped>
.rank-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 34px 24px 64px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.page-title {
  color: #111827;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -1.2px;
  line-height: 1.2;
}

.page-subtitle {
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
}

.refresh-block {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #8a93a6;
  font-size: 11.5px;
  white-space: nowrap;
}

.my-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr) minmax(0, 1.2fr);
  gap: 0;
  margin-bottom: 16px;
  border: 1px solid #e4e7ec;
  border-radius: 12px;
  background: #fff;
}

.my-card > div {
  min-width: 0;
  padding: 22px 24px;
}

.my-card > div + div {
  border-left: 1px solid #edf0f3;
}

.my-label {
  color: #8a93a6;
  font-size: 11px;
}

.my-number {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: 6px;
}

.my-number strong {
  color: #111827;
  font-size: 30px;
  letter-spacing: -1px;
}

.my-number span {
  color: #8a93a6;
  font-size: 11px;
}

.my-tier {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tier-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.tier-copy strong {
  color: #111827;
  font-size: 14px;
}

.tier-copy span {
  color: #8a93a6;
  font-size: 11px;
}

.help-button {
  margin-left: auto;
  color: #8a93a6;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #6b7280;
  font-size: 11px;
}

.progress-head strong {
  color: #374151;
  font-size: 12px;
}

.progress-track {
  height: 6px;
  margin-top: 12px;
  overflow: hidden;
  border-radius: 99px;
  background: #edf0f3;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #1f3a5f;
}

.my-progress p {
  margin-top: 9px;
  color: #8a93a6;
  font-size: 11px;
}

.board-card {
  overflow: hidden;
  border: 1px solid #e4e7ec;
  border-radius: 12px;
  background: #fff;
}

.board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px 16px;
}

.board-head h2 {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.board-head p {
  margin-top: 4px;
  color: #8a93a6;
  font-size: 11.5px;
}

.rank-head,
.rank-row {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr) 110px 140px;
  align-items: center;
  gap: 16px;
}

.rank-head {
  padding: 9px 24px;
  border-top: 1px solid #edf0f3;
  border-bottom: 1px solid #edf0f3;
  color: #8a93a6;
  font-size: 10.5px;
}

.rank-body {
  max-height: 610px;
  overflow-y: auto;
}

.rank-row {
  min-height: 58px;
  padding: 0 24px;
  border-bottom: 1px solid #f1f4f9;
}

.rank-row:last-child {
  border-bottom: 0;
}

.rank-row.is-me {
  background: #f7f8fa;
}

.rank-no {
  color: #8a93a6;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}

.rank-no.is-top {
  color: #111827;
  font-weight: 700;
}

.rank-user {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.rank-avatar {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 12px;
  font-weight: 700;
}

.rank-name {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.rank-name strong {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-name small {
  margin-top: 2px;
  overflow: hidden;
  color: #9aa3b2;
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.me-tag {
  flex-shrink: 0;
  padding: 1px 6px;
  border: 1px solid #1f3a5f33;
  border-radius: 5px;
  color: #1f3a5f;
  font-size: 9px;
}

.rank-ac {
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

.rank-loading {
  padding: 22px 24px;
}

.load-error {
  padding: 30px 24px;
  border-top: 1px solid #edf0f3;
  color: #b42318;
  font-size: 12px;
}

.load-error .el-button {
  margin-top: 10px;
}

@media (max-width: 820px) {
  .rank-page {
    padding: 24px 16px 48px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .page-title {
    font-size: 30px;
  }

  .refresh-block {
    width: 100%;
    justify-content: space-between;
  }

  .my-card {
    grid-template-columns: 1fr;
  }

  .my-card > div + div {
    border-top: 1px solid #edf0f3;
    border-left: 0;
  }

  .my-card > div {
    padding: 16px 18px;
  }

  .rank-head,
  .rank-row {
    grid-template-columns: 34px minmax(0, 1fr) 48px 82px;
    gap: 6px;
    padding-right: 12px;
    padding-left: 12px;
  }

  .rank-head {
    font-size: 10px;
  }

  .rank-row {
    min-height: 54px;
  }

  .rank-avatar {
    width: 24px;
    height: 24px;
    border-radius: 7px;
    font-size: 11px;
  }

  .rank-user {
    gap: 7px;
  }

  .rank-name small {
    display: none;
  }

  .me-tag {
    padding: 1px 4px;
  }
}

@media (min-width: 761px) {
  .page-head {
    justify-content: flex-end;
  }
}
</style>
