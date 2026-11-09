<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as monaco from 'monaco-editor'
import { useTheme } from '@/composables/useTheme'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'cpp'
  },
  height: {
    type: String,
    default: '470px'
  },
  readOnly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])
const container = ref(null)
const { isDark } = useTheme()

let editor = null
let syncing = false

onMounted(() => {
  editor = monaco.editor.create(container.value, {
    value: props.modelValue,
    language: props.language,
    theme: isDark.value ? 'vs-dark' : 'vs',
    readOnly: props.readOnly,
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 14,
    lineHeight: 22,
    fontFamily: "'JetBrains Mono', Consolas, 'Courier New', monospace",
    tabSize: 4,
    insertSpaces: true,
    scrollBeyondLastLine: false,
    padding: { top: 14, bottom: 14 },
    renderLineHighlight: 'all',
    roundedSelection: false,
    overviewRulerBorder: false,
    fixedOverflowWidgets: true
  })

  editor.onDidChangeModelContent(() => {
    if (syncing) return
    const value = editor.getValue()
    if (value !== props.modelValue) emit('update:modelValue', value)
  })
})

watch(
  () => props.modelValue,
  (value) => {
    if (!editor || value === editor.getValue()) return
    syncing = true
    editor.setValue(value ?? '')
    syncing = false
  }
)

watch(
  () => props.language,
  (language) => {
    const model = editor?.getModel()
    if (model) monaco.editor.setModelLanguage(model, language)
  }
)

watch(
  () => props.readOnly,
  (readOnly) => editor?.updateOptions({ readOnly })
)

watch(
  isDark,
  (dark) => monaco.editor.setTheme(dark ? 'vs-dark' : 'vs')
)

onBeforeUnmount(() => {
  editor?.dispose()
  editor = null
})

defineExpose({
  focus() {
    editor?.focus()
  }
})
</script>

<template>
  <div ref="container" class="code-editor" :style="{ height }"></div>
</template>

<style scoped>
.code-editor {
  width: 100%;
  overflow: hidden;
  background: #fff;
}
</style>
