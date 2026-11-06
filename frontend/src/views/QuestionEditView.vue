<template>
  <div class="edit-page">
    <button class="back-link" type="button" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      返回题目
    </button>

    <header class="edit-head">
      <h1>编辑题目</h1>
      <p>修改题面信息；已有样例和参考代码保持不变。</p>
    </header>

    <div v-if="loading" class="edit-panel">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="!question" class="edit-panel empty-panel">
      <el-empty :description="loadError || '题目不存在或无权编辑'" :image-size="96">
        <el-button type="primary" @click="goBack">返回题目</el-button>
      </el-empty>
    </div>

    <section v-else class="edit-panel">
      <div class="form-grid">
        <label class="field wide">
          <span>题目标题</span>
          <el-input v-model="form.title" maxlength="128" show-word-limit />
        </label>
        <label class="field wide">
          <span>题目描述</span>
          <el-input v-model="form.description" type="textarea" :rows="5" maxlength="20000" />
        </label>
        <label class="field">
          <span>输入格式</span>
          <el-input v-model="form.inputDesc" type="textarea" :rows="3" maxlength="10000" />
        </label>
        <label class="field">
          <span>输出格式</span>
          <el-input v-model="form.outputDesc" type="textarea" :rows="3" maxlength="10000" />
        </label>
        <label class="field">
          <span>知识点</span>
          <el-select
            v-model="form.knowledgePointIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :max-collapse-tags="2"
            placeholder="可多选知识点"
          >
            <el-option v-for="point in knowledgePoints" :key="point.id" :label="point.name" :value="point.id" />
          </el-select>
        </label>
        <label class="field">
          <span>难度</span>
          <el-radio-group v-model="form.difficulty">
            <el-radio-button :value="1">入门</el-radio-button>
            <el-radio-button :value="2">进阶</el-radio-button>
            <el-radio-button :value="3">挑战</el-radio-button>
          </el-radio-group>
        </label>
        <div class="field wide">
          <span>可见范围</span>
          <el-radio-group v-model="form.visibility" class="visibility-group">
            <el-radio-button value="PRIVATE">仅自己</el-radio-button>
            <el-radio-button value="SHARED">共享</el-radio-button>
            <el-radio-button v-if="isAdmin" value="PUBLIC">公共</el-radio-button>
          </el-radio-group>
          <small class="field-note">共享题进入公共题库但不计段位 AC；公共题仅管理员可选。</small>
        </div>
      </div>

      <footer class="edit-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getQuestion, listKnowledgePoints, updateQuestion } from '@/api/question'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.user?.role === 'ADMIN')
const knowledgePoints = ref([])
const question = ref(null)
const loading = ref(true)
const saving = ref(false)
const loadError = ref('')
const form = reactive({
  title: '',
  description: '',
  inputDesc: '',
  outputDesc: '',
  difficulty: 1,
  knowledgePointIds: [],
  visibility: 'PRIVATE'
})

function goBack() {
  router.push({ name: 'question-detail', params: { id: route.params.id } })
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getQuestion(route.params.id)
    if (!data || data.type !== 'PERSONAL' || String(data.userId) !== String(userStore.user?.id)) {
      loadError.value = '只能编辑自己创建的题目'
      question.value = null
      return
    }
    question.value = data
    Object.assign(form, {
      title: data.title || '',
      description: data.description || '',
      inputDesc: data.inputDesc || '',
      outputDesc: data.outputDesc || '',
      difficulty: data.difficulty || 1,
      knowledgePointIds: Array.isArray(data.knowledgePoints) && data.knowledgePoints.length
        ? data.knowledgePoints.map((item) => item.id)
        : (data.knowledgePointId ? [data.knowledgePointId] : []),
      visibility: data.visibility || 'PRIVATE'
    })
    knowledgePoints.value = (await listKnowledgePoints()) || []
  } catch (error) {
    loadError.value = error?.message || '题目加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.title.trim()) return ElMessage.warning('请填写题目标题')
  if (!form.description.trim()) return ElMessage.warning('请填写题目描述')
  if (!form.knowledgePointIds.length) return ElMessage.warning('请选择至少一个知识点')
  saving.value = true
  try {
    await updateQuestion(route.params.id, {
      title: form.title.trim(),
      description: form.description,
      inputDesc: form.inputDesc,
      outputDesc: form.outputDesc,
      difficulty: form.difficulty,
      knowledgePointId: form.knowledgePointIds[0],
      knowledgePointIds: [...form.knowledgePointIds],
      visibility: form.visibility
    })
    ElMessage.success('题目已更新')
    goBack()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.edit-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 34px 24px 60px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 18px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
}

.edit-head h1 {
  margin: 0;
  color: #111827;
  font-size: 34px;
  letter-spacing: -1px;
}

.edit-head p {
  margin: 8px 0 22px;
  color: #6b7280;
  font-size: 13px;
}

.edit-panel {
  padding: 26px;
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  background: #fff;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.field.wide {
  grid-column: 1 / -1;
}

.field > span {
  color: #374151;
  font-size: 12px;
  font-weight: 600;
}

.field-note {
  color: #8a94a6;
  font-size: 11px;
  line-height: 1.6;
}

.visibility-group {
  align-self: flex-start;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid #eef1f5;
}

.empty-panel {
  padding: 40px;
}

@media (max-width: 640px) {
  .edit-page {
    padding: 22px 14px 40px;
  }
  .edit-panel {
    padding: 18px;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .field.wide {
    grid-column: auto;
  }
}
</style>
