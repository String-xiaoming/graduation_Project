import request from './request'

export function loginUser(data) {
  return request.post('/user/login', data)
}

export function loginUserByCode(data) {
  return request.post('/user/login/code', data)
}

export function registerUser(data) {
  return request.post('/user/register', data)
}

export function sendEmailCode(data) {
  return request.post('/email/code/send', data)
}

export function fetchUserProfile(id) {
  return request.get(`/user/${id}`)
}

export function updateUserProfile(data) {
  return request.put('/user/profile', data)
}

export function resetUserPassword(data) {
  return request.post('/user/password/reset', data)
}

export function changeUserPassword(data) {
  return request.post('/user/password/change', data)
}
