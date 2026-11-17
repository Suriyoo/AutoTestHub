
import { http } from './http'

// API 用例数据类型
export interface ApiCase {
  id: number
  projectId: number
  name: string
  method: string
  url: string
  headersJson: string | null
  paramsJson: string | null
  body: string | null
  expectedStatus: number
  createdAt: string
  updatedAt: string
}

// API 用例新增和修改参数
export interface ApiCaseRequest {
  projectId: number
  name: string
  method: string
  url: string
  headersJson?: string | null
  paramsJson?: string | null
  body?: string | null
  expectedStatus: number
}

// 统一响应结构
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 查询项目下的 API 用例
export async function getApiCases(projectId?: number) {
  return http.get<ApiResponse<ApiCase[]>>('/cases', {
    params: projectId ? { projectId } : {}
  })
}

// 查询 API 用例详情
export async function getApiCaseDetail(id: number) {
  return http.get<ApiResponse<ApiCase>>(`/cases/${id}`)
}

// 创建 API 用例
export async function createApiCase(data: ApiCaseRequest) {
  return http.post<ApiResponse<ApiCase>>('/cases', data)
}

// 修改 API 用例
export async function updateApiCase(
  id: number,
  data: ApiCaseRequest
) {
  return http.put<ApiResponse<ApiCase>>(`/cases/${id}`, data)
}

// 删除 API 用例
export async function deleteApiCase(id: number) {
  return http.delete<ApiResponse<null>>(`/cases/${id}`)
}

// 执行 API 用例
export async function executeApiCase(
  id: number,
  environmentId: number
) {
  return http.post<ApiResponse<unknown>>(
    `/cases/${id}/execute`,
    null,
    {
      params: { environmentId }
    }
  )
}
