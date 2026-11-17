<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  getExecutionRecords,
  getExecutionRecordDetail,
  type ApiExecutionRecord,
  type ExecutionRecordFilters
} from '../api/executionRecord'
import { getProjects, type Project } from '../api/project'

interface DisplayRecord extends ApiExecutionRecord {
  projectName: string
}

const loading = ref(false)
const detailLoading = ref(false)
const records = ref<DisplayRecord[]>([])
const projects = ref<Project[]>([])
const detailVisible = ref(false)
const currentRecord = ref<ApiExecutionRecord | null>(null)

const filters = reactive<{
  projectId?: number
  result?: ExecutionRecordFilters['result']
  startDate?: string
  endDate?: string
}>({
  projectId: undefined,
  result: undefined,
  startDate: undefined,
  endDate: undefined
})

const projectOptions = computed(() =>
  projects.value.map(project => ({
    label: project.name,
    value: project.id
  }))
)

const columns = [
  { title: '所属项目', dataIndex: 'projectName', ellipsis: true, tooltip: true },
  { title: '用例名称', dataIndex: 'caseName', ellipsis: true, tooltip: true },
  { title: '请求方法', dataIndex: 'method', width: 110 },
  { title: '执行结果', slotName: 'result', width: 120 },
  { title: '耗时(ms)', dataIndex: 'duration', width: 120 },
  { title: '操作', slotName: 'action', width: 110 }
]

async function loadProjects() {
  try {
    const response = await getProjects()
    projects.value = response.data.data
  } catch (error) {
    console.error('加载项目列表失败', error)
    Message.error('项目列表加载失败')
  }
}

async function loadRecords() {
  if (
    filters.startDate &&
    filters.endDate &&
    filters.startDate > filters.endDate
  ) {
    Message.warning('开始日期不能晚于结束日期')
    return
  }

  loading.value = true

  try {
    const query: ExecutionRecordFilters = {
      projectId: filters.projectId,
      result: filters.result,
      startDate: filters.startDate,
      endDate: filters.endDate
    }

    const result = await getExecutionRecords(query)
    const projectNameMap = new Map(
      projects.value.map(project => [project.id, project.name])
    )

    records.value = result.map(record => ({
      ...record,
      projectName: projectNameMap.get(record.projectId) || `项目 #${record.projectId}`
    }))
  } catch (error) {
    console.error('查询执行历史失败', error)
    Message.error('查询执行历史失败')
    records.value = []
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.projectId = undefined
  filters.result = undefined
  filters.startDate = undefined
  filters.endDate = undefined
  loadRecords()
}

async function showDetail(id: number) {
  detailLoading.value = true
  currentRecord.value = null

  try {
    currentRecord.value = await getExecutionRecordDetail(id)
    detailVisible.value = true
  } catch (error) {
    console.error('查询执行详情失败', error)
    Message.error('查询执行详情失败')
  } finally {
    detailLoading.value = false
  }
}

function getResultLabel(record: ApiExecutionRecord) {
  if (record.success === 1 && record.passed === 1) return '通过'
  if (record.success === 1) return '断言失败'
  return '请求失败'
}

function getResultColor(record: ApiExecutionRecord) {
  if (record.success === 1 && record.passed === 1) return 'green'
  if (record.success === 1) return 'orange'
  return 'red'
}

function formatJson(value: string | null) {
  if (!value) return '无'

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function formatDateTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

onMounted(async () => {
  await loadProjects()
  await loadRecords()
})
</script>

<template>
  <div class="execution-history">
    <div class="page-header">
      <div>
        <h2>执行历史</h2>
        <p>统一查看所有项目的 API 用例执行记录</p>
      </div>

      <a-button type="primary" :loading="loading" @click="loadRecords">
        <template #icon>
          <icon-refresh />
        </template>
        刷新记录
      </a-button>
    </div>

    <div class="filter-bar">
      <a-select
        v-model="filters.projectId"
        :options="projectOptions"
        allow-clear
        placeholder="全部项目"
        class="filter-project"
      />

      <a-select
        v-model="filters.result"
        allow-clear
        placeholder="全部执行结果"
        class="filter-result"
      >
        <a-option value="PASSED">通过</a-option>
        <a-option value="ASSERTION_FAILED">断言失败</a-option>
        <a-option value="REQUEST_FAILED">请求失败</a-option>
      </a-select>

      <a-date-picker
        v-model="filters.startDate"
        value-format="YYYY-MM-DD"
        placeholder="开始日期"
        class="filter-date"
      />

      <span class="date-separator">至</span>

      <a-date-picker
        v-model="filters.endDate"
        value-format="YYYY-MM-DD"
        placeholder="结束日期"
        class="filter-date"
      />

      <a-button type="primary" :loading="loading" @click="loadRecords">
        查询
      </a-button>
      <a-button @click="resetFilters">
        重置
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data="records"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      row-key="id"
      stripe
      :scroll="{ x: 850 }"
    >
      <template #result="{ record }">
        <a-tag :color="getResultColor(record)">
          {{ getResultLabel(record) }}
        </a-tag>
      </template>

      <template #action="{ record }">
        <a-button
          type="text"
          size="small"
          :loading="detailLoading"
          @click="showDetail(record.id)"
        >
          查看详情
        </a-button>
      </template>

      <template #empty>
        <a-empty description="暂无执行记录" />
      </template>
    </a-table>

    <a-drawer
      v-model:visible="detailVisible"
      title="执行记录详情"
      :width="650"
      unmount-on-close
    >
      <a-spin :loading="detailLoading" style="width: 100%">
        <div v-if="currentRecord" class="detail-content">
          <a-descriptions :column="2" bordered>
            <a-descriptions-item label="记录ID">
              {{ currentRecord.id }}
            </a-descriptions-item>
            <a-descriptions-item label="用例名称">
              {{ currentRecord.caseName }}
            </a-descriptions-item>
            <a-descriptions-item label="请求方法">
              {{ currentRecord.method }}
            </a-descriptions-item>
            <a-descriptions-item label="执行环境">
              {{ currentRecord.environmentName || '未指定' }}
            </a-descriptions-item>
            <a-descriptions-item label="预期状态码">
              {{ currentRecord.expectedStatus ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="实际状态码">
              {{ currentRecord.actualStatus ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="耗时">
              {{ currentRecord.duration ?? '-' }} ms
            </a-descriptions-item>
            <a-descriptions-item label="执行时间">
              {{ formatDateTime(currentRecord.executedAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="执行结果">
              <a-tag :color="getResultColor(currentRecord)">
                {{ getResultLabel(currentRecord) }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>

          <h3>请求 URL</h3>
          <pre>{{ currentRecord.requestUrl || '无' }}</pre>

          <h3>请求 Headers</h3>
          <pre>{{ formatJson(currentRecord.requestHeaders) }}</pre>

          <h3>请求 Params</h3>
          <pre>{{ formatJson(currentRecord.requestParams) }}</pre>

          <h3>请求 Body</h3>
          <pre>{{ currentRecord.requestBody || '无' }}</pre>

          <h3>响应 Body</h3>
          <pre>{{ currentRecord.responseBody || '无' }}</pre>

          <div v-if="currentRecord.errorMessage">
            <h3>错误信息</h3>
            <pre class="error-text">{{ currentRecord.errorMessage }}</pre>
          </div>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.execution-history {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p {
  margin: 0;
  color: var(--color-text-3);
  font-size: 13px;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 10px;
  margin-bottom: 18px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.filter-project {
  width: 180px;
  flex: 0 0 180px;
}

.filter-result {
  width: 160px;
  flex: 0 0 160px;
}

.filter-date {
  width: 150px;
  flex: 0 0 150px;
}

.date-separator {
  color: var(--color-text-3);
}

.detail-content h3 {
  margin: 20px 0 8px;
  font-size: 14px;
}

.detail-content pre {
  padding: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--color-fill-2);
  border-radius: 4px;
  font-size: 12px;
}

.error-text {
  color: rgb(var(--danger-6));
}

@media (max-width: 768px) {
  .execution-history {
    padding: 12px;
  }

  .page-header {
    align-items: flex-start;
  }

  .filter-bar {
    flex-wrap: wrap;
    overflow-x: visible;
  }

  .filter-project,
  .filter-result,
  .filter-date {
    width: 100%;
    flex: 1 1 100%;
  }
}
</style>
