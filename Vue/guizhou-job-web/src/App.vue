<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { clearCurrentUser, getCurrentUser, isAdminUser, onAuthChange } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const currentUser = ref(getCurrentUser())
let stopAuthListener = null

const navItems = [
  { path: '/', label: '首页' },
  { path: '/jobs', label: '岗位库' },
  { path: '/dashboard', label: '数据看板' },
  { path: '/recommend', label: '岗位推荐' },
]

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(`${path}/`)
}

function logout() {
  clearCurrentUser()
  router.push('/login')
}

onMounted(() => {
  stopAuthListener = onAuthChange((user) => {
    currentUser.value = user
    if (!user && route.meta.requiresAuth) {
      router.replace({ path: '/login', query: { redirect: route.fullPath } })
      return
    }
    if (user && route.meta.requiresAdmin && !isAdminUser(user)) {
      router.replace('/')
    }
  })
})

onUnmounted(() => {
  stopAuthListener?.()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">黔</span>
        <span>
          <strong>Guizhou Job</strong>
          <small>贵州岗位智能推荐平台</small>
        </span>
      </RouterLink>

      <nav class="nav-links" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :class="{ active: isActive(item.path) }"
          :to="item.path"
        >
          {{ item.label }}
        </RouterLink>
        <RouterLink
          v-if="currentUser"
          :class="{ active: isActive('/profile') }"
          to="/profile"
        >
          个人中心
        </RouterLink>
        <RouterLink
          v-if="isAdminUser(currentUser)"
          :class="{ active: isActive('/admin') }"
          to="/admin"
        >
          管理后台
        </RouterLink>
      </nav>

      <div class="auth-area">
        <template v-if="currentUser">
          <RouterLink class="user-pill" to="/profile">
            {{ currentUser.nickname || currentUser.email }}
          </RouterLink>
          <button class="ghost-button auth-button" type="button" @click="logout">退出</button>
        </template>
        <RouterLink v-else class="login-link" to="/login">登录</RouterLink>
      </div>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>
