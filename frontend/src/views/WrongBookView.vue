<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Collection,
  MagicStick,
  RefreshRight,
  Reading,
  Star
} from '@element-plus/icons-vue'
import { listWrongQuestions, markMastered } from '@/api/profile'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const loadError = ref('')

const features = [
  {
    icon: RefreshRight,
    title: '自动收录',
    text: '提交结果为 WA / TLE / RE 等未通过状态时，自动进入错题本，不需要手动收藏。'
  },
  {
    icon: MagicStick,
    title: '分层复盘',
    text: '从编译错误、边界条件到算法思路逐层提示，先定位问题，再决定要不要看题解。'
  },
  {
    icon: Star,
    title: '同类巩固',
    text: '按知识点推荐同类型题目，连续做对后再标记掌握，形成练习闭环。'
  }
]

async function loadWrongQuestions() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listWrongQuestions({ page: 1, size: 20 })
    rows.value = data?.records || []
  } catch (error) {
    loadError.value = error.message || '错题本接口暂未接入'
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function handleMastered(row) {
  try {
    await markMastered(row.questionId)
    ElMessage.success('已标记为掌握')
    rows.value = rows.value.filter((item) => item.questionId !== row.questionId)
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(loadWrongQuestions)
</script>

<template>
  <div class="wrong-page">
    <div class="page-head">
      <div>
        <div class="page-eyebrow">Review Space</div>
        <h1 class="page-title">错题本</h1>
        <p class="page-subtitle">
          做错的题不会消失。把它们收在一起，按知识点复盘，直到真正掌握。
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
        <span>待复习</span>
        <strong>{{ rows.length }}</strong>
      </div>
      <div class="summary-item">
        <span>已掌握</span>
        <strong>0</strong>
      </div>
      <div class="summary-item">
        <span>本周新增</span>
        <strong>0</strong>
      </div>
      <div class="summary-note">
        <el-icon><Collection /></el-icon>
        <span>答错后自动收录，无需手动添加</span>
      </div>
    </section>

    <section class="panel list-panel">
      <div class="panel-head">
        <div>
          <div class="panel-title">待复习题目</div>
          <div class="panel-subtitle">按最近一次答错时间排序</div>
        </div>
        <el-tag type="info" effect="plain" round size="small">S8 接入</el-tag>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="questionId">
        <el-table-column label="题目" min-width="320">
          <template #default="{ row }">
            <div class="wrong-title">{{ row.title || `题目 ${row.questionId}` }}</div>
            <div class="wrong-sub">答错 {{ row.wrongCount || 0 }} 次</div>
          </template>
        </el-table-column>
        <el-table-column label="知识点" width="140">
          <template #default="{ row }">{{ row.knowledgePointName || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="最近答错" width="170">
          <template #default="{ row }">{{ row.lastWrongTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/question/${row.questionId}`)">
              重新练习
            </el-button>
            <el-button link @click="handleMastered(row)">标记掌握</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            :description="
              loadError ? '错题本接口尚未接入（S8），完成判题后这里会自动出现记录' : '还没有错题，继续保持'
            "
            :image-size="96"
          >
            <el-button type="primary" plain @click="router.push('/question')">
              去挑一道题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-empty>
        </template>
      </el-table>
    </section>

    <section class="feature-grid">
      <article v-for="feature in features" :key="feature.title" class="panel feature-card">
        <div class="feature-icon">
          <el-icon><component :is="feature.icon" /></el-icon>
        </div>
        <h3>{{ feature.title }}</h3>
        <p>{{ feature.text }}</p>
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

.list-panel {
  overflow: hidden;
}

.wrong-title {
  color: #1e293b;
  font-size: 13.5px;
  font-weight: 600;
}

.wrong-sub {
  margin-top: 3px;
  color: #a0a9ba;
  font-size: 10.5px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.feature-card {
  padding: 19px 20px;
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  margin-bottom: 13px;
  border-radius: 9px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 17px;
}

.feature-card h3 {
  color: #1e293b;
  font-size: 13px;
  font-weight: 650;
}

.feature-card p {
  margin-top: 6px;
  color: #7c869c;
  font-size: 11.5px;
  line-height: 1.75;
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

  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
