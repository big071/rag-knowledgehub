<template>
  <div class="rounded-lg border border-slate-200 p-3 bg-white">
    <canvas ref="canvas" class="w-full"></canvas>
    <div v-if="!url" class="text-sm text-slate-400">请选择 PDF 文档进行预览</div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import * as pdfjsLib from 'pdfjs-dist'
import workerSrc from 'pdfjs-dist/build/pdf.worker.min.js?url'

pdfjsLib.GlobalWorkerOptions.workerSrc = workerSrc

const props = defineProps({
  url: { type: String, default: '' }
})

const canvas = ref(null)

const renderPdf = async () => {
  if (!props.url || !canvas.value) return
  const loadingTask = pdfjsLib.getDocument(props.url)
  const pdf = await loadingTask.promise
  const page = await pdf.getPage(1)
  const viewport = page.getViewport({ scale: 1.1 })
  const context = canvas.value.getContext('2d')
  canvas.value.height = viewport.height
  canvas.value.width = viewport.width
  await page.render({ canvasContext: context, viewport }).promise
}

watch(() => props.url, () => {
  renderPdf().catch(() => {})
}, { immediate: true })
</script>
