<template>
  <MainLayout>
    <div class="space-y-5">
      <div class="flex flex-wrap gap-3 items-center justify-between">
        <h2 class="text-2xl font-semibold text-slate-800">知识库管理</h2>
        <div class="flex gap-2">
          <el-input v-model="kbKeyword" placeholder="搜索知识库" clearable class="w-56" @keyup.enter="loadKb" />
          <el-button type="primary" @click="loadKb">查询</el-button>
        </div>
      </div>

      <el-card shadow="never">
        <template #header>
          <div class="font-medium">新建知识库</div>
        </template>
        <div class="grid md:grid-cols-[1fr_2fr_auto] gap-3">
          <el-input v-model="kbForm.name" placeholder="知识库名称" />
          <el-input v-model="kbForm.description" placeholder="描述（可选）" />
          <el-button type="success" @click="createKnowledgeBase">创建</el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="font-medium">知识库列表</div>
        </template>
        <el-table :data="kbList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="docCount" label="文档数" width="90" />
          <el-table-column label="操作" width="260">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="selectKb(scope.row)">管理文档</el-button>
              <el-button size="small" type="warning" plain @click="goChat(scope.row)">进入问答</el-button>
              <el-button size="small" type="danger" plain @click="removeKb(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="selectedKb" shadow="never">
        <template #header>
          <div class="font-medium">文档管理 · {{ selectedKb.name }}</div>
        </template>

        <div class="flex flex-wrap gap-3 items-center mb-3">
          <el-upload :http-request="customUpload" :show-file-list="false" multiple>
            <el-button type="primary">上传 PDF/Markdown</el-button>
          </el-upload>
          <el-input v-model="docKeyword" placeholder="搜索文档" class="w-64" @keyup.enter="loadDocs" clearable />
          <el-button @click="loadDocs">刷新列表</el-button>
        </div>

        <el-table :data="documents" stripe>
          <el-table-column prop="fileName" label="文档名" />
          <el-table-column prop="fileType" label="类型" width="90" />
          <el-table-column prop="fileSize" label="大小(B)" width="120" />
          <el-table-column prop="parseStatus" label="状态" width="120" />
          <el-table-column label="操作" width="230">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="preview(scope.row)">预览</el-button>
              <el-button size="small" type="danger" plain @click="removeDoc(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="previewDoc" class="mt-4">
          <h3 class="mb-2 font-medium">预览：{{ previewDoc.fileName }}</h3>
          <PdfPreview v-if="previewDoc.fileType === 'pdf'" :url="previewUrl(previewDoc.previewUrl)" />
          <a v-else :href="previewUrl(previewDoc.previewUrl)" target="_blank" class="text-brand-600 underline">打开文本文件</a>
        </div>
      </el-card>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createKb, deleteDocument, deleteKb, listDocuments, listKb, uploadDocument } from '../api'
import MainLayout from '../components/MainLayout.vue'
import PdfPreview from '../components/PdfPreview.vue'
import { useQaStore } from '../stores/qa'
import { useRouter } from 'vue-router'

const router = useRouter()
const qaStore = useQaStore()

const kbForm = reactive({ name: '', description: '' })
const kbKeyword = ref('')
const kbList = ref([])

const selectedKb = ref(null)
const documents = ref([])
const docKeyword = ref('')
const previewDoc = ref(null)

const loadKb = async () => {
  const res = await listKb({ pageNum: 1, pageSize: 50, keyword: kbKeyword.value })
  kbList.value = res.records
  if (!selectedKb.value && kbList.value.length > 0) {
    selectedKb.value = kbList.value[0]
    qaStore.setKbId(selectedKb.value.id)
    await loadDocs()
  }
}

const createKnowledgeBase = async () => {
  if (!kbForm.name) return ElMessage.warning('请输入知识库名称')
  await createKb(kbForm)
  ElMessage.success('创建成功')
  kbForm.name = ''
  kbForm.description = ''
  await loadKb()
}

const removeKb = async (id) => {
  await ElMessageBox.confirm('确认删除该知识库？', '提示', { type: 'warning' })
  await deleteKb(id)
  ElMessage.success('已删除')
  if (selectedKb.value?.id === id) {
    selectedKb.value = null
    documents.value = []
    previewDoc.value = null
  }
  await loadKb()
}

const selectKb = async (row) => {
  selectedKb.value = row
  qaStore.setKbId(row.id)
  await loadDocs()
}

const goChat = (row) => {
  qaStore.setKbId(row.id)
  router.push('/chat')
}

const loadDocs = async () => {
  if (!selectedKb.value) return
  const res = await listDocuments({
    knowledgeBaseId: selectedKb.value.id,
    pageNum: 1,
    pageSize: 100,
    keyword: docKeyword.value
  })
  documents.value = res.records
}

const customUpload = async (option) => {
  if (!selectedKb.value) return ElMessage.warning('请先选择知识库')
  const fd = new FormData()
  fd.append('knowledgeBaseId', selectedKb.value.id)
  fd.append('file', option.file)
  try {
    await uploadDocument(fd)
    ElMessage.success(`${option.file.name} 上传成功`)
    await loadDocs()
    await loadKb()
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  }
}

const removeDoc = async (id) => {
  await ElMessageBox.confirm('确认删除该文档？', '提示', { type: 'warning' })
  await deleteDocument(id)
  ElMessage.success('已删除')
  await loadDocs()
  await loadKb()
}

const preview = (row) => {
  previewDoc.value = row
}

const previewUrl = (path) => {
  return path?.startsWith('http') ? path : path
}

onMounted(async () => {
  try {
    await loadKb()
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
})
</script>
