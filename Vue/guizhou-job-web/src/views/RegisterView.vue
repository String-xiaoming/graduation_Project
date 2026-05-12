<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { registerUser } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const form = reactive({
  nickname: '',
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
  event.currentTarget.style.setProperty('--bubble-x', '76%')
  event.currentTarget.style.setProperty('--bubble-y', '18%')
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
  if (!form.nickname || !form.email || !form.password) {
    errorMessage.value = '请完整填写注册信息'
    return
  }
  if (form.password.length < 6 || form.password.length > 12) {
    errorMessage.value = '密码长度需要在 6 到 12 位之间'
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await registerUser({
      nickname: form.nickname,
      email: form.email,
      password: form.password,
    })
    successMessage.value = '注册成功，即将跳转登录'
    setTimeout(() => router.push('/login'), 800)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page page-section">
    <div class="auth-visual auth-visual--register" @mousemove="moveBubble" @mouseleave="resetBubble" @click="burstBubble">
      <span class="auth-bubble" :class="{ 'is-bursting': isBursting }" aria-hidden="true"></span>
      <span
        v-for="b in smallBubbles"
        :key="b.id"
        class="auth-bubble-small"
        :style="{ '--bx': b.x + 'px', '--by': b.y + 'px', '--tx': b.tx + 'px', '--ty': b.ty + 'px', '--scale': b.scale }"
      ></span>
      <p class="eyebrow">CREATE ACCOUNT</p>
      <h1>开始贵州求职之旅</h1>
      <p>
        注册
      </p>
    </div>

    <form class="auth-card" @submit.prevent="submit">
      <p class="eyebrow">REGISTER</p>
      <h2>用户注册</h2>

      <label>
        昵称
        <input v-model.trim="form.nickname" placeholder="请输入昵称" autocomplete="nickname" />
      </label>

      <label>
        邮箱
        <input v-model.trim="form.email" type="email" placeholder="请输入邮箱" autocomplete="email" />
      </label>

      <label>
        密码
        <input
          v-model="form.password"
          type="password"
          placeholder="6 到 12 位密码"
          autocomplete="new-password"
        />
      </label>

      <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="form-success">{{ successMessage }}</div>

      <button type="submit" :disabled="loading">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <p class="auth-switch">
        已经有账号？
        <RouterLink to="/login">去登录</RouterLink>
      </p>
    </form>
  </section>
</template>
