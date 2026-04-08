<template>
  <div class="min-h-screen p-4 md:p-6">
    <div class="grid grid-cols-1 md:grid-cols-[240px_1fr] gap-4 fade-up">
      <aside class="glass-card p-4">
        <h1 class="text-xl font-semibold text-brand-700">RAG KnowledgeHub</h1>
        <p class="text-sm text-slate-500 mt-1">Java 全栈 + 检索增强生成</p>
        <el-menu
          class="mt-6 border-none bg-transparent"
          :default-active="route.path"
          @select="onSelect"
        >
          <el-menu-item index="/kb">知识库管理</el-menu-item>
          <el-menu-item index="/chat">智能问答</el-menu-item>
          <el-menu-item index="/stats">数据统计</el-menu-item>
        </el-menu>

        <div class="mt-8 text-sm text-slate-600">
          <div>用户：{{ authStore.user?.nickname || authStore.user?.username }}</div>
          <div class="mt-1">角色：{{ authStore.user?.role }}</div>
        </div>

        <el-button class="mt-4" type="danger" plain @click="logout">退出登录</el-button>
      </aside>

      <main class="glass-card p-4 md:p-6">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const onSelect = (path) => {
  router.push(path)
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>
