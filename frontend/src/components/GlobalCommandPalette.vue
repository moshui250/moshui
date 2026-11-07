<template>
  <Teleport to="body">
    <Transition name="cmd-fade">
      <div v-if="visible" class="command-backdrop" @mousedown.self="close">
        <section class="command-dialog" role="dialog" aria-modal="true" aria-label="全局搜索">
          <div class="command-input-row">
            <el-icon class="command-search-icon"><Search /></el-icon>
            <input
              ref="inputRef"
              v-model="query"
              class="command-input"
              type="text"
              placeholder="搜索题目、知识点或竞赛…"
              autocomplete="off"
              spellcheck="false"
              @input="handleQueryInput"
              @keydown="handleInputKeydown"
            />
            <kbd class="command-esc">esc</kbd>
          </div>

          <div class="command-body">
            <div v-if="loading" class="command-loading">正在搜索…</div>

            <template v-for="group in groups" :key="group.label">
              <p class="command-group-label">{{ group.label }}</p>
              <button
                v-for="item in group.items"
                :key="item.key"
                type="button"
                class="command-item"
                :class="{ 'is-active': isActive(item) }"
                @mouseenter="activate(item)"
                @click="select(item)"
              >
                <span class="command-mark">{{ item.short }}</span>
                <span class="command-copy">
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.meta }}</small>
                </span>
                <span v-if="isActive(item)" class="command-enter">↵</span>
              </button>
            </template>

            <div v-if="!loading && !groups.length" class="command-empty">
              <p>没有找到相关内容</p>
              <span>换一个关键词，或直接从导航进入。</span>
            </div>
          </div>

          <footer class="command-footer">
            <span><kbd>↑</kbd><kbd>↓</kbd> 选择</span>
            <span><kbd>↵</kbd> 打开</span>
            <span><kbd>esc</kbd> 关闭</span>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { listKnowledgePoints, listQuestions } from '@/api/question'
import { listContests } from '@/api/contest'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const visible = ref(false)
const query = ref('')
const inputRef = ref(null)
const loading = ref(false)
const activeIndex = ref(0)
const knowledge = ref([])
const contests = ref([])
const questions = ref([])

let dataLoaded = false
let searchTimer = null
let searchSeq = 0
let previousOverflow = ''

const quickItems = computed(() => {
  const items = [
    { key: 'quick-question', short: '题', label: '题库', meta: '浏览全部练习题', to: { name: 'question-list' } },
    { key: 'quick-knowledge', short: '知', label: '知识点', meta: '查看知识网络', to: { name: 'knowledge' } },
    { key: 'quick-contest', short: '赛', label: '竞赛', meta: '查看正在进行和即将开始的比赛', to: { name: 'contest-list' } },
    { key: 'quick-rank', short: '排', label: '排名', meta: '查看全站段位与排名', to: { name: 'rank' } },
    { key: 'quick-wrong', short: '错', label: '错题本', meta: '复盘最近做错的题', to: { name: 'wrong-book' } },
    { key: 'quick-dashboard', short: '看', label: '学习看板', meta: '查看提交、正确率与薄弱点', to: { name: 'dashboard' } },
    { key: 'quick-import', short: '图', label: '图片录题', meta: '上传图片，生成个人题目', to: { name: 'question-import' } }
  ]
  if (userStore.user?.role === 'ADMIN') {
    items.push({ key: 'quick-admin-contest', short: '建', label: '创建竞赛', meta: '管理员创建比赛与专属题目', to: { name: 'admin-contest-create' } })
  }
  return items
})

const normalizedQuery = computed(() => query.value.trim().toLowerCase())

const knowledgeItems = computed(() => {
  const q = normalizedQuery.value
  const source = q ? knowledge.value : knowledge.value.slice(0, 5)
  return source
    .filter((item) => matches(item, q))
    .map((item) => ({
      key: `kp-${item.id}`,
      short: '知',
      label: item.name,
      meta: `${item.code || 'knowledge'} · ${item.questionCount ?? 0} 题`,
      to: { name: 'question-list', query: { kp: item.id } }
    }))
})

const contestItems = computed(() => {
  const q = normalizedQuery.value
  return contests.value
    .filter((item) => matches(item, q))
    .slice(0, q ? 6 : 3)
    .map((item) => ({
      key: `contest-${item.id}`,
      short: '赛',
      label: item.title,
      meta: `${statusText(item.status)} · ${item.problemCount ?? 0} 题`,
      to: { name: 'contest-detail', params: { id: item.id } }
    }))
})

const questionItems = computed(() =>
  questions.value.map((item) => ({
    key: `question-${item.id}`,
    short: '题',
    label: item.title,
    meta: `${item.knowledgePoint?.name || '题目'} · ${difficultyText(item.difficulty)}`,
    to: { name: 'question-detail', params: { id: item.id } }
  }))
)

const groups = computed(() => {
  const q = normalizedQuery.value
  const quick = quickItems.value.filter((item) => matches(item, q))
  const result = []
  if (quick.length) result.push({ label: q ? '快速前往' : '快速前往', items: quick })
  if (knowledgeItems.value.length) result.push({ label: '知识点', items: knowledgeItems.value })
  if (contestItems.value.length) result.push({ label: '竞赛', items: contestItems.value })
  if (questionItems.value.length) result.push({ label: '题目', items: questionItems.value })
  return result
})

const flatItems = computed(() => groups.value.flatMap((group) => group.items))

function matches(item, q) {
  if (!q) return true
  return [item.label, item.meta, item.name, item.title, item.code, item.knowledgePoint?.name]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
    .includes(q)
}

function statusText(status) {
  if (status === 'RUNNING') return '进行中'
  if (status === 'UPCOMING') return '即将开始'
  return '已结束'
}

function difficultyText(level) {
  return ['入门', '进阶', '挑战'][Number(level) - 1] || '练习'
}

function isActive(item) {
  return flatItems.value[activeIndex.value]?.key === item.key
}

function activate(item) {
  const index = flatItems.value.findIndex((entry) => entry.key === item.key)
  if (index >= 0) activeIndex.value = index
}

async function ensureData() {
  if (dataLoaded) return
  dataLoaded = true
  try {
    knowledge.value = (await listKnowledgePoints()) || []
  } catch {
    knowledge.value = []
  }
  if (!userStore.token) return
  try {
    const page = await listContests({ page: 1, size: 8 })
    contests.value = page?.records || []
  } catch {
    contests.value = []
  }
}

async function searchQuestions(keyword) {
  if (!userStore.token || !keyword) {
    questions.value = []
    return
  }
  const seq = ++searchSeq
  loading.value = true
  try {
    const page = await listQuestions({ keyword, page: 1, size: 6, type: 'PUBLIC' })
    if (seq === searchSeq) questions.value = page?.records || []
  } catch {
    if (seq === searchSeq) questions.value = []
  } finally {
    if (seq === searchSeq) loading.value = false
  }
}

function handleQueryInput() {
  activeIndex.value = 0
  clearTimeout(searchTimer)
  const keyword = query.value.trim()
  if (!keyword) {
    searchSeq += 1
    questions.value = []
    loading.value = false
    return
  }
  searchTimer = setTimeout(() => searchQuestions(keyword), 180)
}

async function open() {
  if (visible.value) return
  visible.value = true
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  query.value = ''
  questions.value = []
  activeIndex.value = 0
  await nextTick()
  inputRef.value?.focus()
  ensureData()
}

function close() {
  if (!visible.value) return
  visible.value = false
  clearTimeout(searchTimer)
  document.body.style.overflow = previousOverflow
}

function select(item) {
  if (!item?.to) return
  close()
  router.push(item.to)
}

function move(delta) {
  const total = flatItems.value.length
  if (!total) return
  activeIndex.value = (activeIndex.value + delta + total) % total
  nextTick(() => {
    document.querySelector('.command-item.is-active')?.scrollIntoView({ block: 'nearest' })
  })
}

function handleInputKeydown(event) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    move(1)
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    move(-1)
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    const item = flatItems.value[activeIndex.value]
    if (item) select(item)
    return
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    close()
  }
}

function onKeydown(event) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    visible.value ? close() : open()
  }
}

function onOpenEvent() {
  open()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('command:open', onOpenEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('command:open', onOpenEvent)
  clearTimeout(searchTimer)
  if (visible.value) document.body.style.overflow = previousOverflow
})
</script>

<style scoped>
.command-backdrop {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 13vh 20px 20px;
  background: rgba(15, 23, 42, 0.38);
}

.command-dialog {
  width: min(620px, 100%);
  max-height: min(70vh, 660px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e4e7ec;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
}

.command-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid #eef1f4;
}

.command-search-icon {
  color: #64748b;
  font-size: 18px;
}

.command-input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #111827;
  font-size: 15px;
}

.command-input::placeholder {
  color: #94a3b8;
}

.command-esc,
.command-footer kbd {
  padding: 2px 5px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  background: #f8fafc;
  color: #94a3b8;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
  line-height: 1.4;
}

.command-body {
  min-height: 100px;
  padding: 8px 8px 10px;
  overflow-y: auto;
}

.command-group-label {
  margin: 8px 10px 4px;
  color: #94a3b8;
  font-size: 11px;
  letter-spacing: 0.04em;
}

.command-item {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
  padding: 9px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #1f2937;
  text-align: left;
  cursor: pointer;
}

.command-item.is-active {
  background: #f1f4f7;
}

.command-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 26px;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: #eef2f6;
  color: #526174;
  font-size: 11px;
}

.command-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
}

.command-copy strong {
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.command-copy small {
  overflow: hidden;
  color: #8a94a6;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.command-enter {
  color: #64748b;
  font-size: 12px;
}

.command-loading,
.command-empty {
  padding: 28px 16px;
  color: #8a94a6;
  font-size: 12px;
  text-align: center;
}

.command-empty p {
  margin: 0 0 4px;
  color: #4b5563;
  font-size: 13px;
}

.command-empty span {
  color: #94a3b8;
  font-size: 11px;
}

.command-footer {
  display: flex;
  gap: 16px;
  padding: 9px 16px;
  border-top: 1px solid #eef1f4;
  color: #94a3b8;
  font-size: 10px;
}

.command-footer span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.cmd-fade-enter-active,
.cmd-fade-leave-active {
  transition: opacity 0.14s ease;
}

.cmd-fade-enter-from,
.cmd-fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .command-backdrop {
    padding: 7vh 12px 12px;
  }
  .command-dialog {
    max-height: 82vh;
  }
  .command-input-row {
    min-height: 50px;
    padding: 0 12px;
  }
  .command-input {
    font-size: 14px;
  }
  .command-footer {
    gap: 10px;
    padding: 8px 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cmd-fade-enter-active,
  .cmd-fade-leave-active {
    transition: none;
  }
}
</style>
