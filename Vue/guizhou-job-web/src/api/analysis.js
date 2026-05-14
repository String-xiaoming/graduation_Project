import request from './request'

export function fetchAnalysisDashboard() {
  return request.get('/analysis/dashboard')
}

export function fetchSkillTfidf(params = {}) {
  return request.get('/analysis/skill-tfidf', { params })
}

export function fetchSkillTfidfCities(limit = 20) {
  return request.get('/analysis/skill-tfidf/cities', { params: { limit } })
}
