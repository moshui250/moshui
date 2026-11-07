<script setup>
const props = defineProps({
  scope: { type: String, default: 'PUBLIC' },
  knowledgePoints: { type: Array, default: () => [] },
  knowledgePointId: { type: Number, default: null },
  difficulty: { type: Number, default: null },
  hasFilters: { type: Boolean, default: false }
})

const emit = defineEmits(['update:scope', 'update:knowledgePointId', 'update:difficulty', 'reset'])

const difficulties = [
  { value: 1, label: '入门' },
  { value: 2, label: '进阶' },
  { value: 3, label: '挑战' }
]

function countOf(item) {
  const count = item.questionCount ?? item.questionNum
  return Number.isFinite(Number(count)) && Number(count) > 0 ? Number(count) : ''
}
</script>

<template>
  <nav class="filter-nav" aria-label="题库筛选">
    <div class="filter-nav-head">
      <span>筛选</span>
      <button v-if="hasFilters" class="filter-clear" type="button" @click="emit('reset')">
        清空
      </button>
    </div>

    <div class="filter-group">
      <p class="filter-label">练习范围</p>
      <button
        class="filter-item"
        :class="{ active: scope === 'PUBLIC' }"
        type="button"
        @click="emit('update:scope', 'PUBLIC')"
      >
        公共题库
      </button>
      <button
        class="filter-item"
        :class="{ active: scope === 'PERSONAL' }"
        type="button"
        @click="emit('update:scope', 'PERSONAL')"
      >
        我的题目
      </button>
      <router-link class="filter-item filter-link" to="/wrong-book">
        错题重练
        <span class="filter-arrow">→</span>
      </router-link>
    </div>

    <div class="filter-group">
      <p class="filter-label">
        知识点
        <span class="filter-meta">{{ knowledgePoints.length }}</span>
      </p>
      <button
        class="filter-item"
        :class="{ active: knowledgePointId === null }"
        type="button"
        @click="emit('update:knowledgePointId', null)"
      >
        全部知识点
      </button>
      <div class="filter-scroll">
        <button
          v-for="item in knowledgePoints"
          :key="item.id"
          class="filter-item"
          :class="{ active: knowledgePointId === item.id }"
          type="button"
          @click="emit('update:knowledgePointId', item.id)"
        >
          <span class="filter-name">{{ item.name }}</span>
          <span v-if="countOf(item)" class="filter-meta">{{ countOf(item) }}</span>
        </button>
      </div>
    </div>

    <div class="filter-group">
      <p class="filter-label">难度</p>
      <div class="difficulty-list">
        <button
          class="filter-chip"
          :class="{ active: difficulty === null }"
          type="button"
          @click="emit('update:difficulty', null)"
        >
          全部
        </button>
        <button
          v-for="item in difficulties"
          :key="item.value"
          class="filter-chip"
          :class="{ active: difficulty === item.value }"
          type="button"
          @click="emit('update:difficulty', item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <p class="filter-note">共享题计入累计 AC，不计入段位</p>
  </nav>
</template>

<style scoped>
.filter-nav {
  width: 100%;
}

.filter-nav-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  color: #111827;
  font-size: 14px;
  font-weight: 650;
}

.filter-clear {
  padding: 0;
  border: 0;
  background: transparent;
  color: #9ca3af;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.filter-clear:hover {
  color: #1f3a5f;
}

.filter-group {
  padding: 16px 0;
  border-top: 1px solid #e5e7eb;
}

.filter-group:first-of-type {
  padding-top: 0;
  border-top: 0;
}

.filter-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 9px;
  color: #9ca3af;
  font-size: 12px;
  font-weight: 500;
}

.filter-meta {
  color: #b0b7c3;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10.5px;
  font-weight: 400;
}

.filter-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 30px;
  padding: 5px 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-family: inherit;
  font-size: 13px;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.filter-item:hover {
  background: #eef1f4;
  color: #111827;
}

.filter-item.active {
  background: #e9edf1;
  color: #111827;
  font-weight: 600;
}

.filter-link {
  margin-top: 2px;
}

.filter-arrow {
  color: #b0b7c3;
  font-size: 12px;
}

.filter-scroll {
  max-height: 236px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.difficulty-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.filter-chip {
  height: 28px;
  padding: 0 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.filter-chip:hover {
  border-color: #d1d5db;
  color: #111827;
}

.filter-chip.active {
  border-color: #d1d5db;
  background: #e9edf1;
  color: #111827;
  font-weight: 600;
}

.filter-note {
  margin: 4px 0 0;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
  color: #9ca3af;
  font-size: 11px;
  line-height: 1.6;
}

.filter-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
