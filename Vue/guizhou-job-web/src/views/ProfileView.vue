<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchJobPage } from '@/api/job'
import { changeUserPassword, fetchUserProfile, sendEmailCode, updateUserProfile } from '@/api/user'
import JobCard from '@/components/JobCard.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { getCurrentUser, setCurrentUser } from '@/utils/auth'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const passwordSaving = ref(false)
const passwordSending = ref(false)
const passwordCountdown = ref(0)
const matching = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const toastMessage = ref('')
const profile = ref(null)
const matchedJobs = ref([])
let passwordTimer = null
let toastTimer = null

const cityOptions = ['贵阳', '遵义', '安顺', '六盘水', '毕节', '铜仁', '黔东南', '黔南', '黔西南']
const educationOptions = ['不限', '初中及以上', '高中', '中专/中技', '大专', '本科', '硕士', '博士']

const form = reactive({
  id: null,
  nickname: '',
  localCity: '',
  educationText: '',
  expectedPosition: '',
  expectedSalaryMin: '',
  expectedSalaryMax: '',
  skillInputText: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  emailCode: '',
})

function showToast(message) {
  toastMessage.value = message
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 5000)
}

const skillDictionary = [
  'Java',
  'Python',
  'C++',
  'C#',
  'JavaScript',
  'TypeScript',
  'Vue',
  'React',
  'Spring Boot',
  'Spring',
  'MySQL',
  'Redis',
  'Linux',
  'Hadoop',
  'Hive',
  'Spark',
  'Excel',
  'CAD',
  'PS',
  'Photoshop',
  '剪辑',
  '销售',
  '客服',
  '运营',
  '会计',
  '财务',
  '行政',
  '人事',
  '招聘',
  '文案',
  '设计',
  '施工',
  '电工',
  '焊工',
  '护士',
  '教师',
  '普通话',
]

const extractedSkills = computed(() => {
  const text = form.skillInputText || ''
  const lowerText = text.toLowerCase()
  const matched = skillDictionary.filter((skill) => lowerText.includes(skill.toLowerCase()))
  const manual = text
    .split(/[，,、；;\s\n]+/)
    .map((item) => item.trim())
    .filter((item) => item.length >= 2 && item.length <= 30)
  return Array.from(new Set([...matched, ...manual])).slice(0, 16)
})

const completionItems = computed(() => [
  Boolean(form.nickname),
  Boolean(form.localCity),
  Boolean(form.educationText),
  Boolean(form.expectedPosition),
  form.expectedSalaryMin !== '' || form.expectedSalaryMax !== '',
  Boolean(form.skillInputText),
])

const completionRate = computed(() => {
  const done = completionItems.value.filter(Boolean).length
  return Math.round((done / completionItems.value.length) * 100)
})

const salaryLabel = computed(() => {
  if (form.expectedSalaryMin && form.expectedSalaryMax) {
    return `${form.expectedSalaryMin} - ${form.expectedSalaryMax} 元/月`
  }
  if (form.expectedSalaryMin) return `${form.expectedSalaryMin} 元/月以上`
  if (form.expectedSalaryMax) return `${form.expectedSalaryMax} 元/月以内`
  return '暂未填写'
})

function fillForm(user) {
  profile.value = user
  Object.assign(form, {
    id: user.id,
    nickname: user.nickname || '',
    localCity: user.localCity || '',
    educationText: user.educationText || '',
    expectedPosition: user.expectedPosition || '',
    expectedSalaryMin: user.expectedSalaryMin ?? '',
    expectedSalaryMax: user.expectedSalaryMax ?? '',
    skillInputText: user.skillInputText || '',
  })
}

function buildJobParams() {
  const fallbackKeyword = extractedSkills.value[0] || ''
  return {
    pageNum: 1,
    pageSize: 4,
    keyword: form.expectedPosition || fallbackKeyword || undefined,
    city: form.localCity || undefined,
    educationText: form.educationText && form.educationText !== '不限' ? form.educationText : undefined,
    salaryMin: form.expectedSalaryMin || undefined,
    salaryMax: form.expectedSalaryMax || undefined,
  }
}

async function loadMatchedJobs() {
  const params = buildJobParams()
  if (!params.keyword && !params.city && !params.educationText && !params.salaryMin && !params.salaryMax) {
    matchedJobs.value = []
    return
  }

  matching.value = true
  try {
    const data = await fetchJobPage(params)
    matchedJobs.value = data?.list || []
  } catch {
    matchedJobs.value = []
  } finally {
    matching.value = false
  }
}

async function loadProfile() {
  const currentUser = getCurrentUser()
  if (!currentUser?.id) {
    router.replace('/login')
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const user = await fetchUserProfile(currentUser.id)
    fillForm(user)
    await loadMatchedJobs()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function normalizeMoney(value) {
  if (value === '' || value === null || value === undefined) return null
  return Number(value)
}

async function saveProfile() {
  if (!form.nickname) {
    errorMessage.value = '请先填写昵称'
    return
  }

  const minSalary = normalizeMoney(form.expectedSalaryMin)
  const maxSalary = normalizeMoney(form.expectedSalaryMax)
  if (minSalary !== null && maxSalary !== null && minSalary > maxSalary) {
    errorMessage.value = '最低期望薪资不能高于最高期望薪资'
    return
  }

  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const payload = {
      id: form.id,
      nickname: form.nickname,
      localCity: form.localCity || null,
      educationText: form.educationText || null,
      expectedPosition: form.expectedPosition || null,
      expectedSalaryMin: minSalary,
      expectedSalaryMax: maxSalary,
      skillInputText: form.skillInputText || null,
    }
    await updateUserProfile(payload)
    const currentUser = getCurrentUser()
    setCurrentUser({ ...currentUser, ...payload, email: currentUser.email, role: currentUser.role })
    successMessage.value = '个人资料已保存'
    await loadProfile()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    saving.value = false
  }
}

function startPasswordCountdown() {
  passwordCountdown.value = 60
  window.clearInterval(passwordTimer)
  passwordTimer = window.setInterval(() => {
    passwordCountdown.value -= 1
    if (passwordCountdown.value <= 0) {
      window.clearInterval(passwordTimer)
      passwordTimer = null
    }
  }, 1000)
}

async function sendChangePasswordCode() {
  const currentUser = getCurrentUser()
  if (!currentUser?.email) {
    errorMessage.value = '请先登录'
    return
  }

  passwordSending.value = true
  errorMessage.value = ''
  try {
    await sendEmailCode({ email: currentUser.email, scene: 'CHANGE_PASSWORD' })
    showToast('验证码已发送，请查看邮箱')
    startPasswordCountdown()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    passwordSending.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword || !passwordForm.emailCode) {
    errorMessage.value = '请完整填写修改密码信息'
    return
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) {
    errorMessage.value = '新密码长度需要在6到20位之间'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    errorMessage.value = '两次输入的新密码不一致'
    return
  }

  passwordSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await changeUserPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      emailCode: passwordForm.emailCode,
    })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '', emailCode: '' })
    successMessage.value = '密码已修改'
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    passwordSaving.value = false
  }
}

function goMatchedJobs() {
  const params = buildJobParams()
  router.push({
    path: '/jobs',
    query: {
      keyword: params.keyword,
      city: params.city,
      educationText: params.educationText,
      salaryMin: params.salaryMin,
      salaryMax: params.salaryMax,
    },
  })
}

onMounted(loadProfile)
</script>

<template>
  <section class="profile-page page-section">
    <div v-if="toastMessage" class="toast-notice" aria-live="polite">{{ toastMessage }}</div>

    <div class="profile-hero">
      <div>
        <p class="eyebrow">PROFILE CENTER</p>
        <h1>个人中心</h1>
        <p>
          完善所在城市、期望岗位、学历、薪资和技能描述后，系统后续可以把这些信息用于岗位推荐和行为画像分析。
        </p>
      </div>
      <div class="profile-score">
        <span>资料完整度</span>
        <strong>{{ completionRate }}%</strong>
        <div class="profile-score__bar">
          <i :style="{ width: `${completionRate}%` }"></i>
        </div>
      </div>
    </div>

    <LoadingBlock v-if="loading" text="正在读取个人资料" />

    <template v-else>
      <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="form-success">{{ successMessage }}</div>

      <div class="profile-layout">
        <aside class="profile-summary-card">
          <div class="profile-avatar">
            {{ (form.nickname || profile?.email || 'U').slice(0, 1).toUpperCase() }}
          </div>
          <h2>{{ form.nickname || '未命名用户' }}</h2>
          <p>{{ profile?.email }}</p>
          <div class="profile-tags">
            <span>{{ form.localCity || '未设置城市' }}</span>
            <span>{{ form.educationText || '未设置学历' }}</span>
            <span>{{ profile?.role === 'ADMIN' ? '管理员' : '普通用户' }}</span>
          </div>

          <dl class="profile-summary-list">
            <div>
              <dt>期望岗位</dt>
              <dd>{{ form.expectedPosition || '暂未填写' }}</dd>
            </div>
            <div>
              <dt>期望薪资</dt>
              <dd>{{ salaryLabel }}</dd>
            </div>
            <div>
              <dt>技能关键词预览</dt>
              <dd>{{ extractedSkills.length ? extractedSkills.slice(0, 5).join('、') : '暂未提取' }}</dd>
            </div>
          </dl>

          <button type="button" @click="goMatchedJobs">按我的偏好找岗位</button>
        </aside>

        <form class="profile-form-card" @submit.prevent="saveProfile">
          <div class="section-head">
            <div>
              <p class="eyebrow">BASIC INFO</p>
              <h2>求职信息</h2>
            </div>
            <button type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存资料' }}</button>
          </div>

          <div class="profile-form-grid">
            <label>
              昵称
              <input v-model.trim="form.nickname" placeholder="请输入昵称" />
            </label>

            <label>
              所在城市
              <select v-model="form.localCity">
                <option value="">请选择城市</option>
                <option v-for="city in cityOptions" :key="city" :value="city">{{ city }}</option>
              </select>
            </label>

            <label>
              学历
              <select v-model="form.educationText">
                <option value="">请选择学历</option>
                <option v-for="item in educationOptions" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>

            <label>
              期望岗位
              <input v-model.trim="form.expectedPosition" placeholder="例如：Java开发、会计、销售" />
            </label>

            <label>
              最低期望薪资
              <input v-model="form.expectedSalaryMin" type="number" min="0" step="500" placeholder="例如：5000" />
            </label>

            <label>
              最高期望薪资
              <input v-model="form.expectedSalaryMax" type="number" min="0" step="500" placeholder="例如：9000" />
            </label>
          </div>

          <label class="profile-skill-field">
            技能与个人描述
            <textarea
              v-model.trim="form.skillInputText"
              rows="6"
              placeholder="可以自然语言填写，例如：会使用 Java、Python，做过 Vue 项目，了解 MySQL 和 Linux。"
            ></textarea>
          </label>

          <div class="skill-preview-panel">
            <div>
              <p class="eyebrow">SKILL PREVIEW</p>
              <h3>前端预览提取结果</h3>
              <p>这里先做页面预览；正式推荐时可以继续接入后端分词或 Spark 分析结果。</p>
            </div>
            <div class="skill-chip-list">
              <span v-for="skill in extractedSkills" :key="skill">{{ skill }}</span>
              <em v-if="!extractedSkills.length">填写技能后会在这里展示关键词</em>
            </div>
          </div>
        </form>
      </div>

      <form class="profile-form-card" @submit.prevent="changePassword">
        <div class="section-head">
          <div>
            <p class="eyebrow">SECURITY</p>
            <h2>修改密码</h2>
          </div>
          <button type="submit" :disabled="passwordSaving">
            {{ passwordSaving ? '修改中...' : '保存新密码' }}
          </button>
        </div>

        <div class="profile-form-grid">
          <label>
            旧密码
            <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" autocomplete="current-password" />
          </label>
          <label>
            新密码
            <input v-model="passwordForm.newPassword" type="password" placeholder="6到20位新密码" autocomplete="new-password" />
          </label>
          <label>
            确认新密码
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" autocomplete="new-password" />
          </label>
          <label>
            邮箱验证码
            <span class="code-input-row">
              <input v-model.trim="passwordForm.emailCode" placeholder="请输入验证码" autocomplete="one-time-code" />
              <button class="ghost-button" type="button" :disabled="passwordSending || passwordCountdown > 0" @click="sendChangePasswordCode">
                {{ passwordCountdown > 0 ? `${passwordCountdown}s` : passwordSending ? '发送中' : '发送' }}
              </button>
            </span>
          </label>
        </div>
      </form>

      <section class="profile-match-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">MATCH PREVIEW</p>
            <h2>按当前资料筛选的岗位</h2>
          </div>
          <button class="ghost-button" type="button" @click="loadMatchedJobs">刷新预览</button>
        </div>

        <LoadingBlock v-if="matching" text="正在匹配岗位" />
        <div v-else-if="matchedJobs.length" class="job-grid">
          <JobCard v-for="job in matchedJobs" :key="job.id" :job="job" />
        </div>
        <div v-else class="empty-state">
          <div>
            <span class="empty-state__mark">?</span>
            <h3>暂无匹配预览</h3>
            <p>完善城市、期望岗位或技能描述后，可以在这里看到初步筛选结果。</p>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>
