<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchJobDetail } from '@/api/job'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { descriptionLines, formatDate, formatSalary } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const errorMessage = ref('')
const job = ref(null)
const descriptionExpanded = ref(false)

const lines = computed(() => descriptionLines(job.value?.jobDescription))
const descriptionTextLength = computed(() => lines.value.join('').length)
const isLongDescription = computed(() => lines.value.length > 8 || descriptionTextLength.value > 520)
const visibleLines = computed(() => {
  if (descriptionExpanded.value || !isLongDescription.value) {
    return lines.value
  }

  const result = []
  let totalLength = 0
  for (const line of lines.value) {
    if (result.length >= 8 || totalLength >= 520) {
      break
    }
    result.push(line)
    totalLength += line.length
  }
  return result
})

const overviewItems = computed(() => [
  { label: '城市', value: job.value?.city || '未知' },
  { label: '学历', value: job.value?.educationText || '不限' },
  { label: '经验', value: job.value?.experienceText || '不限' },
  { label: '发布', value: formatDate(job.value?.publishTime) },
])

async function loadDetail() {
  loading.value = true
  errorMessage.value = ''
  descriptionExpanded.value = false
  try {
    job.value = await fetchJobDetail(route.params.id)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <LoadingBlock v-if="loading" text="正在读取岗位详情" />

  <section v-else-if="errorMessage" class="page-section">
    <div class="notice-card">{{ errorMessage }}</div>
    <button class="ghost-button" type="button" @click="router.push('/jobs')">返回岗位库</button>
  </section>

  <section v-else-if="job" class="page-section detail-page">
    <div class="detail-back-row">
      <button class="text-button" type="button" @click="router.back()">返回上一页</button>
    </div>

    <article class="detail-hero">
      <div>
        <p class="eyebrow">JOB DETAIL</p>
        <h1>{{ job.jobTitle }}</h1>
        <p>{{ job.companyName }}</p>
        <div class="detail-hero__tags">
          <span>{{ job.city || '城市未知' }}</span>
          <span>{{ job.educationText || '学历不限' }}</span>
          <span>{{ job.experienceText || '经验不限' }}</span>
        </div>
      </div>
      <strong>{{ formatSalary(job) }}</strong>
    </article>

    <section class="detail-summary-strip" aria-label="岗位概览">
      <div v-for="item in overviewItems" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
      <div class="detail-summary-strip__address">
        <span>地址</span>
        <strong>{{ job.workAddress || '待确认' }}</strong>
      </div>
    </section>

    <article class="description-card description-card--foldable">
      <div class="description-card__head">
        <div>
          <p class="eyebrow">DESCRIPTION</p>
          <h2>岗位描述</h2>
        </div>
        <span>{{ lines.length || 0 }} 段内容</span>
      </div>

      <div
        class="description-content"
        :class="{ collapsed: isLongDescription && !descriptionExpanded }"
      >
        <template v-if="visibleLines.length">
          <p v-for="(line, index) in visibleLines" :key="index">{{ line }}</p>
        </template>
        <p v-else class="description-empty">该岗位暂未提供详细描述。</p>
      </div>

      <div v-if="isLongDescription" class="description-actions">
        <button class="ghost-button" type="button" @click="descriptionExpanded = !descriptionExpanded">
          {{ descriptionExpanded ? '收起描述' : '展开全文' }}
        </button>
      </div>
    </article>
  </section>
</template>
