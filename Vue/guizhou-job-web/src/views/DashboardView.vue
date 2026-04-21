<script setup>
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { fetchJobPage } from '@/api/job'
import EmptyState from '@/components/EmptyState.vue'
import JobCard from '@/components/JobCard.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import MetricCard from '@/components/MetricCard.vue'

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const cities = ['贵阳', '安顺', '遵义', '六盘水', '毕节', '铜仁', '黔东南', '黔南', '黔西南']
const salaryOrder = ['3K以下', '3K-5K', '5K-8K', '8K-12K', '12K-15K', '15K以上', '未知']

const loading = ref(true)
const errorMessage = ref('')
const total = ref(0)
const cityStats = ref([])
const jobs = ref([])
const selectedCity = ref('')

const cityChartRef = ref(null)
const citySalaryChartRef = ref(null)
const salaryChartRef = ref(null)
const eduChartRef = ref(null)
const expChartRef = ref(null)
const selectedSalaryChartRef = ref(null)
const selectedEduChartRef = ref(null)
const selectedExpChartRef = ref(null)

let cityChart
let citySalaryChart
let salaryChart
let eduChart
let expChart
let selectedSalaryChart
let selectedEduChart
let selectedExpChart

const activeCities = computed(() => cityStats.value.filter((item) => item.total > 0))
const selectedCityStat = computed(
  () => cityStats.value.find((item) => item.city === selectedCity.value) || cityStats.value[0],
)
const selectedJobs = computed(() => selectedCityStat.value?.jobs || [])

const validSalaryCount = computed(
  () => jobs.value.filter((job) => job.salaryMin != null && job.salaryMax != null).length,
)

const avgSalary = computed(() => calculateAvgSalary(jobs.value))

function calculateAvgSalary(list) {
  const salaryJobs = list.filter((job) => job.salaryMin != null && job.salaryMax != null)
  if (!salaryJobs.length) return 0
  const sum = salaryJobs.reduce((acc, job) => acc + (Number(job.salaryMin) + Number(job.salaryMax)) / 2, 0)
  return Math.round(sum / salaryJobs.length)
}

function countBy(list, field) {
  return list.reduce((acc, job) => {
    const key = job[field] || '未知'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
}

function topLabel(list, field) {
  const top = topEntries(countBy(list, field), 1)[0]
  return top ? top[0] : '暂无'
}

function salaryBucket(job) {
  if (job.salaryMin == null || job.salaryMax == null) return '未知'
  const avg = (Number(job.salaryMin) + Number(job.salaryMax)) / 2
  if (avg < 3000) return '3K以下'
  if (avg < 5000) return '3K-5K'
  if (avg < 8000) return '5K-8K'
  if (avg < 12000) return '8K-12K'
  if (avg < 15000) return '12K-15K'
  return '15K以上'
}

function salaryBucketMap(list) {
  const map = Object.fromEntries(salaryOrder.map((name) => [name, 0]))
  list.forEach((job) => {
    const key = salaryBucket(job)
    map[key] = (map[key] || 0) + 1
  })
  return map
}

function topEntries(map, limit = 8) {
  return Object.entries(map)
    .sort((a, b) => b[1] - a[1])
    .slice(0, limit)
}

function buildCityStat(city, pageData) {
  const list = pageData?.list || []
  return {
    city,
    total: pageData?.total || 0,
    jobs: list,
    sampleCount: list.length,
    salaryReady: list.filter((job) => job.salaryMin != null && job.salaryMax != null).length,
    avgSalary: calculateAvgSalary(list),
    topEducation: topLabel(list, 'educationText'),
    topExperience: topLabel(list, 'experienceText'),
  }
}

function chartTextColor() {
  return '#66766f'
}

function renderCharts() {
  if (!jobs.value.length) return

  const cityNames = cityStats.value.map((item) => item.city)
  const cityTotals = cityStats.value.map((item) => item.total)
  const cityAvgSalary = cityStats.value.map((item) => item.avgSalary || 0)
  const salaryEntries = Object.entries(salaryBucketMap(jobs.value)).filter(([, value]) => value > 0)
  const educationEntries = topEntries(countBy(jobs.value, 'educationText'), 8)
  const experienceEntries = topEntries(countBy(jobs.value, 'experienceText'), 8)
  const selectedSalaryEntries = Object.entries(salaryBucketMap(selectedJobs.value)).filter(([, value]) => value > 0)
  const selectedEducationEntries = topEntries(countBy(selectedJobs.value, 'educationText'), 8)
  const selectedExperienceEntries = topEntries(countBy(selectedJobs.value, 'experienceText'), 8)

  cityChart = cityChart || echarts.init(cityChartRef.value)
  citySalaryChart = citySalaryChart || echarts.init(citySalaryChartRef.value)
  salaryChart = salaryChart || echarts.init(salaryChartRef.value)
  eduChart = eduChart || echarts.init(eduChartRef.value)
  expChart = expChart || echarts.init(expChartRef.value)
  selectedSalaryChart = selectedSalaryChart || echarts.init(selectedSalaryChartRef.value)
  selectedEduChart = selectedEduChart || echarts.init(selectedEduChartRef.value)
  selectedExpChart = selectedExpChart || echarts.init(selectedExpChartRef.value)

  cityChart.setOption({
    tooltip: {},
    grid: { left: 42, right: 18, top: 22, bottom: 42 },
    xAxis: { type: 'category', data: cityNames, axisLabel: { color: chartTextColor() }, axisTick: { show: false } },
    yAxis: { type: 'value', axisLabel: { color: chartTextColor() }, splitLine: { lineStyle: { color: '#e8dfcf' } } },
    series: [
      {
        name: '岗位数量',
        type: 'bar',
        data: cityTotals,
        itemStyle: { color: '#1f8a70', borderRadius: [10, 10, 0, 0] },
      },
    ],
  })

  citySalaryChart.setOption({
    tooltip: {},
    grid: { left: 48, right: 18, top: 28, bottom: 42 },
    xAxis: { type: 'category', data: cityNames, axisLabel: { color: chartTextColor() }, axisTick: { show: false } },
    yAxis: { type: 'value', axisLabel: { color: chartTextColor() }, splitLine: { lineStyle: { color: '#e8dfcf' } } },
    series: [
      {
        name: '参考均薪',
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(31, 138, 112, 0.12)' },
        lineStyle: { width: 4, color: '#d88c48' },
        symbolSize: 9,
        data: cityAvgSalary,
      },
    ],
  })

  salaryChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: chartTextColor() } },
    series: [
      {
        name: '薪资区间',
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '44%'],
        data: salaryEntries.map(([name, value]) => ({ name, value })),
        color: ['#1f8a70', '#9fb84d', '#f2a65a', '#d66b4d', '#476a6f', '#e0c46c', '#9aa6a0'],
      },
    ],
  })

  eduChart.setOption({
    tooltip: {},
    grid: { left: 82, right: 18, top: 18, bottom: 34 },
    xAxis: { type: 'value', axisLabel: { color: chartTextColor() }, splitLine: { lineStyle: { color: '#e8dfcf' } } },
    yAxis: {
      type: 'category',
      data: educationEntries.map(([name]) => name),
      axisLabel: { color: chartTextColor() },
      axisTick: { show: false },
    },
    series: [
      {
        name: '学历要求',
        type: 'bar',
        data: educationEntries.map(([, value]) => value),
        itemStyle: { color: '#d88c48', borderRadius: [0, 10, 10, 0] },
      },
    ],
  })

  expChart.setOption({
    tooltip: {},
    grid: { left: 42, right: 18, top: 22, bottom: 42 },
    xAxis: {
      type: 'category',
      data: experienceEntries.map(([name]) => name),
      axisLabel: { color: chartTextColor(), rotate: 18 },
      axisTick: { show: false },
    },
    yAxis: { type: 'value', axisLabel: { color: chartTextColor() }, splitLine: { lineStyle: { color: '#e8dfcf' } } },
    series: [
      {
        name: '经验要求',
        type: 'bar',
        data: experienceEntries.map(([, value]) => value),
        itemStyle: { color: '#9fb84d', borderRadius: [10, 10, 0, 0] },
      },
    ],
  })

  selectedSalaryChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: chartTextColor() } },
    series: [
      {
        name: `${selectedCity.value}薪资`,
        type: 'pie',
        radius: ['0%', '68%'],
        center: ['50%', '43%'],
        data: selectedSalaryEntries.map(([name, value]) => ({ name, value })),
        color: ['#145f52', '#1f8a70', '#9fb84d', '#f2a65a', '#d66b4d', '#476a6f', '#9aa6a0'],
      },
    ],
  })

  selectedEduChart.setOption({
    tooltip: {},
    grid: { left: 82, right: 18, top: 18, bottom: 34 },
    xAxis: { type: 'value', axisLabel: { color: chartTextColor() }, splitLine: { lineStyle: { color: '#e8dfcf' } } },
    yAxis: {
      type: 'category',
      data: selectedEducationEntries.map(([name]) => name),
      axisLabel: { color: chartTextColor() },
      axisTick: { show: false },
    },
    series: [
      {
        name: `${selectedCity.value}学历`,
        type: 'bar',
        data: selectedEducationEntries.map(([, value]) => value),
        itemStyle: { color: '#d88c48', borderRadius: [0, 10, 10, 0] },
      },
    ],
  })

  selectedExpChart.setOption({
    tooltip: {},
    grid: { left: 42, right: 18, top: 22, bottom: 42 },
    xAxis: {
      type: 'category',
      data: selectedExperienceEntries.map(([name]) => name),
      axisLabel: { color: chartTextColor(), rotate: 18 },
      axisTick: { show: false },
    },
    yAxis: { type: 'value', axisLabel: { color: chartTextColor() }, splitLine: { lineStyle: { color: '#e8dfcf' } } },
    series: [
      {
        name: `${selectedCity.value}经验`,
        type: 'bar',
        data: selectedExperienceEntries.map(([, value]) => value),
        itemStyle: { color: '#9fb84d', borderRadius: [10, 10, 0, 0] },
      },
    ],
  })
}

function chooseCity(city) {
  selectedCity.value = city
  nextTick(renderCharts)
}

function resizeCharts() {
  cityChart?.resize()
  citySalaryChart?.resize()
  salaryChart?.resize()
  eduChart?.resize()
  expChart?.resize()
  selectedSalaryChart?.resize()
  selectedEduChart?.resize()
  selectedExpChart?.resize()
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [overview, overviewSample, cityResults] = await Promise.all([
      fetchJobPage({ pageNum: 1, pageSize: 1 }),
      fetchJobPage({ pageNum: 1, pageSize: 50 }),
      Promise.all(
        cities.map((city) =>
          fetchJobPage({ pageNum: 1, pageSize: 50, city }).catch(() => ({ total: 0, list: [] })),
        ),
      ),
    ])

    total.value = overview?.total || 0
    cityStats.value = cities.map((city, index) => buildCityStat(city, cityResults[index]))
    jobs.value = cityStats.value.flatMap((item) => item.jobs)
    if (!jobs.value.length && overviewSample?.list?.length) {
      jobs.value = overviewSample.list
    }
    selectedCity.value = activeCities.value[0]?.city || cityStats.value[0]?.city || ''
    loading.value = false
    await nextTick()
    renderCharts()
  } catch (error) {
    errorMessage.value = error.message
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  cityChart?.dispose()
  citySalaryChart?.dispose()
  salaryChart?.dispose()
  eduChart?.dispose()
  expChart?.dispose()
  selectedSalaryChart?.dispose()
  selectedEduChart?.dispose()
  selectedExpChart?.dispose()
})
</script>

<template>
  <section class="page-title page-section">
    <p class="eyebrow">ANALYTICS</p>
    <h1>数据看板</h1>
    <p>从城市、薪资、学历和经验多个角度观察贵州岗位趋势，为求职方向提供参考。</p>
  </section>

  <LoadingBlock v-if="loading" text="正在生成岗位看板" />

  <section v-else class="page-section dashboard-page">
    <div v-if="errorMessage" class="notice-card">{{ errorMessage }}</div>
    <EmptyState v-else-if="jobs.length === 0" title="暂无看板数据" description="当前还没有可用于展示的岗位信息。" />

    <template v-else>
      <div class="metric-grid">
        <MetricCard label="岗位总量" :value="total" note="当前可浏览岗位" />
        <MetricCard label="覆盖城市" :value="activeCities.length" note="存在岗位数据的城市" />
        <MetricCard label="分析岗位" :value="jobs.length" note="参与趋势计算的岗位" />
        <MetricCard label="参考均薪" :value="avgSalary ? `${avgSalary}元/月` : '未知'" note="按薪资区间均值估算" />
      </div>

      <div class="chart-grid dashboard-chart-grid">
        <article class="chart-card chart-card--wide">
          <h2>各城市岗位总量</h2>
          <div ref="cityChartRef" class="chart-box"></div>
        </article>
        <article class="chart-card">
          <h2>各城市参考均薪</h2>
          <div ref="citySalaryChartRef" class="chart-box"></div>
        </article>
        <article class="chart-card">
          <h2>全省薪资区间</h2>
          <div ref="salaryChartRef" class="chart-box"></div>
        </article>
        <article class="chart-card">
          <h2>学历要求</h2>
          <div ref="eduChartRef" class="chart-box"></div>
        </article>
        <article class="chart-card">
          <h2>经验要求</h2>
          <div ref="expChartRef" class="chart-box"></div>
        </article>
      </div>

      <section class="city-data-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">CITY DATA</p>
            <h2>各城市独立数据</h2>
          </div>
          <span>点击城市卡片切换下方图表</span>
        </div>

        <div class="city-stat-grid">
          <button
            v-for="item in cityStats"
            :key="item.city"
            class="city-stat-card"
            :class="{ active: selectedCity === item.city }"
            type="button"
            @click="chooseCity(item.city)"
          >
            <strong>{{ item.city }}</strong>
            <span>{{ item.total }} 个岗位</span>
            <small>参考均薪：{{ item.avgSalary ? `${item.avgSalary}元/月` : '未知' }}</small>
            <small>学历集中：{{ item.topEducation }}</small>
            <small>经验集中：{{ item.topExperience }}</small>
          </button>
        </div>

        <div class="selected-city-grid selected-city-grid--charts">
          <article class="chart-card">
            <h2>{{ selectedCity }}薪资结构</h2>
            <div ref="selectedSalaryChartRef" class="chart-box"></div>
          </article>
          <article class="chart-card">
            <h2>{{ selectedCity }}学历分布</h2>
            <div ref="selectedEduChartRef" class="chart-box"></div>
          </article>
          <article class="chart-card">
            <h2>{{ selectedCity }}经验分布</h2>
            <div ref="selectedExpChartRef" class="chart-box"></div>
          </article>
          <article class="city-summary-panel">
            <p class="eyebrow">SELECTED CITY</p>
            <h2>{{ selectedCity }}</h2>
            <div class="summary-lines">
              <span>岗位总量：{{ selectedCityStat?.total || 0 }}</span>
              <span>参考岗位：{{ selectedCityStat?.sampleCount || 0 }}</span>
              <span>可算薪资：{{ selectedCityStat?.salaryReady || 0 }}</span>
              <span>参考均薪：{{ selectedCityStat?.avgSalary ? `${selectedCityStat.avgSalary}元/月` : '未知' }}</span>
            </div>
          </article>
        </div>
      </section>

      <section class="page-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">CITY JOBS</p>
            <h2>{{ selectedCity }}近期岗位</h2>
          </div>
        </div>
        <div class="job-grid">
          <JobCard v-for="job in selectedJobs.slice(0, 6)" :key="job.id" :job="job" />
        </div>
      </section>
    </template>
  </section>
</template>
