import { computed, ref } from 'vue'

const STORAGE_KEY = 'dsh-theme'
const root = typeof document !== 'undefined' ? document.documentElement : null
const isDark = ref(Boolean(root?.classList.contains('dark')))

const theme = computed(() => (isDark.value ? 'dark' : 'light'))

function systemTheme() {
  if (typeof window === 'undefined' || !window.matchMedia) return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function apply(value, persist = true) {
  const next = value === 'dark' ? 'dark' : 'light'
  if (root) {
    root.classList.toggle('dark', next === 'dark')
    root.style.colorScheme = next
  }
  isDark.value = next === 'dark'
  if (persist) {
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      // Storage can be unavailable (private mode); in-memory state still works.
    }
  }
  if (typeof document !== 'undefined') {
    const meta = document.querySelector('meta[name="theme-color"]')
    if (meta) meta.setAttribute('content', next === 'dark' ? '#0d1117' : '#f5f6f7')
  }
}

function initTheme() {
  const hasClass = root?.classList.contains('dark')
  let stored = ''
  try {
    stored = localStorage.getItem(STORAGE_KEY) || ''
  } catch {
    stored = ''
  }
  if (stored === 'dark' || stored === 'light') {
    apply(stored, false)
    return
  }
  if (hasClass) {
    apply('dark', false)
    return
  }
  apply(systemTheme(), false)
}

if (root) initTheme()

export function useTheme() {
  return {
    theme,
    isDark,
    setTheme(value) {
      apply(value)
    },
    toggleTheme() {
      apply(isDark.value ? 'light' : 'dark')
    }
  }
}
