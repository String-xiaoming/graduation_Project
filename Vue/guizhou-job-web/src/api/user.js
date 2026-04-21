import request from './request'

export function loginUser(data) {
  return request.post('/user/login', data)
}

export function registerUser(data) {
  return request.post('/user/register', data)
}
