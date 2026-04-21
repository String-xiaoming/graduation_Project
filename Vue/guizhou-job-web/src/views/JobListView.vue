<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchJobPage } from '@/api/job'
import EmptyState from '@/components/EmptyState.vue'
import JobCard from '@/components/JobCard.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const jobs = ref([])
const total = ref(0)

const cityOptions = ['', '贵阳', '安顺', '遵义', '六盘水', '毕节', '铜仁', '黔东南', '黔南', '黔西南']
const educationOptions = ['', '不限', '初中及以下', '高中', '中专/中技', '大专', '本科', '硕士', '博士']
const experienceOptions = ['', '不限', '应届生', '经验不限', '1年以内', '1-3年', '3-5年', '5-10年', '10年以上']

const query = reactive({
  pageNum: Number(route.query.pageNum) || 1,
  pageSize: Number(route.query.pageSize) || 10,
  keyword: route.query.keyword || '',
  city: route.query.city || '',
  educationText: route.query.educationText || '',
  experienceText: route.query.experienceText || '',
  salaryMin: route.query.salaryMin || '',
  salaryMax: route.query.salaryMax || '',
  onlyWithSalary: route.query.onlyWithSalary === 'true',
})

function buildParams() {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    keyword: query.keyword || undefined,
    city: query.city || undefined,
    educationText: query.educationText || undefined,
    experienceText: query.experienceText || undefined,
    salaryMin: query.salaryMin || undefined,
    salaryMax: query.salaryMax || undefined,
    onlyWithSalary: query.onlyWithSalary || undefined,
  }
}

function syncRoute() {
  router.replace({
    path: '/jobs',
    query: buildParams(),
  })
}

async function loadJobs() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await fetchJobPage(buildParams())
    jobs.value = data?.list || []
    total.value = data?.total || 0
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  syncRoute()
  loadJobs()
}

function resetFilters() {
  query.pageNum = 1
  query.pageSize = 10
  query.keyword = ''
  query.city = ''
  query.educationText = ''
  query.experienceText = ''
  query.salaryMin = ''
  query.salaryMax = ''
  query.onlyWithSalary = false
  syncRoute()
  loadJobs()
}

function changePage(delta) {
  const next = query.pageNum + delta
  if (next < 1) return
  query.pageNum = next
  syncRoute()
  loadJobs()
}

watch(
  () => route.query,
  (nextQuery) => {
    query.keyword = nextQuery.keyword || query.keyword
    query.city = nextQuery.city || query.city
  },
)

onMounted(loadJobs)
</script>

<template>
  <section class="page-title page-section">
    <p class="eyebrow">JOB LIBRARY</p>
    <h1>岗位库</h1>
    <p>按关键词、城市、学历、经验和薪资范围筛选岗位，快速定位更适合自己的机会。</p>
  </section>

  <section class="page-section job-layout">
    <aside class="filter-panel">
      <label>
        关键词
        <input v-model.trim="query.keyword" placeholder="岗位、公司、技能" @keyup.enter="search" />
      </label>

      <label>
        城市
        <select v-model="query.city">
          <option v-for="city in cityOptions" :key="city" :value="city">
            {{ city || '全部城市' }}
          </option>
        </select>
      </label>

      <label>
        学历
        <select v-model="query.educationText">
          <option v-for="item in educationOptions" :key="item" :value="item">
            {{ item || '全部学历' }}
          </option>
        </select>
      </label>

      <label>
        经验
        <select v-model="query.experienceText">
          <option v-for="item in experienceOptions" :key="item" :value="item">
            {{ item || '全部经验' }}
          </option>
        </select>
      </label>

      <div class="salary-row">
        <label>
          最低薪资
          <input v-model="query.salaryMin" type="number" placeholder="5000" />
        </label>
        <label>
          最高薪资
          <input v-model="query.salaryMax" type="number" placeholder="9000" />
        </label>
      </div>

      <label class="check-line">
        <input v-model="query.onlyWithSalary" type="checkbox" />
        只看可计算薪资
      </label>

      <div class="filter-actions">
        <button type="button" @click="search">应用筛选</button>
        <button class="ghost-button" type="button" @click="resetFilters">重置</button>
      </div>
    </aside>

    <div class="job-results">
      <div class="result-bar">
        <span>共找到 {{ total }} 个岗位</span>
        <select v-model.number="query.pageSize" @change="search">
          <option :value="10">每页 10 条</option>
          <option :value="20">每页 20 条</option>
          <option :value="50">每页 50 条</option>
        </select>
      </div>

      <div v-if="errorMessage" class="notice-card">{{ errorMessage }}</div>
      <LoadingBlock v-else-if="loading" text="正在加载岗位" />
      <EmptyState v-else-if="jobs.length === 0" title="没有找到岗位" description="换一个城市或关键词试试。" />

      <div v-else class="job-stack">
        <JobCard v-for="job in jobs" :key="job.id" :job="job" />
      </div>

      <div class="pagination">
        <button type="button" :disabled="query.pageNum <= 1 || loading" @click="changePage(-1)">
          上一页
        </button>
        <span>第 {{ query.pageNum }} 页</span>
        <button
          type="button"
          :disabled="query.pageNum * query.pageSize >= total || loading"
          @click="changePage(1)"
        >
          下一页
        </button>
      </div>
    </div>
  </section>
</template>
