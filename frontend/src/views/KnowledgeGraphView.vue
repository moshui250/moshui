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
  sort: ['比较器', '稳定性'],
  algorithm: ['二分', '贪心'],
  tree: ['遍历', '二叉搜索'],
  graph: ['BFS', '最短路'],
  dp: ['状态', '转移'],
  math: ['数论', '组合'],
  hash: ['计数', '去重']
}

const CLUSTER_COLORS = [
  '#6f8fb0', '#8f7aa6', '#6f9a84', '#b08a5a', '#5f9a9a',
  '#a67575', '#7c8fb5', '#9b8a63', '#6d8f6d', '#8f7f9e', '#5f879d'
]

const router = useRouter()
const points = ref([])
const nodes = ref([])
const edges = ref([])
const loading = ref(true)
const error = ref('')
const selectedId = ref('')
const hoveredId = ref('')
const draggingId = ref('')
const graphSvg = ref(null)

const INTRO_FLAG = '__codeagentKnowledgeIntroPlayed'
const INTRO_DELAY = 260
const INTRO_MAX = 4200
const INTRO_SETTLE = 3000
let introRaf = 0
let introTimer = 0
let introStart = 0
let introLast = 0
let introElapsed = 0
let dragOffsetX = 0
let dragOffsetY = 0
let draggingNode = null

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
    vx: 0,
    vy: 0,
    mass: 3,
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
      x: CENTER_X,
      y: CENTER_Y,
      vx: 0,
      vy: 0,
      mass: 1,
      r: 16,
      color: CLUSTER_COLORS[index % CLUSTER_COLORS.length],
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
        x: CENTER_X,
        y: CENTER_Y,
        vx: 0,
        vy: 0,
        mass: 0.72,
        r: 6,
        color: CLUSTER_COLORS[index % CLUSTER_COLORS.length],
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
    ['recursion', 'sort'],
    ['array', 'sort'],
    ['sort', 'algorithm'],
    ['algorithm', 'dp'],
    ['recursion', 'tree'],
    ['tree', 'graph'],
    ['array', 'hash'],
    ['string', 'hash'],
    ['algorithm', 'math'],
    ['math', 'dp'],
    ['graph', 'dp']
  ]
  for (const [from, to] of links) {
    if (byCode.has(from) && byCode.has(to)) {
      nextEdges.push({ sourceId: byCode.get(from), targetId: byCode.get(to), kind: 'cross' })
    }
  }

  const allNodes = [root, ...kpNodes, ...conceptNodes]
  const allById = new Map(allNodes.map((node) => [node.id, node]))
  for (const edge of nextEdges) {
    const source = allById.get(edge.sourceId)
    const target = allById.get(edge.targetId)
    edge.rest = Math.hypot(target.ax - source.ax, target.ay - source.ay)
  }

  nodes.value = allNodes
  edges.value = nextEdges
  selectedId.value = ''
  if (!window[INTRO_FLAG] && !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    for (const node of allNodes) {
      node.x = CENTER_X
      node.y = CENTER_Y
      node.vx = 0
      node.vy = 0
    }
  }
  scheduleIntro()
}

function scheduleIntro() {
  if (window[INTRO_FLAG] || window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    finishIntro()
    return
  }
  introTimer = window.setTimeout(startIntro, INTRO_DELAY)
}

function startIntro() {
  introTimer = 0
  window[INTRO_FLAG] = true
  introStart = performance.now()
  introLast = introStart
  introElapsed = 0

  for (const node of nodes.value) {
    node.x = CENTER_X
    node.y = CENTER_Y
    const angle = Math.random() * Math.PI * 2
    const speed = 0.35 + Math.random() * 0.9
    node.vx = Math.cos(angle) * speed
    node.vy = Math.sin(angle) * speed
  }

  introRaf = requestAnimationFrame(introTick)
}

function introTick(now) {
  const dt = Math.min((now - introLast) / 1000, 0.032)
  introLast = now
  introElapsed = now - introStart
  const step = Math.min(dt * 60, 2)

  applyForces(step)

  let maxSpeed = 0
  for (const node of nodes.value) {
    node.x += node.vx * step
    node.y += node.vy * step
    node.x = Math.max(16, Math.min(WIDTH - 16, node.x))
    node.y = Math.max(16, Math.min(HEIGHT - 16, node.y))
    maxSpeed = Math.max(maxSpeed, Math.hypot(node.vx, node.vy))
  }

  if (introElapsed > INTRO_MAX || (introElapsed > INTRO_SETTLE && maxSpeed < 0.12)) {
    finishIntro()
    return
  }
  introRaf = requestAnimationFrame(introTick)
}

function applyForces(step) {
  const list = nodes.value
  for (const node of list) {
    node.vx += (node.ax - node.x) * 0.003 * step
    node.vy += (node.ay - node.y) * 0.003 * step
  }

  for (let i = 0; i < list.length; i += 1) {
    const a = list[i]
    for (let j = i + 1; j < list.length; j += 1) {
      const b = list[j]
      let dx = b.x - a.x
      let dy = b.y - a.y
      let dist2 = dx * dx + dy * dy
      if (dist2 < 0.01) {
        dx = Math.random() - 0.5
        dy = Math.random() - 0.5
        dist2 = 0.25
      }
      const dist = Math.sqrt(dist2)
      const repulsion = Math.min(0.6, 130 / (dist2 + 900))
      const ux = dx / dist
      const uy = dy / dist
      a.vx -= (ux * repulsion * step) / a.mass
      a.vy -= (uy * repulsion * step) / a.mass
      b.vx += (ux * repulsion * step) / b.mass
      b.vy += (uy * repulsion * step) / b.mass
    }
  }

  for (const edge of edges.value) {
    const a = nodeById(edge.sourceId)
    const b = nodeById(edge.targetId)
    let dx = b.x - a.x
    let dy = b.y - a.y
    const dist = Math.hypot(dx, dy) || 1
    const stretch = dist - edge.rest
    const force = stretch * 0.003
    const ux = dx / dist
    const uy = dy / dist
    a.vx += (ux * force * step) / a.mass
    a.vy += (uy * force * step) / a.mass
    b.vx -= (ux * force * step) / b.mass
    b.vy -= (uy * force * step) / b.mass
  }

  for (const node of list) {
    node.vx *= Math.pow(0.90, step)
    node.vy *= Math.pow(0.90, step)
    const speed = Math.hypot(node.vx, node.vy)
    if (speed > 18) {
      node.vx = (node.vx / speed) * 18
      node.vy = (node.vy / speed) * 18
    }
  }
}

function finishIntro() {
  if (introTimer) {
    window.clearTimeout(introTimer)
    introTimer = 0
  }
  if (introRaf) {
    cancelAnimationFrame(introRaf)
    introRaf = 0
  }
  for (const node of nodes.value) {
    node.x = node.ax
    node.y = node.ay
    node.vx = 0
    node.vy = 0
  }
}

function toSvgPoint(event) {
  const svg = graphSvg.value
  if (!svg) return { x: 0, y: 0 }
  const point = svg.createSVGPoint()
  point.x = event.clientX
  point.y = event.clientY
  return point.matrixTransform(svg.getScreenCTM().inverse())
}

function startDrag(node, event) {
  if (event.button !== 0) return
  finishIntro()
  const point = toSvgPoint(event)
  draggingNode = node
  dragOffsetX = node.x - point.x
  dragOffsetY = node.y - point.y
  draggingId.value = node.id
  window.addEventListener('pointermove', moveDrag, { passive: false })
  window.addEventListener('pointerup', endDrag, { once: true })
  window.addEventListener('pointercancel', endDrag, { once: true })
  window.addEventListener('blur', endDrag, { once: true })
}

function moveDrag(event) {
  if (!draggingNode) return
  event.preventDefault()
  const point = toSvgPoint(event)
  draggingNode.x = Math.max(16, Math.min(WIDTH - 16, point.x + dragOffsetX))
  draggingNode.y = Math.max(16, Math.min(HEIGHT - 16, point.y + dragOffsetY))
}

function endDrag() {
  if (!draggingNode) return
  draggingNode = null
  draggingId.value = ''
  window.removeEventListener('pointermove', moveDrag)
  window.removeEventListener('pointerup', endDrag)
  window.removeEventListener('pointercancel', endDrag)
  window.removeEventListener('blur', endDrag)
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
  endDrag()
  if (introTimer) window.clearTimeout(introTimer)
  cancelAnimationFrame(introRaf)
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
          <span class="graph-tag"><el-icon><Connection /></el-icon> 可交互</span>
        </div>

        <div class="graph-stage">
          <div v-if="loading" class="graph-state">正在生成知识网络…</div>
          <div v-else-if="error" class="graph-state is-error">{{ error }}</div>
          <svg
            v-else
            ref="graphSvg"
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
                    'is-dim': activeId && !nodeVisible(node),
                    'is-dragging': draggingId === node.id
                  }
                ]"
                :transform="`translate(${node.x} ${node.y})`"
                :style="{ '--node-color': node.color || '#294866' }"
                @mouseenter="hoveredId = node.id"
                @mouseleave="hoveredId = ''"
                @pointerdown.stop.prevent="startDrag(node, $event)"
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
  border: 1px solid #e5e9ef;
  border-radius: 14px;
  background: #ffffff;
}

.graph-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px 0;
}

.graph-head h2 {
  color: #1f2937;
  font-size: 16px;
  font-weight: 650;
}

.graph-head p {
  margin-top: 5px;
  color: #8a94a3;
  font-size: 12px;
}

.graph-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #8a94a3;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.graph-stage {
  position: relative;
  flex: 1;
  min-height: 0;
  margin-top: 4px;
  background:
    radial-gradient(circle at 50% 46%, rgba(226, 232, 240, 0.5) 0, rgba(255, 255, 255, 0) 62%),
    #ffffff;
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
  color: #8a94a3;
  font-size: 13px;
}

.graph-state.is-error {
  color: #b42318;
}

.graph-edges line {
  stroke: #cbd5e1;
  stroke-width: 1;
  transition: opacity 160ms ease, stroke 160ms ease;
}

.graph-edges .edge-root {
  stroke: #94a3b8;
  stroke-opacity: 0.7;
}

.graph-edges .edge-branch {
  stroke-opacity: 0.42;
}

.graph-edges .edge-cross {
  stroke: #cbd5e1;
  stroke-dasharray: 3 5;
  stroke-opacity: 0.6;
}

.graph-edges line.is-active {
  stroke: #64748b;
  stroke-opacity: 0.95;
}

.graph-edges line.is-dim {
  stroke-opacity: 0.12;
}

.node {
  cursor: grab;
  outline: none;
  touch-action: none;
}

.node.is-dragging {
  cursor: grabbing;
}

.node-halo {
  fill: transparent;
  transition: fill 160ms ease, opacity 160ms ease;
}

.node-core {
  transition: fill 160ms ease, stroke 160ms ease, opacity 160ms ease;
}

.node-root .node-core {
  fill: #e8eef6;
  stroke: #94a3b8;
  stroke-width: 1.2;
}

.node-kp .node-core {
  fill: var(--node-color);
  stroke: #ffffff;
  stroke-width: 1.5;
}

.node-concept .node-core {
  fill: var(--node-color);
  stroke: #ffffff;
  stroke-width: 0.8;
  opacity: 0.62;
}

.node text {
  fill: #475569;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  pointer-events: none;
}

.node-root text {
  fill: #334155;
  font-size: 13px;
}

.node-concept text {
  fill: #94a3b8;
  font-size: 9px;
  font-weight: 500;
}

.node.is-active .node-halo {
  fill: rgba(148, 163, 184, 0.18);
}

.node.is-active .node-kp,
.node.is-active .node-root {
  filter: drop-shadow(0 2px 7px rgba(51, 65, 85, 0.18));
}

.node.is-active .node-core {
  stroke: #334155;
  stroke-width: 1.5;
}

.node.is-dim {
  opacity: 0.18;
}

.graph-legend {
  position: absolute;
  bottom: 16px;
  left: 20px;
  display: flex;
  gap: 16px;
  color: #8a94a3;
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

.dot-root { background: #cbd5e1; }
.dot-kp { background: #6f8fb0; }
.dot-concept { background: #9aa8b8; }

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
  max-height: 366px;
  margin-top: 8px;
  padding-right: 4px;
  overflow-y: auto;
  scrollbar-width: thin;
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

  .kp-list {
    max-height: none;
    overflow: visible;
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
