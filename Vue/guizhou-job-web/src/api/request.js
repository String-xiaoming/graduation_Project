import axios from 'axios'
import { clearCurrentUser, getAuthToken } from '@/utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 12000,
})

request.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 200) {
        return payload.data
      }

      const message = payload.message || '请求失败'
      if (
        message.includes('登录') ||
        message.includes('过期') ||
        message.includes('鐧诲綍') ||
        message.includes('杩囨湡')
      ) {
        clearCurrentUser()
      }
      return Promise.reject(new Error(message))
    }

    return payload
  },
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      '服务暂时不可用，请稍后重试'
    if (message.includes('登录') || message.includes('过期')) {
      clearCurrentUser()
    }
    return Promise.reject(new Error(message))
  },
)

export default request
