<script setup>
defineProps({
  label: {
    type: String,
    default: '思考中'
  }
})
</script>

<template>
  <span class="thinking-dots" role="img" :aria-label="label">
    <i></i>
    <i></i>
    <i></i>
  </span>
</template>

<style scoped>
.thinking-dots {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 18px;
  height: 16px;
  color: currentColor;
}

.thinking-dots i {
  display: block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.45;
  transform: translateY(1px) scale(0.85);
  animation: thinking-wave 0.9s ease-in-out infinite;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.14s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.28s;
}

@keyframes thinking-wave {
  0%,
  100% {
    opacity: 0.35;
    transform: translateY(1px) scale(0.8);
  }
  50% {
    opacity: 1;
    transform: translateY(-2px) scale(1.15);
  }
}

@media (prefers-reduced-motion: reduce) {
  .thinking-dots i {
    animation: none;
    opacity: 0.65;
    transform: translateY(0) scale(1);
  }
}
</style>
