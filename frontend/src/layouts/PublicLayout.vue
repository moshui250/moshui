<template>
  <div class="public-shell">
    <SiteHeader />
    <main class="public-main">
      <router-view />
    </main>
    <SiteFooter />
  </div>
</template>

<script setup>
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f7;
  color: #0f172a;
}

.public-main {
  flex: 1;
}
</style>
