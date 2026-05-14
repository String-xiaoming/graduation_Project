<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
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

const cityOptions = ['', '贵阳', '遵义', '安顺', '六盘水', '毕节', '铜仁', '黔东南', '黔南', '黔西南']
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

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

const activeFilters = computed(() => {
  const items = []
  if (query.keyword) items.push({ key: 'keyword', label: `关键词：${query.keyword}` })
  if (query.city) items.push({ key: 'city', label: `城市：${query.city}` })
  if (query.educationText) items.push({ key: 'educationText', label: `学历：${query.educationText}` })
  if (query.experienceText) items.push({ key: 'experienceText', label: `经验：${query.experienceText}` })
  if (query.salaryMin || query.salaryMax) {
    const min = query.salaryMin || '不限'
    const max = query.salaryMax || '不限'
    items.push({ key: 'salary', label: `薪资：${min}-${max}` })
  }
  if (query.onlyWithSalary) items.push({ key: 'onlyWithSalary', label: '只看有薪资' })
  return items
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

function removeFilter(key) {
  if (key === 'salary') {
    query.salaryMin = ''
    query.salaryMax = ''
  } else if (key === 'onlyWithSalary') {
    query.onlyWithSalary = false
  } else {
    query[key] = ''
  }
  search()
}

function changePage(delta) {
  const next = query.pageNum + delta
  if (next < 1 || next > totalPages.value) return
  query.pageNum = next
  syncRoute()
  loadJobs()
}

watch(
  () => route.query,
  (nextQuery) => {
    query.keyword = nextQuery.keyword || ''
    query.city = nextQuery.city || ''
    query.educationText = nextQuery.educationText || ''
    query.experienceText = nextQuery.experienceText || ''
    query.salaryMin = nextQuery.salaryMin || ''
    query.salaryMax = nextQuery.salaryMax || ''
    query.onlyWithSalary = nextQuery.onlyWithSalary === 'true'
    query.pageNum = Number(nextQuery.pageNum) || 1
    query.pageSize = Number(nextQuery.pageSize) || query.pageSize
  },
)

onMounted(loadJobs)
</script>

<template>
  <section class="job-library-page page-section">
    <div class="job-library-hero">
      <div>
        <p class="eyebrow">JOB LIBRARY</p>
        <h1>岗位库</h1>
        <p>搜索贵州本地岗位，按城市、薪资、学历和经验快速筛选。</p>
      </div>
      <div class="job-library-count">
        <span>当前岗位</span>
        <strong>{{ total }}</strong>
      </div>
    </div>

    <form class="job-search-panel" @submit.prevent="search">
      <label class="job-search-panel__keyword">
        <span>关键词</span>
        <input v-model.trim="query.keyword" placeholder="岗位、公司、技能" />
      </label>

      <label>
        <span>城市</span>
        <select v-model="query.city">
          <option v-for="city in cityOptions" :key="city" :value="city">
            {{ city || '全部城市' }}
          </option>
        </select>
      </label>

      <button type="submit">搜索岗位</button>
    </form>

    <section class="job-filter-strip">
      <label>
        <span>学历</span>
        <select v-model="query.educationText" @change="search">
          <option v-for="item in educationOptions" :key="item" :value="item">
            {{ item || '全部学历' }}
          </option>
        </select>
      </label>

      <label>
        <span>经验</span>
        <select v-model="query.experienceText" @change="search">
          <option v-for="item in experienceOptions" :key="item" :value="item">
            {{ item || '全部经验' }}
          </option>
        </select>
      </label>

      <label>
        <span>最低薪资</span>
        <input v-model="query.salaryMin" type="number" min="0" step="500" placeholder="5000" @keyup.enter="search" />
      </label>

      <label>
        <span>最高薪资</span>
        <input v-model="query.salaryMax" type="number" min="0" step="500" placeholder="9000" @keyup.enter="search" />
      </label>

      <label class="check-line job-filter-strip__check">
        <input v-model="query.onlyWithSalary" type="checkbox" @change="search" />
        <span>只看有薪资</span>
      </label>

      <button class="ghost-button" type="button" @click="search">应用筛选</button>
      <button class="text-button" type="button" @click="resetFilters">重置</button>
    </section>

    <div v-if="activeFilters.length" class="active-filter-row">
      <span>当前筛选</span>
      <button v-for="item in activeFilters" :key="item.key" type="button" @click="removeFilter(item.key)">
        {{ item.label }} ×
      </button>
    </div>

    <section class="job-results">
      <div class="result-bar">
        <div>
          <strong>共找到 {{ total }} 个岗位</strong>
          <span>第 {{ query.pageNum }} / {{ totalPages }} 页</span>
        </div>
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
        <span>第 {{ query.pageNum }} / {{ totalPages }} 页</span>
        <button
          type="button"
          :disabled="query.pageNum >= totalPages || loading"
          @click="changePage(1)"
        >
          下一页
        </button>
      </div>
    </section>
  </section>
</template>
