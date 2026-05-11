import request from './request'

export function loginUser(data) {
  return request.post('/user/login', data)
}

export function registerUser(data) {
  return request.post('/user/register', data)
}

export function fetchUserProfile(id) {
  return request.get(`/user/${id}`)
}

export function updateUserProfile(data) {
  return request.put('/user/profile', data)
}
