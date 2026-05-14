<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchJobPage } from '@/api/job'
import { fetchUserProfile } from '@/api/user'
import JobCard from '@/components/JobCard.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { getAuthToken, getCurrentUser } from '@/utils/auth'

const router = useRouter()
const currentUser = ref(getCurrentUser())
const authToken = ref(getAuthToken())
const loading = ref(false)
const refreshing = ref(false)
const errorMessage = ref('')
const profile = ref(null)
const recommendedJobs = ref([])

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

const loginRequired = computed(() => !currentUser.value || !authToken.value)

const extractedSkills = computed(() => {
  const text = profile.value?.skillInputText || ''
  const lowerText = text.toLowerCase()
  const matched = skillDictionary.filter((skill) => lowerText.includes(skill.toLowerCase()))
  const manual = text
    .split(/[，,、；;\s\n]+/)
    .map((item) => item.trim())
    .filter((item) => item.length >= 2 && item.length <= 30)
  return Array.from(new Set([...matched, ...manual])).slice(0, 12)
})

const salaryLabel = computed(() => {
  if (!profile.value) return '未读取'
  const min = profile.value.expectedSalaryMin
  const max = profile.value.expectedSalaryMax
  if (min && max) return `${min} - ${max} 元/月`
  if (min) return `${min} 元/月以上`
  if (max) return `${max} 元/月以内`
  return '未设置'
})

const matchSignals = computed(() => [
  { label: '期望岗位', value: profile.value?.expectedPosition || extractedSkills.value[0] || '未设置' },
  { label: '目标城市', value: profile.value?.localCity || '未设置' },
  { label: '学历条件', value: profile.value?.educationText || '未设置' },
  { label: '薪资范围', value: salaryLabel.value },
])

function buildJobParams() {
  if (!profile.value) return null
  const fallbackKeyword = extractedSkills.value[0] || ''
  const educationText = profile.value.educationText
  return {
    pageNum: 1,
    pageSize: 9,
    keyword: profile.value.expectedPosition || fallbackKeyword || undefined,
    city: profile.value.localCity || undefined,
    educationText: educationText && educationText !== '不限' ? educationText : undefined,
    salaryMin: profile.value.expectedSalaryMin || undefined,
    salaryMax: profile.value.expectedSalaryMax || undefined,
  }
}

function hasMatchParams(params) {
  return Boolean(params?.keyword || params?.city || params?.educationText || params?.salaryMin || params?.salaryMax)
}

async function loadRecommendations(isRefresh = false) {
  currentUser.value = getCurrentUser()
  authToken.value = getAuthToken()
  if (loginRequired.value) return

  loading.value = !isRefresh
  refreshing.value = isRefresh
  errorMessage.value = ''

  try {
    profile.value = await fetchUserProfile(currentUser.value.id)
    const params = buildJobParams()
    if (!hasMatchParams(params)) {
      recommendedJobs.value = []
      return
    }

    const data = await fetchJobPage(params)
    recommendedJobs.value = data?.list || []
  } catch (error) {
    errorMessage.value = error.message
    recommendedJobs.value = []
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function goProfile() {
  router.push('/profile')
}

onMounted(() => loadRecommendations())
</script>

<template>
  <section class="recommend-page page-section">
    <div class="recommend-hero">
      <div>
        <p class="eyebrow">JOB RECOMMENDATION</p>
        <h1>岗位推荐</h1>
        <p>根据个人中心里的城市、岗位方向、学历、薪资和技能关键词，筛出更贴近当前偏好的岗位。</p>
      </div>
      <button v-if="!loginRequired" type="button" :disabled="refreshing" @click="loadRecommendations(true)">
        {{ refreshing ? '刷新中...' : '刷新推荐' }}
      </button>
    </div>

    <div v-if="loginRequired" class="recommend-login-panel">
      <div>
        <p class="eyebrow">LOGIN REQUIRED</p>
        <h2>登录后才能使用岗位推荐</h2>
        <p>推荐会读取你的求职资料与技能描述，未登录时不会展示个人化结果。</p>
      </div>
      <div class="recommend-login-actions">
        <RouterLink class="login-link" :to="{ path: '/login', query: { redirect: '/recommend' } }">去登录</RouterLink>
        <RouterLink class="ghost-button" to="/register">注册账号</RouterLink>
      </div>
    </div>

    <LoadingBlock v-else-if="loading" text="正在生成岗位推荐" />

    <template v-else>
      <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>

      <section class="recommend-signals">
        <article v-for="item in matchSignals" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <section class="recommend-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">MATCHED JOBS</p>
            <h2>按我的偏好匹配</h2>
          </div>
          <button class="ghost-button" type="button" @click="goProfile">完善个人资料</button>
        </div>

        <div v-if="extractedSkills.length" class="recommend-skill-row">
          <span>关键词</span>
          <strong v-for="skill in extractedSkills" :key="skill">{{ skill }}</strong>
        </div>

        <div v-if="recommendedJobs.length" class="job-grid recommend-job-grid">
          <JobCard v-for="job in recommendedJobs" :key="job.id" :job="job" />
        </div>

        <div v-else class="empty-state recommend-empty-state">
          <div>
            <span class="empty-state__mark">?</span>
            <h3>暂时没有匹配结果</h3>
            <p>可以先补充期望岗位、所在城市、薪资范围或技能描述，再回来刷新推荐。</p>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>
