<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Connection } from '@element-plus/icons-vue'
import { listKnowledgePoints } from '@/api/question'

const WIDTH = 920
const HEIGHT = 600
const CENTER_X = WIDTH / 2
const CENTER_Y = HEIGHT / 2

const CONCEPTS = {
  array: ['遍历', '子段和'],
  string: ['回文', '滑动窗口'],
  linked_list: ['反转', '双指针'],
  recursion: ['分治', '边界'],
  sort: ['比较器', '稳定性']
}

const router = useRouter()
const points = ref([])
const nodes = ref([])
const edges = ref([])
const loading = ref(true)
const error = ref('')
const selectedId = ref('')
const hoveredId = ref('')
let frame = 0
let lastTick = 0
let raf = 0

const activeId = computed(() => hoveredId.value || selectedId.value)
const selectedPoint = computed(() => points.value.find((item) => `kp-${item.id}` === selectedId.value) || null)
const neighbors = computed(() => {
  if (!activeId.value) return new Set()
  const set = new Set([activeId.value])
  for (const edge of edges.value) {
    if (edge.sourceId === activeId.value) set.add(edge.targetId)
    if (edge.targetId === activeId.value) set.add(edge.sourceId)
  }
  return set
})

function nodeById(id) {
  return nodes.value.find((node) => node.id === id)
}

function nodeX(id) {
  return nodeById(id)?.x || 0
}

function nodeY(id) {
  return nodeById(id)?.y || 0
}

function nodeVisible(node) {
  if (!activeId.value) return true
  return neighbors.value.has(node.id)
}

function edgeActive(edge) {
  return activeId.value === edge.sourceId || activeId.value === edge.targetId
}

function buildGraph(list) {
  const root = {
    id: 'root',
    label: '编程基础',
    type: 'root',
    ax: CENTER_X,
    ay: CENTER_Y,
    x: CENTER_X,
    y: CENTER_Y,
    r: 31,
    phase: 0,
    drift: 2
  }
  const kpNodes = []
  const conceptNodes = []
  const nextEdges = []
  const count = Math.max(list.length, 1)
  const radiusX = 286
  const radiusY = 196

  list.forEach((item, index) => {
    const angle = (-Math.PI / 2) + (Math.PI * 2 * index) / count
    const ax = CENTER_X + Math.cos(angle) * radiusX
    const ay = CENTER_Y + Math.sin(angle) * radiusY
    const node = {
      id: `kp-${item.id}`,
      label: item.name,
      type: 'kp',
      kpId: item.id,
      ax,
      ay,
      x: ax,
      y: ay,
      r: 19,
      phase: index * 1.1,
      drift: 3.2
    }
    kpNodes.push(node)
    nextEdges.push({ sourceId: root.id, targetId: node.id, kind: 'root' })

    const concepts = CONCEPTS[item.code] || ['概念', '练习']
    concepts.forEach((concept, conceptIndex) => {
      const conceptAngle = angle + (conceptIndex === 0 ? -0.44 : 0.44)
      const cx = ax + Math.cos(conceptAngle) * 92
      const cy = ay + Math.sin(conceptAngle) * 74
      const conceptNode = {
        id: `concept-${item.id}-${conceptIndex}`,
        label: concept,
        type: 'concept',
        kpId: item.id,
        ax: cx,
        ay: cy,
        x: cx,
        y: cy,
        r: 8,
        phase: index * 1.7 + conceptIndex,
        drift: 2.4
      }
      conceptNodes.push(conceptNode)
      nextEdges.push({ sourceId: node.id, targetId: conceptNode.id, kind: 'branch' })
    })
  })

  const byCode = new Map(list.map((item) => [item.code, `kp-${item.id}`]))
  const links = [
    ['array', 'string'],
    ['string', 'linked_list'],
    ['linked_list', 'recursion'],
    ['recursion', 'sort']
  ]
  for (const [from, to] of links) {
    if (byCode.has(from) && byCode.has(to)) {
      nextEdges.push({ sourceId: byCode.get(from), targetId: byCode.get(to), kind: 'cross' })
    }
  }

  nodes.value = [root, ...kpNodes, ...conceptNodes]
  edges.value = nextEdges
  selectedId.value = list[0] ? `kp-${list[0].id}` : ''

  const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  if (!reduced) startAnimation()
}

function startAnimation() {
  cancelAnimationFrame(raf)
  lastTick = 0
  const tick = (time) => {
    if (time - lastTick > 32) {
      lastTick = time
      frame += 0.032
      for (const node of nodes.value) {
        node.x = node.ax + Math.sin(frame * 0.72 + node.phase) * node.drift
        node.y = node.ay + Math.cos(frame * 0.58 + node.phase * 1.3) * node.drift
      }
    }
    raf = requestAnimationFrame(tick)
  }
  raf = requestAnimationFrame(tick)
}

function selectNode(node) {
  if (node.type === 'concept') {
    selectedId.value = `kp-${node.kpId}`
    return
  }
  selectedId.value = node.id
}

function selectPoint(point) {
  selectedId.value = `kp-${point.id}`
}

function goPractice() {
  if (!selectedPoint.value) return
  router.push({ path: '/question', query: { kp: selectedPoint.value.id } })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await listKnowledgePoints()
    points.value = Array.isArray(data) ? data : []
    if (!points.value.length) {
      error.value = '暂无知识点'
      return
    }
    buildGraph(points.value)
  } catch (err) {
    error.value = err?.message || '知识点加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
})
</script>

<template>
  <div class="knowledge-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">知识点</h1>
        <p class="page-subtitle">从概念到练习，先看清一张图，再回到题目里。</p>
      </div>
      <div class="head-count">
        <strong>{{ points.length }}</strong>
        <span>个核心知识点</span>
      </div>
    </header>

    <div class="knowledge-grid">
      <section v-reveal class="graph-panel">
        <div class="graph-head">
          <div>
            <h2>知识网络</h2>
            <p>悬停或点击节点，查看相邻概念。</p>
          </div>
          <span class="graph-tag"><el-icon><Connection /></el-icon> INTERACTIVE</span>
        </div>

        <div class="graph-stage">
          <div v-if="loading" class="graph-state">正在生成知识网络…</div>
          <div v-else-if="error" class="graph-state is-error">{{ error }}</div>
          <svg
            v-else
            :viewBox="`0 0 ${WIDTH} ${HEIGHT}`"
            role="img"
            aria-label="知识点关系网络"
          >
            <g class="graph-edges">
              <line
                v-for="edge in edges"
                :key="`${edge.sourceId}-${edge.targetId}`"
                :x1="nodeX(edge.sourceId)"
                :y1="nodeY(edge.sourceId)"
                :x2="nodeX(edge.targetId)"
                :y2="nodeY(edge.targetId)"
                :class="[`edge-${edge.kind}`, { 'is-active': edgeActive(edge), 'is-dim': activeId && !edgeActive(edge) }]"
              />
            </g>
            <g class="graph-nodes">
              <g
                v-for="node in nodes"
                :key="node.id"
                class="node"
                :class="[
                  `node-${node.type}`,
                  {
                    'is-active': activeId === node.id,
                    'is-dim': activeId && !nodeVisible(node)
                  }
                ]"
                :transform="`translate(${node.x} ${node.y})`"
                @mouseenter="hoveredId = node.id"
                @mouseleave="hoveredId = ''"
                @click.stop="selectNode(node)"
              >
                <circle class="node-halo" :r="node.r + 8" />
                <circle class="node-core" :r="node.r" />
                <text
                  v-if="node.type === 'root' || node.type === 'kp' || activeId === node.id"
                  :y="node.r + 18"
                  text-anchor="middle"
                >{{ node.label }}</text>
                <title>{{ node.label }}</title>
              </g>
            </g>
          </svg>

          <div v-if="!loading && !error" class="graph-legend">
            <span><i class="dot dot-root"></i>基础</span>
            <span><i class="dot dot-kp"></i>知识点</span>
            <span><i class="dot dot-concept"></i>概念</span>
          </div>
        </div>
      </section>

      <aside class="knowledge-side">
        <div class="side-head">
          <h2>核心知识点</h2>
          <span>按顺序排列</span>
        </div>

        <div v-if="loading" class="side-loading">
          <i v-for="index in 5" :key="index"></i>
        </div>
        <div v-else-if="error" class="side-error">{{ error }}</div>
        <div v-else class="kp-list">
          <button
            v-for="(point, index) in points"
            :key="point.id"
            class="kp-row"
            :class="{ 'is-active': selectedId === `kp-${point.id}` }"
            type="button"
            @click="selectPoint(point)"
            @mouseenter="hoveredId = `kp-${point.id}`"
            @mouseleave="hoveredId = ''"
          >
            <span class="kp-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="kp-copy">
              <strong>{{ point.name }}</strong>
              <small>{{ point.description }}</small>
            </span>
            <span class="kp-count">{{ point.questionCount }} 题</span>
          </button>
        </div>

        <div v-if="selectedPoint" class="side-detail">
          <div class="side-detail-head">
            <strong>{{ selectedPoint.name }}</strong>
            <span>{{ selectedPoint.questionCount }} 道练习</span>
          </div>
          <p>{{ selectedPoint.description }}</p>
          <el-button type="primary" @click="goPractice">
            去做这一组
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <p v-else class="side-hint">点击左侧节点，或从列表选择知识点。</p>
      </aside>
    </div>

    <p class="knowledge-note">图谱只展示知识关系；具体题目仍按知识点与难度组织。</p>
  </div>
</template>

<style scoped>
.knowledge-page {
  max-width: 1200px;
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
  color: #0f172a;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -1.3px;
  line-height: 1.15;
}

.page-subtitle {
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
}

.head-count {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: #7c8798;
  font-size: 12px;
}

.head-count strong {
  color: #1f3a5f;
  font-size: 28px;
  font-weight: 700;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 16px;
}

.graph-panel {
  display: flex;
  min-height: 622px;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #16263c;
  border-radius: 14px;
  background: #0b1524;
}

.graph-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px 0;
}

.graph-head h2 {
  color: #e5edf6;
  font-size: 16px;
  font-weight: 650;
}

.graph-head p {
  margin-top: 5px;
  color: #7f93a9;
  font-size: 12px;
}

.graph-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #91a7bd;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.graph-stage {
  position: relative;
  flex: 1;
  min-height: 0;
  margin-top: 4px;
  background:
    radial-gradient(circle at 50% 45%, rgba(36, 59, 86, 0.45) 0, rgba(11, 21, 36, 0) 58%),
    #0b1524;
}

.graph-stage svg {
  display: block;
  width: 100%;
  height: 100%;
}

.graph-state {
  display: grid;
  height: 100%;
  min-height: 480px;
  place-items: center;
  color: #8ea3b9;
  font-size: 13px;
}

.graph-state.is-error {
  color: #e4a3a3;
}

.graph-edges line {
  stroke: #334960;
  stroke-width: 1;
  transition: opacity 160ms ease, stroke 160ms ease;
}

.graph-edges .edge-root {
  stroke: #54708c;
  stroke-opacity: 0.7;
}

.graph-edges .edge-branch {
  stroke-opacity: 0.38;
}

.graph-edges .edge-cross {
  stroke: #405b73;
  stroke-dasharray: 3 5;
  stroke-opacity: 0.36;
}

.graph-edges line.is-active {
  stroke: #a9c0d6;
  stroke-opacity: 0.95;
}

.graph-edges line.is-dim {
  stroke-opacity: 0.12;
}

.node {
  cursor: pointer;
  outline: none;
}

.node-halo {
  fill: transparent;
  transition: fill 160ms ease, opacity 160ms ease;
}

.node-core {
  transition: fill 160ms ease, stroke 160ms ease, opacity 160ms ease;
}

.node-root .node-core {
  fill: #dce7f2;
  stroke: #ffffff;
  stroke-width: 1.2;
}

.node-kp .node-core {
  fill: #294866;
  stroke: #91a9c0;
  stroke-width: 1;
}

.node-concept .node-core {
  fill: #74889d;
  stroke: #a3b6c9;
  stroke-width: 0.8;
}

.node text {
  fill: #c2d1e0;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  pointer-events: none;
}

.node-root text {
  fill: #f3f7fb;
  font-size: 13px;
}

.node-concept text {
  fill: #b7c7d8;
  font-size: 11px;
  font-weight: 500;
}

.node.is-active .node-halo {
  fill: rgba(173, 198, 221, 0.14);
}

.node.is-active .node-kp,
.node.is-active .node-root {
  filter: drop-shadow(0 0 8px rgba(189, 213, 234, 0.4));
}

.node.is-active .node-core {
  stroke: #ffffff;
  stroke-width: 1.6;
}

.node.is-dim {
  opacity: 0.2;
}

.graph-legend {
  position: absolute;
  bottom: 16px;
  left: 20px;
  display: flex;
  gap: 16px;
  color: #7f93a9;
  font-size: 10px;
}

.graph-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-root { background: #dce7f2; }
.dot-kp { background: #4f6d8a; }
.dot-concept { background: #74889d; }

.knowledge-side {
  min-height: 622px;
  padding: 20px;
  border: 1px solid #e5e9ef;
  border-radius: 12px;
  background: #ffffff;
}

.side-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf0f4;
}

.side-head h2 {
  color: #1f2937;
  font-size: 15px;
  font-weight: 650;
}

.side-head span {
  color: #8a94a3;
  font-size: 11px;
}

.kp-list {
  display: grid;
  gap: 2px;
  margin-top: 8px;
}

.kp-row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 8px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 150ms ease;
}

.kp-row:hover,
.kp-row.is-active {
  background: #f3f6f8;
}

.kp-index {
  color: #a1acb9;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
}

.kp-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.kp-copy strong {
  color: #1f2937;
  font-size: 13px;
  font-weight: 650;
}

.kp-copy small {
  display: -webkit-box;
  overflow: hidden;
  color: #7b8797;
  font-size: 10.5px;
  font-weight: 400;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.kp-count {
  color: #6f7c8c;
  font-size: 10.5px;
  white-space: nowrap;
}

.side-detail {
  margin-top: 20px;
  padding: 16px;
  border-top: 1px solid #edf0f4;
}

.side-detail-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.side-detail-head strong {
  color: #1f2937;
  font-size: 14px;
}

.side-detail-head span {
  color: #8893a2;
  font-size: 10.5px;
}

.side-detail p {
  margin: 9px 0 14px;
  color: #7b8797;
  font-size: 11.5px;
  line-height: 1.75;
}

.side-hint {
  margin: 24px 8px 0;
  color: #9aa4b2;
  font-size: 11px;
  line-height: 1.7;
}

.side-loading {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.side-loading i {
  display: block;
  height: 52px;
  border-radius: 6px;
  background: #f3f6f8;
}

.side-error {
  padding: 30px 0;
  color: #a56a6a;
  font-size: 12px;
}

.knowledge-note {
  margin-top: 18px;
  color: #8b96a5;
  font-size: 11px;
}

@media (max-width: 960px) {
  .knowledge-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .knowledge-side {
    min-height: 0;
  }
}

@media (max-width: 640px) {
  .knowledge-page {
    padding: 24px 14px 44px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .page-title {
    font-size: 32px;
  }

  .graph-panel {
    min-height: 420px;
  }

  .graph-head {
    padding: 18px 16px 0;
  }

  .graph-tag {
    display: none;
  }

  .graph-state {
    min-height: 340px;
  }

  .graph-legend {
    bottom: 12px;
    left: 14px;
  }
}
</style>
