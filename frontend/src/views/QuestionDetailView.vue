<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CircleCheck, Collection, InfoFilled, VideoPlay } from '@element-plus/icons-vue'
import { getQuestion } from '@/api/question'
import MarkdownBody from '@/components/MarkdownBody.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const question = ref(null)
const loadError = ref('')

const difficultyMap = {
  1: { label: '入门', className: 'is-easy' },
  2: { label: '进阶', className: 'is-medium' },
  3: { label: '挑战', className: 'is-hard' }
}

const difficulty = computed(
  () => difficultyMap[question.value?.difficulty] || { label: '未分级', className: 'is-unknown' }
)
const knowledgeName = computed(
  () => question.value?.knowledgePoint?.name || question.value?.knowledgePointName || '未分类'
)
const samples = computed(() =>
  (question.value?.samples || []).map((sample, index) => ({
    index: index + 1,
    input: sample.inputData ?? sample.input ?? '',
    output: sample.expectedOutput ?? sample.output ?? ''
  }))
)

const typeText = computed(() => {
  const value = question.value
  if (!value) return '公共题'
  if (value.type === 'PUBLIC') return '公共题'
  return value.visibility === 'SHARED' ? '共享题' : '私有题'
})

// 题面 Markdown 通常已经包含输入/输出小节，缺失时才补，避免重复标题。
const statementMarkdown = computed(() => {
  const description = question.value?.description || ''
  const missing = []

  if (!/^#{1,6}\s*输入格式/m.test(description)) {
    missing.push(`## 输入格式\n\n${question.value?.inputDesc || '暂无输入说明'}`)
  }
  if (!/^#{1,6}\s*输出格式/m.test(description)) {
    missing.push(`## 输出格式\n\n${question.value?.outputDesc || '暂无输出说明'}`)
  }

  return [description, ...missing].filter(Boolean).join('\n\n')
})

async function loadQuestion() {
  loading.value = true
  loadError.value = ''
  try {
    question.value = await getQuestion(route.params.id)
  } catch (error) {
    question.value = null
    loadError.value = error?.message || '题目加载失败'
  } finally {
    loading.value = false
  }
}

function startPractice() {
  if (!question.value) return
  router.push({ name: 'question-solve', params: { id: question.value.id } })
}

const prefetchSolve = () => import('@/views/QuestionSolveView.vue')

function backToList() {
  const from = route.query.from
  if (typeof from === 'string' && from.startsWith('/question') && !from.startsWith('//')) {
    router.push(from)
    return
  }
  router.push({ name: 'question-list' })
}

onMounted(loadQuestion)
</script>

<template>
  <div class="detail-page">
    <button class="back-link" type="button" @click="backToList">
      <el-icon><ArrowLeft /></el-icon>
      返回题库
    </button>

    <div v-if="loading" class="detail-skeleton">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="!question" class="empty-panel">
      <el-empty :description="loadError || '题目不存在，或已被删除'" :image-size="96">
        <el-button type="primary" @click="backToList">回到题库</el-button>
        <el-button @click="loadQuestion">重新加载</el-button>
      </el-empty>
    </div>

    <template v-else>
      <header class="detail-head">
        <div class="title-block">
          <h1>{{ question.title }}</h1>
          <div class="meta-row">
            <span class="level-chip">
              <i class="level-dot" :class="difficulty.className"></i>
              {{ difficulty.label }}
            </span>
            <span class="meta-chip">{{ knowledgeName }}</span>
            <span class="meta-chip">{{ typeText }}</span>
            <span v-if="question.solved" class="solved-chip">
              <el-icon><CircleCheck /></el-icon>
              已通过
            </span>
          </div>
        </div>

        <div class="head-cta">
          <span class="sample-count">{{ samples.length }} 组公开样例</span>
          <el-button type="primary" size="large" :icon="VideoPlay" @mouseenter="prefetchSolve" @focus="prefetchSolve" @click="startPractice">
            开始作答
          </el-button>
        </div>
      </header>

      <div class="detail-grid">
        <article class="statement-card">
          <MarkdownBody :content="statementMarkdown" />
        </article>

        <aside class="side-column">
          <section class="side-card info-card">
            <div class="side-title">本题信息</div>
            <div class="info-list">
              <div class="info-row">
                <span>知识点</span>
                <strong>{{ knowledgeName }}</strong>
              </div>
              <div class="info-row">
                <span>难度</span>
                <strong>{{ difficulty.label }}</strong>
              </div>
              <div class="info-row">
                <span>题型</span>
                <strong>{{ typeText }}</strong>
              </div>
              <div class="info-row">
                <span>状态</span>
                <strong :class="{ 'is-done': question.solved }">
                  {{ question.solved ? '已通过' : '未作答' }}
                </strong>
              </div>
            </div>
          </section>

          <section class="side-card samples-card">
            <div class="side-head">
              <div>
                <div class="side-title">样例输入输出</div>
              </div>
              <span class="sample-count">{{ samples.length }} 组</span>
            </div>

            <div v-if="samples.length" class="sample-list">
              <div v-for="sample in samples" :key="sample.index" class="sample-item">
                <div class="sample-label">示例 {{ sample.index }}</div>
                <div class="io-row">
                  <span>输入</span>
                  <pre>{{ sample.input || '(空)' }}</pre>
                </div>
                <div class="io-row">
                  <span>输出</span>
                  <pre>{{ sample.output || '(空)' }}</pre>
                </div>
              </div>
            </div>
            <div v-else class="sample-empty">
              <el-icon><InfoFilled /></el-icon>
              暂无公开样例
            </div>
          </section>

          <section class="side-card ai-card">
            <div class="ai-icon">
              <el-icon><Collection /></el-icon>
            </div>
            <div>
              <strong>失败后再看提示</strong>
              <p>提交错误后，答题页会定位失败用例并给分层提示，不直接给完整答案。</p>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  width: min(1240px, calc(100% - 48px));
  margin: 0 auto;
  padding: 32px 0 64px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
}

.back-link:hover {
  color: #1f3a5f;
}

.detail-skeleton,
.empty-panel {
  padding: 36px 28px;
  border: 1px solid #e5e9f2;
  border-radius: 12px;
  background: #fff;
}

.detail-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 30px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e6e9f1;
}

.eyebrow {
  display: none;
  margin-bottom: 9px;
  color: #1f3a5f;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 2px;
}

.title-block h1 {
  color: #0f172a;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -1.2px;
  line-height: 1.18;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
}

.level-chip,
.meta-chip,
.solved-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 24px;
  padding: 0 9px;
  border: 1px solid #e6ebf4;
  border-radius: 6px;
  background: #f8fafc;
  color: #64748b;
  font-size: 11px;
}

.level-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
}

.level-dot.is-easy { background: #2f7d5b; }
.level-dot.is-medium { background: #a16207; }
.level-dot.is-hard { background: #b42318; }

.solved-chip {
  border-color: #d5e7dd;
  background: #edf5f1;
  color: #2f7d5b;
}

.head-cta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.sample-count {
  color: #94a3b8;
  font-size: 11px;
  white-space: nowrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 20px;
  align-items: start;
  padding-top: 22px;
}

.statement-card {
  padding: 28px 32px 34px;
  border: 1px solid #e5e9f2;
  border-radius: 12px;
  background: #fff;
}

.statement-kicker {
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #eef2f7;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.side-column {
  position: sticky;
  top: 88px;
  display: grid;
  gap: 14px;
}

.side-card {
  border: 1px solid #e5e9f2;
  border-radius: 10px;
  background: #fff;
}

.info-card,
.samples-card {
  padding: 18px;
}

.side-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.side-sub {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 10.5px;
}

.side-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.info-list {
  display: grid;
  gap: 11px;
  margin: 15px 0 17px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.info-row span {
  color: #94a3b8;
}

.info-row strong {
  overflow: hidden;
  color: #334155;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-row strong.is-done {
  color: #2f7d5b;
}

.sample-list {
  display: grid;
  gap: 10px;
}

.sample-item {
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 9px;
  background: #fbfcfe;
}

.sample-label {
  padding: 7px 10px;
  border-bottom: 1px solid #eef2f7;
  color: #7c869c;
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 0.4px;
}

.io-row {
  padding: 9px 10px;
}

.io-row + .io-row {
  border-top: 1px solid #eef2f7;
}

.io-row > span {
  display: block;
  margin-bottom: 4px;
  color: #9aa4b6;
  font-size: 9.5px;
}

.io-row pre {
  max-height: 140px;
  margin: 0;
  overflow: auto;
  color: #334155;
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
  font-size: 11px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.sample-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 0;
  color: #94a3b8;
  font-size: 11.5px;
}

.ai-card {
  display: flex;
  gap: 11px;
  padding: 16px;
  background: #fbfcfd;
}

.ai-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 15px;
}

.ai-card strong {
  color: #334155;
  font-size: 12px;
}

.ai-card p {
  margin-top: 4px;
  color: #7c869c;
  font-size: 11px;
  line-height: 1.65;
}

@media (max-width: 1000px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr) 300px;
  }

  .title-block h1 {
    font-size: 36px;
  }
}

@media (max-width: 860px) {
  .detail-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .head-cta {
    width: 100%;
    justify-content: space-between;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    position: static;
  }
}

@media (max-width: 560px) {
  .detail-page {
    width: calc(100% - 28px);
    padding-top: 22px;
  }

  .title-block h1 {
    font-size: 30px;
    letter-spacing: -1px;
  }

  .statement-card {
    padding: 20px 18px 24px;
  }

  .head-cta .sample-count {
    display: none;
  }

  .head-cta .el-button {
    width: 100%;
  }
}
</style>
