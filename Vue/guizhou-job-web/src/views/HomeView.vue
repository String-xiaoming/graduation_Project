<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHotJobs, fetchJobPage } from '@/api/job'
import JobCard from '@/components/JobCard.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import MetricCard from '@/components/MetricCard.vue'

const router = useRouter()

const loading = ref(true)
const errorMessage = ref('')
const searchKeyword = ref('')
const hotJobs = ref([])
const pageInfo = reactive({
  total: 0,
})

const featuredCities = [
  { name: '贵阳', tone: '省会机会集中，岗位类型更丰富' },
  { name: '安顺', tone: '文旅、制造与本地服务岗位活跃' },
  { name: '遵义', tone: '销售、教育、制造岗位选择多' },
  { name: '黔西南', tone: '能源、工程与基层服务机会突出' },
]

const salaryReadyCount = computed(
  () => hotJobs.value.filter((job) => job.salaryMin != null && job.salaryMax != null).length,
)

function goSearch(city = '') {
  router.push({
    path: '/jobs',
    query: {
      keyword: searchKeyword.value || undefined,
      city: city || undefined,
    },
  })
}

async function loadHome() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [page, hot] = await Promise.all([
      fetchJobPage({ pageNum: 1, pageSize: 1 }),
      fetchHotJobs(6),
    ])
    pageInfo.total = page?.total || 0
    hotJobs.value = Array.isArray(hot) ? hot : []
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <section class="hero page-section">
    <div class="hero__content">
      <p class="eyebrow">贵州就业助手</p>
      <h1>更快找到适合你的贵州本地岗位。</h1>
      <p class="hero__lead">
        汇总贵州多地区招聘信息，支持岗位搜索、城市筛选、薪资分析和个人求职偏好记录。
      </p>

      <div class="hero-search">
        <input
          v-model.trim="searchKeyword"
          type="search"
          placeholder="输入岗位、公司或技能关键词，例如 Java、会计、销售"
          @keyup.enter="goSearch()"
        />
        <button type="button" @click="goSearch()">搜索岗位</button>
      </div>
    </div>

    <aside class="hero-panel">
      <span>SMART MATCH</span>
      <strong>岗位洞察、城市选择、薪资参考一次看清</strong>
      <p>先浏览岗位趋势，再根据城市、学历、经验和薪资范围筛选更合适的机会。</p>
    </aside>
  </section>

  <LoadingBlock v-if="loading" text="正在读取岗位概览" />

  <section v-else class="page-section">
    <div v-if="errorMessage" class="notice-card">
      {{ errorMessage }}
    </div>

    <div class="metric-grid">
      <MetricCard label="在库岗位" :value="pageInfo.total" note="持续更新的本地招聘信息" />
      <MetricCard label="近期机会" :value="hotJobs.length" note="优先展示最新岗位" />
      <MetricCard label="薪资参考" :value="salaryReadyCount" note="可用于薪资区间比较" />
    </div>
  </section>

  <section class="page-section split-section">
      <div>
        <p class="eyebrow">CITY PICKS</p>
        <h2>按城市快速进入岗位库</h2>
      </div>
    <div class="city-grid">
      <button
        v-for="city in featuredCities"
        :key="city.name"
        class="city-card"
        type="button"
        @click="goSearch(city.name)"
      >
        <strong>{{ city.name }}</strong>
        <span>{{ city.tone }}</span>
      </button>
    </div>
  </section>

  <section class="page-section">
    <div class="section-head">
      <div>
        <p class="eyebrow">NEW CHANCES</p>
        <h2>近期岗位机会</h2>
      </div>
      <button class="ghost-button" type="button" @click="router.push('/jobs')">查看全部</button>
    </div>

    <div class="job-grid">
      <JobCard v-for="job in hotJobs" :key="job.id" :job="job" />
    </div>
  </section>
</template>
