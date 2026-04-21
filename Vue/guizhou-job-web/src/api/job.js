import request from './request'

export function fetchJobPage(params = {}) {
  return request.get('/job/page', { params })
}

export function fetchHotJobs(limit = 10) {
  return request.get('/job/hot', { params: { limit } })
}

export function fetchJobDetail(id) {
  return request.get(`/job/${id}`)
}
