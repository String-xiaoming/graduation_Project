import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser, isAdminUser } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/jobs',
      name: 'jobs',
      component: () => import('@/views/JobListView.vue'),
    },
    {
      path: '/jobs/:id',
      name: 'job-detail',
      component: () => import('@/views/JobDetailView.vue'),
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/AdminView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const currentUser = getCurrentUser()

  if (to.meta.requiresAuth && !currentUser) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !isAdminUser(currentUser)) {
    return { path: '/' }
  }

  if ((to.name === 'login' || to.name === 'register') && currentUser) {
    return { path: isAdminUser(currentUser) ? '/admin' : '/profile' }
  }

  return true
})

export default router
