<template>
  <MainLayout>
    <div class="space-y-4">
      <h2 class="text-2xl font-semibold">管理台</h2>

      <el-row :gutter="12">
        <el-col :span="8"><el-card shadow="never">总文档数：{{ overview.totalDocuments || 0 }}</el-card></el-col>
        <el-col :span="8"><el-card shadow="never">总对话数：{{ overview.totalConversations || 0 }}</el-card></el-col>
        <el-col :span="8"><el-card shadow="never">高频问题：{{ (overview.hotQuestions || []).length }}</el-card></el-col>
      </el-row>

      <el-card shadow="never">
        <template #header><div class="font-semibold">高频问题</div></template>
        <el-table :data="overview.hotQuestions || []" stripe>
          <el-table-column prop="question" label="问题" />
          <el-table-column prop="count" label="次数" width="100" />
        </el-table>
      </el-card>

      <el-card v-if="isSuper" shadow="never">
        <template #header><div class="font-semibold">用户管理</div></template>
        <el-table :data="users" stripe>
          <el-table-column prop="username" label="账号" width="160" />
          <el-table-column prop="nickname" label="昵称" width="180" />
          <el-table-column prop="role" label="角色" width="160">
            <template #default="scope">
              <el-select v-model="scope.row.role" class="w-36">
                <el-option label="普通用户" value="USER" />
                <el-option label="文档管理员" value="DOC_ADMIN" />
                <el-option label="超级管理员" value="SUPER_ADMIN" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="120">
            <template #default="scope">
              <el-switch v-model="scope.row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button size="small" type="primary" @click="saveUser(scope.row)">保存</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="isSuper" shadow="never">
        <template #header><div class="font-semibold">系统配置</div></template>
        <div class="flex gap-2 mb-2">
          <el-input v-model="configForm.configKey" placeholder="配置键" class="w-56" />
          <el-input v-model="configForm.configValue" placeholder="配置值" class="w-80" />
          <el-input v-model="configForm.description" placeholder="描述" class="w-80" />
          <el-button type="primary" @click="saveConfig">保存</el-button>
        </div>
        <el-table :data="configs" stripe>
          <el-table-column prop="configKey" label="配置键" width="220" />
          <el-table-column prop="configValue" label="配置值" />
          <el-table-column prop="description" label="描述" width="220" />
        </el-table>
      </el-card>

      <el-card v-if="isSuper" shadow="never">
        <template #header><div class="font-semibold">操作日志</div></template>
        <el-table :data="logs" stripe>
          <el-table-column prop="createdAt" label="时间" width="180" />
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="action" label="动作" />
          <el-table-column prop="success" label="结果" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.success ? 'success' : 'danger'">{{ scope.row.success ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import MainLayout from '../components/MainLayout.vue'
import { adminConfigs, adminLogs, adminOverview, adminUsers, saveAdminConfig, updateAdminUser } from '../api'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const isSuper = computed(() => authStore.user?.role === 'SUPER_ADMIN')

const overview = ref({})
const users = ref([])
const configs = ref([])
const logs = ref([])
const configForm = reactive({ configKey: '', configValue: '', description: '' })

const load = async () => {
  overview.value = await adminOverview()
  if (isSuper.value) {
    users.value = await adminUsers()
    configs.value = await adminConfigs()
    const logPage = await adminLogs({ pageNum: 1, pageSize: 20 })
    logs.value = logPage.records || []
  }
}

const saveUser = async (row) => {
  await updateAdminUser(row.id, {
    nickname: row.nickname,
    role: row.role,
    enabled: row.enabled
  })
  ElMessage.success('用户更新成功')
}

const saveConfig = async () => {
  if (!configForm.configKey || !configForm.configValue) return ElMessage.warning('请填写配置键和值')
  await saveAdminConfig(configForm)
  ElMessage.success('配置保存成功')
  configForm.configKey = ''
  configForm.configValue = ''
  configForm.description = ''
  configs.value = await adminConfigs()
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
})
</script>
