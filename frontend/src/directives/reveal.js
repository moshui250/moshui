const REVEAL_CLASS = 'is-reveal-ready'
const REVEALED_CLASS = 'is-revealed'
const REDUCED_MOTION = '(prefers-reduced-motion: reduce)'

/**
 * 元素首次进入视口时播放一次轻微上浮，不处理重复进入。
 */
export default {
  mounted(el, binding) {
    const delay = Number(binding.value?.delay || 0)
    el.style.setProperty('--reveal-delay', `${delay}ms`)

    const reduced = window.matchMedia?.(REDUCED_MOTION).matches
    if (reduced || typeof window.IntersectionObserver !== 'function') {
      el.classList.add(REVEALED_CLASS)
      return
    }

    el.classList.add(REVEAL_CLASS)
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          el.classList.add(REVEALED_CLASS)
          observer.disconnect()
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -6% 0px' }
    )
    observer.observe(el)
    el.__revealObserver = observer
  },

  unmounted(el) {
    el.__revealObserver?.disconnect()
    delete el.__revealObserver
  }
}
