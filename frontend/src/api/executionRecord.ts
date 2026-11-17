import { http } from './http'

export interface ApiExecutionRecord {
  id: number
  caseId: number
  projectId: number
  environmentId: number | null

  caseName: string
  environmentName: string | null

  method: string
  requestUrl: string | null
  requestHeaders: string | null
  requestParams: string | null
  requestBody: string | null

  success: number
  passed: number

  expectedStatus: number | null
  actualStatus: number | null

  duration: number | null
  responseBody: string | null
  errorMessage: string | null

  executedBy: number
  executedAt: string
}

export interface ExecutionRecordFilters {
  projectId?: number
  caseId?: number
  result?: 'PASSED' | 'ASSERTION_FAILED' | 'REQUEST_FAILED'
  startDate?: string
  endDate?: string
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 查询当前用户的 API 执行历史。
 * 不传 projectId 时查询当前用户全部项目。
 */
export async function getExecutionRecords(
  filters: ExecutionRecordFilters = {}
): Promise<ApiExecutionRecord[]> {
  const response = await http.get<ApiResponse<ApiExecutionRecord[]>>(
    '/execution-records',
    {
      params: filters
    }
  )

  return response.data.data
}

/**
 * 查询单条执行记录详情
 */
export async function getExecutionRecordDetail(
  id: number
): Promise<ApiExecutionRecord> {
  const response = await http.get<ApiResponse<ApiExecutionRecord>>(
    `/execution-records/${id}`
  )

  return response.data.data
}
