<script setup>
import { useRouter } from 'vue-router'
import { clampText, formatDate, formatSalary } from '@/utils/format'

const props = defineProps({
  job: {
    type: Object,
    required: true,
  },
})

const router = useRouter()

function openDetail() {
  if (!props.job?.id) return
  router.push(`/jobs/${props.job.id}`)
}
</script>

<template>
  <article
    class="job-card"
    role="link"
    tabindex="0"
    @click="openDetail"
    @keydown.enter.prevent="openDetail"
    @keydown.space.prevent="openDetail"
  >
    <div class="job-card__head">
      <div>
        <span class="job-card__title">
          {{ job.jobTitle }}
        </span>
        <p class="job-card__company" :title="job.companyName">{{ job.companyName }}</p>
      </div>
      <strong>{{ formatSalary(job) }}</strong>
    </div>

    <div class="job-card__tags">
      <span>{{ job.city || '城市未知' }}</span>
      <span>{{ job.educationText || '学历不限' }}</span>
      <span>{{ job.experienceText || '经验不限' }}</span>
    </div>

    <p class="job-card__desc">
      {{ clampText(job.jobDescription, 130) }}
    </p>

    <footer class="job-card__foot">
      <span>{{ job.workAddress || '地址待确认' }}</span>
      <span>{{ formatDate(job.publishTime) }}</span>
    </footer>
  </article>
</template>
