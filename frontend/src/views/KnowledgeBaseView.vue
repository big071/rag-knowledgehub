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

      <el-card v-if="isManager" shadow="never">
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
          <el-table-column label="操作" width="300">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="selectKb(scope.row)">查看文档</el-button>
              <el-button size="small" type="warning" plain @click="goChat(scope.row)">进入问答</el-button>
              <el-button v-if="isManager" size="small" type="danger" plain @click="removeKb(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="selectedKb" shadow="never">
        <template #header>
          <div class="font-medium">文档管理 - {{ selectedKb.name }}</div>
        </template>

        <div class="flex flex-wrap gap-3 items-center mb-3">
          <el-upload v-if="isManager" :http-request="customUpload" :show-file-list="false" multiple>
            <el-button type="primary">批量上传文档</el-button>
          </el-upload>
          <el-input v-model="docKeyword" placeholder="搜索文档" class="w-64" @keyup.enter="loadDocs" clearable />
          <el-select v-model="docType" placeholder="类型" clearable class="w-32" @change="loadDocs">
            <el-option label="PDF" value="pdf" />
            <el-option label="Word" value="docx" />
            <el-option label="Excel" value="xlsx" />
            <el-option label="PPT" value="pptx" />
            <el-option label="图片" value="png" />
          </el-select>
          <el-button @click="loadDocs">刷新列表</el-button>
        </div>

        <el-table :data="documents" stripe @selection-change="onSelectionChange">
          <el-table-column v-if="isManager" type="selection" width="55" />
          <el-table-column prop="fileName" label="文档名" min-width="220" />
          <el-table-column prop="fileType" label="类型" width="90" />
          <el-table-column prop="reviewStatus" label="审核" width="100" />
          <el-table-column prop="versionNo" label="版本" width="70" />
          <el-table-column label="敏感" width="90">
            <template #default="scope">
              <el-tag v-if="scope.row.sensitiveHit" type="danger">命中</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="tags" label="标签" width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="420">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="preview(scope.row)">预览</el-button>
              <el-button v-if="isManager" size="small" plain @click="openTagEditor(scope.row)">标签</el-button>
              <el-button v-if="isManager" size="small" type="success" plain @click="approve(scope.row, 'APPROVED')">通过</el-button>
              <el-button v-if="isManager" size="small" type="warning" plain @click="approve(scope.row, 'REJECTED')">驳回</el-button>
              <el-upload v-if="isManager" :http-request="(option) => uploadVersion(scope.row, option)" :show-file-list="false">
                <el-button size="small" plain>新版本</el-button>
              </el-upload>
              <el-button v-if="isManager" size="small" type="danger" plain @click="removeDoc(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-3" v-if="isManager">
          <el-button type="danger" plain @click="batchDeleteSelected" :disabled="!selectedDocIds.length">批量删除</el-button>
        </div>

        <el-alert v-if="uploading" class="mt-3" :title="`上传中 ${uploadProgress}%`" type="info" :closable="false" />

        <div v-if="previewDoc" class="mt-4">
          <h3 class="mb-2 font-medium">预览：{{ previewDoc.fileName }}</h3>
          <a :href="previewUrl(previewDoc.previewUrl)" target="_blank" class="text-brand-600 underline">打开原文</a>
          <p v-if="previewDoc.sensitiveTip" class="mt-2 text-red-500">{{ previewDoc.sensitiveTip }}</p>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="tagDialog" title="编辑标签" width="480px">
      <el-input v-model="tagValue" placeholder="多个标签用英文逗号分隔" />
      <template #footer>
        <el-button @click="tagDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTags">保存</el-button>
      </template>
    </el-dialog>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  batchDeleteDocuments,
  batchUploadDocuments,
  createKb,
  deleteDocument,
  deleteKb,
  listDocuments,
  listKb,
  reviewDocument,
  setDocumentTags,
  uploadDocumentVersion
} from '../api'
import MainLayout from '../components/MainLayout.vue'
import { useQaStore } from '../stores/qa'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const qaStore = useQaStore()
const authStore = useAuthStore()

const kbForm = reactive({ name: '', description: '' })
const kbKeyword = ref('')
const kbList = ref([])
const isManager = computed(() => ['DOC_ADMIN', 'SUPER_ADMIN'].includes(authStore.user?.role))

const selectedKb = ref(null)
const documents = ref([])
const selectedDocIds = ref([])
const docKeyword = ref('')
const docType = ref('')
const previewDoc = ref(null)

const uploading = ref(false)
const uploadProgress = ref(0)

const tagDialog = ref(false)
const tagValue = ref('')
const tagTarget = ref(null)

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
    keyword: docKeyword.value,
    fileType: docType.value
  })
  documents.value = res.records
}

const customUpload = async (option) => {
  if (!selectedKb.value) return ElMessage.warning('请先选择知识库')
  const fd = new FormData()
  fd.append('knowledgeBaseId', selectedKb.value.id)
  for (const item of option.fileList) {
    fd.append('files', item)
  }
  uploading.value = true
  uploadProgress.value = 0
  try {
    await batchUploadDocuments(fd, (e) => {
      uploadProgress.value = e.total ? Math.round((e.loaded / e.total) * 100) : 0
    })
    ElMessage.success('批量上传成功')
    await loadDocs()
    await loadKb()
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const uploadVersion = async (row, option) => {
  const fd = new FormData()
  fd.append('file', option.file)
  uploading.value = true
  try {
    await uploadDocumentVersion(row.id, fd, (e) => {
      uploadProgress.value = e.total ? Math.round((e.loaded / e.total) * 100) : 0
    })
    ElMessage.success('已上传新版本')
    await loadDocs()
  } finally {
    uploading.value = false
  }
}

const approve = async (row, status) => {
  await reviewDocument(row.id, { reviewStatus: status })
  ElMessage.success('审核完成')
  await loadDocs()
}

const openTagEditor = (row) => {
  tagTarget.value = row
  tagValue.value = row.tags || ''
  tagDialog.value = true
}

const saveTags = async () => {
  await setDocumentTags(tagTarget.value.id, tagValue.value)
  ElMessage.success('标签已更新')
  tagDialog.value = false
  await loadDocs()
}

const onSelectionChange = (rows) => {
  selectedDocIds.value = rows.map((r) => r.id)
}

const batchDeleteSelected = async () => {
  if (!selectedDocIds.value.length) return
  await ElMessageBox.confirm('确认批量删除选中文档？', '提示', { type: 'warning' })
  await batchDeleteDocuments(selectedDocIds.value)
  ElMessage.success('已删除')
  selectedDocIds.value = []
  await loadDocs()
}

const removeDoc = async (id) => {
  await ElMessageBox.confirm('确认删除该文档？', '提示', { type: 'warning' })
  await deleteDocument(id)
  ElMessage.success('已删除')
  await loadDocs()
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
