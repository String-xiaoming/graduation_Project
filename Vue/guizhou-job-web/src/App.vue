<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { clearCurrentUser, getCurrentUser, onAuthChange } from '@/utils/auth'

const route = useRoute()
const currentUser = ref(getCurrentUser())
let stopAuthListener = null

const navItems = [
  { path: '/', label: '首页' },
  { path: '/jobs', label: '岗位库' },
  { path: '/dashboard', label: '数据看板' },
]

function logout() {
  clearCurrentUser()
}

onMounted(() => {
  stopAuthListener = onAuthChange((user) => {
    currentUser.value = user
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
          :class="{ active: route.path === item.path }"
          :to="item.path"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="auth-area">
        <template v-if="currentUser">
          <span class="user-pill">{{ currentUser.nickname || currentUser.email }}</span>
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
