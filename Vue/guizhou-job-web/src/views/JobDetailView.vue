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

const lines = computed(() => descriptionLines(job.value?.jobDescription))

async function loadDetail() {
  loading.value = true
  errorMessage.value = ''
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
    <button class="text-button" type="button" @click="router.back()">返回上一页</button>

    <article class="detail-hero">
      <div>
        <p class="eyebrow">JOB DETAIL</p>
        <h1>{{ job.jobTitle }}</h1>
        <p>{{ job.companyName }}</p>
      </div>
      <strong>{{ formatSalary(job) }}</strong>
    </article>

    <div class="detail-grid">
      <aside class="detail-card">
        <h2>岗位概览</h2>
        <dl>
          <div>
            <dt>城市</dt>
            <dd>{{ job.city || '未知' }}</dd>
          </div>
          <div>
            <dt>学历</dt>
            <dd>{{ job.educationText || '不限' }}</dd>
          </div>
          <div>
            <dt>经验</dt>
            <dd>{{ job.experienceText || '不限' }}</dd>
          </div>
          <div>
            <dt>地址</dt>
            <dd>{{ job.workAddress || '待确认' }}</dd>
          </div>
          <div>
            <dt>发布</dt>
            <dd>{{ formatDate(job.publishTime) }}</dd>
          </div>
        </dl>
      </aside>

      <article class="description-card">
        <h2>岗位描述</h2>
        <p v-for="(line, index) in lines" :key="index">{{ line }}</p>
      </article>
    </div>
  </section>
</template>
