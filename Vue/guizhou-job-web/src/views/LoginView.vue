<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { loginUser } from '@/api/user'
import { setCurrentUser } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')

const form = reactive({
  email: '',
  password: '',
})

const isBursting = ref(false)
const smallBubbles = ref([])

function moveBubble(event) {
  if (isBursting.value) return
  const rect = event.currentTarget.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  event.currentTarget.style.setProperty('--bubble-x', `${x}px`)
  event.currentTarget.style.setProperty('--bubble-y', `${y}px`)
}

function resetBubble(event) {
  if (isBursting.value) return
  event.currentTarget.style.setProperty('--bubble-x', '78%')
  event.currentTarget.style.setProperty('--bubble-y', '22%')
}

function burstBubble(event) {
  if (isBursting.value) return
  isBursting.value = true
  const rect = event.currentTarget.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const newBubbles = []
  for (let i = 0; i < 15; i++) {
    const angle = (Math.PI * 2 * i) / 15 + Math.random() * 0.8
    const distance = 120 + Math.random() * 180
    newBubbles.push({
      id: Date.now() + i,
      x,
      y,
      tx: x + Math.cos(angle) * distance,
      ty: y + Math.sin(angle) * distance,
      scale: Math.random() * 0.6 + 0.2,
    })
  }
  smallBubbles.value = newBubbles

  setTimeout(() => {
    smallBubbles.value = []
    isBursting.value = false
  }, 1200)
}

async function submit() {
  if (!form.email || !form.password) {
    errorMessage.value = '请填写邮箱和密码'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const user = await loginUser({
      email: form.email,
      password: form.password,
    })
    setCurrentUser(user)
    router.push('/')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page page-section">
    <div class="auth-visual" @pointermove="moveBubble" @pointerleave="resetBubble" @click="burstBubble">
      <span class="auth-bubble" :class="{ 'is-bursting': isBursting }" aria-hidden="true"></span>
      <span
        v-for="b in smallBubbles"
        :key="b.id"
        class="auth-bubble-small"
        :style="{ '--bx': b.x + 'px', '--by': b.y + 'px', '--tx': b.tx + 'px', '--ty': b.ty + 'px', '--scale': b.scale }"
      ></span>
      <p class="eyebrow">WELCOME BACK</p>
      <h1>开始求职之旅吧</h1>
      <p>
        登录
      </p>
    </div>

    <form class="auth-card" @submit.prevent="submit">
      <p class="eyebrow">LOGIN</p>
      <h2>用户登录</h2>

      <label>
        邮箱
        <input v-model.trim="form.email" type="email" placeholder="请输入邮箱" autocomplete="email" />
      </label>

      <label>
        密码
        <input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
        />
      </label>

      <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>

      <button type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <p class="auth-switch">
        还没有账号？
        <RouterLink to="/register">去注册</RouterLink>
      </p>
    </form>
  </section>
</template>
