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

const activeStep = computed(() => (stage.value === 'upload' ? 0 : 1))

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
  if (!selected) {
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
    <header class="import-head">
      <div>
        <div class="eyebrow">IMAGE TO DRAFT</div>
        <h1>图片录题</h1>
        <p>上传题目截图或照片，AI 先识别成草稿；核对、修改并确认后，才会写入个人题库。</p>
      </div>
      <div class="head-badge">
        <span class="badge-dot"></span>
        识别结果不直接入库
      </div>
    </header>

    <div class="import-layout">
      <aside class="flow-card">
        <div class="flow-title">录题流程</div>
        <ol class="flow-list">
          <li :class="{ active: activeStep === 0, done: activeStep > 0 }">
            <span class="flow-index">01</span>
            <div>
              <strong>上传图片</strong>
              <p>截图或拍照，最大 5MB</p>
            </div>
          </li>
          <li :class="{ active: activeStep === 1 }">
            <span class="flow-index">02</span>
            <div>
              <strong>核对草稿</strong>
              <p>题面、样例、知识点</p>
            </div>
          </li>
          <li>
            <span class="flow-index">03</span>
            <div>
              <strong>确认入库</strong>
              <p>创建个人题目</p>
            </div>
          </li>
        </ol>
        <div class="flow-note">
          <span>10 分钟</span>
          识别草稿只在内存中保留 10 分钟，过期后请重新识别。
        </div>
      </aside>

      <main class="work-card">
        <div class="work-head">
          <div>
            <span class="work-kicker">{{ stageHint }}</span>
            <h2>{{ stage === 'upload' ? '选择一张题目图片' : stage === 'manual' ? '手动录入题目' : '核对 AI 草稿' }}</h2>
          </div>
          <span class="work-state" :class="{ manual: stage === 'manual' }">
            {{ stage === 'upload' ? '等待上传' : stage === 'manual' ? '手动模式' : '待确认' }}
          </span>
        </div>

        <template v-if="stage === 'upload'">
          <div class="upload-grid">
            <el-upload
              drag
              :auto-upload="false"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp"
              :on-change="onFileChange"
              class="upload-box"
            >
              <div class="drop-icon"><el-icon><UploadFilled /></el-icon></div>
              <div class="drop-title">拖拽题目图片到这里</div>
              <p>或点击浏览文件 · JPG / PNG / WebP · 最大 5MB</p>
              <span class="drop-action">选择图片</span>
            </el-upload>

            <div v-if="previewUrl" class="preview-box">
              <img :src="previewUrl" alt="题目预览" />
              <div class="preview-meta">
                <span class="preview-name">{{ file?.name }}</span>
                <span>{{ Math.ceil((file?.size || 0) / 1024) }} KB</span>
              </div>
            </div>
            <div v-else class="preview-placeholder">
              <span class="preview-mark">PREVIEW</span>
              <p>选择图片后在这里预览</p>
            </div>
          </div>

          <div v-if="recognizing" class="recognizing">
            <div class="recognizing-copy">
              <span class="scan-dot"></span>
              <strong>正在识别题面与样例</strong>
              <span>{{ progress }}%</span>
            </div>
            <el-progress :percentage="progress" :show-text="false" :stroke-width="6" color="#1f3a5f" />
            <div class="skeleton-lines">
              <i></i><i></i><i class="short"></i>
            </div>
          </div>

          <div class="upload-tip">
            图片会自动压缩到长边 1600px；AI 只负责生成草稿，不会直接创建题目。
          </div>

          <div class="actions">
            <el-button type="primary" size="large" :loading="recognizing" @click="recognize">
              开始识别
            </el-button>
            <el-button size="large" @click="startManual()">直接手动录入</el-button>
          </div>
        </template>

        <template v-else>
          <div class="draft-summary">
            <div class="draft-thumb">
              <img v-if="previewUrl" :src="previewUrl" alt="题目图片" />
              <span v-else>无图片</span>
            </div>
            <div>
              <strong>{{ stage === 'manual' ? '未使用识别结果' : 'AI 草稿已生成' }}</strong>
              <p>{{ stage === 'manual' ? '手动填写不会覆盖或调用识别服务。' : '请重点检查公式、输入输出格式和样例。' }}</p>
            </div>
            <el-button text @click="goUpload">重新上传</el-button>
          </div>

          <div v-if="stage === 'manual'" class="notice warning">
            <strong>已切换到手动录入</strong>
            <span>{{ manualReason || '请手动填写题目信息，不会调用图片识别。' }}</span>
          </div>
          <div v-else class="notice info">
            <strong>请核对 AI 识别结果</strong>
            <span>识别结果可能有误，确认后才会创建个人题目。</span>
          </div>

          <div v-if="missingFields.length" class="notice danger">
            <strong>以下字段缺失</strong>
            <span>{{ missingFields.join('、') }}</span>
          </div>
          <div v-if="warnText" class="notice plain">
            <strong>识别提示</strong>
            <span>{{ warnText }}</span>
          </div>

          <section class="form-card">
            <div class="form-row three">
              <label class="field">
                <span>题目标题 <em>*</em></span>
                <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：数组元素求和" />
              </label>
              <label class="field">
                <span>知识点 <em>*</em></span>
                <el-select v-model="form.knowledgePointCode" placeholder="请选择知识点" filterable class="full-width">
                  <el-option
                    v-for="item in knowledgePoints"
                    :key="item.id"
                    :label="item.name"
                    :value="item.code"
                  />
                </el-select>
              </label>
              <label class="field">
                <span>难度 <em>*</em></span>
                <el-radio-group v-model="form.difficulty" class="difficulty-group">
                  <el-radio-button v-for="item in difficultyOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </el-radio-button>
                </el-radio-group>
              </label>
            </div>

            <label class="field block">
              <span>题目描述 <em>*</em></span>
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="5"
                maxlength="20000"
                show-word-limit
                placeholder="支持 Markdown，建议保留原题的标题层级"
              />
            </label>

            <div class="form-row two">
              <label class="field">
                <span>输入格式</span>
                <el-input v-model="form.inputDesc" type="textarea" :rows="3" placeholder="例如：第一行一个整数 n" />
              </label>
              <label class="field">
                <span>输出格式</span>
                <el-input v-model="form.outputDesc" type="textarea" :rows="3" placeholder="例如：输出一个整数" />
              </label>
            </div>
          </section>

          <section class="case-card">
            <div class="case-head">
              <div>
                <h3>样例用例</h3>
                <p>至少一条完整样例，会展示在题目详情页。</p>
              </div>
              <el-button plain :icon="Plus" @click="addSample">添加样例</el-button>
            </div>
            <div v-for="(item, index) in form.samples" :key="`sample-${index}`" class="case-row">
              <span class="case-no">{{ String(index + 1).padStart(2, '0') }}</span>
              <label class="case-field">
                <span>输入</span>
                <el-input v-model="item.input" type="textarea" :rows="2" placeholder="样例输入" />
              </label>
              <label class="case-field">
                <span>输出</span>
                <el-input v-model="item.output" type="textarea" :rows="2" placeholder="样例输出" />
              </label>
              <el-button link type="danger" :icon="Delete" @click="removeSample(index)" />
            </div>
          </section>

          <section class="case-card">
            <div class="case-head">
              <div>
                <h3>隐藏用例（可选）</h3>
                <p>不填时会复制样例生成 2 条隐藏用例，入库后需人工确认。</p>
              </div>
              <el-button plain :icon="Plus" @click="addHiddenCase">添加隐藏用例</el-button>
            </div>
            <div v-for="(item, index) in form.hiddenCases" :key="`hidden-${index}`" class="case-row">
              <span class="case-no">{{ String(index + 1).padStart(2, '0') }}</span>
              <label class="case-field">
                <span>输入</span>
                <el-input v-model="item.input" type="textarea" :rows="2" placeholder="隐藏输入" />
              </label>
              <label class="case-field">
                <span>输出</span>
                <el-input v-model="item.output" type="textarea" :rows="2" placeholder="隐藏输出" />
              </label>
              <el-button link type="danger" :icon="Delete" @click="removeHiddenCase(index)" />
            </div>
            <div v-if="!form.hiddenCases.length" class="hidden-empty">
              当前没有隐藏用例，提交后会复制样例生成 2 条隐藏用例。
            </div>
          </section>

          <div class="actions draft-actions">
            <el-button size="large" @click="goUpload">返回上一步</el-button>
            <el-button type="primary" size="large" :loading="submitting" @click="submit">
              确认入库
            </el-button>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.import-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 42px 24px 64px;
}

.import-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.eyebrow {
  display: none;
  color: #1f3a5f;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.18em;
}

.import-head h1 {
  margin: 8px 0 6px;
  color: #101426;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.import-head p {
  max-width: 620px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.8;
}

.head-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e6eaf2;
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  font-size: 11px;
  white-space: nowrap;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2f7d5b;
}

.import-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  align-items: start;
  gap: 20px;
}

.flow-card {
  position: sticky;
  top: 88px;
  padding: 20px;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: #fff;
}

.flow-title {
  color: #94a3b8;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.flow-list {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.flow-list li {
  position: relative;
  display: flex;
  gap: 12px;
  padding-bottom: 22px;
  color: #94a3b8;
}

.flow-list li::before {
  position: absolute;
  top: 22px;
  bottom: 0;
  left: 13px;
  width: 1px;
  background: #e8edf5;
  content: '';
}

.flow-list li:last-child::before {
  display: none;
}

.flow-index {
  z-index: 1;
  display: grid;
  width: 27px;
  height: 27px;
  flex: 0 0 27px;
  place-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
}

.flow-list strong {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 650;
}

.flow-list p {
  margin: 3px 0 0;
  font-size: 10.5px;
  line-height: 1.5;
}

.flow-list li.active .flow-index,
.flow-list li.done .flow-index {
  border-color: #c8d2dc;
  background: #eef2f6;
  color: #1f3a5f;
}

.flow-list li.active strong {
  color: #1e293b;
}

.flow-note {
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
  color: #94a3b8;
  font-size: 10.5px;
  line-height: 1.7;
}

.flow-note span {
  display: inline-block;
  margin-right: 4px;
  color: #1f3a5f;
  font-weight: 700;
}

.work-card {
  min-height: 560px;
  padding: 26px 28px 28px;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: #fff;
}

.work-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eef2f7;
}

.work-kicker {
  color: #94a3b8;
  font-size: 11px;
}

.work-head h2 {
  margin: 6px 0 0;
  color: #1e293b;
  font-size: 22px;
  font-weight: 720;
  letter-spacing: -0.02em;
}

.work-state {
  padding: 5px 9px;
  border-radius: 999px;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 10px;
  white-space: nowrap;
}

.work-state.manual {
  background: #f7f3ea;
  color: #b45309;
}

.upload-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 16px;
  margin-top: 20px;
}

.upload-box {
  width: 100%;
}

.upload-box :deep(.el-upload) {
  width: 100%;
}

.upload-box :deep(.el-upload-dragger) {
  display: flex;
  min-height: 246px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;
  border: 1px dashed #c8d2dc;
  border-radius: 10px;
  background: #f7f9fb;
}

.drop-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  margin-bottom: 10px;
  border-radius: 10px;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 24px;
}

.drop-title {
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.upload-box p {
  margin: 7px 0 14px;
  color: #8a94a6;
  font-size: 11px;
}

.drop-action {
  padding: 7px 14px;
  border: 1px solid #dfe5f0;
  border-radius: 8px;
  background: #fff;
  color: #1f3a5f;
  font-size: 11px;
  font-weight: 650;
}

.preview-box,
.preview-placeholder {
  overflow: hidden;
  min-height: 246px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  background: #fbfcfe;
}

.preview-box img {
  display: block;
  width: 100%;
  height: 206px;
  object-fit: cover;
  object-position: top;
  background: #f8fafc;
}

.preview-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 12px;
  color: #94a3b8;
  font-size: 10px;
}

.preview-name {
  overflow: hidden;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #a8b3c4;
}

.preview-mark {
  color: #c8d2dc;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.16em;
}

.preview-placeholder p {
  margin: 8px 0 0;
  font-size: 11px;
}

.recognizing {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #dfe6ed;
  border-radius: 12px;
  background: #f7f9fb;
}

.recognizing-copy {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 9px;
  color: #1f3a5f;
  font-size: 11px;
}

.recognizing-copy strong {
  color: #334155;
  font-size: 12px;
}

.recognizing-copy span:last-child {
  margin-left: auto;
  color: #64748b;
}

.scan-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #1f3a5f;
  box-shadow: 0 0 0 4px #dfe6ed;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  50% {
    opacity: 0.35;
  }
}

.skeleton-lines {
  display: grid;
  gap: 7px;
  margin-top: 13px;
}

.skeleton-lines i {
  display: block;
  height: 7px;
  border-radius: 999px;
  background: linear-gradient(90deg, #eef2f7, #f8fafc, #eef2f7);
  background-size: 200% 100%;
  animation: shimmer 1.3s linear infinite;
}

.skeleton-lines i.short {
  width: 62%;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

.upload-tip {
  margin-top: 14px;
  color: #94a3b8;
  font-size: 11px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 22px;
}

.actions :deep(.el-button) {
  border-radius: 8px;
}

.draft-summary {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-top: 20px;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fbfcfe;
}

.draft-thumb {
  display: grid;
  width: 60px;
  height: 48px;
  flex: 0 0 60px;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: #eef2f7;
  color: #94a3b8;
  font-size: 9px;
}

.draft-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.draft-summary strong {
  display: block;
  color: #334155;
  font-size: 12px;
}

.draft-summary p {
  margin: 3px 0 0;
  color: #94a3b8;
  font-size: 10.5px;
}

.draft-summary :deep(.el-button) {
  margin-left: auto;
}

.notice {
  display: flex;
  gap: 10px;
  align-items: baseline;
  margin-top: 12px;
  padding: 11px 13px;
  border-radius: 9px;
  font-size: 11px;
  line-height: 1.6;
}

.notice strong {
  flex: 0 0 auto;
  font-size: 11px;
}

.notice.warning {
  background: #f7f3ea;
  color: #92400e;
}

.notice.info {
  background: #f5f7f8;
  color: #1f3a5f;
}

.notice.danger {
  background: #f7efee;
  color: #9f2d2d;
}

.notice.plain {
  background: #f8fafc;
  color: #64748b;
}

.form-card {
  margin-top: 18px;
}

.form-row {
  display: grid;
  gap: 14px;
  margin-bottom: 14px;
}

.form-row.three {
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) minmax(0, 0.9fr);
}

.form-row.two {
  grid-template-columns: 1fr 1fr;
}

.field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.field.block {
  margin-bottom: 14px;
}

.field > span {
  color: #64748b;
  font-size: 10.5px;
  font-weight: 600;
}

.field em {
  color: #9f2d2d;
  font-style: normal;
}

.field :deep(.el-input__wrapper),
.field :deep(.el-textarea__inner),
.case-field :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e6ebf3 inset;
}

.field :deep(.el-textarea__inner) {
  padding: 10px 12px;
  color: #334155;
  font-size: 12px;
  line-height: 1.8;
}

.full-width {
  width: 100%;
}

.difficulty-group :deep(.el-radio-button__inner) {
  padding: 8px 11px;
  font-size: 11px;
}

.case-card {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #eef2f7;
}

.case-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.case-head h3 {
  margin: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.case-head p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 10.5px;
}

.case-head :deep(.el-button) {
  height: 28px;
  padding: 0 10px;
  border-radius: 7px;
  font-size: 10.5px;
}

.case-row {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) minmax(0, 1fr) 24px;
  align-items: end;
  gap: 10px;
  margin-bottom: 9px;
}

.case-no {
  padding-bottom: 8px;
  color: #c0cad8;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
}

.case-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.case-field > span {
  color: #94a3b8;
  font-size: 9.5px;
}

.case-field :deep(.el-textarea__inner) {
  padding: 7px 9px;
  color: #475569;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10.5px;
  line-height: 1.6;
}

.case-row :deep(.el-button) {
  margin-bottom: 6px;
}

.hidden-empty {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 10.5px;
}

.draft-actions {
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid #eef2f7;
}

@media (max-width: 980px) {
  .import-layout {
    grid-template-columns: 1fr;
  }

  .flow-card {
    position: static;
    padding: 14px 16px;
  }

  .flow-list {
    display: flex;
    gap: 8px;
    margin-top: 10px;
  }

  .flow-list li {
    flex: 1;
    padding: 0;
  }

  .flow-list li::before {
    display: none;
  }

  .flow-note {
    display: none;
  }

  .upload-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .import-page {
    padding: 26px 14px 42px;
  }

  .import-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .import-head h1 {
    font-size: 32px;
  }

  .head-badge {
    font-size: 10px;
  }

  .work-card {
    padding: 18px 14px;
  }

  .work-head h2 {
    font-size: 18px;
  }

  .form-row.three,
  .form-row.two,
  .case-row {
    grid-template-columns: 1fr;
  }

  .case-no {
    padding: 0;
  }

  .case-row :deep(.el-button) {
    justify-self: end;
    margin: 0;
  }

  .draft-summary {
    align-items: flex-start;
  }

  .draft-summary :deep(.el-button) {
    align-self: flex-start;
  }

  .actions {
    flex-wrap: wrap;
  }

  .flow-list strong {
    font-size: 11px;
  }

  .flow-list p {
    display: none;
  }
}
</style>
