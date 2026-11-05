<script setup>
import TierBadge from '@/components/TierBadge.vue'

defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  tiers: {
    type: Array,
    default: () => []
  },
  currentCode: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="段位规则"
    width="480px"
    class="tier-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p class="tier-intro">
      段位按全站唯一 AC 题数计算。同一道题重复 AC 只计一次；WA 后重新 AC 不会重复增加题数。
    </p>
    <div class="tier-list">
      <div
        v-for="tier in tiers"
        :key="tier.code"
        class="tier-row"
        :class="{ 'is-current': currentCode === tier.code }"
      >
        <TierBadge :tier="tier" size="sm" />
        <span class="tier-range">{{ tier.rangeText }}</span>
        <span v-if="currentCode === tier.code" class="current-label">当前</span>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.tier-intro {
  margin: 0 0 14px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.7;
}

.tier-list {
  border-top: 1px solid #e4e7ec;
}

.tier-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  border-bottom: 1px solid #edf0f3;
}

.tier-row.is-current {
  margin: 0 -10px;
  padding: 0 10px;
  background: #f5f6f7;
}

.tier-range {
  margin-left: auto;
  color: #6b7280;
  font-size: 12px;
}

.current-label {
  width: 32px;
  color: #1f3a5f;
  font-size: 11px;
  text-align: right;
}
</style>

<style>
.tier-dialog {
  border-radius: 12px;
}

.tier-dialog .el-dialog__header {
  margin-right: 0;
  padding-bottom: 10px;
}
</style>
