import { computed, ref, watch } from 'vue'

const OPTIONS = [
  { key: 'light', label: '浅色', color: '#f5f6f7' },
  { key: 'dark', label: '深色', color: '#0d1117' },
]

const LEGACY_LIGHT = new Set(['paper', 'mist', 'sand', 'sage'])
const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('codeagent-surface') : ''
const initial = stored === 'dark' ? 'dark' : LEGACY_LIGHT.has(stored) || stored === 'light' ? 'light' : 'light'
const themeKey = ref(initial)
const activeTheme = computed(() => OPTIONS.find((item) => item.key === themeKey.value) || OPTIONS[0])

function applyTheme(option) {
  if (typeof document === 'undefined') return
  document.documentElement.style.setProperty('--page-bg', option.color)
  document.documentElement.dataset.theme = option.key
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
    toggleTheme() {
      themeKey.value = themeKey.value === 'dark' ? 'light' : 'dark'
    },
  }
}
