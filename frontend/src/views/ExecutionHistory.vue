
<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getExecutionRecords,
  getExecutionRecordDetail,
  type ApiExecutionRecord
} from '../api/executionRecord'

const props = defineProps<{
  projectId: number
  caseId?: number
}>()

const loading = ref(false)
const detailLoading = ref(false)

const records = ref<ApiExecutionRecord[]>([])
const detailVisible = ref(false)
const currentRecord = ref<ApiExecutionRecord | null>(null)

// 表格列配置
const columns = [
  { title: '用例名称', dataIndex: 'caseName' },
  { title: '请求方法', dataIndex: 'method' },
  { title: '执行环境', dataIndex: 'environmentName' },
  { title: '预期状态码', dataIndex: 'expectedStatus' },
  { title: '实际状态码', dataIndex: 'actualStatus' },
  { title: '耗时(ms)', dataIndex: 'duration' },
  { title: '执行结果', slotName: 'result' },
  { title: '执行时间', dataIndex: 'executedAt' },
  { title: '操作', slotName: 'action' }
]

// 加载执行历史
async function loadRecords() {
  if (!props.projectId) {
    records.value = []
    return
  }

  loading.value = true

  try {
    // API 封装函数已经返回数组，不需要再次读取 data.data
    records.value = await getExecutionRecords(
      props.projectId,
      props.caseId
    )
  } catch (error) {
    console.error('查询执行历史失败', error)
    records.value = []
  } finally {
    loading.value = false
  }
}

// 查看执行详情
async function showDetail(id: number) {
  detailLoading.value = true
  currentRecord.value = null

  try {
    // API 封装函数已经返回单条记录
    currentRecord.value = await getExecutionRecordDetail(id)
    detailVisible.value = true
  } catch (error) {
    console.error('查询执行详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

// projectId 或 caseId 变化时重新查询
watch(
  () => [props.projectId, props.caseId],
  () => {
    loadRecords()
  },
  { immediate: true }
)

// 格式化 JSON
function formatJson(value: string | null) {
  if (!value) return '无'

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

// 格式化执行时间
function formatDateTime(value: string) {
  if (!value) return '-'

  return value.replace('T', ' ')
}
</script>

<template>
  <div class="execution-history">
    <div class="page-header">
      <div>
        <h2>执行历史</h2>
        <p>查看 API 用例的执行结果和请求响应详情</p>
      </div>

      <a-button
        type="primary"
        :loading="loading"
        @click="loadRecords"
      >
        <template #icon>
          <icon-refresh />
        </template>
        刷新记录
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data="records"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      row-key="id"
      stripe
      :scroll="{ x: 1100 }"
    >
      <template #result="{ record }">
        <a-tag
          v-if="record.success === 1 && record.passed === 1"
          color="green"
        >
          通过
        </a-tag>

        <a-tag
          v-else-if="record.success === 1"
          color="orange"
        >
          断言失败
        </a-tag>

        <a-tag v-else color="red">
          请求失败
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

    <!-- 执行详情抽屉 -->
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
              <a-tag
                v-if="currentRecord.success === 1 && currentRecord.passed === 1"
                color="green"
              >
                通过
              </a-tag>

              <a-tag
                v-else-if="currentRecord.success === 1"
                color="orange"
              >
                断言失败
              </a-tag>

              <a-tag v-else color="red">
                请求失败
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
            <pre class="error-text">
{{ currentRecord.errorMessage }}
            </pre>
          </div>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.execution-history {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
</style>
