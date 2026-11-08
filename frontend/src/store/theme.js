import { computed, ref, watch } from 'vue'

const OPTIONS = [
  { key: 'paper', label: '纸白', color: '#f5f6f7' },
  { key: 'mist', label: '雾蓝', color: '#edf2f6' },
  { key: 'sand', label: '暖沙', color: '#f6f2ea' },
  { key: 'sage', label: '浅苔', color: '#eef4ef' },
]

const saved = typeof localStorage !== 'undefined' ? localStorage.getItem('codeagent-surface') : ''
const themeKey = ref(OPTIONS.some((item) => item.key === saved) ? saved : 'paper')
const activeTheme = computed(() => OPTIONS.find((item) => item.key === themeKey.value) || OPTIONS[0])

function applyTheme(option) {
  if (typeof document === 'undefined') return
  document.documentElement.style.setProperty('--page-bg', option.color)
  document.documentElement.dataset.surface = option.key
  try {
    localStorage.setItem('codeagent-surface', option.key)
  } catch {
    // Storage can be unavailable in private mode; the in-memory theme still works.
  }
}

watch(activeTheme, applyTheme, { immediate: true })

export function useSurfaceTheme() {
  return {
    options: OPTIONS,
    themeKey,
    activeTheme,
    setTheme(key) {
      if (OPTIONS.some((item) => item.key === key)) themeKey.value = key
    },
  }
}
