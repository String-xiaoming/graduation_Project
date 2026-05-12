export function formatSalary(job) {
  if (!job) return '薪资待确认'
  if (job.salaryText) return job.salaryText
  if (job.salaryMin != null && job.salaryMax != null) {
    return `${Math.round(job.salaryMin)}-${Math.round(job.salaryMax)}元/月`
  }
  return '薪资待确认'
}

export function formatDate(value) {
  if (!value) return '发布时间未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

export function descriptionLines(value) {
  if (!value) return []
  return String(value)
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean)
}

export function clampText(value, max = 120) {
  if (!value) return ''
  const text = String(value).replace(/\s+/g, ' ').trim()
  if (text.length <= max) return text
  return `${text.slice(0, max)}...`
}
