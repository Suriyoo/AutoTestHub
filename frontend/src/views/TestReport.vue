
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import axios from 'axios'

import {
  getProjectScenarioExecutionReports,
  getScenarioExecutionDetail,
  exportScenarioExecutionReport,
  type ProjectScenarioExecutionReport,
  type ScenarioExecutionDetail,
  type ScenarioExecution
} from '../api/scenario'

const props = defineProps<{
  projectId?: number
}>()

type StatusFilter = 'ALL' | 'PASSED' | 'FAILED'

interface ReportRow extends ProjectScenarioExecutionReport {
  id: number
}

const reports = ref<ProjectScenarioExecutionReport[]>([])
const loading = ref(false)
const statusFilter = ref<StatusFilter>('ALL')

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<ScenarioExecutionDetail | null>(null)
const currentScenarioName = ref('')
const currentEnvironmentName = ref('')

const exportingExecutionId = ref<number | null>(null)

const tableRows = computed<ReportRow[]>(() => {
  return reports.value.map(item => ({
    ...item,
    id: item.execution.id
  }))
})

const filteredReports = computed(() => {
  if (statusFilter.value === 'ALL') {
    return tableRows.value
  }

  return tableRows.value.filter(
    item => item.execution.status === statusFilter.value
  )
})

const totalCount = computed(() => reports.value.length)

const passedCount = computed(() => {
  return reports.value.filter(
    item => item.execution.status === 'PASSED'
  ).length
})

const failedCount = computed(() => {
  return reports.value.filter(
    item => item.execution.status === 'FAILED'
  ).length
})

const overallPassRate = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((passedCount.value / totalCount.value) * 100)
})

async function loadReports() {
  loading.value = true

  try {
    const res = await getProjectScenarioExecutionReports(props.projectId)
    reports.value = res.data.data || []
  } catch {
    Message.error('测试报告加载失败')
  } finally {
    loading.value = false
  }
}

function getStatusText(status: string) {
  if (status === 'PASSED') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RUNNING') return '执行中'
  return status || '未知'
}

function getStatusColor(status: string) {
  if (status === 'PASSED') return 'green'
  if (status === 'FAILED') return 'red'
  if (status === 'RUNNING') return 'arcoblue'
  return 'gray'
}

function formatDuration(duration: number | null) {
  if (duration === null || duration === undefined) return '-'
  if (duration < 1000) return `${duration} ms`
  return `${(duration / 1000).toFixed(2)} 秒`
}

function formatDate(value: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

function getPassRate(execution: ScenarioExecution) {
  if (!execution.totalSteps) return 0

  return Math.round(
    ((execution.passedSteps || 0) / execution.totalSteps) * 100
  )
}

async function openDetail(record: ReportRow) {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  currentScenarioName.value = record.scenarioName
  currentEnvironmentName.value = record.environmentName

  try {
    const res = await getScenarioExecutionDetail(
      record.execution.scenarioId,
      record.execution.id
    )

    currentDetail.value = res.data.data
  } catch {
    Message.error('执行详情加载失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function getExportErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data

    if (data instanceof Blob) {
      try {
        const text = await data.text()
        const parsed = JSON.parse(text)
        return parsed.message || '导出报告失败'
      } catch {
        return '导出报告失败'
      }
    }

    if (data && typeof data === 'object' && 'message' in data) {
      return String(data.message)
    }

    return error.message || '导出报告失败'
  }

  if (error instanceof Error) {
    return error.message
  }

  return '导出报告失败'
}

async function exportReport(record: ReportRow) {
  const executionId = record.execution.id
  exportingExecutionId.value = executionId

  try {
    const res = await exportScenarioExecutionReport(
      record.execution.scenarioId,
      executionId
    )

    const blob = res.data

    if (!(blob instanceof Blob) || blob.size === 0) {
      throw new Error('报告文件为空')
    }

    if (blob.type.includes('application/json')) {
      const text = await blob.text()
      const parsed = JSON.parse(text)
      throw new Error(parsed.message || '导出报告失败')
    }

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = url
    link.download = `AutoTestHub-测试报告-${executionId}.xlsx`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    window.URL.revokeObjectURL(url)

    Message.success('Excel 报告导出成功')
  } catch (error) {
    Message.error(await getExportErrorMessage(error))
  } finally {
    exportingExecutionId.value = null
  }
}

watch(
  () => props.projectId,
  () => {
    statusFilter.value = 'ALL'
    loadReports()
  },
  { immediate: true }
)
</script>

<template>
  <div class="report-page">
    <div class="report-toolbar">
      <div>
        <h2>测试报告</h2>
        <p>统一查看所有项目下测试场景的执行报告</p>
      </div>

      <a-button
        type="primary"
        :loading="loading"
        @click="loadReports"
      >
        <template #icon>
          <icon-refresh />
        </template>
        刷新报告
      </a-button>
    </div>

    <!-- 统计卡片 -->
    <a-grid :cols="4" :col-gap="16" :row-gap="16" class="statistics">
      <a-grid-item>
        <a-card class="stat-card">
          <div class="stat-card-content">
            <div class="stat-icon total-icon">
              <icon-file />
            </div>
            <div>
              <div class="stat-label">执行总次数</div>
              <div class="stat-value">{{ totalCount }}</div>
            </div>
          </div>
        </a-card>
      </a-grid-item>

      <a-grid-item>
        <a-card class="stat-card">
          <div class="stat-card-content">
            <div class="stat-icon success-icon">
              <icon-check-circle />
            </div>
            <div>
              <div class="stat-label">成功次数</div>
              <div class="stat-value success">{{ passedCount }}</div>
            </div>
          </div>
        </a-card>
      </a-grid-item>

      <a-grid-item>
        <a-card class="stat-card">
          <div class="stat-card-content">
            <div class="stat-icon failed-icon">
              <icon-close-circle />
            </div>
            <div>
              <div class="stat-label">失败次数</div>
              <div class="stat-value failed">{{ failedCount }}</div>
            </div>
          </div>
        </a-card>
      </a-grid-item>

      <a-grid-item>
        <a-card class="stat-card">
          <div class="stat-card-content">
            <div class="stat-icon rate-icon">
              <icon-chart-bar />
            </div>
            <div>
              <div class="stat-label">整体通过率</div>
              <div class="stat-value">{{ overallPassRate }}%</div>
            </div>
          </div>
        </a-card>
      </a-grid-item>
    </a-grid>

    <!-- 执行报告列表 -->
    <a-card class="report-card">
      <template #title>
        <div class="table-title">
          <span>执行报告列表</span>
          <a-tag color="arcoblue">
            共 {{ filteredReports.length }} 条
          </a-tag>
        </div>
      </template>

      <template #extra>
        <a-select
          v-model="statusFilter"
          style="width: 140px"
          placeholder="执行状态"
        >
          <a-option value="ALL">全部状态</a-option>
          <a-option value="PASSED">执行成功</a-option>
          <a-option value="FAILED">执行失败</a-option>
        </a-select>
      </template>

      <a-table
        :data="filteredReports"
        :loading="loading"
        :pagination="{ pageSize: 10, showTotal: true }"
        row-key="id"
        stripe
        table-layout="fixed"
        class="report-table"
      >
        <template #columns>
          <a-table-column
            title="执行 ID"
            :width="85"
            align="center"
          >
            <template #cell="{ record }">
              <span class="id-text">#{{ record.execution.id }}</span>
            </template>
          </a-table-column>

          <a-table-column title="所属项目" :width="130" ellipsis tooltip>
            <template #cell="{ record }">{{ record.projectName || `项目 #${record.execution.projectId}` }}</template>
          </a-table-column>

          <a-table-column
            title="场景名称"
            :width="145"
            ellipsis
            tooltip
          >
            <template #cell="{ record }">
              <span class="scenario-name">{{ record.scenarioName }}</span>
            </template>
          </a-table-column>

          <a-table-column
            title="测试环境"
            :width="105"
            ellipsis
            tooltip
          >
            <template #cell="{ record }">
              {{ record.environmentName }}
            </template>
          </a-table-column>

          <a-table-column
            title="执行状态"
            :width="85"
            align="center"
          >
            <template #cell="{ record }">
              <a-tag
                :color="getStatusColor(record.execution.status)"
                class="status-tag"
              >
                {{ getStatusText(record.execution.status) }}
              </a-tag>
            </template>
          </a-table-column>

          <a-table-column
            title="步骤统计"
            :width="95"
            align="center"
          >
            <template #cell="{ record }">
              <span class="success">
                {{ record.execution.passedSteps }}
              </span>
              <span class="stat-separator">/</span>
              <span class="failed">
                {{ record.execution.failedSteps }}
              </span>
              <span class="stat-separator">/</span>
              {{ record.execution.totalSteps }}
            </template>
          </a-table-column>

          <a-table-column
            title="通过率"
            :width="75"
            align="center"
          >
            <template #cell="{ record }">
              <span class="rate-text">
                {{ getPassRate(record.execution) }}%
              </span>
            </template>
          </a-table-column>

          <a-table-column
            title="执行耗时"
            :width="90"
            align="center"
          >
            <template #cell="{ record }">
              {{ formatDuration(record.execution.duration) }}
            </template>
          </a-table-column>

          <a-table-column
            title="执行时间"
            :width="165"
            align="center"
          >
            <template #cell="{ record }">
              <span class="date-text">
                {{ formatDate(record.execution.startedAt) }}
              </span>
            </template>
          </a-table-column>

          <a-table-column
            title="操作"
            :width="155"
            align="center"
          >
            <template #cell="{ record }">
              <div class="action-buttons">
                <a-button
                  type="text"
                  size="small"
                  @click="openDetail(record)"
                >
                  查看详情
                </a-button>

                <a-button
                  type="text"
                  size="small"
                  :loading="exportingExecutionId === record.execution.id"
                  :disabled="
                    exportingExecutionId !== null &&
                    exportingExecutionId !== record.execution.id
                  "
                  @click="exportReport(record)"
                >
                  导出
                </a-button>
              </div>
            </template>
          </a-table-column>
        </template>
      </a-table>

      <a-empty
        v-if="!loading && filteredReports.length === 0"
        description="暂无符合条件的测试报告"
      />
    </a-card>

    <!-- 执行详情弹窗 -->
    <a-modal
      v-model:visible="detailVisible"
      title="测试执行报告详情"
      :width="1000"
      :footer="false"
      unmount-on-close
      class="report-detail-modal"
    >
      <a-spin :loading="detailLoading" style="width: 100%">
        <template v-if="currentDetail">
          <div class="detail-heading">
            <div>
              <h3>{{ currentScenarioName }}</h3>
              <div class="detail-subtitle">
                测试环境：{{ currentEnvironmentName }}
                · 执行 ID：{{ currentDetail.execution.id }}
              </div>
            </div>

            <a-tag
              size="large"
              :color="getStatusColor(currentDetail.execution.status)"
            >
              {{ getStatusText(currentDetail.execution.status) }}
            </a-tag>
          </div>

          <a-grid :cols="4" :col-gap="12" :row-gap="12">
            <a-grid-item>
              <a-card class="detail-stat-card">
                <div class="stat-label">总步骤</div>
                <div class="detail-stat">
                  {{ currentDetail.execution.totalSteps }}
                </div>
              </a-card>
            </a-grid-item>

            <a-grid-item>
              <a-card class="detail-stat-card">
                <div class="stat-label">成功</div>
                <div class="detail-stat success">
                  {{ currentDetail.execution.passedSteps }}
                </div>
              </a-card>
            </a-grid-item>

            <a-grid-item>
              <a-card class="detail-stat-card">
                <div class="stat-label">失败</div>
                <div class="detail-stat failed">
                  {{ currentDetail.execution.failedSteps }}
                </div>
              </a-card>
            </a-grid-item>

            <a-grid-item>
              <a-card class="detail-stat-card">
                <div class="stat-label">执行耗时</div>
                <div class="detail-stat">
                  {{ formatDuration(currentDetail.execution.duration) }}
                </div>
              </a-card>
            </a-grid-item>
          </a-grid>

          <div class="detail-section">
            <h4>步骤执行明细</h4>

            <a-table
              :data="currentDetail.steps"
              :pagination="false"
              row-key="id"
              :scroll="{ x: 750 }"
            >
              <template #columns>
                <a-table-column
                  title="顺序"
                  data-index="sortOrder"
                  :width="70"
                  align="center"
                />

                <a-table-column
                  title="步骤 ID"
                  data-index="scenarioStepId"
                  :width="90"
                />

                <a-table-column
                  title="API 用例 ID"
                  data-index="caseId"
                  :width="110"
                />

                <a-table-column
                  title="执行状态"
                  :width="100"
                >
                  <template #cell="{ record }">
                    <a-tag :color="getStatusColor(record.status)">
                      {{ getStatusText(record.status) }}
                    </a-tag>
                  </template>
                </a-table-column>

                <a-table-column
                  title="耗时"
                  :width="100"
                >
                  <template #cell="{ record }">
                    {{ formatDuration(record.duration) }}
                  </template>
                </a-table-column>

                <a-table-column
                  title="失败原因"
                  data-index="errorMessage"
                  ellipsis
                  tooltip
                >
                  <template #cell="{ record }">
                    <span v-if="record.errorMessage" class="failed">
                      {{ record.errorMessage }}
                    </span>
                    <span v-else class="empty-text">-</span>
                  </template>
                </a-table-column>
              </template>
            </a-table>
          </div>

          <div class="detail-footer">
            <a-button
              type="primary"
              :loading="
                exportingExecutionId === currentDetail.execution.id
              "
              @click="
                exportReport({
                  id: currentDetail.execution.id,
                  execution: currentDetail.execution,
                  scenarioName: currentScenarioName,
                  environmentName: currentEnvironmentName
                })
              "
            >
              <template #icon>
                <icon-download />
              </template>
              导出 Excel 报告
            </a-button>
          </div>
        </template>
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.report-page {
  width: 100%;
  min-width: 0;
  color: #1d2129;
}

.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.report-toolbar h2 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 600;
  color: #1d2129;
}

.report-toolbar p {
  margin: 0;
  color: #86909c;
  font-size: 13px;
}

.statistics {
  margin-bottom: 20px;
}

.stat-card {
  height: 100%;
  min-height: 105px;
  border-radius: 6px;
}

.stat-card-content {
  display: flex;
  align-items: center;
  gap: 15px;
  min-height: 60px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  font-size: 21px;
}

.total-icon {
  color: #165dff;
  background: #e8f3ff;
}

.success-icon {
  color: #00b42a;
  background: #e8ffea;
}

.failed-icon {
  color: #f53f3f;
  background: #ffece8;
}

.rate-icon {
  color: #722ed1;
  background: #f5e8ff;
}

.stat-label {
  color: #86909c;
  font-size: 13px;
}

.stat-value {
  margin-top: 6px;
  color: #1d2129;
  font-size: 27px;
  line-height: 1.2;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.success {
  color: #00b42a;
}

.failed {
  color: #f53f3f;
}

.report-card {
  margin-bottom: 20px;
  border-radius: 6px;
  min-width: 0;
}

.table-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

/* 主表格使用固定布局，避免列宽被内容撑开 */
.report-table {
  width: 100%;
  min-width: 0;
}

.report-table :deep(.arco-table-container) {
  width: 100%;
  overflow-x: hidden;
}

.report-table :deep(.arco-table) {
  width: 100%;
  table-layout: fixed;
}

.report-table :deep(.arco-table-th),
.report-table :deep(.arco-table-td) {
  padding-left: 8px;
  padding-right: 8px;
  white-space: nowrap;
}

.report-table :deep(.arco-table-th) {
  font-weight: 600;
  color: #4e5969;
}

.id-text {
  display: inline-block;
  white-space: nowrap;
  color: #86909c;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.scenario-name {
  color: #1d2129;
  font-weight: 500;
}

.status-tag {
  min-width: 44px;
  justify-content: center;
}

.stat-separator {
  color: #c9cdd4;
  margin: 0 2px;
}

.rate-text {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.date-text {
  display: inline-block;
  white-space: nowrap;
  color: #4e5969;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.action-buttons :deep(.arco-btn) {
  flex-shrink: 0;
  padding: 0 4px;
  white-space: nowrap;
}

.detail-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-heading h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.detail-subtitle {
  color: #86909c;
  font-size: 13px;
}

.detail-stat-card {
  height: 100%;
  border-radius: 6px;
}

.detail-stat {
  margin-top: 8px;
  font-size: 21px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.detail-section {
  margin-top: 24px;
}

.detail-section h4 {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e5e6eb;
}

.empty-text {
  color: #c9cdd4;
}

@media (max-width: 1100px) {
  .stat-card-content {
    gap: 10px;
  }

  .stat-icon {
    width: 38px;
    height: 38px;
    font-size: 18px;
  }

  .stat-value {
    font-size: 23px;
  }

  .report-table :deep(.arco-table-th),
  .report-table :deep(.arco-table-td) {
    padding-left: 5px;
    padding-right: 5px;
  }
}

@media (max-width: 768px) {
  .report-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .report-toolbar :deep(.arco-btn) {
    align-self: flex-end;
  }

  .stat-card-content {
    gap: 8px;
  }

  .stat-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .stat-value {
    font-size: 20px;
  }

  .detail-heading {
    align-items: flex-start;
  }
}
</style>
