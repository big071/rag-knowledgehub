<template>
  <MainLayout>
    <div class="grid xl:grid-cols-[1.2fr_0.8fr] gap-4">
      <div class="space-y-4">
        <div class="flex flex-wrap gap-3 items-center">
          <el-select v-model="kbId" placeholder="选择知识库" class="w-72" @change="changeKb">
            <el-option v-for="item in kbOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="fileType" placeholder="文档类型过滤" clearable class="w-44">
            <el-option label="PDF" value="pdf" />
            <el-option label="Word" value="docx" />
            <el-option label="Excel" value="xlsx" />
            <el-option label="PPT" value="pptx" />
            <el-option label="图片" value="png" />
          </el-select>
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
          <el-tag type="success">支持5轮上下文</el-tag>
        </div>

        <el-card shadow="never" class="fade-up">
          <template #header>
            <div class="font-semibold">智能问答</div>
          </template>
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            placeholder="输入你的问题"
          />
          <div class="mt-3 flex gap-2">
            <el-button type="primary" :loading="loading" @click="submit">开始问答</el-button>
            <el-button @click="question=''">清空</el-button>
          </div>
        </el-card>

        <el-card v-if="answer" shadow="never" class="fade-up">
          <template #header>
            <div class="font-semibold">回答结果</div>
          </template>
          <el-alert v-if="summary" :title="`摘要：${summary}`" type="success" show-icon class="mb-3" />
          <div class="prose prose-slate max-w-none" v-html="answerHtml"></div>
          <el-tag class="mt-3" :type="cached ? 'warning' : 'primary'">{{ cached ? '命中缓存' : '实时生成' }}</el-tag>
        </el-card>

        <SourceList :sources="sources" />
      </div>

      <div class="space-y-4">
        <el-card shadow="never">
          <template #header>
            <div class="font-semibold">历史对话（最近5轮）</div>
          </template>
          <div class="space-y-3 max-h-[70vh] overflow-y-auto">
            <div v-for="(item, idx) in latestFiveHistory" :key="idx" class="rounded-lg border border-slate-200 p-3 bg-white">
              <div class="text-xs text-slate-400">{{ item.time }}</div>
              <div class="font-medium mt-1">Q: {{ item.question }}</div>
              <div class="text-sm text-slate-600 mt-1 line-clamp-3">A: {{ item.answer }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import { askQuestion, listKb } from '../api'
import MainLayout from '../components/MainLayout.vue'
import SourceList from '../components/SourceList.vue'
import { useQaStore } from '../stores/qa'
import { useAuthStore } from '../stores/auth'
import { connectQaSocket } from '../utils/ws'

const md = new MarkdownIt({ html: false, breaks: true, linkify: true })
const qaStore = useQaStore()
const authStore = useAuthStore()

const kbOptions = ref([])
const kbId = ref(qaStore.selectedKbId || 0)
const question = ref('')
const loading = ref(false)
const answer = ref('')
const summary = ref('')
const sources = ref([])
const cached = ref(false)
const fileType = ref('')
const dateRange = ref([])
const conversationId = ref('')
let ws = null

const answerHtml = computed(() => md.render(answer.value || ''))
const latestFiveHistory = computed(() => qaStore.history.slice(-5).reverse())

const loadKbs = async () => {
  const data = await listKb({ pageNum: 1, pageSize: 100 })
  kbOptions.value = data.records
  if (!kbId.value && kbOptions.value.length > 0) {
    kbId.value = kbOptions.value[0].id
    qaStore.setKbId(kbId.value)
  }
}

const changeKb = (id) => {
  qaStore.setKbId(id)
  conversationId.value = ''
}

const submit = async () => {
  if (!kbId.value) return ElMessage.warning('请先选择知识库')
  if (!question.value.trim()) return ElMessage.warning('请输入问题')

  loading.value = true
  try {
    const [start, end] = dateRange.value || []
    const resp = await askQuestion({
      knowledgeBaseId: kbId.value,
      question: question.value,
      conversationId: conversationId.value,
      fileType: fileType.value || undefined,
      startTime: start ? `${start.toISOString().slice(0, 10)} 00:00:00` : undefined,
      endTime: end ? `${end.toISOString().slice(0, 10)} 23:59:59` : undefined
    })
    answer.value = resp.answer
    summary.value = resp.summary || ''
    sources.value = resp.sources || []
    cached.value = !!resp.cached
    conversationId.value = resp.conversationId
    qaStore.addHistory({
      question: question.value,
      answer: resp.summary || resp.answer,
      time: new Date().toLocaleString()
    })
    question.value = ''
  } catch (e) {
    ElMessage.error(e.message || '问答失败')
  } finally {
    loading.value = false
  }
}

const initWs = () => {
  if (!authStore.token) return
  ws = connectQaSocket(authStore.token, (payload) => {
    if (payload.type === 'qa_result' && payload.data?.answer) {
      answer.value = payload.data.answer
      summary.value = payload.data.summary || ''
      sources.value = payload.data.sources || []
      cached.value = !!payload.data.cached
    }
  })
}

onMounted(async () => {
  try {
    await loadKbs()
    initWs()
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>
