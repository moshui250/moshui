<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  options: { type: Array, default: () => [] },
  max: { type: Number, default: 8 },
  placeholder: { type: String, default: '搜索知识点' },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])
const open = ref(false)
const query = ref('')
const activeIndex = ref(0)
const searchInput = ref(null)

const selectedIds = computed(() => (props.modelValue || []).map((id) => Number(id)))
const selectedPoints = computed(() =>
  selectedIds.value.map((id) => props.options.find((item) => Number(item.id) === id) || { id, name: `#${id}` })
)
const filteredOptions = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return props.options
  return props.options.filter((item) => {
    const text = `${item.name || ''} ${item.code || ''} ${item.description || ''}`.toLowerCase()
    return text.includes(keyword)
  })
})
const atLimit = computed(() => selectedIds.value.length >= props.max)

watch(open, async (value) => {
  if (!value) return
  await nextTick()
  searchInput.value?.focus()
  activeIndex.value = 0
})

watch(query, () => {
  activeIndex.value = 0
})

function isSelected(id) {
  return selectedIds.value.includes(Number(id))
}

function togglePoint(point) {
  if (props.disabled) return
  const id = Number(point.id)
  const next = [...selectedIds.value]
  const index = next.indexOf(id)
  if (index >= 0) {
    next.splice(index, 1)
  } else if (next.length >= props.max) {
    ElMessage.warning(`最多选择 ${props.max} 个知识点`)
    return
  } else {
    next.push(id)
  }
  emit('update:modelValue', next)
}

function removePoint(point) {
  togglePoint(point)
}

function moveActive(delta) {
  if (!filteredOptions.value.length) return
  const next = activeIndex.value + delta
  activeIndex.value = Math.min(Math.max(next, 0), filteredOptions.value.length - 1)
}

function handleKeydown(event) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveActive(1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveActive(-1)
  } else if (event.key === 'Enter') {
    const point = filteredOptions.value[activeIndex.value]
    if (point) {
      event.preventDefault()
      togglePoint(point)
    }
  } else if (event.key === 'Escape') {
    open.value = false
  } else if (event.key === 'Backspace' && !query.value && selectedPoints.value.length) {
    event.preventDefault()
    removePoint(selectedPoints.value[selectedPoints.value.length - 1])
  }
}
</script>

<template>
  <el-popover
    v-model:visible="open"
    trigger="click"
    placement="bottom-start"
    :width="330"
    :show-arrow="false"
    popper-class="kp-popper"
    :disabled="disabled"
  >
    <template #reference>
      <div
        class="kp-control"
        :class="{ 'is-open': open, 'is-disabled': disabled }"
        tabindex="0"
        role="combobox"
        :aria-expanded="open"
        aria-haspopup="listbox"
        @keydown="handleKeydown"
      >
        <span v-for="point in selectedPoints" :key="point.id" class="kp-chip">
          {{ point.name }}
          <button
            v-if="!disabled"
            type="button"
            class="kp-chip-remove"
            :aria-label="`移除 ${point.name}`"
            @click.stop="removePoint(point)"
          >×</button>
        </span>
        <span v-if="!selectedPoints.length" class="kp-placeholder">{{ placeholder }}</span>
        <span class="kp-caret">⌄</span>
      </div>
    </template>

    <div class="kp-panel">
      <label class="kp-search">
        <el-icon><Search /></el-icon>
        <input
          ref="searchInput"
          v-model="query"
          type="text"
          :placeholder="placeholder"
          @keydown="handleKeydown"
        />
        <span class="kp-count">{{ selectedPoints.length }}/{{ max }}</span>
      </label>

      <div class="kp-list" role="listbox" aria-multiselectable="true">
        <button
          v-for="(point, index) in filteredOptions"
          :key="point.id"
          type="button"
          class="kp-option"
          :class="{ 'is-active': index === activeIndex, 'is-selected': isSelected(point.id) }"
          role="option"
          :aria-selected="isSelected(point.id)"
          @mouseenter="activeIndex = index"
          @click="togglePoint(point)"
        >
          <span class="kp-option-copy">
            <span class="kp-option-name">{{ point.name }}</span>
            <span class="kp-option-meta">{{ point.code }}<template v-if="point.questionCount != null"> · {{ point.questionCount }} 题</template></span>
          </span>
          <el-icon v-if="isSelected(point.id)" class="kp-option-check"><Check /></el-icon>
        </button>
        <p v-if="!filteredOptions.length" class="kp-empty">没有匹配的知识点</p>
      </div>

      <div class="kp-foot">
        <span>支持多选</span>
        <span>最多 {{ max }} 个</span>
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.kp-control {
  position: relative;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  width: 100%;
  min-height: 40px;
  padding: 5px 32px 5px 9px;
  border: 1px solid #dfe6ed;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.kp-control:hover {
  border-color: #b9c6d3;
}
.kp-control.is-open {
  border-color: #1f3a5f;
  box-shadow: 0 0 0 3px rgba(31, 58, 95, 0.09);
}
.kp-control.is-disabled {
  background: #f7f9fb;
  cursor: not-allowed;
  opacity: 0.7;
}
.kp-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  max-width: 100%;
  padding: 3px 5px 3px 7px;
  border-radius: 6px;
  background: #eef2f6;
  color: #1f3a5f;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
}
.kp-chip-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #6b7c8f;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
}
.kp-chip-remove:hover {
  background: #dce5ee;
  color: #1f3a5f;
}
.kp-placeholder {
  color: #98a3b2;
  font-size: 13px;
  line-height: 24px;
}
.kp-caret {
  position: absolute;
  right: 11px;
  top: 50%;
  transform: translateY(-50%);
  color: #8a94a6;
  font-size: 14px;
  pointer-events: none;
}
.kp-panel {
  margin: -12px;
}
.kp-search {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 10px;
  border-bottom: 1px solid #edf1f5;
  color: #8a94a6;
}
.kp-search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #172033;
  font: inherit;
  font-size: 13px;
}
.kp-search input::placeholder {
  color: #a3adba;
}
.kp-count {
  color: #98a3b2;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}
.kp-list {
  max-height: 252px;
  overflow-y: auto;
  padding: 5px;
}
.kp-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 10px;
  padding: 8px 9px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #344054;
  text-align: left;
  cursor: pointer;
}
.kp-option.is-active {
  background: #f4f6f8;
}
.kp-option.is-selected {
  color: #1f3a5f;
}
.kp-option-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.kp-option-name {
  font-size: 13px;
  font-weight: 600;
}
.kp-option-meta {
  color: #8a94a6;
  font-size: 11px;
}
.kp-option-check {
  color: #1f3a5f;
  font-size: 15px;
}
.kp-empty {
  margin: 0;
  padding: 18px 10px;
  color: #8a94a6;
  font-size: 12px;
  text-align: center;
}
.kp-foot {
  display: flex;
  justify-content: space-between;
  padding: 7px 10px;
  border-top: 1px solid #edf1f5;
  color: #98a3b2;
  font-size: 11px;
}
</style>
