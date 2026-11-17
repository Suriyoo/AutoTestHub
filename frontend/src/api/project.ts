
import { http } from './http'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface Project {
  id: number
  name: string
  description: string | null
  ownerId: number
  createdAt: string
  updatedAt: string
}

export interface ProjectRequest {
  name: string
  description: string
}

// 查询项目列表
export function getProjects() {
  return http.get<ApiResponse<Project[]>>('/projects')
}

// 新增项目
export function createProject(data: ProjectRequest) {
  return http.post<ApiResponse<Project>>('/projects', data)
}

// 查询项目详情
export function getProjectDetail(id: number) {
  return http.get<ApiResponse<Project>>(`/projects/${id}`)
}

// 修改项目
export function updateProject(id: number, data: ProjectRequest) {
  return http.put<ApiResponse<Project>>(`/projects/${id}`, data)
}

// 删除项目
export function deleteProject(id: number) {
  return http.delete<ApiResponse<void>>(`/projects/${id}`)
}
