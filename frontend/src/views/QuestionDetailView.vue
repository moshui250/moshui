<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  CircleCheck,
  Collection,
  InfoFilled,
  MagicStick,
  VideoPlay
} from '@element-plus/icons-vue'
import { getQuestion } from '@/api/question'
import MarkdownBody from '@/components/MarkdownBody.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const question = ref(null)
const loadError = ref('')

const difficultyMap = {
  1: { label: '入门', type: 'success' },
  2: { label: '进阶', type: 'warning' },
  3: { label: '挑战', type: 'danger' }
}

const difficulty = computed(() => difficultyMap[question.value?.difficulty] || difficultyMap[1])
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

// 题库描述通常已经按 Markdown 写好输入/输出小节；缺失时再补，避免重复标题。
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

/**
 * 按路由中的题目 ID 加载题面、样例和知识点信息。
 * @returns {Promise<void>} 加载结束后完成
 */
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

/**
 * 进入当前题目的答题页。
 * @returns {void}
 */
function startPractice() {
  if (!question.value) return
  router.push({ name: 'question-solve', params: { id: question.value.id } })
}

function openAiCoach() {
  ElMessage.info('AI 调试将在提交失败后接入（S12）')
}

onMounted(loadQuestion)
</script>

<template>
  <div v-loading="loading" class="detail-page">
    <button class="back-link" @click="router.push('/question')">
      <el-icon><ArrowLeft /></el-icon>
      返回题库
    </button>

    <div v-if="!loading && !question" class="panel empty-panel">
      <el-empty :description="loadError || '题目不存在，或已被删除'" :image-size="100">
        <el-button type="primary" @click="router.push('/question')">回到题库</el-button>
        <el-button @click="loadQuestion">重新加载</el-button>
      </el-empty>
    </div>

    <div v-else-if="question" class="detail-grid">
      <article class="panel statement">
        <header class="statement-head">
          <div class="page-eyebrow">Problem {{ question.id }}</div>
          <h1>{{ question.title }}</h1>
          <div class="meta-row">
            <el-tag :type="difficulty.type" effect="light" round size="small">
              {{ difficulty.label }}
            </el-tag>
            <span class="meta-chip">{{ knowledgeName }}</span>
            <span class="meta-chip">{{ question.type === 'PERSONAL' ? '个人题' : '公共题' }}</span>
            <span v-if="question.solved" class="meta-solved">
              <el-icon><CircleCheck /></el-icon>
              已通过
            </span>
          </div>
        </header>

        <div class="statement-body">
          <section class="statement-section statement-markdown">
            <MarkdownBody :content="statementMarkdown" />
          </section>
        </div>
      </article>

      <aside class="side-column">
        <section class="panel sample-panel">
          <div class="panel-head">
            <div>
              <div class="panel-title">样例输入输出</div>
              <div class="panel-subtitle">提交前先手动推演一遍</div>
            </div>
            <span class="sample-count">{{ samples.length }} 组</span>
          </div>

          <div v-if="samples.length" class="sample-list">
            <div v-for="sample in samples" :key="sample.index" class="sample-card">
              <div class="sample-label">示例 {{ sample.index }}</div>
              <div class="io-block">
                <span class="io-label">输入</span>
                <pre>{{ sample.input || '(空)' }}</pre>
              </div>
              <div class="io-block">
                <span class="io-label">输出</span>
                <pre>{{ sample.output || '(空)' }}</pre>
              </div>
            </div>
          </div>
          <div v-else class="sample-empty">
            <el-icon><InfoFilled /></el-icon>
            暂无公开样例
          </div>
        </section>

        <section class="panel side-card">
          <div class="side-title">题目信息</div>
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
              <strong>{{ question.type === 'PERSONAL' ? '个人题' : '公共题' }}</strong>
            </div>
            <div class="info-row">
              <span>状态</span>
              <strong :class="{ 'is-done': question.solved }">
                {{ question.solved ? '已通过' : '未作答' }}
              </strong>
            </div>
          </div>

          <el-button class="start-btn" type="primary" @click="startPractice">
            <el-icon><VideoPlay /></el-icon>
            开始作答
          </el-button>
          <el-button class="ai-btn" @click="openAiCoach">
            <el-icon><MagicStick /></el-icon>
            AI 调试
            <span class="soon">S12</span>
          </el-button>
        </section>

        <section class="panel tip-card">
          <div class="tip-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <div>
            <strong>先自己试一次</strong>
            <p>提交失败后，AI 会从编译错误、边界条件到算法思路逐层提示，不会直接给出完整代码。</p>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 300px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 17px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 12.5px;
  cursor: pointer;
}

.back-link:hover {
  color: var(--ca-primary);
}

.empty-panel {
  padding: 34px 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 308px;
  gap: 18px;
  align-items: start;
}

.statement {
  overflow: hidden;
}

.statement-head {
  padding: 25px 28px 21px;
  border-bottom: 1px solid #eef2f7;
}

.statement-head h1 {
  color: var(--ca-ink);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
  line-height: 1.35;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 9px;
  margin-top: 13px;
}

.meta-chip {
  padding: 2px 8px;
  border: 1px solid #e6ebf4;
  border-radius: 5px;
  background: #f8fafc;
  color: #64748b;
  font-size: 11px;
}

.meta-solved {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #059669;
  font-size: 11.5px;
  font-weight: 500;
}

.statement-body {
  padding-bottom: 8px;
}

.statement-section {
  padding: 21px 28px 0;
}

.statement-section:last-child {
  padding-bottom: 22px;
}

.statement-section h3 {
  margin-bottom: 9px;
  color: #1e293b;
  font-size: 13.5px;
  font-weight: 650;
}

.scope-hint {
  display: grid;
  gap: 5px;
  padding: 10px 12px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fbfcfe;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.side-column {
  position: sticky;
  top: 84px;
  display: grid;
  gap: 14px;
}

.sample-panel {
  overflow: hidden;
}

.sample-count {
  color: #94a3b8;
  font-size: 11px;
}

.sample-list {
  display: grid;
  gap: 11px;
  padding: 13px;
}

.sample-card {
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 9px;
  background: #fbfcfe;
}

.sample-label {
  padding: 7px 11px;
  border-bottom: 1px solid #eef2f7;
  color: #7c869c;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.4px;
}

.io-block {
  padding: 9px 11px;
}

.io-block + .io-block {
  border-top: 1px solid #eef2f7;
}

.io-label {
  display: block;
  margin-bottom: 5px;
  color: #9aa4b6;
  font-size: 10px;
}

.io-block pre {
  max-height: 150px;
  margin: 0;
  overflow: auto;
  color: #334155;
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
  font-size: 11.5px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.sample-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 22px 16px;
  color: #94a3b8;
  font-size: 12px;
}

.side-card {
  padding: 19px;
}

.side-title {
  margin-bottom: 12px;
  color: var(--ca-ink);
  font-size: 13px;
  font-weight: 650;
}

.info-list {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
}

.info-row span {
  color: #94a3b8;
}

.info-row strong {
  overflow: hidden;
  color: #334155;
  font-weight: 550;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-row strong.is-done {
  color: #059669;
}

.start-btn,
.ai-btn {
  width: 100%;
  height: 38px;
  border-radius: 8px;
}

.ai-btn {
  margin: 8px 0 0;
}

.soon {
  margin-left: 4px;
  padding: 1px 5px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 9px;
}

.tip-card {
  display: flex;
  gap: 11px;
  padding: 16px;
  background: #fbfcff;
}

.tip-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 29px;
  height: 29px;
  border-radius: 8px;
  background: #eef2ff;
  color: #6366f1;
  font-size: 15px;
}

.tip-card strong {
  color: #334155;
  font-size: 12px;
}

.tip-card p {
  margin-top: 4px;
  color: #7c869c;
  font-size: 11px;
  line-height: 1.65;
}

@media (max-width: 1000px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr) 280px;
  }
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    position: static;
  }
}

@media (max-width: 560px) {
  .statement-head,
  .statement-section {
    padding-left: 18px;
    padding-right: 18px;
  }
}
</style>
