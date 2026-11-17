
import { http } from './http'

// 测试环境数据类型
export interface Environment {
  id: number
  projectId: number
  name: string
  baseUrl: string
  variablesJson: string | null
  createdAt: string
  updatedAt: string
}

// 新增和修改环境的请求参数
export interface EnvironmentRequest {
  projectId: number
  name: string
  baseUrl: string
  variablesJson?: string | null
}

// 统一响应结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 查询项目下的环境列表
export function getEnvironments(projectId?: number) {
  return http.get<ApiResponse<Environment[]>>('/environments', {
    params: projectId ? { projectId } : {}
  })
}

// 查询环境详情
export function getEnvironmentDetail(id: number) {
  return http.get<ApiResponse<Environment>>(`/environments/${id}`)
}

// 新增环境
export function createEnvironment(data: EnvironmentRequest) {
  return http.post<ApiResponse<Environment>>('/environments', data)
}

// 修改环境
export function updateEnvironment(
  id: number,
  data: EnvironmentRequest
) {
  return http.put<ApiResponse<Environment>>(`/environments/${id}`, data)
}

// 删除环境
export function deleteEnvironment(id: number) {
  return http.delete<ApiResponse<null>>(`/environments/${id}`)
}
