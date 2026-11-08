<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import GlobalCommandPalette from '@/components/GlobalCommandPalette.vue'
import AuthDialog from '@/components/AuthDialog.vue'

const router = useRouter()
const routeLoading = ref(false)
const routeDone = ref(false)
let resetTimer = 0

function startProgress() {
  window.clearTimeout(resetTimer)
  routeDone.value = false
  routeLoading.value = true
}

function finishProgress() {
  routeLoading.value = false
  routeDone.value = true
  window.clearTimeout(resetTimer)
  resetTimer = window.setTimeout(() => {
    routeDone.value = false
  }, 420)
}

router.beforeEach(() => {
  startProgress()
  return true
})
router.afterEach(finishProgress)
router.onError(finishProgress)

onBeforeUnmount(() => {
  window.clearTimeout(resetTimer)
})
</script>

<template>
  <div
    class="route-progress"
    :class="{ 'is-active': routeLoading, 'is-done': routeDone }"
    aria-hidden="true"
  ></div>
  <router-view />
  <GlobalCommandPalette />
  <AuthDialog />
</template>

<style>
.route-progress {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 9999;
  height: 2px;
  background: #1f3a5f;
  opacity: 0;
  pointer-events: none;
  transform: scaleX(0);
  transform-origin: left center;
  transition: transform 0.2s ease, opacity 0.25s ease;
}

.route-progress.is-active {
  opacity: 1;
  transform: scaleX(0.72);
  transition: transform 1.2s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.2s ease;
}

.route-progress.is-done {
  opacity: 0;
  transform: scaleX(1);
  transition: transform 0.2s ease, opacity 0.25s ease 0.08s;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.compact-head {
  justify-content: space-between !important;
  gap: 16px !important;
  padding-bottom: 18px !important;
}

.compact-page-title {
  margin: 0 !important;
  color: #111827 !important;
  font-size: 20px !important;
  font-weight: 650 !important;
  letter-spacing: -0.015em !important;
  line-height: 1.25 !important;
}

.edit-title {
  margin: 10px 0 18px;
}

@media (min-width: 761px) {
  .compact-head {
    align-items: center !important;
  }
}

@media (max-width: 760px) {
  .compact-page-title {
    font-size: 19px !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  .route-progress,
  .route-progress.is-active,
  .route-progress.is-done {
    transition: none;
  }
  .route-progress.is-active {
    transform: scaleX(1);
  }
}
</style>
