<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHotJobs } from '@/api/job'
import JobCard from '@/components/JobCard.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'

const router = useRouter()

const loading = ref(true)
const errorMessage = ref('')
const searchKeyword = ref('')
const hotJobs = ref([])

const featuredCities = [
  { name: '贵阳', tone: '省会岗位集中，互联网、销售、财务与服务岗位丰富' },
  { name: '遵义', tone: '制造、教育、医疗、销售与本地服务机会较多' },
  { name: '安顺', tone: '文旅服务、装备制造、行政与基层岗位活跃' },
  { name: '六盘水', tone: '能源、工程、物流、生产制造岗位更突出' },
  { name: '毕节', tone: '教育、销售、运营、生产与服务岗位覆盖广' },
  { name: '铜仁', tone: '文旅、零售、医疗、教育与县域服务机会稳定' },
  { name: '黔东南', tone: '旅游服务、教育培训、销售与行政岗位选择多' },
  { name: '黔南', tone: '制造、工程、医药、职能与本地服务岗位均衡' },
  { name: '黔西南', tone: '能源、工程、销售、餐饮与基层服务机会突出' },
]

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
    const hot = await fetchHotJobs(6)
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
  <section class="hero hero--home page-section">
    <div class="hero__content home-hero__content">
      <p class="eyebrow">贵州就业助手</p>
      <h1>更快找到适合你的贵州本地岗位。</h1>
      <p class="hero__lead">
        聚合贵州九个城市的招聘信息，按岗位、公司、技能和城市快速筛选，让求职入口更直接。
      </p>

      <div class="hero-search home-search">
        <input
          v-model.trim="searchKeyword"
          type="search"
          placeholder="输入岗位、公司或技能关键词，例如 Java、会计、销售"
          @keyup.enter="goSearch()"
        />
        <button type="button" @click="goSearch()">搜索岗位</button>
      </div>

      <div class="home-hero__quick">
        <button class="ghost-button" type="button" @click="router.push('/jobs')">浏览全部岗位</button>
        <button class="ghost-button" type="button" @click="router.push('/dashboard')">查看数据看板</button>
      </div>
    </div>
  </section>

  <section class="page-section city-entry-section">
    <div class="section-head city-entry-head">
      <div>
        <p class="eyebrow">CITY PICKS</p>
        <h2>按城市快速进入岗位库</h2>
        <p>覆盖贵州 9 个城市，选择城市后直接进入对应岗位列表。</p>
      </div>
      <button class="ghost-button" type="button" @click="router.push('/jobs')">全部城市</button>
    </div>

    <div class="city-grid city-grid--home">
      <button
        v-for="city in featuredCities"
        :key="city.name"
        class="city-card city-card--home"
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

    <LoadingBlock v-if="loading" text="正在读取近期岗位" />

    <div v-else-if="errorMessage" class="notice-card">
      {{ errorMessage }}
    </div>

    <div v-else class="job-grid">
      <JobCard v-for="job in hotJobs" :key="job.id" :job="job" />
    </div>
  </section>
</template>
