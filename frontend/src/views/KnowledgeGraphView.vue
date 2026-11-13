<template>
  <Teleport to="body">
    <div
      ref="canvasRef"
      class="kg-canvas"
      :class="{ 'is-panning': panning }"
    >
      <div class="kg-mode" role="tablist" aria-label="知识点视图">
        <button
          v-for="mode in [{ key: 'graph', label: '关系图' }, { key: 'chart', label: '学习曲线' }]"
          :key="mode.key"
          type="button"
          role="tab"
          :aria-selected="viewMode === mode.key"
          :class="{ 'is-active': viewMode === mode.key }"
          @click="switchView(mode.key)"
        >{{ mode.label }}</button>
      </div>

      <svg
        v-if="!error && viewMode === 'graph'"
        ref="graphSvg"
        class="kg-svg"
        :viewBox="`0 0 ${canvasWidth} ${canvasHeight}`"
        role="img"
        aria-label="编程基础知识网络"
        @pointermove="onCanvasPointerMove"
        @pointerleave="onCanvasPointerLeave"
        @pointerdown="onCanvasPointerDown"
        @wheel.prevent="onWheel"
      >
        <g class="kg-camera" :transform="cameraTransform">
          <g class="kg-edges" aria-hidden="true">
            <line
              v-for="(edge, index) in edges"
              :key="`base-${edge.sourceId}-${edge.targetId}-${index}`"
              :x1="nodeX(edge.sourceId)"
              :y1="nodeY(edge.sourceId)"
              :x2="nodeX(edge.targetId)"
              :y2="nodeY(edge.targetId)"
              class="kg-edge"
              :class="[`is-${edge.kind}`, { 'is-active': edgeActive(edge), 'is-dim': activeId && !edgeActive(edge) }]"
            />
            <line
              v-for="(edge, index) in edges"
              :key="`flow-${edge.sourceId}-${edge.targetId}-${index}`"
              :x1="nodeX(edge.sourceId)"
              :y1="nodeY(edge.sourceId)"
              :x2="nodeX(edge.targetId)"
              :y2="nodeY(edge.targetId)"
              class="kg-flow"
              :class="[`is-${edge.kind}`, { 'is-active': edgeActive(edge), 'is-dim': activeId && !edgeActive(edge) }]"
              :style="{ animationDelay: `${-index * 0.37}s` }"
            />
          </g>

          <g class="kg-nodes">
            <g
              v-for="node in nodes"
              :key="node.id"
              class="kg-node"
              :class="[
                `kg-node-${node.type}`,
                {
                  'is-active': activeId === node.id,
                  'is-dim': activeId && !nodeVisible(node),
                  'is-related': activeId && nodeVisible(node) && node.id !== activeId,
                  'is-dragging': draggingId === node.id
                }
              ]"
              :transform="`translate(${node.x} ${node.y})`"
              :style="{ '--node-color': node.color || '#93a7ba' }"
              @pointerenter="hoverNode(node)"
              @pointerleave="leaveNode(node)"
              @pointerdown.stop.prevent="startNodeDrag(node, $event)"
            >
              <g class="kg-breathe" :style="breatheStyle(node)">
                <circle class="kg-halo" :r="node.r + 9" />
                <circle class="kg-core" :r="node.r" />
                <text
                  class="kg-label"
                  :y="node.r + (node.type === 'root' ? 23 : node.type === 'kp' ? 19 : 14)"
                  text-anchor="middle"
                >{{ node.label }}</text>
              </g>
              <title>{{ node.label }}</title>
            </g>
          </g>
        </g>
      </svg>

      <section v-if="!error && viewMode === 'chart'" class="kg-chart" aria-label="知识点学习曲线">
        <header class="kg-chart-head">
          <div>
            <span class="kg-chart-kicker">学习曲线</span>
            <h1>知识点练习分布</h1>
            <p>{{ chartHasData ? '选择知识点查看练习与错误率，再回到对应题目。' : '按题库题量展示知识点分布；登录后切换为你的练习数据。' }}</p>
          </div>
          <div class="kg-chart-total">
            <strong>{{ chartTotal }}</strong>
            <span>{{ chartHasData ? '次练习' : '道题目' }}</span>
          </div>
        </header>

        <div class="kg-chart-chips" role="tablist" aria-label="切换知识点">
          <button
            v-for="item in chartPoints"
            :key="item.id"
            type="button"
            :class="{ 'is-active': Number(chartSelectedPoint?.id) === Number(item.id) }"
            @click="selectChartPoint(item)"
          >
            {{ item.name }}
            <span>{{ item.value }}</span>
          </button>
        </div>

        <div class="kg-chart-main">
          <div ref="chartRef" class="kg-chart-canvas"></div>
          <aside v-if="chartSelectedPoint" class="kg-chart-side">
            <span class="kg-chart-kicker">当前知识点</span>
            <h2>{{ chartSelectedPoint.name }}</h2>
            <p v-if="chartSelectedPoint.hasData">
              练习 {{ chartSelectedPoint.submitCount }} 次 · 通过 {{ chartSelectedPoint.acCount }} 次
              <br />错误率 {{ Math.round(chartSelectedPoint.errorRate * 100) }}%
            </p>
            <p v-else>{{ chartSelectedPoint.questionCount || 0 }} 道练习题，暂无练习记录。</p>
            <p class="kg-chart-desc">{{ chartSelectedPoint.description || '把这一个知识点练稳，再串到下一题。' }}</p>
            <button type="button" class="kg-chart-action" @click="practicePoint(chartSelectedPoint)">
              去练习
              <el-icon><ArrowRight /></el-icon>
            </button>
          </aside>
        </div>
      </section>

      <div v-if="error" class="kg-state">
        <p>{{ error }}</p>
      </div>

      <Transition name="kg-card">
        <aside
          v-if="selectedPoint && viewMode === 'graph'"
          class="kg-card"
          @pointerdown.stop
          @wheel.stop
        >
          <button class="kg-card-close" type="button" aria-label="关闭知识点详情" @click="closeCard">
            <el-icon><Close /></el-icon>
          </button>
          <span class="kg-card-kicker">知识点</span>
          <h2>{{ selectedPoint.name }}</h2>
          <p class="kg-card-meta">{{ selectedPoint.questionCount || 0 }} 道练习</p>
          <p class="kg-card-desc">{{ selectedPoint.description || '从这组题目开始，把这一个知识点练稳。' }}</p>
          <button class="kg-card-action" type="button" @click="goPractice">
            去练习
            <el-icon><ArrowRight /></el-icon>
          </button>
        </aside>
      </Transition>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Close } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { listKnowledgePoints } from '@/api/question'
import { getProfileStat } from '@/api/profile'

const CONCEPTS = {
  array: ['遍历', '前缀和', '滑动窗口', '双指针'],
  string: ['回文', '子串', 'KMP', '字典树'],
  linked_list: ['反转', '双指针', '环形链表', '合并'],
  recursion: ['分治', '回溯', '记忆化', '递归树'],
  sort: ['快排', '归并', '堆', '稳定性'],
  algorithm: ['二分', '贪心', '模拟', '枚举'],
  tree: ['遍历', '二叉搜索', '深度', 'LCA'],
  graph: ['BFS', 'DFS', '最短路', '拓扑'],
  dp: ['状态', '转移', '背包', '区间'],
  math: ['数论', '组合', '快速幂', '概率'],
  hash: ['计数', '去重', '前缀哈希', '冲突']
}

const COLORS = [
  '#8fa9be', '#9b8aa8', '#87a596', '#b39a78', '#7fa8a2',
  '#b58e8e', '#8c9cbf', '#a69a7c', '#7f9b82', '#9a8cae', '#7e9bb0'
]

const PREREQUISITES = [
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

const ROOT_ID = 'root'
const ROOT_LABEL = '编程基础'
const INTRO_DELAY = 180
const INTRO_MAX = 2800
const INTRO_SETTLE = 1550
const SETTLE_MS = 320
const CURSOR_RADIUS = 96
const MIN_ZOOM = 0.5
const MAX_ZOOM = 2.5

const router = useRouter()
const canvasRef = ref(null)
const graphSvg = ref(null)
const canvasWidth = ref(0)
const canvasHeight = ref(0)
const points = ref([])
const nodes = ref([])
const edges = ref([])
const error = ref('')
const hoveredId = ref('')
const selectedId = ref('')
const draggingId = ref('')
const panning = ref(false)
const viewMode = ref('graph')
const chartRef = ref(null)
const knowledgeStats = ref([])
const chartSelectedId = ref('')
let chartInstance = null

const cam = ref({ x: 0, y: 0, scale: 1 })
const reduced = ref(false)
const mediaQuery = typeof window !== 'undefined'
  ? window.matchMedia?.('(prefers-reduced-motion: reduce)')
  : null

let introTimer = 0
let raf = 0
let loopState = 'idle'
let loopStart = 0
let loopLast = 0
let loopElapsed = 0
let settleStart = 0
let settleFrom = []
let dragState = null
let panState = null
let resizeObserver = null
let previousBodyOverflow = ''

const cursor = { x: 0, y: 0, active: false }

const activeId = computed(() => hoveredId.value || selectedId.value)
const selectedPoint = computed(() => {
  if (!selectedId.value.startsWith('kp-')) return null
  const id = Number(selectedId.value.slice(3))
  return points.value.find((point) => Number(point.id) === id) || null
})
const cameraTransform = computed(() => `translate(${cam.value.x} ${cam.value.y}) scale(${cam.value.scale})`)

const chartPoints = computed(() => points.value.map((point) => {
  const stat = knowledgeStats.value.find((item) => Number(item.knowledgePointId) === Number(point.id))
  const hasData = Boolean(stat?.hasData)
  return {
    ...point,
    hasData,
    submitCount: Number(stat?.submitCount || 0),
    acCount: Number(stat?.acCount || 0),
    errorRate: Number(stat?.errorRate || 0),
    value: hasData ? Number(stat.submitCount) : Number(point.questionCount || 0)
  }
}))
const chartHasData = computed(() => chartPoints.value.some((item) => item.hasData))
const chartSelectedPoint = computed(() => (
  chartPoints.value.find((item) => Number(item.id) === Number(chartSelectedId.value))
  || chartPoints.value[0]
  || null
))
const chartTotal = computed(() => chartPoints.value.reduce((sum, item) => sum + item.value, 0))

const neighbors = computed(() => {
  const id = activeId.value
  if (!id) return new Set()
  const set = new Set([id])
  for (const edge of edges.value) {
    if (edge.sourceId === id) set.add(edge.targetId)
    if (edge.targetId === id) set.add(edge.sourceId)
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

function breatheStyle(node) {
  return {
    animationDelay: `${-(node.phase || 0) * 1.4}s`,
    animationDuration: `${node.type === 'concept' ? 6.8 : 7.6}s`
  }
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}

function clampNode(node) {
  const margin = node.type === 'root' ? 0 : 18
  node.x = clamp(node.x, margin + node.r, canvasWidth.value - margin - node.r)
  node.y = clamp(node.y, margin + node.r, canvasHeight.value - margin - node.r)
}

function resolveAnchors(list) {
  for (let iteration = 0; iteration < 24; iteration += 1) {
    let changed = false
    for (let i = 0; i < list.length; i += 1) {
      for (let j = i + 1; j < list.length; j += 1) {
        const a = list[i]
        const b = list[j]
        if (a.type === 'root' && b.type === 'root') continue
        let dx = b.ax - a.ax
        let dy = b.ay - a.ay
        let dist = Math.hypot(dx, dy)
        if (dist < 0.001) {
          dx = (i % 2 ? -1 : 1) * (j + 1)
          dy = (j % 2 ? -1 : 1) * (i + 1)
          dist = Math.hypot(dx, dy)
        }
        const min = a.r + b.r + (a.type === 'concept' || b.type === 'concept' ? 7 : 13)
        if (dist >= min) continue
        const push = min - dist
        const ux = dx / dist
        const uy = dy / dist
        if (a.type === 'root') {
          b.ax += ux * push
          b.ay += uy * push
        } else if (b.type === 'root') {
          a.ax -= ux * push
          a.ay -= uy * push
        } else {
          a.ax -= ux * push * 0.5
          a.ay -= uy * push * 0.5
          b.ax += ux * push * 0.5
          b.ay += uy * push * 0.5
        }
        changed = true
      }
    }
    for (const node of list) {
      if (node.type === 'root') continue
      node.ax = clamp(node.ax, node.r + 24, canvasWidth.value - node.r - 24)
      node.ay = clamp(node.ay, node.r + 28, canvasHeight.value - node.r - 34)
    }
    if (!changed) break
  }
}

function makeGraph(list, animate = true) {
  const width = canvasWidth.value
  const height = canvasHeight.value
  if (!width || !height) return

  const centerX = width / 2
  const centerY = height / 2
  const ringX = Math.min(width * 0.34, 430)
  const ringY = Math.min(height * 0.34, 300)
  const compact = width < 700
  const conceptFan = compact ? 1.5 : 2.2
  const conceptDistance = compact ? 78 : 100
  const root = {
    id: ROOT_ID,
    label: ROOT_LABEL,
    type: 'root',
    ax: centerX,
    ay: centerY,
    x: centerX,
    y: centerY,
    vx: 0,
    vy: 0,
    mass: 12,
    r: 42,
    phase: 0,
    color: '#dfeaf5'
  }

  const kpNodes = []
  const conceptNodes = []
  const nextEdges = []
  const count = Math.max(list.length, 1)

  list.forEach((point, index) => {
    const angle = (-Math.PI / 2) + (Math.PI * 2 * index) / count
    const ax = centerX + Math.cos(angle) * ringX
    const ay = centerY + Math.sin(angle) * ringY
    const color = COLORS[index % COLORS.length]
    const kpNode = {
      id: `kp-${point.id}`,
      label: point.name,
      type: 'kp',
      kpId: point.id,
      ax,
      ay,
      x: centerX,
      y: centerY,
      vx: 0,
      vy: 0,
      mass: 1.4,
      r: 19,
      color,
      phase: index * 0.73
    }
    kpNodes.push(kpNode)
    nextEdges.push({ sourceId: ROOT_ID, targetId: kpNode.id, kind: 'root' })

    const concepts = CONCEPTS[point.code] || ['概念', '练习']
    concepts.forEach((concept, conceptIndex) => {
      const fan = concepts.length > 1 ? (conceptIndex / (concepts.length - 1) - 0.5) * conceptFan : 0
      const offsetAngle = angle + fan
      const distance = conceptDistance
      const conceptNode = {
        id: `concept-${point.id}-${conceptIndex}`,
        label: concept,
        type: 'concept',
        kpId: point.id,
        ax: ax + Math.cos(offsetAngle) * distance,
        ay: ay + Math.sin(offsetAngle) * distance,
        x: centerX,
        y: centerY,
        vx: 0,
        vy: 0,
        mass: 0.55,
        r: 7.5,
        color,
        phase: index * 0.73 + conceptIndex * 1.7 + 0.9
      }
      conceptNodes.push(conceptNode)
      nextEdges.push({ sourceId: kpNode.id, targetId: conceptNode.id, kind: 'branch' })
    })
  })

  const byCode = new Map(list.map((point) => [point.code, `kp-${point.id}`]))
  for (const [from, to] of PREREQUISITES) {
    if (byCode.has(from) && byCode.has(to)) {
      nextEdges.push({ sourceId: byCode.get(from), targetId: byCode.get(to), kind: 'pre' })
    }
  }

  const allNodes = [root, ...kpNodes, ...conceptNodes]
  resolveAnchors(allNodes)

  const byId = new Map(allNodes.map((node) => [node.id, node]))
  for (const edge of nextEdges) {
    const source = byId.get(edge.sourceId)
    const target = byId.get(edge.targetId)
    edge.rest = Math.hypot(target.ax - source.ax, target.ay - source.ay)
  }

  nodes.value = allNodes
  edges.value = nextEdges
  hoveredId.value = ''
  selectedId.value = ''
  cam.value = { x: 0, y: 0, scale: 1 }

  if (!animate || reduced.value) {
    for (const node of allNodes) {
      node.x = node.ax
      node.y = node.ay
    }
    finishIntro()
    return
  }

  for (const node of allNodes) {
    node.x = centerX
    node.y = centerY
    node.vx = 0
    node.vy = 0
  }
  scheduleIntro()
}

function scheduleIntro() {
  if (reduced.value) {
    finishIntro()
    return
  }
  if (introTimer) window.clearTimeout(introTimer)
  introTimer = window.setTimeout(() => {
    introTimer = 0
    startIntro()
  }, INTRO_DELAY)
}

function startIntro() {
  stopLoop()
  loopState = 'intro'
  loopStart = performance.now()
  loopLast = loopStart
  loopElapsed = 0

  for (const node of nodes.value) {
    node.x = node.ax
    node.y = node.ay
  }
  const centerX = canvasWidth.value / 2
  const centerY = canvasHeight.value / 2
  for (const node of nodes.value) {
    if (node.type === 'root') {
      node.x = centerX
      node.y = centerY
      continue
    }
    node.x = centerX
    node.y = centerY
    const angle = Math.random() * Math.PI * 2
    const speed = 0.35 + Math.random() * 0.95
    node.vx = Math.cos(angle) * speed
    node.vy = Math.sin(angle) * speed
  }
  raf = requestAnimationFrame(tick)
}

function stopLoop() {
  if (raf) cancelAnimationFrame(raf)
  raf = 0
  loopState = 'idle'
}

function startInteractionLoop() {
  if (reduced.value || loopState === 'intro' || loopState === 'settle') return
  if (loopState === 'interact') return
  if (raf) cancelAnimationFrame(raf)
  loopState = 'interact'
  loopStart = performance.now()
  loopLast = loopStart
  loopElapsed = 0
  raf = requestAnimationFrame(tick)
}

function tick(now) {
  if (loopState === 'settle') {
    settleTick(now)
    return
  }
  if (!loopLast) loopLast = now
  const dt = Math.min((now - loopLast) / 1000, 0.032)
  loopLast = now
  loopElapsed = now - loopStart
  const step = Math.min(dt * 60, 2)

  applyForces(step)
  integrate(step)
  separateNodes()

  let maxSpeed = 0
  for (const node of nodes.value) {
    maxSpeed = Math.max(maxSpeed, Math.hypot(node.vx, node.vy))
  }

  if (loopState === 'intro') {
    if (loopElapsed > INTRO_MAX || (loopElapsed > INTRO_SETTLE && maxSpeed < 0.09)) {
      beginSettle(now)
      raf = requestAnimationFrame(tick)
      return
    }
  } else if (loopState === 'interact') {
    if (!draggingId.value && !cursor.active && maxSpeed < 0.055) {
      stopLoop()
      return
    }
  }

  raf = requestAnimationFrame(tick)
}

function beginSettle(now) {
  if (loopState !== 'intro') return
  loopState = 'settle'
  settleStart = now
  settleFrom = nodes.value.map((node) => ({ x: node.x, y: node.y }))
}

function settleTick(now) {
  const progress = Math.min(1, (now - settleStart) / SETTLE_MS)
  const eased = 1 - Math.pow(1 - progress, 3)
  nodes.value.forEach((node, index) => {
    const from = settleFrom[index]
    if (!from) return
    node.x = from.x + (node.ax - from.x) * eased
    node.y = from.y + (node.ay - from.y) * eased
    node.vx = 0
    node.vy = 0
  })
  if (progress < 1) {
    raf = requestAnimationFrame(tick)
    return
  }
  finishIntro()
}

function applyForces(step) {
  const list = nodes.value
  for (const node of list) {
    const strength = node.type === 'root' ? 0.0018 : 0.0055
    node.vx += (node.ax - node.x) * strength * step
    node.vy += (node.ay - node.y) * strength * step
  }

  if (cursor.active && !reduced.value) {
    for (const node of list) {
      if (node.id === draggingId.value) continue
      const dx = node.x - cursor.x
      const dy = node.y - cursor.y
      const dist = Math.hypot(dx, dy) || 1
      if (dist >= CURSOR_RADIUS) continue
      const push = (1 - dist / CURSOR_RADIUS) * 1.85
      const ux = dx / dist
      const uy = dy / dist
      const jitter = (Math.random() - 0.5) * 0.42
      node.vx += (ux * push - uy * jitter) * step
      node.vy += (uy * push + ux * jitter) * step
    }
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
      if (dist > 260) continue
      const repulsion = Math.min(0.42, 78 / (dist2 + 700))
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
    if (!a || !b) continue
    let dx = b.x - a.x
    let dy = b.y - a.y
    const dist = Math.hypot(dx, dy) || 1
    const stretch = dist - edge.rest
    const force = stretch * 0.0045
    const ux = dx / dist
    const uy = dy / dist
    a.vx += (ux * force * step) / a.mass
    a.vy += (uy * force * step) / a.mass
    b.vx -= (ux * force * step) / b.mass
    b.vy -= (uy * force * step) / b.mass
  }

  for (const node of list) {
    node.vx *= Math.pow(0.9, step)
    node.vy *= Math.pow(0.9, step)
    const speed = Math.hypot(node.vx, node.vy)
    if (speed > 16) {
      node.vx = (node.vx / speed) * 16
      node.vy = (node.vy / speed) * 16
    }
  }
}

function integrate(step) {
  for (const node of nodes.value) {
    node.x += node.vx * step
    node.y += node.vy * step
    clampNode(node)
  }
}

function separateNodes() {
  const list = nodes.value
  for (let i = 0; i < list.length; i += 1) {
    const a = list[i]
    for (let j = i + 1; j < list.length; j += 1) {
      const b = list[j]
      let dx = b.x - a.x
      let dy = b.y - a.y
      let dist = Math.hypot(dx, dy)
      if (dist < 0.001) {
        dx = 1
        dy = 0.4
        dist = Math.hypot(dx, dy)
      }
      const min = a.r + b.r + (a.type === 'concept' || b.type === 'concept' ? 5 : 9)
      if (dist >= min) continue
      const push = min - dist
      const ux = dx / dist
      const uy = dy / dist
      if (a.type === 'root') {
        b.x += ux * push
        b.y += uy * push
      } else if (b.type === 'root') {
        a.x -= ux * push
        a.y -= uy * push
      } else {
        a.x -= ux * push * 0.5
        a.y -= uy * push * 0.5
        b.x += ux * push * 0.5
        b.y += uy * push * 0.5
      }
      clampNode(a)
      clampNode(b)
    }
  }
}

function finishIntro() {
  if (introTimer) {
    window.clearTimeout(introTimer)
    introTimer = 0
  }
  stopLoop()
  for (const node of nodes.value) {
    node.x = node.ax
    node.y = node.ay
    node.vx = 0
    node.vy = 0
  }
}

function localPoint(event) {
  const svg = graphSvg.value
  if (!svg) return { x: 0, y: 0 }
  const rect = svg.getBoundingClientRect()
  return {
    x: (event.clientX - rect.left) * (canvasWidth.value / rect.width),
    y: (event.clientY - rect.top) * (canvasHeight.value / rect.height)
  }
}

function toWorld(event) {
  const point = localPoint(event)
  return {
    x: (point.x - cam.value.x) / cam.value.scale,
    y: (point.y - cam.value.y) / cam.value.scale
  }
}

function hoverNode(node) {
  hoveredId.value = node.id
}

function leaveNode(node) {
  if (hoveredId.value === node.id) hoveredId.value = ''
}

function selectNode(node) {
  if (node.type === 'root') {
    selectedId.value = ''
    return
  }
  if (node.type === 'concept') {
    selectedId.value = `kp-${node.kpId}`
    return
  }
  selectedId.value = node.id
}

function startNodeDrag(node, event) {
  if (event.button !== 0) return
  stopCursor()
  if (loopState === 'intro' || loopState === 'settle') finishIntro()
  const point = toWorld(event)
  dragState = {
    node,
    offsetX: node.x - point.x,
    offsetY: node.y - point.y,
    startX: event.clientX,
    startY: event.clientY,
    moved: false
  }
  draggingId.value = node.id
  window.addEventListener('pointermove', onDragMove, { passive: false })
  window.addEventListener('pointerup', onDragEnd, { once: true })
  window.addEventListener('pointercancel', onDragEnd, { once: true })
  window.addEventListener('blur', onDragEnd, { once: true })
  startInteractionLoop()
}

function onDragMove(event) {
  if (!dragState) return
  event.preventDefault()
  if (Math.hypot(event.clientX - dragState.startX, event.clientY - dragState.startY) > 3) {
    dragState.moved = true
  }
  const point = toWorld(event)
  dragState.node.x = clamp(point.x + dragState.offsetX, dragState.node.r + 18, canvasWidth.value - dragState.node.r - 18)
  dragState.node.y = clamp(point.y + dragState.offsetY, dragState.node.r + 18, canvasHeight.value - dragState.node.r - 18)
  dragState.node.vx = 0
  dragState.node.vy = 0
}

function onDragEnd() {
  if (!dragState) return
  const { node, moved } = dragState
  dragState = null
  draggingId.value = ''
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)
  window.removeEventListener('pointercancel', onDragEnd)
  window.removeEventListener('blur', onDragEnd)

  if (moved) {
    if (node.type === 'root') {
      node.ax = node.x
      node.ay = node.y
    }
    if (reduced.value) {
      node.x = node.ax
      node.y = node.ay
    } else {
      startInteractionLoop()
    }
    return
  }
  selectNode(node)
}

function onCanvasPointerDown(event) {
  if (event.button !== 0) return
  if (event.target.closest?.('.kg-node')) return
  if (event.target.closest?.('.kg-card')) return
  stopCursor()
  if (loopState === 'intro' || loopState === 'settle') finishIntro()
  panState = {
    startX: event.clientX,
    startY: event.clientY,
    camX: cam.value.x,
    camY: cam.value.y,
    moved: false
  }
  panning.value = true
  window.addEventListener('pointermove', onPanMove, { passive: false })
  window.addEventListener('pointerup', onPanEnd, { once: true })
  window.addEventListener('pointercancel', onPanEnd, { once: true })
  event.preventDefault()
}

function onPanMove(event) {
  if (!panState) return
  const dx = event.clientX - panState.startX
  const dy = event.clientY - panState.startY
  if (Math.hypot(dx, dy) > 3) panState.moved = true
  cam.value.x = panState.camX + dx
  cam.value.y = panState.camY + dy
}

function onPanEnd() {
  if (!panState) return
  const moved = panState.moved
  panState = null
  panning.value = false
  window.removeEventListener('pointermove', onPanMove)
  window.removeEventListener('pointerup', onPanEnd)
  window.removeEventListener('pointercancel', onPanEnd)
  if (!moved) {
    selectedId.value = ''
    hoveredId.value = ''
  }
}

function onCanvasPointerMove(event) {
  if (event.pointerType === 'touch' || panning.value || draggingId.value) return
  if (loopState === 'intro') return
  if (reduced.value) return
  const point = toWorld(event)
  cursor.x = point.x
  cursor.y = point.y
  cursor.active = true
  startInteractionLoop()
}

function onCanvasPointerLeave() {
  cursor.active = false
}

function stopCursor() {
  cursor.active = false
}

function onWheel(event) {
  event.preventDefault()
  const svg = graphSvg.value
  if (!svg) return
  const rect = svg.getBoundingClientRect()
  const mx = (event.clientX - rect.left) * (canvasWidth.value / rect.width)
  const my = (event.clientY - rect.top) * (canvasHeight.value / rect.height)
  const oldScale = cam.value.scale
  const nextScale = clamp(oldScale * Math.exp(-event.deltaY * 0.00135), MIN_ZOOM, MAX_ZOOM)
  if (nextScale === oldScale) return
  const worldX = (mx - cam.value.x) / oldScale
  const worldY = (my - cam.value.y) / oldScale
  cam.value.x = mx - worldX * nextScale
  cam.value.y = my - worldY * nextScale
  cam.value.scale = nextScale
}

function closeCard() {
  selectedId.value = ''
  hoveredId.value = ''
}

function goPractice() {
  if (!selectedPoint.value) return
  router.push({ path: '/question', query: { kp: selectedPoint.value.id } })
}

function practicePoint(point) {
  if (!point) return
  router.push({ path: '/question', query: { kp: point.id } })
}

function selectChartPoint(point) {
  chartSelectedId.value = point?.id ?? ''
  renderChart()
}

function switchView(mode) {
  if (viewMode.value === mode) return
  viewMode.value = mode
  if (mode === 'chart') {
    if (!chartSelectedId.value && chartPoints.value.length) {
      chartSelectedId.value = chartPoints.value[0].id
    }
    nextTick(renderChart)
  } else {
    disposeChart()
  }
}

function disposeChart() {
  if (!chartInstance) return
  chartInstance.dispose()
  chartInstance = null
}

async function loadKnowledgeStats() {
  try {
    const data = await getProfileStat()
    knowledgeStats.value = Array.isArray(data?.knowledgeStats) ? data.knowledgeStats : []
  } catch {
    knowledgeStats.value = []
  }
  if (viewMode.value === 'chart') nextTick(renderChart)
}

function renderChart() {
  if (viewMode.value !== 'chart' || !chartRef.value || !chartPoints.value.length) return
  // 切换视图会销毁并重建 DOM；旧实例仍绑在已卸载节点上，直接 setOption 会得到空白画布。
  if (chartInstance && chartInstance.getDom() !== chartRef.value) {
    disposeChart()
  }
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const data = chartPoints.value
  const selectedId = Number(chartSelectedPoint.value?.id)
  const seriesName = chartHasData.value ? '练习次数' : '题目数量'
  chartInstance.setOption({
    animationDuration: 280,
    grid: { left: 18, right: 16, top: 24, bottom: 14, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#334155', fontSize: 12 },
      formatter: (params) => {
        const item = data[params[0]?.dataIndex]
        if (!item) return ''
        const detail = item.hasData
          ? `练习 ${item.submitCount} 次<br/>通过 ${item.acCount} 次 · 错误率 ${Math.round(item.errorRate * 100)}%`
          : `${item.questionCount || 0} 道练习题`
        return `<strong>${item.name}</strong><br/>${detail}`
      }
    },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#7b8797',
        fontSize: 11,
        interval: 0,
        rotate: data.length > 8 ? 26 : 0,
        margin: 12
      }
    },
    yAxis: {
      type: 'value',
      splitNumber: 4,
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#eef2f7', type: 'dashed' } }
    },
    series: [{
      name: seriesName,
      type: 'line',
      smooth: 0.34,
      symbol: 'circle',
      symbolSize: 7,
      showSymbol: true,
      data: data.map((item) => ({
        value: item.value,
        itemStyle: Number(item.id) === selectedId
          ? { color: '#b58552', borderColor: '#ffffff', borderWidth: 2 }
          : { color: '#6d8cab', borderColor: '#ffffff', borderWidth: 2 }
      })),
      lineStyle: { width: 2.2, color: '#6d8cab' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(109, 140, 171, 0.30)' },
          { offset: 1, color: 'rgba(109, 140, 171, 0.02)' }
        ])
      },
      emphasis: { focus: 'series', scale: 1.25 }
    }]
  }, true)
  chartInstance.resize()
}

function onKeydown(event) {
  if (event.key === 'Escape') closeCard()
}

function measure() {
  const el = canvasRef.value
  if (!el) return false
  const width = Math.round(el.clientWidth)
  const height = Math.round(el.clientHeight)
  if (!width || !height) return false
  if (width === canvasWidth.value && height === canvasHeight.value) return false
  canvasWidth.value = width
  canvasHeight.value = height
  return true
}

function handleResize() {
  if (!measure()) return
  if (viewMode.value === 'chart') {
    chartInstance?.resize()
    return
  }
  if (points.value.length && !error.value) {
    makeGraph(points.value, false)
  }
}

function onMotionChange() {
  reduced.value = Boolean(mediaQuery?.matches)
  if (reduced.value) {
    finishIntro()
    stopCursor()
  }
}

async function load() {
  try {
    const data = await listKnowledgePoints()
    points.value = Array.isArray(data) ? data : []
    if (!points.value.length) {
      error.value = '暂无知识点'
      return
    }
    if (!canvasWidth.value || !canvasHeight.value) measure()
    if (!canvasWidth.value || !canvasHeight.value) {
      await nextTick()
      measure()
    }
    if (!canvasWidth.value || !canvasHeight.value) {
      error.value = '画布初始化失败'
      return
    }
    makeGraph(points.value)
    loadKnowledgeStats()
  } catch (err) {
    error.value = err?.message || '知识点加载失败'
  }
}

onMounted(async () => {
  reduced.value = Boolean(mediaQuery?.matches)
  if (mediaQuery?.addEventListener) mediaQuery.addEventListener('change', onMotionChange)
  previousBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  window.addEventListener('keydown', onKeydown)
  await nextTick()
  measure()
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => handleResize())
    if (canvasRef.value) resizeObserver.observe(canvasRef.value)
  } else {
    window.addEventListener('resize', handleResize)
  }
  load()
})

onBeforeUnmount(() => {
  if (introTimer) window.clearTimeout(introTimer)
  stopLoop()
  stopCursor()
  if (dragState) {
    window.removeEventListener('pointermove', onDragMove)
    window.removeEventListener('pointerup', onDragEnd)
    window.removeEventListener('pointercancel', onDragEnd)
    window.removeEventListener('blur', onDragEnd)
  }
  if (panState) {
    window.removeEventListener('pointermove', onPanMove)
    window.removeEventListener('pointerup', onPanEnd)
    window.removeEventListener('pointercancel', onPanEnd)
  }
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', onKeydown)
  mediaQuery?.removeEventListener?.('change', onMotionChange)
  resizeObserver?.disconnect()
  disposeChart()
  document.body.style.overflow = previousBodyOverflow
})
</script>

<style scoped>
.kg-canvas {
  position: fixed;
  top: 64px;
  left: 0;
  z-index: 5;
  width: 100vw;
  height: calc(100vh - 64px);
  overflow: hidden;
  background: #ffffff;
  user-select: none;
}

.kg-canvas.is-panning {
  cursor: grabbing;
}

.kg-svg {
  display: block;
  width: 100%;
  height: 100%;
  cursor: grab;
  touch-action: none;
}

.kg-canvas.is-panning .kg-svg {
  cursor: grabbing;
}

.kg-camera {
  transform-origin: 0 0;
}

.kg-edge,
.kg-flow {
  vector-effect: non-scaling-stroke;
  transition: opacity 150ms ease, stroke 150ms ease;
}

.kg-edge {
  stroke: #cbd5e1;
  stroke-width: 1;
}

.kg-edge.is-root {
  stroke: #b8c5d2;
  stroke-width: 1.15;
}

.kg-edge.is-branch {
  stroke: #d7dfe8;
  stroke-width: 0.85;
}

.kg-edge.is-pre {
  stroke: #c8d2dd;
  stroke-width: 0.9;
  stroke-dasharray: 3 6;
}

.kg-edge.is-active {
  stroke: #6f8ca8;
  stroke-opacity: 0.95;
}

.kg-edge.is-dim {
  stroke-opacity: 0.12;
}

.kg-flow {
  stroke: #8ea8bd;
  stroke-width: 1.1;
  stroke-opacity: 0.12;
  stroke-dasharray: 2 25;
  pointer-events: none;
  animation: kg-flow 8.5s linear infinite;
}

.kg-flow.is-root {
  stroke-opacity: 0.16;
}

.kg-flow.is-branch {
  stroke-opacity: 0.1;
}

.kg-flow.is-pre {
  stroke-opacity: 0.08;
}

.kg-flow.is-active {
  stroke: #6f8ca8;
  stroke-opacity: 0.5;
}

.kg-flow.is-dim {
  stroke-opacity: 0.03;
}

.kg-node {
  cursor: grab;
  outline: none;
  transition: opacity 150ms ease;
}

.kg-node.is-dragging {
  cursor: grabbing;
}

.kg-node.is-dim {
  opacity: 0.14;
}

.kg-breathe {
  animation: kg-breathe 7.6s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

.kg-core {
  stroke-width: 2;
  transition: fill 150ms ease, stroke 150ms ease, opacity 150ms ease;
}

.kg-node-root .kg-core {
  fill: #dfeaf5;
  stroke: #9aa9ba;
  stroke-width: 1.25;
}

.kg-node-kp .kg-core {
  fill: var(--node-color);
  stroke: #ffffff;
}

.kg-node-concept .kg-core {
  fill: var(--node-color);
  fill-opacity: 0.48;
  stroke: #ffffff;
  stroke-width: 1;
}

.kg-halo {
  fill: transparent;
  opacity: 0;
  pointer-events: none;
  transform-box: fill-box;
  transform-origin: center;
  transition: opacity 150ms ease;
}

.kg-node.is-active .kg-halo {
  fill: var(--node-color);
  opacity: 0.16;
  animation: kg-halo 1.8s ease-in-out infinite;
}

.kg-node.is-active .kg-core {
  stroke: #5e7d9b;
  stroke-width: 2.4;
  filter: drop-shadow(0 3px 8px rgba(38, 58, 78, 0.2));
}

.kg-node-root.is-active .kg-core {
  stroke: #5e7d9b;
}

.kg-label {
  fill: #334155;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.01em;
  paint-order: stroke;
  stroke: #ffffff;
  stroke-width: 3px;
  stroke-linejoin: round;
  pointer-events: none;
}

.kg-node-root .kg-label {
  fill: #263b53;
  font-size: 15px;
  font-weight: 650;
}

.kg-node-concept .kg-label {
  fill: #64748b;
  font-size: 9.5px;
  font-weight: 500;
  opacity: 0.92;
}

@media (max-width: 640px) {
  .kg-node-concept .kg-label {
    display: none;
  }

  .kg-node-concept.is-related .kg-label,
  .kg-node-concept.is-active .kg-label {
    display: block;
  }

  .kg-node-concept .kg-label {
    font-size: 9px;
  }
}

.kg-card {
  position: absolute;
  right: 24px;
  bottom: 24px;
  z-index: 4;
  width: 312px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 20px 50px rgba(30, 50, 70, 0.15), 0 2px 8px rgba(30, 50, 70, 0.06);
  backdrop-filter: blur(18px) saturate(1.15);
  -webkit-backdrop-filter: blur(18px) saturate(1.15);
}

.kg-card-close {
  position: absolute;
  top: 12px;
  right: 12px;
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: rgba(31, 58, 95, 0.06);
  color: #657588;
  cursor: pointer;
}

.kg-card-close:hover {
  background: rgba(31, 58, 95, 0.11);
  color: #1f3a5f;
}

.kg-card-kicker {
  color: #8b98a8;
  font-size: 10px;
  letter-spacing: 0.14em;
}

.kg-card h2 {
  margin: 6px 32px 0 0;
  color: #1f2937;
  font-size: 20px;
  font-weight: 680;
  letter-spacing: -0.4px;
}

.kg-card-meta {
  margin-top: 5px;
  color: #7b8797;
  font-size: 11px;
}

.kg-card-desc {
  min-height: 42px;
  margin: 13px 0 16px;
  color: #5f6f80;
  font-size: 12px;
  line-height: 1.75;
}

.kg-card-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  background: #1f3a5f;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.kg-card-action:hover {
  background: #172e4d;
}

.kg-state {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  pointer-events: none;
  color: #8a94a3;
  font-size: 13px;
}

.kg-card-enter-active,
.kg-card-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.kg-card-enter-from,
.kg-card-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@keyframes kg-flow {
  from { stroke-dashoffset: 0; }
  to { stroke-dashoffset: -54; }
}

@keyframes kg-breathe {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-1.8px); }
}

@keyframes kg-halo {
  0%, 100% { opacity: 0.1; transform: scale(1); }
  50% { opacity: 0.26; transform: scale(1.16); }
}

.kg-mode {
  position: absolute;
  top: 16px;
  right: 24px;
  z-index: 7;
  display: inline-flex;
  padding: 3px;
  border: 1px solid #e3e8ef;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 6px 20px rgba(31, 58, 95, 0.06);
  backdrop-filter: blur(8px);
}

.kg-mode button {
  height: 30px;
  padding: 0 14px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.kg-mode button.is-active {
  background: #1f3a5f;
  color: #fff;
}

.kg-chart {
  position: absolute;
  inset: 54px 24px 24px;
  z-index: 4;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 22px 16px;
  overflow: hidden;
  border: 1px solid #e3e8ef;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 18px 50px rgba(31, 58, 95, 0.08);
}

.kg-chart-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.kg-chart-kicker {
  color: #8b98a8;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.kg-chart-head h1 {
  margin: 5px 0 0;
  color: #1f2937;
  font-size: 22px;
  font-weight: 680;
  letter-spacing: -0.4px;
}

.kg-chart-head p {
  margin: 6px 0 0;
  color: #7b8797;
  font-size: 12px;
  line-height: 1.6;
}

.kg-chart-total {
  display: flex;
  align-items: baseline;
  gap: 5px;
  padding-top: 2px;
  white-space: nowrap;
}

.kg-chart-total strong {
  color: #1f3a5f;
  font-size: 30px;
  font-weight: 720;
  letter-spacing: -1px;
}

.kg-chart-total span {
  color: #8b98a8;
  font-size: 11px;
}

.kg-chart-chips {
  display: flex;
  gap: 7px;
  overflow-x: auto;
  padding-bottom: 3px;
  scrollbar-width: thin;
}

.kg-chart-chips button {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e3e8ef;
  border-radius: 7px;
  background: #fff;
  color: #64748b;
  font-size: 11.5px;
  cursor: pointer;
}

.kg-chart-chips button span {
  color: #9aa6b5;
  font-size: 10px;
}

.kg-chart-chips button.is-active {
  border-color: #c9d6e5;
  background: #eef3f8;
  color: #1f3a5f;
  font-weight: 650;
}

.kg-chart-main {
  display: grid;
  flex: 1;
  grid-template-columns: minmax(0, 1fr) 238px;
  gap: 18px;
  min-height: 0;
}

.kg-chart-canvas {
  width: 100%;
  height: 100%;
  min-height: 230px;
}

.kg-chart-side {
  align-self: stretch;
  min-height: 0;
  padding: 16px;
  border: 1px solid #edf1f6;
  border-radius: 10px;
  background: #f8fafc;
}

.kg-chart-side h2 {
  margin: 6px 0 8px;
  color: #1f2937;
  font-size: 19px;
  font-weight: 680;
  letter-spacing: -0.3px;
}

.kg-chart-side p {
  margin: 0;
  color: #64748b;
  font-size: 11.5px;
  line-height: 1.75;
}

.kg-chart-desc {
  margin-top: 10px !important;
  color: #8b98a8 !important;
}

.kg-chart-action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  margin-top: 14px;
  padding: 0 12px;
  border: 0;
  border-radius: 7px;
  background: #1f3a5f;
  color: #fff;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}

.kg-chart-action:hover {
  background: #172e4d;
}

@media (max-width: 760px) {
  .kg-mode {
    top: 12px;
    right: 12px;
  }

  .kg-chart {
    inset: 50px 12px 12px;
    padding: 16px;
    overflow-y: auto;
  }

  .kg-chart-head h1 {
    font-size: 19px;
  }

  .kg-chart-head p {
    max-width: 230px;
    font-size: 11px;
  }

  .kg-chart-total strong {
    font-size: 24px;
  }

  .kg-chart-main {
    flex: none;
    grid-template-columns: 1fr;
  }

  .kg-chart-canvas {
    height: 250px;
  }

  .kg-chart-side {
    padding: 13px;
  }
}

@media (max-width: 640px) {
  .kg-card {
    right: 12px;
    bottom: 12px;
    left: 12px;
    width: auto;
    padding: 17px;
  }

  .kg-card h2 {
    font-size: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .kg-breathe,
  .kg-flow,
  .kg-node.is-active .kg-halo {
    animation: none !important;
  }

  .kg-edge,
  .kg-flow,
  .kg-node,
  .kg-core,
  .kg-halo,
  .kg-card-enter-active,
  .kg-card-leave-active {
    transition: none !important;
  }
}
</style>
