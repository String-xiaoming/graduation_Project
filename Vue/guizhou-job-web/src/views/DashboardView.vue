<script setup>
import { BarChart, BoxplotChart, HeatmapChart, PieChart, ScatterChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAnalysisDashboard } from '@/api/analysis'
import EmptyState from '@/components/EmptyState.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import MetricCard from '@/components/MetricCard.vue'

echarts.use([
  BarChart,
  BoxplotChart,
  HeatmapChart,
  PieChart,
  ScatterChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
  CanvasRenderer,
])

const loading = ref(true)
const errorMessage = ref('')
const dashboard = ref(null)

const cityJobRef = ref(null)
const salaryBucketRef = ref(null)
const salaryBoxRef = ref(null)
const heatmapRef = ref(null)
const categoryPieRef = ref(null)
const categorySalaryRef = ref(null)
const skillBubbleRef = ref(null)

let cityJobChart
let salaryBucketChart
let salaryBoxChart
let heatmapChart
let categoryPieChart
let categorySalaryChart
let skillBubbleChart

const overview = computed(() => dashboard.value?.overview || {})
const cityJobs = computed(() => dashboard.value?.cityJobDistribution || [])
const salaryBuckets = computed(() => dashboard.value?.salaryBuckets || [])
const salaryBoxes = computed(() => dashboard.value?.citySalaryBoxes || [])
const heatmap = computed(() => dashboard.value?.educationExperienceHeatmap || [])
const categories = computed(() => dashboard.value?.categoryDistribution || [])
const skills = computed(() => dashboard.value?.skillKeywords || [])

const topSalaryBucket = computed(() => {
  return salaryBuckets.value.reduce((top, item) => (!top || item.value > top.value ? item : top), null)
})

const topCities = computed(() => cityJobs.value.slice(0, 3))

const analysisInsights = computed(() => {
  const items = []
  if (overview.value.topCity) {
    items.push(`${overview.value.topCity}岗位供给最集中，是当前贵州招聘市场的主要机会城市。`)
  }
  if (topSalaryBucket.value?.name) {
    items.push(`${topSalaryBucket.value.name}是当前样本中覆盖最多的薪资区间，市场薪资主要聚集在中位区间。`)
  }
  if (overview.value.topCategory) {
    items.push(`${overview.value.topCategory}岗位占比最高，说明该类岗位需求更旺盛。`)
  }
  if (skills.value[0]?.keyword) {
    items.push(`技能关键词中“${skills.value[0].keyword}”权重最高，可作为岗位能力需求的重点观察项。`)
  }
  return items
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatMoney(value) {
  return `¥${formatNumber(value)}/月`
}

function chartTextColor() {
  return '#66766f'
}

function chartLineColor() {
  return '#e8dfcf'
}

function renderCharts() {
  if (!dashboard.value) return

  cityJobChart = cityJobChart || echarts.init(cityJobRef.value)
  salaryBucketChart = salaryBucketChart || echarts.init(salaryBucketRef.value)
  salaryBoxChart = salaryBoxChart || echarts.init(salaryBoxRef.value)
  heatmapChart = heatmapChart || echarts.init(heatmapRef.value)
  categoryPieChart = categoryPieChart || echarts.init(categoryPieRef.value)
  categorySalaryChart = categorySalaryChart || echarts.init(categorySalaryRef.value)
  skillBubbleChart = skillBubbleChart || echarts.init(skillBubbleRef.value)

  cityJobChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 56, right: 18, top: 20, bottom: 34 },
    xAxis: {
      type: 'category',
      data: cityJobs.value.map((item) => item.name),
      axisLabel: { color: chartTextColor() },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: chartTextColor() },
      splitLine: { lineStyle: { color: chartLineColor() } },
    },
    series: [
      {
        name: '岗位数量',
        type: 'bar',
        data: cityJobs.value.map((item) => item.value),
        itemStyle: { color: '#145f52', borderRadius: [8, 8, 0, 0] },
      },
    ],
  })

  salaryBucketChart.setOption({
    tooltip: {},
    grid: { left: 54, right: 18, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: salaryBuckets.value.map((item) => item.name),
      axisLabel: { color: chartTextColor(), rotate: 12 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: chartTextColor() },
      splitLine: { lineStyle: { color: chartLineColor() } },
    },
    series: [
      {
        name: '岗位数量',
        type: 'bar',
        data: salaryBuckets.value.map((item) => item.value),
        itemStyle: { color: '#d88c48', borderRadius: [8, 8, 0, 0] },
      },
    ],
  })

  salaryBoxChart.setOption({
    tooltip: { trigger: 'item' },
    grid: { left: 58, right: 22, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: salaryBoxes.value.map((item) => item.city),
      axisLabel: { color: chartTextColor() },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: chartTextColor() },
      splitLine: { lineStyle: { color: chartLineColor() } },
    },
    series: [
      {
        name: '城市薪资分布',
        type: 'boxplot',
        data: salaryBoxes.value.map((item) => [item.min, item.q1, item.median, item.q3, item.max]),
        itemStyle: { color: '#fffdf7', borderColor: '#1f8a70' },
      },
    ],
  })

  const xLabels = Array.from(new Set(heatmap.value.map((item) => item.x)))
  const yLabels = Array.from(new Set(heatmap.value.map((item) => item.y)))
  const maxHeat = Math.max(...heatmap.value.map((item) => item.value), 1)
  heatmapChart.setOption({
    tooltip: {
      formatter(params) {
        return `${xLabels[params.data[0]]} / ${yLabels[params.data[1]]}<br/>岗位数量：${params.data[2]}`
      },
    },
    grid: { left: 88, right: 24, top: 24, bottom: 48 },
    xAxis: { type: 'category', data: xLabels, axisLabel: { color: chartTextColor(), rotate: 16 } },
    yAxis: { type: 'category', data: yLabels, axisLabel: { color: chartTextColor() } },
    visualMap: {
      min: 0,
      max: maxHeat,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: { color: ['#f6ead7', '#1f8a70'] },
    },
    series: [
      {
        type: 'heatmap',
        data: heatmap.value.map((item) => [xLabels.indexOf(item.x), yLabels.indexOf(item.y), item.value]),
        emphasis: { itemStyle: { borderColor: '#145f52', borderWidth: 1 } },
      },
    ],
  })

  categoryPieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: chartTextColor() } },
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '42%'],
        label: { show: false },
        data: categories.value.map((item) => ({ name: item.name, value: item.value })),
        color: ['#145f52', '#1f8a70', '#9fb84d', '#d88c48', '#d66b4d', '#476a6f', '#e0c46c', '#7ca982', '#b7835f', '#9aa6a0'],
      },
    ],
  })

  categorySalaryChart.setOption({
    tooltip: {},
    grid: { left: 88, right: 18, top: 18, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLabel: { color: chartTextColor() },
      splitLine: { lineStyle: { color: chartLineColor() } },
    },
    yAxis: {
      type: 'category',
      data: categories.value.map((item) => item.name).reverse(),
      axisLabel: { color: chartTextColor() },
    },
    series: [
      {
        type: 'bar',
        data: categories.value.map((item) => item.avgSalary || 0).reverse(),
        itemStyle: { color: '#1f8a70', borderRadius: [0, 8, 8, 0] },
      },
    ],
  })

  skillBubbleChart.setOption({
    tooltip: {
      formatter(params) {
        return `${params.data[2]}<br/>TF-IDF：${params.data[0]}<br/>关联岗位：${params.data[1]}`
      },
    },
    grid: { left: 62, right: 24, top: 24, bottom: 42 },
    xAxis: {
      name: 'TF-IDF',
      type: 'value',
      axisLabel: { color: chartTextColor() },
      splitLine: { lineStyle: { color: chartLineColor() } },
    },
    yAxis: {
      name: '岗位数',
      type: 'value',
      axisLabel: { color: chartTextColor() },
      splitLine: { lineStyle: { color: chartLineColor() } },
    },
    series: [
      {
        type: 'scatter',
        data: skills.value.map((item) => [
          Number(item.tfidfScore || 0),
          Number(item.jobCount || 0),
          item.keyword,
        ]),
        symbolSize(data) {
          return Math.max(12, Math.min(44, Math.sqrt(data[1]) * 2.35))
        },
        itemStyle: { color: '#1f8a70', opacity: 0.78 },
      },
    ],
  })
}

function resizeCharts() {
  cityJobChart?.resize()
  salaryBucketChart?.resize()
  salaryBoxChart?.resize()
  heatmapChart?.resize()
  categoryPieChart?.resize()
  categorySalaryChart?.resize()
  skillBubbleChart?.resize()
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await fetchAnalysisDashboard()
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
  cityJobChart?.dispose()
  salaryBucketChart?.dispose()
  salaryBoxChart?.dispose()
  heatmapChart?.dispose()
  categoryPieChart?.dispose()
  categorySalaryChart?.dispose()
  skillBubbleChart?.dispose()
})
</script>

<template>
  <section class="page-title page-section dashboard-title">
    <p class="eyebrow">ANALYTICS</p>
    <h1>数据看板</h1>
    <p>从城市机会、薪资结构、岗位类型、学历经验门槛与技能关键词观察贵州招聘市场。</p>
  </section>

  <LoadingBlock v-if="loading" text="正在生成分析看板" />

  <section v-else class="page-section dashboard-page dashboard-page--analysis">
    <div v-if="errorMessage" class="notice-card">{{ errorMessage }}</div>
    <EmptyState v-else-if="!dashboard" title="暂无看板数据" description="当前还没有可用于展示的分析数据。" />

    <template v-else>
      <section class="dashboard-overview">
        <div class="dashboard-overview__copy">
          <p class="eyebrow">MARKET SNAPSHOT</p>
          <h2>贵州岗位市场分析</h2>
          <p>当前样本显示，{{ overview.topCity }}是岗位供给最集中的城市，{{ topSalaryBucket?.name || '主流薪资区间' }}覆盖岗位最多。</p>
          <div class="dashboard-top-cities">
            <span v-for="item in topCities" :key="item.name">{{ item.name }} {{ formatNumber(item.value) }}</span>
          </div>
        </div>
        <RouterLink class="dashboard-screen-entry" to="/analysis/screen" aria-label="打开贵州就业分析大屏">
          <span class="screen-entry__kicker">贵州就业分析大屏</span>
          <strong>进入大屏</strong>
          <small>地图联动城市、薪资、岗位类型与技能热点</small>
          <b class="screen-entry__action">查看地图联动 <i aria-hidden="true">→</i></b>
        </RouterLink>
      </section>

      <div class="metric-grid dashboard-metric-grid">
        <MetricCard label="平均薪资" :value="formatMoney(overview.avgSalary)" note="基于可解析薪资岗位" />
        <MetricCard label="薪资中位数" :value="formatMoney(overview.medianSalary)" note="更能反映主流薪资水平" />
        <MetricCard label="岗位集中城市" :value="overview.topCity || '-'" note="按岗位供给数量排序" />
        <MetricCard label="热门岗位类型" :value="overview.topCategory || '-'" note="按岗位分类规则统计" />
      </div>

      <div class="dashboard-bento">
        <article class="chart-card dashboard-chart-card dashboard-chart-card--large">
          <div class="chart-card__head">
            <span>CITY OPPORTUNITY</span>
            <h2>各城市岗位供给分布</h2>
          </div>
          <div ref="cityJobRef" class="chart-box chart-box--compact"></div>
        </article>

        <article class="chart-card dashboard-chart-card">
          <div class="chart-card__head">
            <span>JOB TYPE</span>
            <h2>岗位类型占比</h2>
          </div>
          <div ref="categoryPieRef" class="chart-box chart-box--compact"></div>
        </article>

        <article class="chart-card dashboard-chart-card">
          <div class="chart-card__head">
            <span>SALARY</span>
            <h2>薪资区间分布</h2>
          </div>
          <div ref="salaryBucketRef" class="chart-box chart-box--compact"></div>
        </article>

        <article class="chart-card dashboard-chart-card">
          <div class="chart-card__head">
            <span>CATEGORY SALARY</span>
            <h2>岗位类型平均薪资</h2>
          </div>
          <div ref="categorySalaryRef" class="chart-box chart-box--compact"></div>
        </article>

        <article class="chart-card dashboard-chart-card dashboard-chart-card--wide">
          <div class="chart-card__head">
            <span>CITY SALARY</span>
            <h2>城市薪资箱线图</h2>
          </div>
          <div ref="salaryBoxRef" class="chart-box chart-box--middle"></div>
        </article>

        <article class="chart-card dashboard-chart-card dashboard-chart-card--wide">
          <div class="chart-card__head">
            <span>EDUCATION × EXPERIENCE</span>
            <h2>学历经验门槛分析</h2>
          </div>
          <div ref="heatmapRef" class="chart-box chart-box--middle"></div>
        </article>

        <article class="chart-card dashboard-chart-card dashboard-chart-card--wide">
          <div class="chart-card__head">
            <span>SKILL DEMAND</span>
            <h2>岗位技能需求热点分析</h2>
          </div>
          <div ref="skillBubbleRef" class="chart-box chart-box--middle"></div>
        </article>

        <section class="dashboard-insights">
          <div class="section-head">
            <div>
              <p class="eyebrow">INSIGHTS</p>
              <h2>综合分析结论</h2>
            </div>
          </div>
          <div class="insight-grid">
            <article v-for="item in analysisInsights" :key="item" class="insight-card">
              {{ item }}
            </article>
          </div>
        </section>
      </div>
    </template>
  </section>
</template>
