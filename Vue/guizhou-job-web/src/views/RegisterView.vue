<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { registerUser, sendEmailCode } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const errorMessage = ref('')
const successMessage = ref('')
const toastMessage = ref('')

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  emailCode: '',
})

let timer = null
let toastTimer = null

function showToast(message) {
  toastMessage.value = message
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 5000)
}

function startCountdown() {
  countdown.value = 60
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function sendCode() {
  if (!form.email) {
    errorMessage.value = '请先填写邮箱'
    return
  }
  sending.value = true
  errorMessage.value = ''
  try {
    await sendEmailCode({ email: form.email, scene: 'REGISTER' })
    showToast('验证码已发送，请查看邮箱')
    startCountdown()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!form.nickname || !form.email || !form.password || !form.confirmPassword || !form.emailCode) {
    errorMessage.value = '请完整填写注册信息'
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    errorMessage.value = '密码长度需要在6到20位之间'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
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
      emailCode: form.emailCode,
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
    <div v-if="toastMessage" class="toast-notice" aria-live="polite">{{ toastMessage }}</div>

    <div class="auth-visual auth-visual--register">
      <p class="eyebrow">CREATE ACCOUNT</p>
      <h1>注册求职账号</h1>
      <p>注册前需要完成邮箱验证码校验，避免他人冒用邮箱。</p>
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
        <input v-model="form.password" type="password" placeholder="6到20位密码" autocomplete="new-password" />
      </label>

      <label>
        确认密码
        <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" autocomplete="new-password" />
      </label>

      <label>
        邮箱验证码
        <span class="code-input-row">
          <input v-model.trim="form.emailCode" placeholder="请输入验证码" autocomplete="one-time-code" />
          <button class="ghost-button" type="button" :disabled="sending || countdown > 0" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s` : sending ? '发送中' : '发送' }}
          </button>
        </span>
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
