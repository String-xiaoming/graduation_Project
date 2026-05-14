<script setup>
import { BarChart, MapChart, PieChart, ScatterChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { fetchAnalysisDashboard } from '@/api/analysis'
import guizhouGeoJson from '@/assets/geo/guizhou.json'
import EmptyState from '@/components/EmptyState.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'

echarts.use([
  BarChart,
  MapChart,
  PieChart,
  ScatterChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
  CanvasRenderer,
])

echarts.registerMap('guizhou', guizhouGeoJson)

const loading = ref(true)
const errorMessage = ref('')
const dashboard = ref(null)
const selectedCity = ref('贵阳')

const mapRef = ref(null)
const cityRankRef = ref(null)
const salaryRef = ref(null)
const categoryRef = ref(null)
const skillRef = ref(null)

let mapChart
let cityRankChart
let salaryChart
let categoryChart
let skillChart

const geoNameToCity = {
  贵阳市: '贵阳',
  六盘水市: '六盘水',
  遵义市: '遵义',
  安顺市: '安顺',
  毕节市: '毕节',
  铜仁市: '铜仁',
  黔西南布依族苗族自治州: '黔西南',
  黔东南苗族侗族自治州: '黔东南',
  黔南布依族苗族自治州: '黔南',
}

const cityToGeoName = Object.fromEntries(Object.entries(geoNameToCity).map(([geoName, city]) => [city, geoName]))

const overview = computed(() => dashboard.value?.overview || {})
const cityJobs = computed(() => dashboard.value?.cityJobDistribution || [])
const salaryBuckets = computed(() => dashboard.value?.salaryBuckets || [])
const salaryBoxes = computed(() => dashboard.value?.citySalaryBoxes || [])
const categories = computed(() => dashboard.value?.categoryDistribution || [])
const skills = computed(() => dashboard.value?.skillKeywords || [])

const selectedCityJobs = computed(() => cityJobs.value.find((item) => item.name === selectedCity.value)?.value || 0)
const selectedSalary = computed(() => salaryBoxes.value.find((item) => item.city === selectedCity.value) || null)
const maxCityJobs = computed(() => Math.max(...cityJobs.value.map((item) => Number(item.value || 0)), 1))

const topCities = computed(() => cityJobs.value.slice(0, 5))
const topCategory = computed(() => categories.value[0])
const topSkill = computed(() => skills.value[0])
const topSalaryBucket = computed(() => salaryBuckets.value.reduce((top, item) => (!top || item.value > top.value ? item : top), null))

const mapData = computed(() => {
  return guizhouGeoJson.features.map((feature) => {
    const city = geoNameToCity[feature.properties.name]
    const jobCount = cityJobs.value.find((item) => item.name === city)?.value || 0
    const salary = salaryBoxes.value.find((item) => item.city === city)
    return {
      name: feature.properties.name,
      value: jobCount,
      city,
      medianSalary: salary?.median || 0,
    }
  })
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatMoney(value) {
  return `¥${formatNumber(value)}/月`
}

function percentOfMax(value) {
  return `${Math.max(8, Math.round((Number(value || 0) / maxCityJobs.value) * 100))}%`
}

function renderCharts() {
  if (!dashboard.value) return

  mapChart = mapChart || echarts.init(mapRef.value)
  cityRankChart = cityRankChart || echarts.init(cityRankRef.value)
  salaryChart = salaryChart || echarts.init(salaryRef.value)
  categoryChart = categoryChart || echarts.init(categoryRef.value)
  skillChart = skillChart || echarts.init(skillRef.value)

  const selectedGeoName = cityToGeoName[selectedCity.value]

  mapChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter(params) {
        const city = params.data?.city || params.name
        return `${city}<br/>岗位数量：${formatNumber(params.value)}<br/>薪资中位数：${formatMoney(params.data?.medianSalary)}`
      },
    },
    visualMap: {
      min: 0,
      max: maxCityJobs.value,
      show: false,
      inRange: { color: ['#21433b', '#2f806d', '#d8a64b'] },
    },
    series: [
      {
        type: 'map',
        map: 'guizhou',
        roam: false,
        selectedMode: 'single',
        data: mapData.value,
        zoom: 1.08,
        label: {
          show: true,
          color: '#e7fff4',
          fontWeight: 800,
          formatter(params) {
            return params.data?.city || ''
          },
        },
        emphasis: {
          label: { color: '#fff7d6' },
          itemStyle: { areaColor: '#e6b85d', borderColor: '#fff7d6', borderWidth: 2 },
        },
        select: {
          itemStyle: { areaColor: '#f0c96a', borderColor: '#fff8dc', borderWidth: 2 },
          label: { color: '#17362f' },
        },
        itemStyle: {
          borderColor: 'rgba(207, 246, 226, 0.72)',
          borderWidth: 1.2,
          areaColor: '#2f806d',
        },
      },
    ],
  })
  mapChart.dispatchAction({ type: 'select', seriesIndex: 0, name: selectedGeoName })
  mapChart.off('click')
  mapChart.on('click', (params) => {
    const city = geoNameToCity[params.name]
    if (!city) return
    selectedCity.value = city
    renderCharts()
  })

  cityRankChart.setOption({
    tooltip: {},
    grid: { left: 48, right: 12, top: 12, bottom: 26 },
    xAxis: { type: 'value', axisLabel: { color: '#92b7a9' }, splitLine: { lineStyle: { color: 'rgba(168, 218, 197, 0.12)' } } },
    yAxis: {
      type: 'category',
      data: cityJobs.value.map((item) => item.name).reverse(),
      axisLabel: { color: '#d9f6e9' },
    },
    series: [
      {
        type: 'bar',
        data: cityJobs.value.map((item) => item.value).reverse(),
        itemStyle: { color: '#47c09b', borderRadius: [0, 8, 8, 0] },
      },
    ],
  })

  salaryChart.setOption({
    tooltip: {},
    grid: { left: 40, right: 12, top: 12, bottom: 34 },
    xAxis: {
      type: 'category',
      data: salaryBuckets.value.map((item) => item.name),
      axisLabel: { color: '#92b7a9', rotate: 12 },
    },
    yAxis: { type: 'value', axisLabel: { color: '#92b7a9' }, splitLine: { lineStyle: { color: 'rgba(168, 218, 197, 0.12)' } } },
    series: [
      {
        type: 'bar',
        data: salaryBuckets.value.map((item) => item.value),
        itemStyle: { color: '#d8a64b', borderRadius: [8, 8, 0, 0] },
      },
    ],
  })

  categoryChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#b9d7cb' } },
    series: [
      {
        type: 'pie',
        radius: ['44%', '70%'],
        center: ['50%', '43%'],
        data: categories.value.slice(0, 8).map((item) => ({ name: item.name, value: item.value })),
        color: ['#47c09b', '#d8a64b', '#7cb36b', '#e06f4f', '#8ab9d6', '#d0bf68', '#c48b68', '#7aa29b'],
      },
    ],
  })

  skillChart.setOption({
    tooltip: {
      formatter(params) {
        return `${params.data[2]}<br/>TF-IDF：${params.data[0]}<br/>关联岗位：${params.data[1]}`
      },
    },
    grid: { left: 52, right: 16, top: 18, bottom: 34 },
    xAxis: { name: 'TF-IDF', type: 'value', axisLabel: { color: '#92b7a9' }, splitLine: { lineStyle: { color: 'rgba(168, 218, 197, 0.12)' } } },
    yAxis: { name: '岗位数', type: 'value', axisLabel: { color: '#92b7a9' }, splitLine: { lineStyle: { color: 'rgba(168, 218, 197, 0.12)' } } },
    series: [
      {
        type: 'scatter',
        data: skills.value.map((item) => [Number(item.tfidfScore || 0), Number(item.jobCount || 0), item.keyword]),
        symbolSize(data) {
          return Math.max(12, Math.min(44, Math.sqrt(data[1]) * 2.3))
        },
        itemStyle: { color: '#47c09b', opacity: 0.82 },
      },
    ],
  })
}

function resizeCharts() {
  mapChart?.resize()
  cityRankChart?.resize()
  salaryChart?.resize()
  categoryChart?.resize()
  skillChart?.resize()
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await fetchAnalysisDashboard()
    selectedCity.value = overview.value.topCity || cityJobs.value[0]?.name || '贵阳'
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
  mapChart?.dispose()
  cityRankChart?.dispose()
  salaryChart?.dispose()
  categoryChart?.dispose()
  skillChart?.dispose()
})
</script>

<template>
  <LoadingBlock v-if="loading" text="正在加载贵州就业分析大屏" />

  <section v-else class="screen-page">
    <div v-if="errorMessage" class="notice-card">{{ errorMessage }}</div>
    <EmptyState v-else-if="!dashboard" title="暂无大屏数据" description="当前还没有可用于展示的分析数据。" />

    <template v-else>
      <header class="screen-header">
        <div>
          <p class="screen-kicker">GUIZHOU EMPLOYMENT INTELLIGENCE</p>
          <h1>贵州就业分析大屏</h1>
        </div>
        <div class="screen-clock">
          <span>{{ overview.minPublishTime }} - {{ overview.maxPublishTime }}</span>
          <strong>岗位市场分析</strong>
        </div>
      </header>

      <div class="screen-layout">
        <aside class="screen-panel screen-panel--left">
          <div class="screen-metric">
            <span>平均薪资</span>
            <strong>{{ formatMoney(overview.avgSalary) }}</strong>
          </div>
          <div class="screen-metric">
            <span>薪资中位数</span>
            <strong>{{ formatMoney(overview.medianSalary) }}</strong>
          </div>
          <div class="screen-metric">
            <span>岗位集中城市</span>
            <strong>{{ overview.topCity || '-' }}</strong>
          </div>
          <div class="screen-metric">
            <span>热门岗位类型</span>
            <strong>{{ overview.topCategory || '-' }}</strong>
          </div>

          <article class="screen-card">
            <h2>城市机会排行</h2>
            <div ref="cityRankRef" class="screen-chart screen-chart--rank"></div>
          </article>
        </aside>

        <main class="screen-map-stage">
          <div class="screen-map-shell">
            <div ref="mapRef" class="screen-map"></div>
            <div class="screen-selected-city">
              <span>当前城市</span>
              <strong>{{ selectedCity }}</strong>
              <small>岗位 {{ formatNumber(selectedCityJobs) }} 个</small>
              <small>中位薪资 {{ selectedSalary ? formatMoney(selectedSalary.median) : '暂无' }}</small>
            </div>
          </div>

          <div class="screen-city-strip">
            <button
              v-for="item in topCities"
              :key="item.name"
              type="button"
              :class="{ active: selectedCity === item.name }"
              @click="selectedCity = item.name; renderCharts()"
            >
              <span>{{ item.name }}</span>
              <strong>{{ formatNumber(item.value) }}</strong>
              <i :style="{ width: percentOfMax(item.value) }"></i>
            </button>
          </div>
        </main>

        <aside class="screen-panel screen-panel--right">
          <article class="screen-card">
            <h2>岗位类型结构</h2>
            <div ref="categoryRef" class="screen-chart"></div>
          </article>

          <article class="screen-card screen-card--insight">
            <h2>分析结论</h2>
            <p>{{ overview.topCity }}岗位供给最高，是当前样本中的主要就业承载城市。</p>
            <p>{{ topSalaryBucket?.name || '主流薪资区间' }}覆盖岗位最多，市场薪资分布集中度较高。</p>
            <p>{{ topCategory?.name || '热门岗位类型' }}需求较强，{{ topSkill?.keyword || '核心技能' }}是值得重点关注的技能关键词。</p>
          </article>
        </aside>
      </div>

      <div class="screen-bottom-grid">
        <article class="screen-card">
          <h2>薪资区间分布</h2>
          <div ref="salaryRef" class="screen-chart"></div>
        </article>
        <article class="screen-card">
          <h2>技能关键词热度</h2>
          <div ref="skillRef" class="screen-chart"></div>
        </article>
      </div>
    </template>
  </section>
</template>
