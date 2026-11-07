<template>
  <div v-if="total > 0" class="list-pager">
    <span class="pager-total">共 {{ total }} 条</span>
    <el-pagination
      background
      layout="prev, pager, next"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      @current-change="(value) => emit('change', value)"
    />
  </div>
</template>

<script setup>
defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})

const emit = defineEmits(['change'])
</script>

<style scoped>
.list-pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 44px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
}

.pager-total {
  color: #9ca3af;
  font-size: 12px;
  white-space: nowrap;
}

.list-pager :deep(.el-pagination) {
  margin-left: auto;
}

@media (max-width: 640px) {
  .list-pager {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .list-pager :deep(.el-pagination) {
    margin: 0 auto;
  }
}
</style>
