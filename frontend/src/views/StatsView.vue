<template>
  <MainLayout>
    <div class="space-y-4">
      <h2 class="text-2xl font-semibold">问答数据统计</h2>
      <div class="grid lg:grid-cols-2 gap-4">
        <el-card shadow="never">
          <template #header>
            <div class="font-semibold">问答热度趋势（Top10）</div>
          </template>
          <div ref="hotRef" class="h-80"></div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="font-semibold">文档使用频次（Top10）</div>
          </template>
          <div ref="docRef" class="h-80"></div>
        </el-card>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import MainLayout from '../components/MainLayout.vue'
import { documentUsage, hotQuestions } from '../api'

const hotRef = ref(null)
const docRef = ref(null)

const renderHot = (data) => {
  const chart = echarts.init(hotRef.value)
  chart.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: data.map(i => i.question) },
    yAxis: { type: 'value' },
    series: [{
      data: data.map(i => i.count),
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(15,118,110,0.18)' },
      lineStyle: { color: '#0f766e', width: 3 }
    }]
  })
}

const renderDoc = (data) => {
  const chart = echarts.init(docRef.value)
  chart.setOption({
    tooltip: {},
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(i => i.documentName) },
    series: [{
      data: data.map(i => i.count),
      type: 'bar',
      itemStyle: {
        color: '#0369a1'
      }
    }]
  })
}

onMounted(async () => {
  try {
    const [hot, doc] = await Promise.all([hotQuestions(), documentUsage()])
    await nextTick()
    renderHot(hot)
    renderDoc(doc)
  } catch (e) {
    ElMessage.error(e.message || '统计数据加载失败')
  }
})
</script>
