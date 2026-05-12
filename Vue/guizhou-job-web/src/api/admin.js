import request from './request'

export function fetchAdminUsers(params) {
  return request.get('/admin/users', { params })
}

export function createAdminUser(data) {
  return request.post('/admin/users', data)
}

export function updateAdminUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

export function deleteAdminUser(id) {
  return request.delete(`/admin/users/${id}`)
}

export function fetchAdminJobs(params) {
  return request.get('/admin/jobs', { params })
}

export function updateAdminJob(id, data) {
  return request.put(`/admin/jobs/${id}`, data)
}

export function deleteAdminJob(id) {
  return request.delete(`/admin/jobs/${id}`)
}
