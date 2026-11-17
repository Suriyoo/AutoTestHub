
import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动携带 JWT
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('autotest_token')

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理错误
http.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API 请求失败：', error.response?.data || error.message)
    return Promise.reject(error)
  }
)
