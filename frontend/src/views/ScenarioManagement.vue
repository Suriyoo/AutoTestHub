
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'

import {
  getScenarios,
  getScenarioDetail,
  createScenario,
  updateScenario,
  deleteScenario,
  addScenarioStep,
  updateScenarioStepOrder,
  deleteScenarioStep,
  executeScenario,
  getScenarioExecutions,
  getScenarioExecutionDetail,
  exportScenarioExecutionReport,
  type TestScenario,
  type ScenarioStep,
  type ScenarioExecution,
  type ScenarioExecutionDetail
} from '../api/scenario'

import {
  getApiCases,
  type ApiCase
} from '../api/apiCase'

import {
  getEnvironments,
  type Environment
} from '../api/environment'

import {
  getExecutionRecordDetail,
  type ApiExecutionRecord
} from '../api/executionRecord'

const props = defineProps<{
  projectId: number
}>()

// ==================== 基础数据 ====================

const loading = ref(false)
const scenarios = ref<TestScenario[]>([])
const apiCases = ref<ApiCase[]>([])
const environments = ref<Environment[]>([])

// ==================== 场景新增和编辑 ====================

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  name: '',
  description: ''
})

// ==================== 场景详情和步骤管理 ====================

const selectedScenario = ref<TestScenario | null>(null)
const steps = ref<ScenarioStep[]>([])
const detailVisible = ref(false)
const stepCaseId = ref<number | undefined>()

// ==================== 场景执行 ====================

const selectedEnvironmentId = ref<number | undefined>()
const executing = ref(false)
const executionHistory = ref<ScenarioExecution[]>([])
const historyLoading = ref(false)

// ==================== 执行详情 ====================

const executionDetailVisible = ref(false)
const executionDetailLoading = ref(false)
const currentExecutionDetail = ref<ScenarioExecutionDetail | null>(null)

// ==================== Excel 报告导出 ====================

const exportingExecutionId = ref<number | null>(null)

// ==================== API 请求详情 ====================

const apiRecordVisible = ref(false)
const apiRecordLoading = ref(false)
const currentApiRecord = ref<ApiExecutionRecord | null>(null)

// ==================== 通过率计算 ====================

const currentPassRate = computed(() => {
  const execution = currentExecutionDetail.value?.execution

  if (!execution || !execution.totalSteps) {
    return 0
  }

  return Math.round(
    (execution.passedSteps / execution.totalSteps) * 100
  )
})

// ==================== 场景列表 ====================

async function loadScenarios() {
  loading.value = true

  try {
    const res = await getScenarios(props.projectId)
    scenarios.value = res.data.data || []
  } catch {
    Message.error('加载场景列表失败')
  } finally {
    loading.value = false
  }
}

// ==================== 加载 API 用例 ====================

async function loadApiCases() {
  try {
    const res = await getApiCases(props.projectId)
    apiCases.value = res.data.data || []
  } catch {
    Message.error('加载 API 用例失败')
  }
}

// ==================== 加载测试环境 ====================

async function loadEnvironments() {
  try {
    const res = await getEnvironments(props.projectId)
    environments.value = res.data.data || []
  } catch {
    Message.error('加载测试环境失败')
  }
}

// ==================== 新增场景 ====================

function openCreate() {
  editingId.value = null

  form.value = {
    name: '',
    description: ''
  }

  dialogVisible.value = true
}

// ==================== 编辑场景 ====================

function openEdit(record: TestScenario) {
  editingId.value = record.id

  form.value = {
    name: record.name,
    description: record.description || ''
  }

  dialogVisible.value = true
}

// ==================== 保存场景 ====================

async function saveScenario() {
  if (!form.value.name.trim()) {
    Message.warning('请输入场景名称')
    return
  }

  try {
    const data = {
      name: form.value.name.trim(),
      description: form.value.description.trim() || undefined
    }

    if (editingId.value) {
      await updateScenario(editingId.value, data)
      Message.success('场景更新成功')
    } else {
      await createScenario({
        projectId: props.projectId,
        ...data
      })

      Message.success('场景创建成功')
    }

    dialogVisible.value = false
    await loadScenarios()
  } catch {
    Message.error('保存场景失败')
  }
}

// ==================== 删除场景 ====================

function removeScenario(record: TestScenario) {
  Modal.confirm({
    title: '删除场景',
    content: `确定删除场景「${record.name}」及其全部步骤吗？`,

    onOk: async () => {
      try {
        await deleteScenario(record.id)

        Message.success('删除成功')

        if (selectedScenario.value?.id === record.id) {
          detailVisible.value = false
        }

        await loadScenarios()
      } catch {
        Message.error('删除场景失败')
      }
    }
  })
}

// ==================== 打开场景详情 ====================

async function openDetail(record: TestScenario) {
  selectedScenario.value = record
  detailVisible.value = true
  selectedEnvironmentId.value = undefined
  executionHistory.value = []

  await Promise.all([
    loadDetail(record.id),
    loadExecutionHistory(record.id)
  ])
}

// ==================== 加载场景步骤 ====================

async function loadDetail(id: number) {
  try {
    const res = await getScenarioDetail(id)
    const data = res.data.data

    selectedScenario.value = data.scenario
    steps.value = data.steps || []
  } catch {
    Message.error('加载场景详情失败')
  }
}

// ==================== 添加场景步骤 ====================

async function addStep() {
  if (!selectedScenario.value || !stepCaseId.value) {
    Message.warning('请先选择 API 用例')
    return
  }

  try {
    await addScenarioStep(selectedScenario.value.id, {
      caseId: stepCaseId.value
    })

    Message.success('添加步骤成功')
    stepCaseId.value = undefined

    await loadDetail(selectedScenario.value.id)
  } catch {
    Message.error('添加步骤失败')
  }
}

// ==================== 删除场景步骤 ====================

async function removeStep(step: ScenarioStep) {
  if (!selectedScenario.value) return

  try {
    await deleteScenarioStep(
      selectedScenario.value.id,
      step.id
    )

    Message.success('步骤删除成功')
    await loadDetail(selectedScenario.value.id)
  } catch {
    Message.error('删除步骤失败')
  }
}

// ==================== 调整步骤顺序 ====================

async function moveStep(index: number, direction: number) {
  if (!selectedScenario.value) return

  const target = index + direction

  if (target < 0 || target >= steps.value.length) {
    return
  }

  const newSteps = [...steps.value]
  const temp = newSteps[index]

  newSteps[index] = newSteps[target]
  newSteps[target] = temp

  const payload = {
    steps: newSteps.map((step, i) => ({
      stepId: step.id,
      sortOrder: i + 1
    }))
  }

  try {
    await updateScenarioStepOrder(
      selectedScenario.value.id,
      payload
    )

    Message.success('步骤顺序已更新')
    await loadDetail(selectedScenario.value.id)
  } catch {
    Message.error('调整步骤顺序失败')
  }
}

// ==================== 获取 API 用例名称 ====================

function getCaseName(caseId: number) {
  return apiCases.value.find(
    item => item.id === caseId
  )?.name || `用例 #${caseId}`
}

// ==================== 格式化耗时 ====================

function formatDuration(duration: number | null | undefined) {
  if (duration === null || duration === undefined) {
    return '-'
  }

  if (duration < 1000) {
    return `${duration} ms`
  }

  return `${(duration / 1000).toFixed(2)} s`
}

// ==================== 计算通过率 ====================

function getPassRate(
  passed: number | null | undefined,
  total: number | null | undefined
) {
  if (!total || total <= 0) {
    return 0
  }

  return Math.round(((passed || 0) / total) * 100)
}

// ==================== 执行整个场景 ====================

async function handleExecuteScenario() {
  if (!selectedScenario.value) return

  if (!selectedEnvironmentId.value) {
    Message.warning('请先选择测试环境')
    return
  }

  if (steps.value.length === 0) {
    Message.warning('当前场景没有执行步骤')
    return
  }

  executing.value = true

  try {
    const res = await executeScenario(
      selectedScenario.value.id,
      selectedEnvironmentId.value
    )

    const result = res.data.data

    if (result.status === 'PASSED') {
      Message.success('场景执行成功，全部步骤通过')
    } else {
      Message.warning('场景执行完成，但存在失败步骤')
    }

    await loadExecutionHistory(selectedScenario.value.id)

    await openExecutionDetail(
      selectedScenario.value.id,
      result.executionId
    )
  } catch (error: any) {
    Message.error(
      error?.response?.data?.message || '场景执行失败'
    )
  } finally {
    executing.value = false
  }
}

// ==================== 查询执行历史 ====================

async function loadExecutionHistory(scenarioId: number) {
  historyLoading.value = true

  try {
    const res = await getScenarioExecutions(scenarioId)
    executionHistory.value = res.data.data || []
  } catch {
    Message.error('加载场景执行历史失败')
  } finally {
    historyLoading.value = false
  }
}

// ==================== 打开执行详情 ====================

async function openExecutionDetail(
  scenarioId: number,
  executionId: number
) {
  executionDetailVisible.value = true
  executionDetailLoading.value = true
  currentExecutionDetail.value = null

  try {
    const res = await getScenarioExecutionDetail(
      scenarioId,
      executionId
    )

    currentExecutionDetail.value = res.data.data
  } catch {
    Message.error('加载执行详情失败')
    executionDetailVisible.value = false
  } finally {
    executionDetailLoading.value = false
  }
}

// ==================== 导出场景执行 Excel 报告 ====================

async function handleExportReport(
  scenarioId: number,
  executionId: number
) {
  if (exportingExecutionId.value !== null) {
    return
  }

  exportingExecutionId.value = executionId

  try {
    const res = await exportScenarioExecutionReport(
      scenarioId,
      executionId
    )

    const blob = res.data

    if (!(blob instanceof Blob) || blob.size === 0) {
      throw new Error('服务器返回的报告文件为空')
    }

    const downloadUrl = window.URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = `AutoTestHub-测试报告-${executionId}.xlsx`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    window.setTimeout(() => {
      window.URL.revokeObjectURL(downloadUrl)
    }, 1000)

    Message.success('测试报告导出成功')
  } catch (error: any) {
    let errorMessage = '测试报告导出失败'

    const responseData = error?.response?.data

    if (responseData instanceof Blob) {
      try {
        const text = await responseData.text()
        const errorData = JSON.parse(text)

        errorMessage = errorData.message || errorMessage
      } catch {
        // 无法解析错误响应时使用默认提示
      }
    } else if (responseData?.message) {
      errorMessage = responseData.message
    } else if (error?.message) {
      errorMessage = error.message
    }

    Message.error(errorMessage)
  } finally {
    exportingExecutionId.value = null
  }
}

// ==================== 查看 API 执行记录 ====================

async function openApiRecord(executionRecordId: number | null) {
  if (!executionRecordId) {
    Message.warning('该步骤没有关联 API 执行记录')
    return
  }

  apiRecordVisible.value = true
  apiRecordLoading.value = true
  currentApiRecord.value = null

  try {
    currentApiRecord.value = await getExecutionRecordDetail(
      executionRecordId
    )
  } catch {
    Message.error('加载 API 请求详情失败')
    apiRecordVisible.value = false
  } finally {
    apiRecordLoading.value = false
  }
}

// ==================== 状态显示 ====================

function getStatusColor(status: string) {
  switch (status) {
    case 'PASSED':
      return 'green'
    case 'FAILED':
      return 'red'
    case 'RUNNING':
      return 'arcoblue'
    default:
      return 'gray'
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'PASSED':
      return '通过'
    case 'FAILED':
      return '失败'
    case 'RUNNING':
      return '执行中'
    case 'PENDING':
      return '等待执行'
    default:
      return status || '未知'
  }
}

// ==================== 时间格式化 ====================

function formatTime(value: string | null | undefined) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

// ==================== 监听项目切换 ====================

watch(
  () => props.projectId,
  async () => {
    await Promise.all([
      loadScenarios(),
      loadApiCases(),
      loadEnvironments()
    ])

    detailVisible.value = false
    executionDetailVisible.value = false
    apiRecordVisible.value = false
  },
  {
    immediate: true
  }
)
</script>

<template>
  <div class="scenario-page">

    <div class="toolbar">
      <div>
        <h2>测试场景管理</h2>
        <p>将多个 API 用例组合成完整业务流程</p>
      </div>

      <a-button type="primary" @click="openCreate">
        新增场景
      </a-button>
    </div>

    <a-table
      :data="scenarios"
      :loading="loading"
      row-key="id"
      :pagination="{ pageSize: 10 }"
    >
      <template #columns>
        <a-table-column
          title="场景 ID"
          data-index="id"
          :width="100"
        />

        <a-table-column
          title="场景名称"
          data-index="name"
        />

        <a-table-column
          title="描述"
          data-index="description"
        />

        <a-table-column
          title="创建时间"
          data-index="createdAt"
        />

        <a-table-column title="操作" :width="250">
          <template #cell="{ record }">
            <a-space>
              <a-button
                size="small"
                type="primary"
                @click="openDetail(record)"
              >
                步骤管理 / 执行
              </a-button>

              <a-button
                size="small"
                @click="openEdit(record)"
              >
                编辑
              </a-button>

              <a-button
                size="small"
                status="danger"
                @click="removeScenario(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </template>
    </a-table>

    <!-- 新增和编辑场景 -->

    <a-modal
      v-model:visible="dialogVisible"
      :title="editingId ? '编辑测试场景' : '新增测试场景'"
      @ok="saveScenario"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="场景名称" required>
          <a-input
            v-model="form.name"
            placeholder="请输入场景名称"
            :max-length="150"
          />
        </a-form-item>

        <a-form-item label="场景描述">
          <a-textarea
            v-model="form.description"
            placeholder="请输入场景描述"
            :max-length="500"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 场景步骤管理抽屉 -->

    <a-drawer
      v-model:visible="detailVisible"
      :width="760"
      title="场景步骤管理与执行"
    >
      <template v-if="selectedScenario">
        <div class="scenario-info">
          <h3>{{ selectedScenario.name }}</h3>
          <p>
            {{ selectedScenario.description || '暂无场景描述' }}
          </p>
        </div>

        <a-divider>场景执行</a-divider>

        <div class="execute-panel">
          <div class="execute-form">
            <div class="execute-label">测试环境</div>

            <a-select
              v-model="selectedEnvironmentId"
              placeholder="请选择测试环境"
              style="width: 260px"
              allow-clear
            >
              <a-option
                v-for="item in environments"
                :key="item.id"
                :value="item.id"
              >
                {{ item.name }}
              </a-option>
            </a-select>

            <a-button
              type="primary"
              :loading="executing"
              :disabled="steps.length === 0"
              @click="handleExecuteScenario"
            >
              <icon-play-arrow />
              执行场景
            </a-button>
          </div>

          <div class="execute-tip">
            将按照当前步骤顺序，依次执行场景中的 API 用例。
          </div>
        </div>

        <a-divider>添加 API 用例步骤</a-divider>

        <a-space class="add-step">
          <a-select
            v-model="stepCaseId"
            placeholder="请选择 API 用例"
            style="width: 380px"
            allow-clear
          >
            <a-option
              v-for="item in apiCases"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }} ({{ item.method }})
            </a-option>
          </a-select>

          <a-button type="primary" @click="addStep">
            添加步骤
          </a-button>
        </a-space>

        <a-divider>场景执行步骤</a-divider>

        <a-empty
          v-if="steps.length === 0"
          description="暂无场景步骤，请先添加 API 用例"
        />

        <div
          v-for="(step, index) in steps"
          :key="step.id"
          class="step-item"
        >
          <div class="step-info">
            <a-tag color="arcoblue">
              步骤 {{ index + 1 }}
            </a-tag>

            <span class="step-name">
              {{ getCaseName(step.caseId) }}
            </span>

            <span class="case-id">
              用例 ID: {{ step.caseId }}
            </span>
          </div>

          <a-space>
            <a-button
              size="mini"
              :disabled="index === 0"
              @click="moveStep(index, -1)"
            >
              上移
            </a-button>

            <a-button
              size="mini"
              :disabled="index === steps.length - 1"
              @click="moveStep(index, 1)"
            >
              下移
            </a-button>

            <a-button
              size="mini"
              status="danger"
              @click="removeStep(step)"
            >
              删除
            </a-button>
          </a-space>
        </div>

        <a-divider>场景执行历史</a-divider>

        <div class="history-header">
          <span class="history-tip">
            共 {{ executionHistory.length }} 次执行记录
          </span>

          <a-button
            size="small"
            :loading="historyLoading"
            @click="loadExecutionHistory(selectedScenario!.id)"
          >
            刷新历史
          </a-button>
        </div>

        <a-table
          :data="executionHistory"
          :loading="historyLoading"
          row-key="id"
          :pagination="{ pageSize: 5 }"
          size="small"
          :scroll="{ x: 850 }"
        >
          <template #columns>
            <a-table-column
              title="执行 ID"
              data-index="id"
              :width="85"
            />

            <a-table-column title="执行状态" :width="100">
              <template #cell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
            </a-table-column>

            <a-table-column title="执行结果" :width="120">
              <template #cell="{ record }">
                <span class="result-text">
                  {{ record.passedSteps }}/{{ record.totalSteps }} 通过
                </span>
              </template>
            </a-table-column>

            <a-table-column title="通过率" :width="100">
              <template #cell="{ record }">
                {{ getPassRate(record.passedSteps, record.totalSteps) }}%
              </template>
            </a-table-column>

            <a-table-column title="执行耗时" :width="110">
              <template #cell="{ record }">
                {{ formatDuration(record.duration) }}
              </template>
            </a-table-column>

            <a-table-column title="开始时间" :width="170">
              <template #cell="{ record }">
                {{ formatTime(record.startedAt) }}
              </template>
            </a-table-column>

            <a-table-column title="操作" :width="180">
              <template #cell="{ record }">
                <a-space>
                  <a-button
                    size="mini"
                    type="primary"
                    @click="openExecutionDetail(
                      selectedScenario!.id,
                      record.id
                    )"
                  >
                    查看详情
                  </a-button>

                  <a-button
                    size="mini"
                    :loading="exportingExecutionId === record.id"
                    :disabled="exportingExecutionId !== null"
                    @click="handleExportReport(
                      selectedScenario!.id,
                      record.id
                    )"
                  >
                    导出报告
                  </a-button>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </template>
    </a-drawer>

    <!-- 场景执行详情 -->

    <a-modal
      v-model:visible="executionDetailVisible"
      title="场景执行详情"
      :width="1100"
      :footer="false"
      :mask-closable="false"
    >
      <a-spin :loading="executionDetailLoading">
        <template v-if="currentExecutionDetail">

          <!-- Excel 报告导出按钮 -->

          <div class="report-toolbar">
            <div class="report-toolbar-tip">
              <icon-file-excel />
              可将本次场景执行结果导出为 Excel 测试报告
            </div>

            <a-button
              type="primary"
              :loading="
                exportingExecutionId === currentExecutionDetail.execution.id
              "
              :disabled="exportingExecutionId !== null"
              @click="handleExportReport(
                currentExecutionDetail.execution.scenarioId,
                currentExecutionDetail.execution.id
              )"
            >
              <icon-download />
              导出 Excel 报告
            </a-button>
          </div>

          <div class="execution-summary">
            <div class="summary-item">
              <div class="summary-label">执行状态</div>

              <a-tag
                size="large"
                :color="getStatusColor(
                  currentExecutionDetail.execution.status
                )"
              >
                {{ getStatusText(
                  currentExecutionDetail.execution.status
                ) }}
              </a-tag>
            </div>

            <div class="summary-item">
              <div class="summary-label">总步骤</div>
              <div class="summary-value">
                {{ currentExecutionDetail.execution.totalSteps }}
              </div>
            </div>

            <div class="summary-item">
              <div class="summary-label">通过步骤</div>
              <div class="summary-value success-text">
                {{ currentExecutionDetail.execution.passedSteps }}
              </div>
            </div>

            <div class="summary-item">
              <div class="summary-label">失败步骤</div>
              <div class="summary-value error-text">
                {{ currentExecutionDetail.execution.failedSteps }}
              </div>
            </div>
          </div>

          <div class="report-panel">
            <div class="report-metric">
              <span class="summary-label">场景执行总耗时</span>
              <strong>
                {{ formatDuration(
                  currentExecutionDetail.execution.duration
                ) }}
              </strong>
            </div>

            <div class="report-metric">
              <span class="summary-label">执行通过率</span>
              <strong>{{ currentPassRate }}%</strong>
            </div>

            <a-progress
              :percent="currentPassRate / 100"
              :status="
                currentExecutionDetail.execution.status === 'FAILED'
                  ? 'danger'
                  : 'success'
              "
              :show-text="true"
            />
          </div>

          <div class="execution-time">
            <div>
              开始时间：
              {{ formatTime(
                currentExecutionDetail.execution.startedAt
              ) }}
            </div>

            <div>
              结束时间：
              {{ formatTime(
                currentExecutionDetail.execution.finishedAt
              ) }}
            </div>

            <div>
              执行环境 ID：
              {{ currentExecutionDetail.execution.environmentId }}
            </div>
          </div>

          <a-divider>步骤执行结果</a-divider>

          <a-table
            :data="currentExecutionDetail.steps"
            row-key="id"
            :pagination="false"
            :scroll="{ x: 900 }"
          >
            <template #columns>
              <a-table-column title="步骤" :width="70">
                <template #cell="{ record }">
                  {{ record.sortOrder }}
                </template>
              </a-table-column>

              <a-table-column title="API 用例" :width="180">
                <template #cell="{ record }">
                  {{ getCaseName(record.caseId) }}
                </template>
              </a-table-column>

              <a-table-column title="执行状态" :width="100">
                <template #cell="{ record }">
                  <a-tag :color="getStatusColor(record.status)">
                    {{ getStatusText(record.status) }}
                  </a-tag>
                </template>
              </a-table-column>

              <a-table-column title="执行耗时" :width="110">
                <template #cell="{ record }">
                  {{ formatDuration(record.duration) }}
                </template>
              </a-table-column>

              <a-table-column title="错误信息" :width="220">
                <template #cell="{ record }">
                  <span
                    v-if="record.errorMessage"
                    class="error-message"
                  >
                    {{ record.errorMessage }}
                  </span>

                  <span v-else class="muted-text">-</span>
                </template>
              </a-table-column>

              <a-table-column title="API 请求" :width="110">
                <template #cell="{ record }">
                  <a-button
                    v-if="record.executionRecordId"
                    size="mini"
                    type="primary"
                    @click="openApiRecord(record.executionRecordId)"
                  >
                    请求详情
                  </a-button>

                  <span v-else class="muted-text">
                    无记录
                  </span>
                </template>
              </a-table-column>
            </template>
          </a-table>

          <a-divider>步骤时间明细</a-divider>

          <div
            v-for="step in currentExecutionDetail.steps"
            :key="step.id"
            class="step-time-item"
          >
            <div class="step-time-title">
              <strong>
                步骤 {{ step.sortOrder }}：
                {{ getCaseName(step.caseId) }}
              </strong>

              <a-tag :color="getStatusColor(step.status)">
                {{ getStatusText(step.status) }}
              </a-tag>
            </div>

            <div class="step-time-content">
              <span>
                开始：{{ formatTime(step.startedAt) }}
              </span>

              <span>
                结束：{{ formatTime(step.finishedAt) }}
              </span>

              <span>
                耗时：{{ formatDuration(step.duration) }}
              </span>
            </div>

            <div
              v-if="step.errorMessage"
              class="step-error-detail"
            >
              {{ step.errorMessage }}
            </div>
          </div>
        </template>
      </a-spin>
    </a-modal>

    <!-- API 请求详情 -->

    <a-modal
      v-model:visible="apiRecordVisible"
      title="API 请求执行详情"
      :width="800"
      :footer="false"
    >
      <a-spin :loading="apiRecordLoading">
        <template v-if="currentApiRecord">
          <div class="api-record-summary">
            <div>
              <span class="summary-label">用例名称：</span>
              {{ currentApiRecord.caseName }}
            </div>

            <div>
              <span class="summary-label">请求方法：</span>
              <a-tag color="arcoblue">
                {{ currentApiRecord.method }}
              </a-tag>
            </div>

            <div class="url-text">
              <span class="summary-label">请求地址：</span>
              {{ currentApiRecord.requestUrl || '-' }}
            </div>

            <div>
              <span class="summary-label">HTTP 状态码：</span>
              {{ currentApiRecord.actualStatus ?? '-' }}
            </div>

            <div>
              <span class="summary-label">预期状态码：</span>
              {{ currentApiRecord.expectedStatus ?? '-' }}
            </div>

            <div>
              <span class="summary-label">执行耗时：</span>
              {{ currentApiRecord.duration ?? '-' }} ms
            </div>

            <div>
              <span class="summary-label">执行结果：</span>
              <a-tag
                :color="currentApiRecord.passed === 1 ? 'green' : 'red'"
              >
                {{ currentApiRecord.passed === 1 ? '通过' : '失败' }}
              </a-tag>
            </div>
          </div>

          <a-divider>请求头</a-divider>
          <pre class="json-content">{{ currentApiRecord.requestHeaders || '无请求头' }}</pre>

          <a-divider>请求参数</a-divider>
          <pre class="json-content">{{ currentApiRecord.requestParams || '无请求参数' }}</pre>

          <a-divider>请求 Body</a-divider>
          <pre class="json-content">{{ currentApiRecord.requestBody || '无请求 Body' }}</pre>

          <a-divider>响应内容</a-divider>
          <pre class="json-content">{{ currentApiRecord.responseBody || '无响应内容' }}</pre>

          <div
            v-if="currentApiRecord.errorMessage"
            class="api-error"
          >
            <a-divider>错误信息</a-divider>
            {{ currentApiRecord.errorMessage }}
          </div>
        </template>
      </a-spin>
    </a-modal>

  </div>
</template>

<style scoped>
:deep(.arco-modal) {
  max-width: calc(100vw - 32px);
}

:deep(.arco-modal-body) {
  overflow-x: auto;
}

:deep(.arco-modal-body table) {
  width: 100%;
  min-width: 700px;
}

.scenario-page {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar h2 {
  margin: 0 0 6px;
}

.toolbar p {
  margin: 0;
  color: var(--color-text-3);
}

.scenario-info h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.scenario-info p {
  color: var(--color-text-3);
  margin: 0;
}

.execute-panel {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-fill-1);
}

.execute-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.execute-label {
  font-weight: 500;
}

.execute-tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--color-text-3);
}

.add-step {
  display: flex;
  align-items: center;
}

.step-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.step-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.step-name {
  font-weight: 500;
}

.case-id {
  color: var(--color-text-3);
  font-size: 12px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-tip {
  color: var(--color-text-2);
  font-size: 13px;
}

.result-text {
  font-size: 13px;
}

/* Excel 报告导出区域 */

.report-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.report-toolbar-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-2);
  font-size: 13px;
}

.execution-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px;
  background: var(--color-fill-1);
  border-radius: 6px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-label {
  color: var(--color-text-3);
  font-size: 13px;
}

.summary-value {
  font-size: 22px;
  font-weight: 600;
}

.success-text {
  color: rgb(0, 160, 90);
}

.error-text {
  color: rgb(245, 63, 63);
}

.report-panel {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.report-metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.report-metric strong {
  font-size: 18px;
}

.execution-time {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
  font-size: 13px;
  color: var(--color-text-2);
}

.error-message {
  color: rgb(245, 63, 63);
  word-break: break-all;
}

.muted-text {
  color: var(--color-text-3);
}

.step-time-item {
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.step-time-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.step-time-content {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--color-text-2);
}

.step-error-detail {
  margin-top: 10px;
  padding: 10px;
  border-radius: 4px;
  background: var(--color-danger-light-1);
  color: rgb(245, 63, 63);
  font-size: 12px;
  word-break: break-all;
}

.api-record-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13px;
}

.url-text {
  word-break: break-all;
}

.json-content {
  max-height: 220px;
  overflow: auto;
  padding: 12px;
  border-radius: 6px;
  background: var(--color-fill-2);
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.6;
}

.api-error {
  color: rgb(245, 63, 63);
  word-break: break-all;
}

@media (max-width: 700px) {
  .execution-summary {
    grid-template-columns: repeat(2, 1fr);
  }

  .step-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .execute-form {
    align-items: flex-start;
    flex-direction: column;
  }

  .step-time-content {
    flex-direction: column;
    gap: 6px;
  }

  .report-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
