<template>
  <div class="min-h-screen p-4 md:p-6">
    <div class="grid grid-cols-1 md:grid-cols-[240px_1fr] gap-4 fade-up">
      <aside class="glass-card p-4">
        <h1 class="text-xl font-semibold text-brand-700">RAG KnowledgeHub</h1>
        <p class="text-sm text-slate-500 mt-1">企业知识库问答平台</p>
        <el-menu class="mt-6 border-none bg-transparent" :default-active="route.path" @select="onSelect">
          <el-menu-item index="/kb">知识库</el-menu-item>
          <el-menu-item index="/chat">智能问答</el-menu-item>
          <el-menu-item index="/stats">个人统计</el-menu-item>
          <el-menu-item v-if="isManager" index="/admin">管理台</el-menu-item>
        </el-menu>

        <div class="mt-8 text-sm text-slate-600">
          <div>用户：{{ authStore.user?.nickname || authStore.user?.username }}</div>
          <div class="mt-1">角色：{{ authStore.user?.role }}</div>
        </div>

        <el-button class="mt-3" @click="showPwd = true">修改密码</el-button>
        <el-button class="mt-2" type="danger" plain @click="logout">退出登录</el-button>
      </aside>

      <main class="glass-card p-4 md:p-6">
        <slot />
      </main>
    </div>

    <el-dialog v-model="showPwd" title="修改密码" width="420px">
      <el-form>
        <el-form-item label="旧密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPwd = false">取消</el-button>
        <el-button type="primary" @click="submitPwd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { changePassword } from '../api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const showPwd = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '' })

const isManager = computed(() => ['DOC_ADMIN', 'SUPER_ADMIN'].includes(authStore.user?.role))

const onSelect = (path) => {
  router.push(path)
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}

const submitPwd = async () => {
  if (!pwd.oldPassword || !pwd.newPassword) {
    return ElMessage.warning('请完整填写密码')
  }
  await changePassword(pwd)
  ElMessage.success('密码修改成功')
  pwd.oldPassword = ''
  pwd.newPassword = ''
  showPwd.value = false
}
</script>
