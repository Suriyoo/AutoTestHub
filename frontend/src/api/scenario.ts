import { http } from './http'

// 测试场景
export interface TestScenario {
  id: number
  projectId: number
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
}

// 场景步骤
export interface ScenarioStep {
  id: number
  scenarioId: number
  caseId: number
  sortOrder: number
  createdAt: string
}

// 场景详情
export interface ScenarioDetail {
  scenario: TestScenario
  steps: ScenarioStep[]
}

// 新增场景参数
export interface ScenarioRequest {
  projectId: number
  name: string
  description?: string
}

// 修改场景参数
export interface ScenarioUpdateRequest {
  name: string
  description?: string
}

// 添加场景步骤参数
export interface ScenarioStepRequest {
  caseId: number
}

// 调整步骤顺序参数
export interface ScenarioStepOrderRequest {
  steps: {
    stepId: number
    sortOrder: number
  }[]
}

// 场景执行记录
export interface ScenarioExecution {
  id: number
  scenarioId: number
  projectId: number
  environmentId: number
  status: string
  totalSteps: number
  passedSteps: number
  failedSteps: number
  startedAt: string | null
  finishedAt: string | null
  createdAt: string | null
  duration: number | null
}

// 场景执行步骤记录
export interface ScenarioExecutionStep {
  id: number
  executionId: number
  scenarioStepId: number
  caseId: number
  sortOrder: number
  executionRecordId: number | null
  status: string
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string | null
  duration: number | null
}

// 执行场景后返回的数据
export interface ScenarioExecutionResult {
  executionId: number
  scenarioId: number
  scenarioName: string
  environmentId: number
  environmentName: string
  status: string
  totalSteps: number
  passedSteps: number
  failedSteps: number
  steps: ScenarioExecutionStep[]
}

// 单次场景执行详情
export interface ScenarioExecutionDetail {
  execution: ScenarioExecution
  steps: ScenarioExecutionStep[]
}

// 统一响应结构
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 查询项目下的场景列表
export function getScenarios(projectId?: number) {
  return http.get<ApiResponse<TestScenario[]>>('/scenarios', {
    params: projectId ? { projectId } : {}
  })
}

// 查询场景详情
export function getScenarioDetail(id: number) {
  return http.get<ApiResponse<ScenarioDetail>>(`/scenarios/${id}`)
}

// 新增场景
export function createScenario(data: ScenarioRequest) {
  return http.post<ApiResponse<TestScenario>>('/scenarios', data)
}

// 修改场景
export function updateScenario(
  id: number,
  data: ScenarioUpdateRequest
) {
  return http.put<ApiResponse<TestScenario>>(`/scenarios/${id}`, data)
}

// 删除场景
export function deleteScenario(id: number) {
  return http.delete<ApiResponse<null>>(`/scenarios/${id}`)
}

// 添加场景步骤
export function addScenarioStep(
  scenarioId: number,
  data: ScenarioStepRequest
) {
  return http.post<ApiResponse<ScenarioStep>>(
    `/scenarios/${scenarioId}/steps`,
    data
  )
}

// 调整场景步骤顺序
export function updateScenarioStepOrder(
  scenarioId: number,
  data: ScenarioStepOrderRequest
) {
  return http.put<ApiResponse<null>>(
    `/scenarios/${scenarioId}/steps`,
    data
  )
}

// 删除场景步骤
export function deleteScenarioStep(
  scenarioId: number,
  stepId: number
) {
  return http.delete<ApiResponse<null>>(
    `/scenarios/${scenarioId}/steps/${stepId}`
  )
}

// 执行整个场景
export function executeScenario(
  scenarioId: number,
  environmentId: number
) {
  return http.post<ApiResponse<ScenarioExecutionResult>>(
    `/scenarios/${scenarioId}/execute`,
    null,
    {
      params: { environmentId }
    }
  )
}

// 查询场景执行历史
export function getScenarioExecutions(scenarioId: number) {
  return http.get<ApiResponse<ScenarioExecution[]>>(
    `/scenarios/${scenarioId}/executions`
  )
}

// 查询单次场景执行详情
export function getScenarioExecutionDetail(
  scenarioId: number,
  executionId: number
) {
  return http.get<ApiResponse<ScenarioExecutionDetail>>(
    `/scenarios/${scenarioId}/executions/${executionId}`
  )
}

/**
 * 导出场景执行 Excel 报告
 */
export function exportScenarioExecutionReport(
  scenarioId: number,
  executionId: number
) {
  return http.get(
    `/scenarios/${scenarioId}/executions/${executionId}/report`,
    {
      responseType: 'blob'
    }
  )
}


/**
 * 项目级场景执行报告
 */
export interface ProjectScenarioExecutionReport {
  execution: ScenarioExecution
  scenarioName: string
  environmentName: string
}

/**
 * 查询项目下所有场景执行报告
 */
export function getProjectScenarioExecutionReports(projectId: number) {
  return http.get<ApiResponse<ProjectScenarioExecutionReport[]>>(
    `/projects/${projectId}/scenario-executions`
  )
}


export interface ScenarioExecutionDailyTrend {
  date: string
  executionCount: number
  passedCount: number
  failedCount: number
  passRate: number
  avgDuration: number
}

export interface ScenarioExecutionTrend {
  totalCount: number
  passedCount: number
  failedCount: number
  overallPassRate: number
  startDate: string
  endDate: string
  trends: ScenarioExecutionDailyTrend[]
}

export function getProjectScenarioExecutionTrends(
  projectId: number,
  startDate: string,
  endDate: string
) {
  return http.get<ApiResponse<ScenarioExecutionTrend>>(
    `/projects/${projectId}/scenario-execution-trends`,
    {
      params: {
        startDate,
        endDate
      }
    }
  )
}
