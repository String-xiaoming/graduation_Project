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
    <div class="job-card__main">
      <div class="job-card__content">
        <div class="job-card__head">
          <div>
            <span class="job-card__title">
              {{ job.jobTitle }}
            </span>
            <p class="job-card__company" :title="job.companyName">{{ job.companyName || '公司信息待确认' }}</p>
          </div>
          <strong>{{ formatSalary(job) }}</strong>
        </div>

        <div class="job-card__tags">
          <span>{{ job.city || '城市未知' }}</span>
          <span>{{ job.educationText || '学历不限' }}</span>
          <span>{{ job.experienceText || '经验不限' }}</span>
        </div>

        <p class="job-card__desc">
          {{ clampText(job.jobDescription, 96) || '暂无岗位描述，点击查看岗位详情。' }}
        </p>
      </div>

      <aside class="job-card__meta">
        <span>{{ formatDate(job.publishTime) }}</span>
        <button class="text-button" type="button" @click.stop="openDetail">查看详情</button>
      </aside>
    </div>

    <footer class="job-card__foot">
      <span>{{ job.workAddress || '地址待确认' }}</span>
    </footer>
  </article>
</template>
