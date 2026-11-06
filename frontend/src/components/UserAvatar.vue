<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  avatar: { type: String, default: 'navy' },
  size: { type: String, default: 'md' }
})

const keys = ['navy', 'slate', 'sage', 'sand', 'clay', 'ink']
const initial = computed(() => (props.name || 'U').trim().slice(0, 1).toUpperCase())
const avatarKey = computed(() => (keys.includes(props.avatar) ? props.avatar : 'navy'))
</script>

<template>
  <span class="user-avatar" :class="[`avatar-${avatarKey}`, `size-${size}`]" aria-hidden="true">
    {{ initial }}
  </span>
</template>

<style scoped>
.user-avatar {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  color: #fff;
  font-weight: 700;
  line-height: 1;
  user-select: none;
}

.size-sm {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  font-size: 11px;
}

.size-md {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  font-size: 15px;
}

.size-lg {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  font-size: 23px;
}

.avatar-navy { background: #1f3a5f; }
.avatar-slate { background: #475569; }
.avatar-sage { background: #4f6b5d; }
.avatar-sand { background: #78664c; }
.avatar-clay { background: #7a5148; }
.avatar-ink { background: #273244; }
</style>
