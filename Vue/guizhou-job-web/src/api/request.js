import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 12000,
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 200) {
        return payload.data
      }
      return Promise.reject(new Error(payload.message || '请求失败'))
    }

    return payload
  },
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      '服务暂时不可用，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

export default request
