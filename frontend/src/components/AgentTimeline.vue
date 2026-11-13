<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import MarkdownBody from '@/components/MarkdownBody.vue'
import { MagicStick } from '@element-plus/icons-vue'
import ThinkingDots from '@/components/ThinkingDots.vue'

const props = defineProps({
  answer: {
    type: String,
    default: ''
  },
  running: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const timelineRef = ref(null)

const REASONING_MARKER = /(?:^|\n)\s*(?:#{1,6}\s*)?(?:[一二三四五六七八九十]+[、.．]\s*)?【AI调试推理日志[^】]*】/

function splitAnswer(text) {
  if (!text) return { answer: '', reasoning: '' }
  const sourceMatch = text.match(/(?:\n\s*)?📎 参考来源：[^\n]*$/s)
  const sourceLine = sourceMatch ? sourceMatch[0].trim() : ''
  let body = sourceMatch ? text.slice(0, -sourceMatch[0].length) : text
  const marker = body.match(REASONING_MARKER)
  if (!marker) {
    return { answer: text, reasoning: '' }
  }
  const answer = body.slice(0, marker.index).trim()
  const reasoning = body.slice((marker.index || 0) + marker[0].length).trim()
  return {
    answer: [answer, sourceLine].filter(Boolean).join('\n\n'),
    reasoning
  }
}

const parsedAnswer = computed(() => splitAnswer(props.answer))

const statusText = computed(() => {
  if (props.error) return '调试失败'
  if (props.running) return '思考中'
  if (props.answer) return '已完成'
  return '等待调试'
})

const statusType = computed(() => {
  if (props.error) return 'danger'
  if (props.running) return 'primary'
  if (props.answer) return 'success'
  return 'info'
})

function scrollToBottom() {
  nextTick(() => {
    if (timelineRef.value) {
      timelineRef.value.scrollTop = timelineRef.value.scrollHeight
    }
  })
}

watch(() => [props.answer, props.error], scrollToBottom, { deep: true })
</script>

<template>
  <section class="panel agent-panel" :class="{ 'is-compact': compact }">
    <div v-if="!compact" class="agent-head">
      <div>
        <div class="panel-title">
          <el-icon><MagicStick /></el-icon>
          AI 调试
        </div>
        <div class="panel-subtitle">先写下你的疑问，AI 会一步步引导，不直接给答案</div>
      </div>
      <el-tag :type="statusType" effect="light" round size="small">{{ statusText }}</el-tag>
    </div>

    <div ref="timelineRef" class="timeline">
      <div v-if="!answer && !error" class="timeline-empty">
        <template v-if="running">
          <ThinkingDots />
          <span>AI 正在读题、定位错误点…</span>
        </template>
        <template v-else>
          写下你的疑问或思路，AI 会结合判题结果一步步引导。
        </template>
      </div>

      <div v-if="answer" class="answer-block">
        <div class="answer-label">助教回答</div>
        <pre v-if="running" class="answer-text">{{ parsedAnswer.answer }}<span class="caret">▍</span></pre>
        <MarkdownBody v-else :content="parsedAnswer.answer" class="answer-markdown" />
      </div>

      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        show-icon
        class="agent-error"
      />

    </div>
  </section>
</template>

<style scoped>
.agent-panel {
  overflow: hidden;
}

.agent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid #eef2f7;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 650;
}

.panel-title .el-icon {
  color: #3d5a78;
}

.panel-subtitle {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 11.5px;
}

.timeline {
  max-height: 430px;
  padding: 14px 18px 18px;
  overflow-y: auto;
}

.timeline-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.7;
}

.timeline-empty .thinking-dots {
  flex: 0 0 auto;
  color: #3d5a78;
}

.answer-block {
  margin-top: 12px;
  padding: 13px 14px;
  border: 1px solid #e8edf5;
  border-radius: 9px;
  background: #fbfcfe;
}

.answer-label {
  margin-bottom: 6px;
  color: #3d5a78;
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 0.04em;
}

.answer-text {
  margin: 0;
  color: #334155;
  font-family: inherit;
  font-size: 12.5px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.answer-markdown {
  color: #334155;
  font-size: 12.5px;
  line-height: 1.8;
}

.answer-markdown :deep(p) {
  margin: 6px 0;
}

.answer-markdown :deep(code) {
  font-size: 11.5px;
}

.caret {
  color: #3d5a78;
  animation: blink 0.9s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.agent-error {
  margin-top: 12px;
}

.agent-panel.is-compact {
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.agent-panel.is-compact .timeline {
  max-height: 380px;
  padding: 16px 18px 20px;
}

.agent-panel.is-compact .timeline-empty {
  padding: 26px 0;
}

</style>
