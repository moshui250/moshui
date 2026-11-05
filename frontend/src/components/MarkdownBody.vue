<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const html = computed(() => renderMarkdown(props.content))
</script>

<template>
  <div class="markdown-body" v-html="html"></div>
</template>

<style scoped>
.markdown-body {
  color: #4b5a70;
  font-size: 13px;
  line-height: 1.9;
  overflow-wrap: anywhere;
}

.markdown-body :deep(> :first-child) {
  margin-top: 0;
}

.markdown-body :deep(> :last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 20px 0 8px;
  color: #1e293b;
  font-weight: 650;
  line-height: 1.45;
}

.markdown-body :deep(h1) {
  font-size: 19px;
}

.markdown-body :deep(h2) {
  font-size: 16px;
}

.markdown-body :deep(h3) {
  font-size: 14px;
}

.markdown-body :deep(h4) {
  font-size: 13px;
}

.markdown-body :deep(p) {
  margin: 9px 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 9px 0;
  padding-left: 20px;
}

.markdown-body :deep(li + li) {
  margin-top: 4px;
}

.markdown-body :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 12px;
  border-left: 3px solid #c8d2dc;
  background: #f7f9fb;
  color: #64748b;
}

.markdown-body :deep(code) {
  padding: 1px 5px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #152a45;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
}

.markdown-body :deep(pre) {
  margin: 12px 0;
  padding: 12px 14px;
  overflow-x: auto;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #f8fafc;
  line-height: 1.65;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #334155;
  font-size: 12px;
}

.markdown-body :deep(table) {
  width: 100%;
  margin: 12px 0;
  border-collapse: collapse;
  font-size: 12px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 7px 9px;
  border: 1px solid #e8edf5;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f8fafc;
  color: #475569;
}

.markdown-body :deep(a) {
  color: #1f3a5f;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.markdown-body :deep(hr) {
  margin: 18px 0;
  border: 0;
  border-top: 1px solid #eef2f7;
}
</style>
