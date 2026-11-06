<template>
  <section class="admin-page">
    <div class="page-inner">
      <header class="admin-head">
        <div>
          <router-link class="back-link" to="/contests">返回竞赛</router-link>
          <h1>创建竞赛</h1>
          <p>从题库选题，或直接添加只属于本场竞赛的题目。</p>
        </div>
        <span class="admin-badge">管理员</span>
      </header>

      <div class="admin-layout">
        <main class="admin-main">
          <section class="panel">
            <div class="panel-head">
              <h2>竞赛信息</h2>
              <span>创建后不能修改题单</span>
            </div>
            <div class="form-grid">
              <label class="field wide">
                <span>标题</span>
                <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="例如：CodeAgent 周赛 #2" />
              </label>
              <label class="field wide">
                <span>说明</span>
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="3"
                  maxlength="2000"
                  placeholder="比赛规则、难度说明或参赛提示"
                />
              </label>
              <label class="field">
                <span>开始时间</span>
                <el-date-picker
                  v-model="form.startTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm"
                  placeholder="选择开始时间"
                />
              </label>
              <label class="field">
                <span>结束时间</span>
                <el-date-picker
                  v-model="form.endTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm"
                  placeholder="选择结束时间"
                />
              </label>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>从题库选题</h2>
              <span>已选 {{ form.questionIds.length }} 道</span>
            </div>
            <el-input v-model="keyword" clearable placeholder="搜索公共题目标题" class="search" @input="filterQuestions" />
            <el-checkbox-group v-model="form.questionIds" class="question-picker">
              <el-checkbox v-for="item in filteredQuestions" :key="item.id" :value="item.id" class="question-option">
                <span class="option-title">{{ item.title }}</span>
                <span class="option-meta">{{ item.knowledgePointName || '未分类' }} · {{ difficultyText(item.difficulty) }}</span>
              </el-checkbox>
            </el-checkbox-group>
            <p v-if="!filteredQuestions.length" class="muted">没有匹配的题目。</p>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>竞赛专属题</h2>
              <button class="text-btn" type="button" @click="addDraft">+ 添加题目</button>
            </div>
            <p class="panel-note">这些题只出现在本场竞赛，不会进入公共题库，也不会计入全站段位。</p>

            <div v-if="!drafts.length" class="empty-draft">还没有专属题，可以从上方题库选题，或添加一道新题。</div>

            <article v-for="(draft, index) in drafts" :key="draft.key" class="draft-card">
              <div class="draft-head">
                <strong>题目 {{ index + 1 }}</strong>
                <button class="remove-btn" type="button" @click="drafts.splice(index, 1)">删除</button>
              </div>
              <div class="form-grid">
                <label class="field wide">
                  <span>标题</span>
                  <el-input v-model="draft.title" maxlength="128" placeholder="题目标题" />
                </label>
                <label class="field wide">
                  <span>题目描述</span>
                  <el-input v-model="draft.description" type="textarea" :rows="2" maxlength="20000" placeholder="题干说明" />
                </label>
                <label class="field">
                  <span>输入格式</span>
                  <el-input v-model="draft.inputDesc" placeholder="例如：第一行一个整数 n" />
                </label>
                <label class="field">
                  <span>输出格式</span>
                  <el-input v-model="draft.outputDesc" placeholder="例如：输出一个整数" />
                </label>
                <label class="field">
                  <span>知识点</span>
                  <el-select
                    v-model="draft.knowledgePointIds"
                    multiple
                    filterable
                    collapse-tags
                    collapse-tags-tooltip
                    :max-collapse-tags="2"
                    placeholder="可多选知识点"
                  >
                    <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
                  </el-select>
                </label>
                <label class="field">
                  <span>难度</span>
                  <el-select v-model="draft.difficulty">
                    <el-option :value="1" label="入门" />
                    <el-option :value="2" label="进阶" />
                    <el-option :value="3" label="挑战" />
                  </el-select>
                </label>
                <label class="field">
                  <span>样例输入</span>
                  <el-input v-model="draft.sampleInput" type="textarea" :rows="2" placeholder="输入数据" />
                </label>
                <label class="field">
                  <span>样例输出</span>
                  <el-input v-model="draft.sampleOutput" type="textarea" :rows="2" placeholder="期望输出" />
                </label>
              </div>
            </article>
          </section>
        </main>

        <aside class="admin-side">
          <div class="side-card">
            <h3>本场题单</h3>
            <p class="side-total">{{ totalCount }} 道题</p>
            <ol class="side-list">
              <li v-for="(item, index) in selectedItems" :key="item.key">
                <span class="label">{{ labelOf(index) }}</span>
                <span>{{ item.title }}</span>
              </li>
            </ol>
            <p v-if="!totalCount" class="muted">至少选择或添加一道题。</p>
            <el-button class="submit-btn" type="primary" :loading="saving" @click="submit">创建竞赛</el-button>
          </div>
          <div class="side-card tip-card">
            <h3>创建后</h3>
            <p>竞赛会立即出现在竞赛列表。开始时间到达后，参赛者可以报名并提交。</p>
          </div>
        </aside>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAdminContest } from '@/api/contest'
import { listKnowledgePoints, listQuestions } from '@/api/question'

const router = useRouter()
const knowledgePoints = ref([])
const questions = ref([])
const filteredQuestions = ref([])
const keyword = ref('')
const saving = ref(false)
const form = reactive({
  title: '',
  description: '',
  startTime: defaultTime(1),
  endTime: defaultTime(3),
  questionIds: []
})
const drafts = ref([])

const totalCount = computed(() => form.questionIds.length + drafts.value.length)
const selectedItems = computed(() => {
  const fromBank = form.questionIds.map((id) => {
    const found = questions.value.find((item) => item.id === id)
    return { key: `bank-${id}`, title: found?.title || `题目 ${id}` }
  })
  const fromDrafts = drafts.value.map((item) => ({ key: item.key, title: item.title || '未命名题目' }))
  return [...fromBank, ...fromDrafts]
})

function pad(value) {
  return String(value).padStart(2, '0')
}

function defaultTime(offsetHours) {
  const date = new Date(Date.now() + offsetHours * 60 * 60 * 1000)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function difficultyText(value) {
  return { 1: '入门', 2: '进阶', 3: '挑战' }[value] || '未设置'
}

function labelOf(index) {
  return String.fromCharCode(65 + index)
}

function filterQuestions() {
  const text = keyword.value.trim().toLowerCase()
  filteredQuestions.value = text
    ? questions.value.filter((item) => item.title.toLowerCase().includes(text))
    : questions.value
}

function addDraft() {
  drafts.value.push({
    key: `draft-${Date.now()}-${Math.random()}`,
    title: '',
    description: '',
    inputDesc: '',
    outputDesc: '',
    difficulty: 1,
    knowledgePointIds: [],
    sampleInput: '',
    sampleOutput: ''
  })
}

async function loadOptions() {
  try {
    const [kps, page] = await Promise.all([
      listKnowledgePoints(),
      listQuestions({ page: 1, size: 50, type: 'PUBLIC' })
    ])
    knowledgePoints.value = kps || []
    questions.value = page?.records || []
    filteredQuestions.value = questions.value
  } catch {
    ElMessage.error('题库或知识点加载失败')
  }
}

function validateDraft(draft) {
  if (!draft.title.trim() || !draft.description.trim() || !draft.knowledgePointIds?.length) {
    return '每题都需要标题、描述和知识点'
  }
  if (!draft.sampleInput.trim() || !draft.sampleOutput.trim()) {
    return '每题至少需要一组样例输入和输出'
  }
  return ''
}

async function submit() {
  if (!form.title.trim()) return ElMessage.warning('请填写竞赛标题')
  if (!form.startTime || !form.endTime) return ElMessage.warning('请选择起止时间')
  if (new Date(form.endTime.replace(' ', 'T')) <= new Date(form.startTime.replace(' ', 'T'))) {
    return ElMessage.warning('结束时间必须晚于开始时间')
  }
  if (!totalCount.value) return ElMessage.warning('至少选择或添加一道题')
  for (const draft of drafts.value) {
    const error = validateDraft(draft)
    if (error) return ElMessage.warning(error)
  }

  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      startTime: form.startTime,
      endTime: form.endTime,
      questionIds: form.questionIds,
      newQuestions: drafts.value.map((draft) => ({
        title: draft.title.trim(),
        description: draft.description,
        inputDesc: draft.inputDesc,
        outputDesc: draft.outputDesc,
        difficulty: draft.difficulty,
        knowledgePointId: draft.knowledgePointIds[0],
        knowledgePointIds: [...draft.knowledgePointIds],
        visibility: 'PRIVATE',
        samples: [{ inputData: draft.sampleInput, expectedOutput: draft.sampleOutput }]
      }))
    }
    const contestId = await createAdminContest(payload)
    ElMessage.success('竞赛已创建')
    router.replace({ name: 'contest-detail', params: { id: contestId } })
  } catch (error) {
    ElMessage.error(error?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadOptions)
</script>

<style scoped>
.admin-page {
  padding: 42px 0 30px;
}

.admin-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}

.back-link {
  display: inline-flex;
  margin-bottom: 18px;
  color: #64748b;
  font-size: 12px;
  text-decoration: none;
}

.admin-head h1 {
  margin: 0;
  color: #111827;
  font-size: 36px;
  letter-spacing: -1px;
}

.admin-head p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 13px;
}

.admin-badge {
  padding: 6px 11px;
  border: 1px solid #dfe6ed;
  border-radius: 999px;
  color: #3d5a78;
  background: #f5f7f9;
  font-size: 11px;
}

.admin-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.admin-main {
  display: grid;
  gap: 18px;
}

.panel,
.side-card {
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  background: #fff;
}

.panel {
  padding: 22px 24px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h2,
.side-card h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 650;
}

.panel-head span,
.panel-note,
.muted,
.side-card p {
  color: #8a94a6;
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.field.wide {
  grid-column: span 2;
}

.field > span {
  color: #4b5a70;
  font-size: 12px;
  font-weight: 600;
}

.field :deep(.el-select),
.field :deep(.el-date-editor) {
  width: 100%;
}

.search {
  max-width: 380px;
  margin-bottom: 12px;
}

.question-picker {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
  max-height: 260px;
  overflow: auto;
}

.question-option {
  height: auto;
  min-height: 44px;
  margin: 0;
  padding: 5px 0;
  align-items: flex-start;
}

.question-option :deep(.el-checkbox__label) {
  display: grid;
  gap: 3px;
  line-height: 1.35;
}

.option-title {
  color: #1e293b;
  font-size: 13px;
}

.option-meta {
  color: #8a94a6;
  font-size: 11px;
}

.text-btn,
.remove-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: #335f8f;
  cursor: pointer;
  font-size: 12px;
}

.remove-btn {
  color: #b42318;
}

.empty-draft {
  padding: 24px;
  border: 1px dashed #dfe6ed;
  border-radius: 8px;
  color: #8a94a6;
  font-size: 12px;
  text-align: center;
}

.draft-card {
  margin-top: 14px;
  padding: 16px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fbfcfd;
}

.draft-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: #334155;
  font-size: 13px;
}

.admin-side {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 88px;
}

.side-card {
  padding: 20px;
}

.side-total {
  margin: 10px 0 14px;
  color: #1f3a5f;
  font-size: 24px;
  font-weight: 700;
}

.side-list {
  display: grid;
  gap: 9px;
  margin: 0 0 18px;
  padding: 0;
  list-style: none;
}

.side-list li {
  display: flex;
  gap: 9px;
  color: #334155;
  font-size: 12px;
  line-height: 1.4;
}

.side-list .label {
  flex: 0 0 18px;
  color: #8a94a6;
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.submit-btn {
  width: 100%;
}

.tip-card p {
  margin: 10px 0 0;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-side {
    position: static;
    grid-row: 1;
  }
}

@media (max-width: 640px) {
  .admin-page {
    padding-top: 26px;
  }

  .admin-head {
    align-items: flex-start;
  }

  .admin-head h1 {
    font-size: 30px;
  }

  .panel {
    padding: 18px 16px;
  }

  .form-grid,
  .question-picker {
    grid-template-columns: 1fr;
  }

  .field.wide {
    grid-column: auto;
  }
}
</style>
