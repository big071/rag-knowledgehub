import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import KnowledgeBaseView from '../views/KnowledgeBaseView.vue'
import ChatView from '../views/ChatView.vue'
import StatsView from '../views/StatsView.vue'
import AdminView from '../views/AdminView.vue'

const routes = [
  { path: '/login', component: LoginView },
  { path: '/', redirect: '/kb' },
  { path: '/kb', component: KnowledgeBaseView, meta: { auth: true } },
  { path: '/chat', component: ChatView, meta: { auth: true } },
  { path: '/stats', component: StatsView, meta: { auth: true } },
  { path: '/admin', component: AdminView, meta: { auth: true, roles: ['DOC_ADMIN', 'SUPER_ADMIN'] } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.auth && !authStore.token) {
    return '/login'
  }
  if (to.path === '/login' && authStore.token) {
    return '/kb'
  }
  if (to.meta.roles?.length) {
    const role = authStore.user?.role
    if (!to.meta.roles.includes(role)) {
      return '/chat'
    }
  }
  return true
})

export default router
