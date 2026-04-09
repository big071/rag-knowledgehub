<template>
  <el-card shadow="never">
    <template #header>
      <div class="font-semibold">回答来源溯源</div>
    </template>
    <div v-if="!sources?.length" class="text-sm text-slate-400">暂无来源片段</div>
    <div v-for="(item, idx) in sources" :key="idx" class="mb-3 rounded-lg bg-slate-50 p-3 border border-slate-100">
      <div class="text-xs text-slate-500 mb-1">
        {{ item.documentName }} · chunk #{{ item.chunkIndex }} · score: {{ (item.score || 0).toFixed(3) }}
      </div>
      <div class="text-sm text-slate-700 leading-6">{{ item.snippet }}</div>
      <a v-if="item.sourceUrl" :href="item.sourceUrl" target="_blank" class="text-xs text-brand-600 underline mt-1 inline-block">跳转原文</a>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  sources: {
    type: Array,
    default: () => []
  }
})
</script>
