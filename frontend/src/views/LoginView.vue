<template>
  <div class="min-h-screen flex items-center justify-center px-4">
    <div class="glass-card w-full max-w-5xl grid md:grid-cols-2 overflow-hidden fade-up">
      <section class="p-8 bg-gradient-to-br from-emerald-700 to-teal-800 text-white">
        <h1 class="text-3xl font-bold">智能知识库问答系统</h1>
        <p class="mt-3 text-emerald-100 leading-7">
          通过 RAG 将你的 PDF/Markdown 文档转为可问答知识，支持来源追溯、热点统计与实时回答推送。
        </p>
        <ul class="mt-6 space-y-2 text-sm text-emerald-50">
          <li>Sentence-BERT 向量化</li>
          <li>Elasticsearch TopK 检索</li>
          <li>ChatGPT/通义千问 增强生成</li>
        </ul>
      </section>

      <section class="p-8">
        <el-tabs v-model="tab">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" label-position="top" @submit.prevent>
              <el-form-item label="用户名">
                <el-input v-model="loginForm.username" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="loginForm.password" type="password" show-password />
              </el-form-item>
              <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">登录</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" label-position="top" @submit.prevent>
              <el-form-item label="用户名">
                <el-input v-model="registerForm.username" />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="registerForm.nickname" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="registerForm.password" type="password" show-password />
              </el-form-item>
              <el-button type="success" class="w-full" :loading="loading" @click="handleRegister">注册并登录</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { login, register } from '../api'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const tab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '' })

const toHome = (resp) => {
  authStore.setAuth(resp.token, resp.user)
  router.push('/kb')
}

const handleLogin = async () => {
  loading.value = true
  try {
    const resp = await login(loginForm)
    toHome(resp)
    ElMessage.success('登录成功')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  try {
    const resp = await register(registerForm)
    toHome(resp)
    ElMessage.success('注册成功')
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>
