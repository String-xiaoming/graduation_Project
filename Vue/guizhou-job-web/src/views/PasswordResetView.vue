<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { resetUserPassword, sendEmailCode } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const errorMessage = ref('')
const successMessage = ref('')
const toastMessage = ref('')

const form = reactive({
  email: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: '',
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
    await sendEmailCode({ email: form.email, scene: 'RESET_PASSWORD' })
    showToast('验证码已发送，请查看邮箱')
    startCountdown()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!form.email || !form.emailCode || !form.newPassword || !form.confirmPassword) {
    errorMessage.value = '请完整填写重置密码信息'
    return
  }
  if (form.newPassword.length < 6 || form.newPassword.length > 20) {
    errorMessage.value = '密码长度需要在6到20位之间'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await resetUserPassword({
      email: form.email,
      emailCode: form.emailCode,
      newPassword: form.newPassword,
    })
    successMessage.value = '密码已重置，即将返回登录'
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

    <div class="auth-visual">
      <p class="eyebrow">PASSWORD RESET</p>
      <h1>重置登录密码</h1>
      <p>输入注册邮箱并完成验证码校验后，即可设置新密码。</p>
    </div>

    <form class="auth-card" @submit.prevent="submit">
      <p class="eyebrow">RESET</p>
      <h2>找回密码</h2>

      <label>
        邮箱
        <input v-model.trim="form.email" type="email" placeholder="请输入邮箱" autocomplete="email" />
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

      <label>
        新密码
        <input v-model="form.newPassword" type="password" placeholder="6到20位新密码" autocomplete="new-password" />
      </label>

      <label>
        确认新密码
        <input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" autocomplete="new-password" />
      </label>

      <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="form-success">{{ successMessage }}</div>

      <button type="submit" :disabled="loading">
        {{ loading ? '提交中...' : '重置密码' }}
      </button>

      <p class="auth-switch">
        想起来了？
        <RouterLink to="/login">返回登录</RouterLink>
      </p>
    </form>
  </section>
</template>
