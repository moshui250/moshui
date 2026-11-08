<template>
  <div class="public-shell">
    <SiteHeader />
    <main class="public-main">
      <RouterView v-slot="{ Component, route }">
        <Transition :name="transitionName(route)" mode="out-in">
          <component :is="Component" :key="viewKey(route)" />
        </Transition>
      </RouterView>
    </main>
    <SiteFooter v-if="$route.name !== 'knowledge'" />
  </div>
</template>

<script setup>
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'

function viewKey(route) {
  if (route.name === 'question-list') return 'question-list'
  return [route.name, route.params.id ?? '', route.params.questionId ?? ''].join(':')
}

function transitionName(route) {
  return route.name === 'question-solve' || route.name === 'contest-solve' ? 'none' : 'page'
}
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--page-bg, #f5f6f7);
  color: #0f172a;
}

.public-main {
  flex: 1;
}

:global(.page-enter-active),
:global(.page-leave-active) {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

:global(.page-enter-from) {
  opacity: 0;
  transform: translateY(6px);
}

:global(.page-leave-to) {
  opacity: 0;
  transform: translateY(-4px);
}

@media (prefers-reduced-motion: reduce) {
  :global(.page-enter-active),
  :global(.page-leave-active) {
    transition: none;
  }
  :global(.page-enter-from),
  :global(.page-leave-to) {
    opacity: 1;
    transform: none;
  }
}
</style>
