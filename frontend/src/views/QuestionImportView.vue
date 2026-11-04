<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete, Plus, UploadFilled } from '@element-plus/icons-vue'
import {
  createQuestion,
  importParsedQuestion,
  listKnowledgePoints,
  parseQuestionImage,
  uploadQuestionImage
} from '@/api/question'

const router = useRouter()

const stage = ref('upload')
const step = computed(() => (stage.value === 'upload' ? 0 : 1))
const knowledgePoints = ref([])
const file = ref(null)
const previewUrl = ref('')
const imageUrl = ref('')
const parseId = ref('')
const recognizing = ref(false)
const progress = ref(0)
const submitting = ref(false)
const missingFields = ref([])
const warnText = ref('')
const manualReason = ref('')

const form = reactive({
  title: '',
  description: '',
  inputDesc: '',
  outputDesc: '',
  difficulty: 1,
  knowledgePointCode: '',
  samples: [{ input: '', output: '' }],
  hiddenCases: []
})

const difficultyOptions = [
  { label: '入门', value: 1 },
  { label: '进阶', value: 2 },
  { label: '挑战', value: 3 }
]

const stageHint = computed(() => {
  if (stage.value === 'confirm') return 'AI 已生成草稿，请逐项核对后再入库'
  if (stage.value === 'manual') return '手动录入模式，不会调用图片识别'
  return '上传题目截图或照片，先识别成草稿，确认后才会入库'
})

function revokePreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

function onFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(raw.type)) {
    ElMessage.error('只支持 JPG、PNG、WebP 图片')
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return
  }
  revokePreview()
  file.value = raw
  previewUrl.value = URL.createObjectURL(raw)
  stage.value = 'upload'
  progress.value = 0
}

function addSample() {
  form.samples.push({ input: '', output: '' })
}

function removeSample(index) {
  if (form.samples.length <= 1) {
    ElMessage.warning('至少保留 1 条样例用例')
    return
  }
  form.samples.splice(index, 1)
}

function addHiddenCase() {
  form.hiddenCases.push({ input: '', output: '' })
}

function removeHiddenCase(index) {
  form.hiddenCases.splice(index, 1)
}

function applyDraft(data) {
  const draft = data?.draft || {}
  form.title = draft.title || ''
  form.description = draft.description || ''
  form.inputDesc = draft.inputDesc || ''
  form.outputDesc = draft.outputDesc || ''
  form.difficulty = Number(draft.difficulty) || 1
  form.knowledgePointCode = draft.knowledgePointCode || ''
  form.samples = Array.isArray(draft.samples) && draft.samples.length
    ? draft.samples.map((item) => ({ input: item.input || '', output: item.output || '' }))
    : [{ input: '', output: '' }]
  form.hiddenCases = []
  parseId.value = data?.parseId || ''
  imageUrl.value = data?.imageUrl || imageUrl.value
  missingFields.value = data?.missingFields || []
  warnText.value = data?.warn || ''
  stage.value = 'confirm'
}

async function recognize() {
  if (!file.value) {
    ElMessage.warning('请先选择一张题目图片')
    return
  }

  recognizing.value = true
  progress.value = 0
  try {
    const uploaded = await uploadQuestionImage(file.value, (value) => {
      progress.value = value
    })
    imageUrl.value = uploaded?.url || ''
    const parsed = await parseQuestionImage(file.value, imageUrl.value, (value) => {
      progress.value = Math.max(progress.value, value)
    })
    applyDraft(parsed)
    ElMessage.success('识别完成，请核对草稿')
  } catch (error) {
    const reason = error?.message || '识别失败，请手动录入'
    ElMessage.error(reason)
    startManual(reason)
  } finally {
    recognizing.value = false
  }
}

function startManual(reason = '') {
  manualReason.value = reason
  stage.value = 'manual'
  if (!form.title && file.value?.name) {
    form.title = file.value.name.replace(/\.[^.]+$/, '')
  }
}

function goUpload() {
  stage.value = 'upload'
  manualReason.value = ''
}

function normalizedSamples() {
  return form.samples.map((item, index) => ({
    inputData: item.input,
    expectedOutput: item.output,
    isSample: true,
    score: 25,
    sortNo: index + 1
  }))
}

function normalizedHiddenCases() {
  return form.hiddenCases.map((item, index) => ({
    inputData: item.input,
    expectedOutput: item.output,
    isSample: false,
    score: 25,
    sortNo: index + 1
  }))
}

function validateForm() {
  if (!form.title.trim()) {
    ElMessage.error('请填写题目标题')
    return false
  }
  if (!form.description.trim()) {
    ElMessage.error('请填写题目描述')
    return false
  }
  const selected = knowledgePoints.value.find(
    (item) => item.code === form.knowledgePointCode || item.id === Number(form.knowledgePointCode)
  )
  if (!selected && stage.value === 'confirm') {
    ElMessage.error('请选择知识点')
    return false
  }
  if (stage.value === 'manual' && !selected) {
    ElMessage.error('请选择知识点')
    return false
  }
  if (!form.samples.length || !form.samples.every((item) => item.input.trim() && item.output.trim())) {
    ElMessage.error('请至少填写 1 条完整的样例输入和输出')
    return false
  }
  return true
}

async function submit() {
  if (!validateForm()) return
  const selected = knowledgePoints.value.find(
    (item) => item.code === form.knowledgePointCode || item.id === Number(form.knowledgePointCode)
  )

  submitting.value = true
  try {
    let questionId
    if (stage.value === 'confirm') {
      if (!parseId.value) {
        ElMessage.error('识别结果已失效，请重新识别或手动录入')
        return
      }
      const result = await importParsedQuestion({
        parseId: parseId.value,
        title: form.title,
        description: form.description,
        inputDesc: form.inputDesc,
        outputDesc: form.outputDesc,
        difficulty: form.difficulty,
        knowledgePointCode: selected?.code || form.knowledgePointCode,
        samples: normalizedSamples(),
        testCases: normalizedHiddenCases(),
        sourceImageUrl: imageUrl.value
      })
      questionId = result?.questionId
      if (result?.needManualConfirm) {
        ElMessage.warning(result.message || '已复制样例生成隐藏用例，请人工确认')
      } else {
        ElMessage.success('题目已入库')
      }
    } else {
      questionId = await createQuestion({
        title: form.title,
        description: form.description,
        inputDesc: form.inputDesc,
        outputDesc: form.outputDesc,
        difficulty: form.difficulty,
        knowledgePointId: selected?.id,
        samples: normalizedSamples(),
        testCases: normalizedHiddenCases(),
        sourceImageUrl: imageUrl.value
      })
      ElMessage.success('个人题目已创建')
    }

    if (questionId) {
      router.push(`/question/${questionId}/solve`)
    }
  } catch (error) {
    ElMessage.error(error?.message || '入库失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const data = await listKnowledgePoints()
    knowledgePoints.value = Array.isArray(data) ? data : []
  } catch {
    ElMessage.error('知识点加载失败，请刷新页面')
  }
})

onBeforeUnmount(revokePreview)
</script>

<template>
  <div class="import-page">
    <section class="import-hero">
      <div>
        <div class="page-eyebrow">Image To Draft</div>
        <h1 class="page-title">图片录题</h1>
        <p class="page-subtitle">
          上传题目截图或照片，AI 先识别成草稿；你核对、修改并确认后才会写入题库。
        </p>
      </div>
      <el-tag type="warning" effect="plain" round>两段式 · 识别结果不直接入库</el-tag>
    </section>

    <section class="panel import-panel">
      <el-steps :active="step" finish-status="success" simple class="import-steps">
        <el-step title="上传图片" />
        <el-step title="核对草稿" />
        <el-step title="确认入库" />
      </el-steps>

      <div class="stage-hint">{{ stageHint }}</div>

      <div v-if="stage === 'upload'" class="upload-stage">
        <el-upload
          drag
          :auto-upload="false"
          :show-file-list="false"
          accept="image/jpeg,image/png,image/webp"
          :on-change="onFileChange"
          class="upload-drop"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-title">拖拽图片到这里，或点击选择</div>
          <div class="upload-sub">支持 JPG / PNG / WebP，最大 5MB；会自动压缩到长边 1600px</div>
        </el-upload>

        <div v-if="previewUrl" class="preview-card">
          <img :src="previewUrl" alt="题目预览" class="preview-image" />
          <div class="preview-meta">
            <span>{{ file?.name }}</span>
            <span>{{ Math.ceil((file?.size || 0) / 1024) }} KB</span>
          </div>
        </div>

        <el-progress
          v-if="recognizing"
          :percentage="progress"
          :stroke-width="8"
          class="upload-progress"
        />

        <div class="upload-actions">
          <el-button type="primary" size="large" :loading="recognizing" @click="recognize">
            开始识别
          </el-button>
          <el-button text @click="startManual()">直接手动录入</el-button>
        </div>
      </div>

      <div v-else class="draft-stage">
        <el-alert
          v-if="stage === 'manual'"
          type="warning"
          :closable="false"
          show-icon
          class="draft-alert"
          title="已切换到手动录入"
          :description="manualReason || '请手动填写题目信息，不会调用图片识别。'"
        />

        <el-alert
          v-else
          type="warning"
          :closable="false"
          show-icon
          class="draft-alert"
          title="请核对 AI 识别结果"
          description="识别结果可能有误，尤其是输入输出格式、公式和样例。确认后才会创建个人题目。"
        />

        <el-alert
          v-if="missingFields.length"
          type="error"
          :closable="false"
          show-icon
          class="draft-alert"
          title="以下字段缺失，请补充"
          :description="missingFields.join('、')"
        />

        <el-alert
          v-if="warnText"
          type="info"
          :closable="false"
          show-icon
          class="draft-alert"
          title="识别提示"
          :description="warnText"
        />

        <el-form label-position="top" class="draft-form">
          <div class="form-grid">
            <el-form-item label="题目标题" required>
              <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：数组元素求和" />
            </el-form-item>

            <el-form-item label="知识点" required>
              <el-select v-model="form.knowledgePointCode" placeholder="请选择知识点" filterable class="full-width">
                <el-option
                  v-for="item in knowledgePoints"
                  :key="item.id"
                  :label="item.name"
                  :value="item.code"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="难度" required>
              <el-radio-group v-model="form.difficulty">
                <el-radio-button v-for="item in difficultyOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </div>

          <el-form-item label="题目描述" required>
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="支持 Markdown" />
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="输入格式">
              <el-input v-model="form.inputDesc" type="textarea" :rows="3" placeholder="例如：第一行一个整数 n" />
            </el-form-item>
            <el-form-item label="输出格式">
              <el-input v-model="form.outputDesc" type="textarea" :rows="3" placeholder="例如：输出一个整数" />
            </el-form-item>
          </div>
        </el-form>

        <div class="case-section">
          <div class="section-head">
            <div>
              <h3>样例用例</h3>
              <p>至少要有一条完整样例，用户可见。</p>
            </div>
            <el-button plain :icon="Plus" @click="addSample">添加样例</el-button>
          </div>

          <div v-for="(item, index) in form.samples" :key="`sample-${index}`" class="case-row">
            <div class="case-index">样例 {{ index + 1 }}</div>
            <el-input v-model="item.input" type="textarea" :rows="2" placeholder="输入" />
            <el-input v-model="item.output" type="textarea" :rows="2" placeholder="输出" />
            <el-button link type="danger" :icon="Delete" @click="removeSample(index)">删除</el-button>
          </div>
        </div>

        <div class="case-section">
          <div class="section-head">
            <div>
              <h3>隐藏用例（可选）</h3>
              <p>不填时会复制样例生成 2 条隐藏用例，入库后需人工确认。</p>
            </div>
            <el-button plain :icon="Plus" @click="addHiddenCase">添加隐藏用例</el-button>
          </div>

          <div v-for="(item, index) in form.hiddenCases" :key="`hidden-${index}`" class="case-row">
            <div class="case-index">隐藏 {{ index + 1 }}</div>
            <el-input v-model="item.input" type="textarea" :rows="2" placeholder="输入" />
            <el-input v-model="item.output" type="textarea" :rows="2" placeholder="输出" />
            <el-button link type="danger" :icon="Delete" @click="removeHiddenCase(index)">删除</el-button>
          </div>

          <div v-if="!form.hiddenCases.length" class="hidden-empty">
            当前没有隐藏用例，提交后将复制样例生成 2 条隐藏用例。
          </div>
        </div>

        <div class="draft-actions">
          <el-button @click="goUpload">重新上传</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">
            确认入库
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.import-page {
  max-width: 1080px;
}

.import-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.import-panel {
  padding: 26px 30px 30px;
}

.import-steps {
  margin-bottom: 16px;
}

.stage-hint {
  margin-bottom: 22px;
  color: #6b7280;
  font-size: 13px;
}

.upload-stage {
  max-width: 760px;
}

.upload-drop {
  width: 100%;
}

.upload-drop :deep(.el-upload-dragger) {
  padding: 38px 24px;
  border-radius: 16px;
  border-color: #c7d2fe;
  background: #f8faff;
}

.upload-icon {
  font-size: 44px;
  color: #6366f1;
}

.upload-title {
  margin-top: 8px;
  color: #1f2937;
  font-size: 17px;
  font-weight: 700;
}

.upload-sub {
  margin-top: 6px;
  color: #8a94a6;
  font-size: 13px;
}

.preview-card {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.preview-image {
  display: block;
  width: 100%;
  max-height: 340px;
  object-fit: contain;
  border-radius: 10px;
  background: #f8fafc;
}

.preview-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  color: #6b7280;
  font-size: 12px;
}

.upload-progress {
  margin-top: 16px;
}

.upload-actions,
.draft-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 22px;
}

.draft-alert {
  margin-bottom: 12px;
  border-radius: 10px;
}

.draft-form {
  margin-top: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

.full-width {
  width: 100%;
}

.case-section {
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px dashed #e5e7eb;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.section-head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 15px;
}

.section-head p {
  margin: 4px 0 0;
  color: #8a94a6;
  font-size: 12px;
}

.case-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) minmax(0, 1fr) 48px;
  align-items: start;
  gap: 10px;
  margin-bottom: 10px;
}

.case-index {
  padding-top: 8px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
}

.hidden-empty {
  padding: 12px 14px;
  border-radius: 10px;
  background: #fffbeb;
  color: #92400e;
  font-size: 12px;
}

@media (max-width: 860px) {
  .import-hero {
    flex-direction: column;
  }

  .form-grid,
  .case-row {
    grid-template-columns: 1fr;
  }

  .case-index {
    padding-top: 0;
  }
}
</style>
