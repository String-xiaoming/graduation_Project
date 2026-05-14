<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { loginUser, loginUserByCode, sendEmailCode } from '@/api/user'
import { clearCurrentUser, getAuthToken, getCurrentUser, isAdminUser, setCurrentUser } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const loginMode = ref('password')
const errorMessage = ref('')
const toastMessage = ref('')

const form = reactive({
  email: '',
  password: '',
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

function targetAfterLogin(user) {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect === '/admin' && !isAdminUser(user)) {
    return '/'
  }
  return redirect || (isAdminUser(user) ? '/admin' : '/')
}

async function sendCode() {
  if (!form.email) {
    errorMessage.value = '请先填写邮箱'
    return
  }
  sending.value = true
  errorMessage.value = ''
  try {
    await sendEmailCode({ email: form.email, scene: 'EMAIL_LOGIN' })
    showToast('验证码已发送，请查看邮箱')
    startCountdown()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!form.email) {
    errorMessage.value = '请填写邮箱'
    return
  }
  if (loginMode.value === 'password' && !form.password) {
    errorMessage.value = '请填写密码'
    return
  }
  if (loginMode.value === 'code' && !form.emailCode) {
    errorMessage.value = '请填写验证码'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const user =
      loginMode.value === 'password'
        ? await loginUser({ email: form.email, password: form.password })
        : await loginUserByCode({ email: form.email, emailCode: form.emailCode })
    if (!user?.token) {
      clearCurrentUser()
      errorMessage.value = '登录成功但后端没有返回令牌，请重启后端服务后重新登录'
      return
    }
    setCurrentUser(user)
    await router.replace(targetAfterLogin(user))
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const currentUser = getCurrentUser()
  const token = getAuthToken()
  if (currentUser && token) {
    router.replace(targetAfterLogin(currentUser))
    return
  }
  if (currentUser && !token) {
    clearCurrentUser()
  }
})
</script>

<template>
  <section class="auth-page page-section">
    <div v-if="toastMessage" class="toast-notice" aria-live="polite">{{ toastMessage }}</div>

    <div class="auth-visual">
      <p class="eyebrow">WELCOME BACK</p>
      <h1>登录贵州岗位平台</h1>
      <p>支持密码登录，也可以使用邮箱验证码安全登录。</p>
    </div>

    <form class="auth-card" @submit.prevent="submit">
      <p class="eyebrow">LOGIN</p>
      <h2>用户登录</h2>

      <div class="auth-tabs">
        <button type="button" :class="{ active: loginMode === 'password' }" @click="loginMode = 'password'">
          密码登录
        </button>
        <button type="button" :class="{ active: loginMode === 'code' }" @click="loginMode = 'code'">
          验证码登录
        </button>
      </div>

      <label>
        邮箱
        <input v-model.trim="form.email" type="email" placeholder="请输入邮箱" autocomplete="email" />
      </label>

      <label v-if="loginMode === 'password'">
        密码
        <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
      </label>

      <label v-else>
        邮箱验证码
        <span class="code-input-row">
          <input v-model.trim="form.emailCode" placeholder="请输入验证码" autocomplete="one-time-code" />
          <button class="ghost-button" type="button" :disabled="sending || countdown > 0" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s` : sending ? '发送中' : '发送' }}
          </button>
        </span>
      </label>

      <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>

      <button type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <p class="auth-switch">
        <RouterLink to="/password-reset">忘记密码</RouterLink>
        <span> / </span>
        <RouterLink to="/register">去注册</RouterLink>
      </p>
    </form>
  </section>
</template>
