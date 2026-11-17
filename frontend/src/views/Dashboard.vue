
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import type { Project } from '../api/project'
import { getApiCases } from '../api/apiCase'
import {
  getScenarios,
  getProjectScenarioExecutionReports
} from '../api/scenario'

const props = defineProps<{
  projects: Project[]
}>()

const emit = defineEmits<{
  (e: 'navigate', key: string): void
  (e: 'openProject', project: Project, module?: string): void
}>()

interface ExecutionReport {
  execution: {
    id: number
    scenarioId: number
    projectId: number
    environmentId: number
    status: string
    totalSteps: number
    passedSteps: number
    failedSteps: number
    startedAt?: string | null
    finishedAt?: string | null
    createdAt?: string | null
    duration?: number | null
  }
  scenarioName: string
  environmentName: string
}

interface RecentExecution extends ExecutionReport {
  projectName: string
}

const loading = ref(false)
const requestVersion = ref(0)

const apiCaseCount = ref(0)
const scenarioCount = ref(0)
const executionReports = ref<RecentExecution[]>([])

const quickActions = [
  {
    key: 'cases',
    title: 'API 用例',
    description: '管理接口测试用例',
    icon: 'cases',
    theme: 'blue'
  },
  {
    key: 'environments',
    title: '测试环境',
    description: '管理项目测试环境',
    icon: 'environment',
    theme: 'cyan'
  },
  {
    key: 'scenarios',
    title: '测试场景',
    description: '编排和执行测试场景',
    icon: 'scenario',
    theme: 'purple'
  },
  {
    key: 'history',
    title: '执行历史',
    description: '查看自动化执行记录',
    icon: 'history',
    theme: 'orange'
  },
  {
    key: 'reports',
    title: '测试报告',
    description: '查看测试执行报告',
    icon: 'report',
    theme: 'green'
  },
  {
    key: 'trends',
    title: '趋势分析',
    description: '分析自动化测试质量',
    icon: 'trend',
    theme: 'red'
  }
]

const projectCount = computed(() => props.projects.length)

const executionCount = computed(() => executionReports.value.length)

const passedCount = computed(() => {
  return executionReports.value.filter(item =>
    ['PASSED', 'SUCCESS', 'SUCCEEDED', 'PASS'].includes(
      item.execution.status?.toUpperCase()
    )
  ).length
})

const failedCount = computed(() => {
  return executionReports.value.filter(item =>
    ['FAILED', 'FAILURE', 'FAIL'].includes(
      item.execution.status?.toUpperCase()
    )
  ).length
})

const finishedCount = computed(() => passedCount.value + failedCount.value)

const passRate = computed(() => {
  if (!finishedCount.value) return 0
  return Math.round((passedCount.value / finishedCount.value) * 100)
})

const recentProjects = computed(() => {
  return [...props.projects].sort((a, b) => {
    const timeA = new Date(a.createdAt || '').getTime()
    const timeB = new Date(b.createdAt || '').getTime()

    return (Number.isNaN(timeB) ? 0 : timeB) -
      (Number.isNaN(timeA) ? 0 : timeA)
  })
})

const recentExecutions = computed(() => {
  return [...executionReports.value]
    .sort((a, b) => {
      const timeA = new Date(
        a.execution.startedAt || a.execution.createdAt || ''
      ).getTime()

      const timeB = new Date(
        b.execution.startedAt || b.execution.createdAt || ''
      ).getTime()

      return (Number.isNaN(timeB) ? 0 : timeB) -
        (Number.isNaN(timeA) ? 0 : timeA)
    })
    .slice(0, 5)
})

const platformStats = computed(() => [
  {
    label: '项目总数',
    value: projectCount.value,
    footer: '当前管理的测试项目'
  },
  {
    label: 'API 用例',
    value: apiCaseCount.value,
    footer: '已创建的接口测试用例'
  },
  {
    label: '测试场景',
    value: scenarioCount.value,
    footer: '已编排的测试场景'
  },
  {
    label: '执行记录',
    value: executionCount.value,
    footer: '累计场景执行记录'
  }
])

const qualityStats = computed(() => [
  {
    label: '整体通过率',
    value: `${passRate.value}%`,
    footer: `已完成 ${finishedCount.value} 次执行`,
    type: 'rate'
  },
  {
    label: '执行成功',
    value: passedCount.value,
    footer: '状态为 PASSED 的执行记录',
    type: 'success'
  },
  {
    label: '执行失败',
    value: failedCount.value,
    footer: '状态为 FAILED 的执行记录',
    type: 'failed'
  }
])

function getStatusColor(status: string) {
  const normalized = status?.toUpperCase()

  if (['PASSED', 'SUCCESS', 'SUCCEEDED', 'PASS'].includes(normalized)) {
    return 'green'
  }

  if (['FAILED', 'FAILURE', 'FAIL'].includes(normalized)) {
    return 'red'
  }

  if (normalized === 'RUNNING') {
    return 'arcoblue'
  }

  return 'gray'
}

function getStatusText(status: string) {
  const normalized = status?.toUpperCase()

  if (['PASSED', 'SUCCESS', 'SUCCEEDED', 'PASS'].includes(normalized)) {
    return '成功'
  }

  if (['FAILED', 'FAILURE', 'FAIL'].includes(normalized)) {
    return '失败'
  }

  if (normalized === 'RUNNING') {
    return '执行中'
  }

  return status || '未知'
}

function formatDate(value?: string | null) {
  if (!value) return '-'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDuration(value?: number | null) {
  if (value === undefined || value === null) return '-'

  if (value < 1000) {
    return `${value} ms`
  }

  return `${(value / 1000).toFixed(2)} s`
}

async function loadDashboard() {
  const currentVersion = ++requestVersion.value
  loading.value = true

  try {
    const results = await Promise.all(
      props.projects.map(async project => {
        const [caseResult, scenarioResult, reportResult] =
          await Promise.allSettled([
            getApiCases(project.id),
            getScenarios(project.id),
            getProjectScenarioExecutionReports(project.id)
          ])

        const cases =
          caseResult.status === 'fulfilled'
            ? caseResult.value.data?.data ?? []
            : []

        const scenarios =
          scenarioResult.status === 'fulfilled'
            ? scenarioResult.value.data?.data ?? []
            : []

        const reports =
          reportResult.status === 'fulfilled'
            ? reportResult.value.data?.data ?? []
            : []

        const projectReports: RecentExecution[] = reports.map(
          (item: ExecutionReport) => ({
            ...item,
            projectName: project.name
          })
        )

        return {
          caseCount: cases.length,
          scenarioCount: scenarios.length,
          reports: projectReports
        }
      })
    )

    if (currentVersion !== requestVersion.value) return

    apiCaseCount.value = results.reduce(
      (total, item) => total + item.caseCount,
      0
    )

    scenarioCount.value = results.reduce(
      (total, item) => total + item.scenarioCount,
      0
    )

    executionReports.value = results.flatMap(item => item.reports)
  } catch (error) {
    console.error('加载工作台数据失败：', error)
    Message.error('工作台数据加载失败')
  } finally {
    if (currentVersion === requestVersion.value) {
      loading.value = false
    }
  }
}

function openQuickAction(key: string) {
  emit('navigate', key)
}

function openProject(project: Project) {
  emit('openProject', project, 'cases')
}

function openScenarioProject(record: RecentExecution) {
  const project = props.projects.find(
    item => item.id === record.execution.projectId
  )

  if (!project) {
    Message.warning('未找到该执行记录所属的项目')
    return
  }

  emit('openProject', project, 'scenarios')
}

watch(
  () => props.projects.map(project => project.id).join(','),
  () => {
    loadDashboard()
  },
  { immediate: true }
)
</script>

<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-panel">
      <div class="welcome-content">
        <div class="welcome-title">欢迎使用 AutoTest Hub</div>
        <div class="welcome-description">
          自动化测试管理平台，让测试工作更高效、更直观
        </div>
      </div>

      <a-button
        type="primary"
        size="large"
        @click="emit('navigate', 'projects')"
      >
        <template #icon>
          <icon-plus />
        </template>
        管理项目
      </a-button>
    </div>

    <!-- 平台概览 -->
    <section class="dashboard-section">
      <div class="section-header">
        <div>
          <div class="section-title">平台概览</div>
          <div class="section-description">平台当前的测试资产情况</div>
        </div>

        <a-button
          type="text"
          :loading="loading"
          @click="loadDashboard"
        >
          <template #icon>
            <icon-refresh />
          </template>
          刷新数据
        </a-button>
      </div>

      <div class="stats-grid">
        <div
          v-for="item in platformStats"
          :key="item.label"
        >
          <a-card class="stat-card" :bordered="false">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">
              <a-spin :loading="loading" :size="18">
                {{ item.value }}
              </a-spin>
            </div>
            <div class="stat-footer">{{ item.footer }}</div>
          </a-card>
        </div>
      </div>
    </section>

    <!-- 执行质量概览 -->
    <section class="dashboard-section">
      <div class="section-header">
        <div>
          <div class="section-title">执行质量概览</div>
          <div class="section-description">场景自动化执行情况</div>
        </div>

        <a-button
          type="text"
          @click="emit('navigate', 'reports')"
        >
          查看测试报告
          <template #icon>
            <icon-right />
          </template>
        </a-button>
      </div>

      <div class="quality-grid">
        <div
          v-for="item in qualityStats"
          :key="item.type"
        >
          <a-card class="quality-card" :bordered="false">
            <div class="quality-label">{{ item.label }}</div>

            <div
              class="quality-value"
              :class="item.type"
            >
              {{ item.value }}
            </div>

            <a-progress
              v-if="item.type === 'rate'"
              :percent="passRate / 100"
              :show-text="false"
              :stroke-width="8"
              :color="passRate >= 80 ? '#00b42a' : '#ff7d00'"
            />

            <div class="quality-footer">
              {{ item.footer }}
            </div>
          </a-card>
        </div>
      </div>
    </section>

    <!-- 常用功能 -->
    <section class="dashboard-section">
      <div class="section-header">
        <div>
          <div class="section-title">常用功能</div>
          <div class="section-description">快速进入平台功能模块</div>
        </div>
      </div>

      <div class="actions-grid">
        <div
          v-for="item in quickActions"
          :key="item.key"
        >
          <div
            class="action-card"
            role="button"
            tabindex="0"
            @click="openQuickAction(item.key)"
            @keydown.enter="openQuickAction(item.key)"
            @keydown.space.prevent="openQuickAction(item.key)"
          >
            <div class="action-icon" :class="item.theme">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <template v-if="item.icon === 'cases'">
                  <rect x="4" y="3" width="16" height="18" rx="2" />
                  <path d="M8 8h8M8 12h8M8 16h5" />
                </template>

                <template v-else-if="item.icon === 'environment'">
                  <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11z" />
                  <path d="m9 12 2 2 4-4" />
                </template>

                <template v-else-if="item.icon === 'scenario'">
                  <circle cx="5" cy="5" r="2" />
                  <circle cx="19" cy="19" r="2" />
                  <circle cx="19" cy="5" r="2" />
                  <circle cx="5" cy="19" r="2" />
                  <path d="M7 5h8a4 4 0 0 1 4 4v8M5 7v8a4 4 0 0 0 4 4h8" />
                </template>

                <template v-else-if="item.icon === 'history'">
                  <path d="M3 12a9 9 0 1 0 2.6-6.4L3 8" />
                  <path d="M3 3v5h5M12 7v5l3 2" />
                </template>

                <template v-else-if="item.icon === 'report'">
                  <path d="M6 3h9l4 4v14H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z" />
                  <path d="M14 3v5h5M8 13h8M8 17h8" />
                </template>

                <template v-else-if="item.icon === 'trend'">
                  <path d="M3 3v18h18" />
                  <path d="m7 14 4-4 4 3 6-7" />
                  <path d="M16 6h5v5" />
                </template>
              </svg>
            </div>

            <div class="action-content">
              <div class="action-title">{{ item.title }}</div>
              <div class="action-desc">{{ item.description }}</div>
            </div>

            <icon-right class="action-arrow" />
          </div>
        </div>
      </div>
    </section>

    <!-- 最近执行记录 -->
    <section class="dashboard-section">
      <div class="section-header">
        <div>
          <div class="section-title">最近执行记录</div>
          <div class="section-description">最近的测试场景执行情况</div>
        </div>

        <a-button
          type="text"
          @click="emit('navigate', 'history')"
        >
          查看全部
          <template #icon>
            <icon-right />
          </template>
        </a-button>
      </div>

      <a-card class="table-card" :bordered="false">
        <a-table
          :data="recentExecutions"
          :pagination="false"
          :loading="loading"
          row-key="execution.id"
          :scroll="{ x: 850 }"
        >
          <template #columns>
            <a-table-column title="测试场景" :width="180">
              <template #cell="{ record }">
                <button
                  class="scenario-link"
                  type="button"
                  @click="openScenarioProject(record)"
                >
                  {{ record.scenarioName || '-' }}
                </button>
              </template>
            </a-table-column>

            <a-table-column title="所属项目" :width="150">
              <template #cell="{ record }">
                {{ record.projectName || '-' }}
              </template>
            </a-table-column>

            <a-table-column title="测试环境" :width="130">
              <template #cell="{ record }">
                {{ record.environmentName || '-' }}
              </template>
            </a-table-column>

            <a-table-column title="执行状态" :width="110">
              <template #cell="{ record }">
                <a-tag
                  :color="getStatusColor(record.execution.status)"
                >
                  {{ getStatusText(record.execution.status) }}
                </a-tag>
              </template>
            </a-table-column>

            <a-table-column title="步骤结果" :width="130">
              <template #cell="{ record }">
                <span class="step-success">
                  {{ record.execution.passedSteps ?? 0 }}
                </span>
                /
                <span class="step-failed">
                  {{ record.execution.failedSteps ?? 0 }}
                </span>
                /
                {{ record.execution.totalSteps ?? 0 }}
              </template>
            </a-table-column>

            <a-table-column title="执行耗时" :width="110">
              <template #cell="{ record }">
                {{ formatDuration(record.execution.duration) }}
              </template>
            </a-table-column>

            <a-table-column title="执行时间" :width="180">
              <template #cell="{ record }">
                {{ formatDate(record.execution.startedAt || record.execution.createdAt) }}
              </template>
            </a-table-column>
          </template>

          <template #empty>
            <a-empty description="暂无执行记录" />
          </template>
        </a-table>
      </a-card>
    </section>

    <!-- 最近项目 -->
    <section class="dashboard-section">
      <div class="section-header">
        <div>
          <div class="section-title">最近项目</div>
          <div class="section-description">
            共 {{ projectCount }} 个项目，按创建时间排序
          </div>
        </div>

        <a-button
          type="text"
          @click="emit('navigate', 'projects')"
        >
          项目管理
          <template #icon>
            <icon-right />
          </template>
        </a-button>
      </div>

      <a-card class="table-card" :bordered="false">
        <a-table
          :data="recentProjects"
          :pagination="{
            pageSize: 5,
            showTotal: true,
            showPageSize: false
          }"
          row-key="id"
          :scroll="{ x: 650 }"
        >
          <template #columns>
            <a-table-column title="项目名称" :width="220">
              <template #cell="{ record }">
                <span class="table-primary">
                  {{ record.name }}
                </span>
              </template>
            </a-table-column>

            <a-table-column title="项目描述">
              <template #cell="{ record }">
                <span class="project-description">
                  {{ record.description || '暂无项目描述' }}
                </span>
              </template>
            </a-table-column>

            <a-table-column title="创建时间" :width="180">
              <template #cell="{ record }">
                {{ formatDate(record.createdAt) }}
              </template>
            </a-table-column>

            <a-table-column title="操作" :width="130" align="center">
              <template #cell="{ record }">
                <a-button
                  type="primary"
                  size="small"
                  @click="openProject(record)"
                >
                  进入项目
                </a-button>
              </template>
            </a-table-column>
          </template>

          <template #empty>
            <a-empty description="暂无项目，请先创建项目" />
          </template>
        </a-table>
      </a-card>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  width: 100%;
  min-width: 0;
  padding-bottom: 24px;
}

.welcome-panel {
  min-height: 142px;
  padding: 28px 32px;
  margin-bottom: 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-radius: 12px;
  background: linear-gradient(115deg, #e8f3ff 0%, #f3f0ff 100%);
  box-sizing: border-box;
}

.welcome-title {
  color: #1d2129;
  font-size: 25px;
  font-weight: 700;
}

.welcome-description {
  margin-top: 12px;
  color: #64748b;
  font-size: 14px;
}

.dashboard-section {
  margin-bottom: 28px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  color: #1d2129;
  font-size: 19px;
  font-weight: 700;
}

.section-description {
  margin-top: 6px;
  color: #86909c;
  font-size: 13px;
}

/* 平台概览 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  height: 142px;
  border-radius: 9px;
}

.stat-card :deep(.arco-card-body) {
  padding: 22px 24px;
}

.stat-label {
  color: #64748b;
  font-size: 15px;
  font-weight: 600;
}

.stat-value {
  margin-top: 14px;
  color: #1d2129;
  font-size: 30px;
  font-weight: 700;
}

.stat-footer {
  margin-top: 8px;
  color: #86909c;
  font-size: 12px;
}

/* 执行质量 */
.quality-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.quality-card {
  height: 158px;
  border-radius: 9px;
  box-sizing: border-box;
}

.quality-card :deep(.arco-card-body) {
  height: 100%;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.quality-label {
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
}

.quality-value {
  margin: 12px 0;
  color: #1d2129;
  font-size: 28px;
  font-weight: 700;
  line-height: 34px;
}

.quality-value.rate {
  color: #165dff;
}

.quality-value.success {
  color: #00a870;
}

.quality-value.failed {
  color: #f53f3f;
}

.quality-card :deep(.arco-progress) {
  margin-top: auto;
}

.quality-footer {
  margin-top: auto;
  color: #86909c;
  font-size: 12px;
}

.quality-card :deep(.arco-progress + .quality-footer) {
  margin-top: 9px;
}

/* 常用功能 */
.actions-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.action-card {
  min-height: 94px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.action-card:hover {
  border-color: #c9ddff;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.08);
  transform: translateY(-2px);
}

.action-card:focus-visible {
  outline: 2px solid #165dff;
  outline-offset: 3px;
}

.action-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
}

.action-icon svg {
  display: block;
  width: 21px;
  height: 21px;
  flex-shrink: 0;
}

.action-icon.blue {
  color: #165dff;
  background: #e8f1ff;
}

.action-icon.cyan {
  color: #08979c;
  background: #e6fffb;
}

.action-icon.purple {
  color: #722ed1;
  background: #f3e8ff;
}

.action-icon.orange {
  color: #d46b08;
  background: #fff1e6;
}

.action-icon.green {
  color: #00a870;
  background: #e8ffea;
}

.action-icon.red {
  color: #f53f3f;
  background: #fff1f0;
}

.action-content {
  flex: 1;
  min-width: 0;
}

.action-title {
  color: #1d2129;
  font-size: 15px;
  font-weight: 600;
}

.action-desc {
  margin-top: 7px;
  color: #86909c;
  font-size: 12px;
}

.action-arrow {
  flex-shrink: 0;
  color: #c9cdd4;
  font-size: 16px;
}

/* 表格 */
.table-card {
  border-radius: 9px;
}

.table-card :deep(.arco-card-body) {
  padding: 8px 18px 14px;
}

.table-primary {
  color: #165dff;
  font-weight: 600;
}

.scenario-link {
  max-width: 100%;
  padding: 0;
  overflow: hidden;
  border: none;
  background: transparent;
  color: #165dff;
  font: inherit;
  font-weight: 600;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.scenario-link:hover {
  color: #4080ff;
  text-decoration: underline;
}

.scenario-link:focus-visible {
  outline: 2px solid #165dff;
  outline-offset: 3px;
}

.step-success {
  color: #00a870;
  font-weight: 600;
}

.step-failed {
  color: #f53f3f;
  font-weight: 600;
}

.project-description {
  color: #86909c;
}

/* 中等屏幕 */
@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quality-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .actions-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-card :deep(.arco-card-body) {
    padding: 18px;
  }

  .action-card {
    padding: 16px;
  }
}

/* 手机屏幕 */
@media (max-width: 640px) {
  .welcome-panel {
    padding: 22px;
    align-items: flex-start;
    flex-direction: column;
  }

  .welcome-title {
    font-size: 21px;
  }

  .section-title {
    font-size: 17px;
  }

  .stats-grid,
  .quality-grid,
  .actions-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .section-header {
    align-items: flex-start;
  }

  .action-card {
    min-height: 82px;
  }
}
</style>
