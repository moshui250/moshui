<script setup>
import { computed } from 'vue'

const props = defineProps({
  tier: {
    type: Object,
    default: null
  },
  size: {
    type: String,
    default: 'md'
  }
})

const badgeStyle = computed(() => {
  if (!props.tier?.color) return {}
  const color = props.tier.color
  return {
    color,
    borderColor: `${color}55`,
    background: `${color}12`
  }
})
</script>

<template>
  <span v-if="tier" class="tier-badge" :class="`is-${size}`" :style="badgeStyle">
    <i :style="{ background: tier.color }"></i>
    {{ tier.name }}
  </span>
</template>

<style scoped>
.tier-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 9px;
  border: 1px solid;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 650;
  line-height: 1.4;
  white-space: nowrap;
}

.tier-badge i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.tier-badge.is-sm {
  padding: 2px 7px;
  font-size: 10.5px;
}

.tier-badge.is-lg {
  padding: 5px 12px;
  font-size: 13px;
}
</style>
