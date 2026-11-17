
<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import axios from 'axios'
import {
  getApiCases,
  createApiCase,
  updateApiCase,
  deleteApiCase,
  executeApiCase,
  type ApiCase,
  type ApiCaseRequest
} from '../api/apiCase'

interface Environment {
  id: number
  projectId: number
  name: string
  baseUrl: string
  variablesJson: string | null
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface ExecutionResult {
  caseId: number
  caseName: string
  environmentId: number
  environmentName: string
  success: boolean
  passed: boolean
  expectedStatus: number
  actualStatus: number | null
  duration: number | null
  responseBody: string | null
  errorMessage?: string | null
  executionRecordId: number
}

const props = defineProps<{
  projectId: number
}>()

const cases = ref<ApiCase[]>([])
const environments = ref<Environment[]>([])

const loading = ref(false)
const saving = ref(false)
const executing = ref(false)

const modalVisible = ref(false)
const editingId = ref<number | null>(null)

const resultVisible = ref(false)
const executionResult = ref<ExecutionResult | null>(null)

const selectedEnvironmentId = ref<number | undefined>()

const form = reactive({
  name: '',
  method: 'GET',
  url: '',
  headersJson: '',
  paramsJson: '',
  body: '',
  expectedStatus: 200
})

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message || fallback
  }

  return fallback
}

// 加载 API 用例
async function loadCases() {
  if (!props.projectId) {
    cases.value = []
    return
  }

  loading.value = true

  try {
    const response = await getApiCases(props.projectId)
    cases.value = response.data.data || []
  } catch (error) {
    Message.error(getErrorMessage(error, 'API 用例加载失败'))
  } finally {
    loading.value = false
  }
}

// 加载测试环境
async function loadEnvironments() {
  if (!props.projectId) {
    environments.value = []
    return
  }

  try {
    const response = await axios.get<ApiResponse<Environment[]>>(
      '/api/environments',
      {
        params: { projectId: props.projectId },
        headers: {
          Authorization: `Bearer ${localStorage.getItem('autotest_token') || ''}`
        }
      }
    )

    environments.value = response.data.data || []

    if (
      selectedEnvironmentId.value &&
      !environments.value.some(
        item => item.id === selectedEnvironmentId.value
      )
    ) {
      selectedEnvironmentId.value = undefined
    }

    if (!selectedEnvironmentId.value && environments.value.length > 0) {
      selectedEnvironmentId.value = environments.value[0].id
    }
  } catch (error) {
    Message.error(getErrorMessage(error, '测试环境加载失败'))
  }
}

// 初始化数据
async function loadPage() {
  await Promise.all([
    loadCases(),
    loadEnvironments()
  ])
}

// 重置表单
function resetForm() {
  form.name = ''
  form.method = 'GET'
  form.url = ''
  form.headersJson = ''
  form.paramsJson = ''
  form.body = ''
  form.expectedStatus = 200
}

// 新增用例
function openCreateModal() {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 编辑用例
function openEditModal(item: ApiCase) {
  editingId.value = item.id

  form.name = item.name
  form.method = item.method
  form.url = item.url
  form.headersJson = item.headersJson || ''
  form.paramsJson = item.paramsJson || ''
  form.body = item.body || ''
  form.expectedStatus = item.expectedStatus

  modalVisible.value = true
}

// JSON 格式校验
function validateJson(value: string, fieldName: string) {
  if (!value.trim()) return true

  try {
    const parsed = JSON.parse(value)

    if (
      parsed === null ||
      Array.isArray(parsed) ||
      typeof parsed !== 'object'
    ) {
      Message.warning(`${fieldName} 必须是 JSON 对象`)
      return false
    }

    return true
  } catch {
    Message.warning(`${fieldName} JSON 格式不正确`)
    return false
  }
}

// 保存用例
async function saveCase() {
  if (!form.name.trim()) {
    Message.warning('请输入用例名称')
    return
  }

  if (!form.url.trim()) {
    Message.warning('请输入请求 URL')
    return
  }

  if (
    !validateJson(form.headersJson, 'Headers') ||
    !validateJson(form.paramsJson, 'Params')
  ) {
    return
  }

  if (
    !Number.isInteger(form.expectedStatus) ||
    form.expectedStatus < 100 ||
    form.expectedStatus > 599
  ) {
    Message.warning('预期状态码必须是 100-599 之间的整数')
    return
  }

  const data: ApiCaseRequest = {
    projectId: props.projectId,
    name: form.name.trim(),
    method: form.method,
    url: form.url.trim(),
    headersJson: form.headersJson.trim() || null,
    paramsJson: form.paramsJson.trim() || null,
    body: form.body || null,
    expectedStatus: form.expectedStatus
  }

  saving.value = true

  try {
    if (editingId.value !== null) {
      await updateApiCase(editingId.value, data)
      Message.success('用例修改成功')
    } else {
      await createApiCase(data)
      Message.success('用例创建成功')
    }

    modalVisible.value = false
    await loadCases()
  } catch (error) {
    Message.error(getErrorMessage(error, '保存用例失败'))
  } finally {
    saving.value = false
  }
}

// 删除用例
async function removeCase(id: number) {
  try {
    await deleteApiCase(id)
    Message.success('用例删除成功')
    await loadCases()
  } catch (error) {
    Message.error(getErrorMessage(error, '删除用例失败'))
  }
}

// 执行用例
async function runCase(item: ApiCase) {
  if (!selectedEnvironmentId.value) {
    Message.warning('请先选择测试环境')
    return
  }

  executing.value = true
  executionResult.value = null

  try {
    const response = await executeApiCase(
      item.id,
      selectedEnvironmentId.value
    )

    executionResult.value = response.data.data as ExecutionResult
    resultVisible.value = true

    if (executionResult.value.passed) {
      Message.success('用例执行通过')
    } else {
      Message.warning('用例执行完成，但断言未通过或请求失败')
    }

    await loadCases()
  } catch (error) {
    Message.error(getErrorMessage(error, '用例执行失败'))
  } finally {
    executing.value = false
  }
}

// 监听项目变化
watch(
  () => props.projectId,
  () => {
    selectedEnvironmentId.value = undefined
    loadPage()
  },
  { immediate: true }
)
</script>

<template>
  <div class="api-case-management">
    <div class="page-header">
      <div>
        <h2>API 用例管理</h2>
        <p>创建、维护和执行 API 自动化测试用例</p>
      </div>

      <a-space>
        <a-button @click="loadPage">
          <template #icon>
            <icon-refresh />
          </template>
          刷新
        </a-button>

        <a-button type="primary" @click="openCreateModal">
          + 新建用例
        </a-button>
      </a-space>
    </div>

    <!-- 执行环境选择 -->
    <a-card class="toolbar-card">
      <a-space wrap>
        <span class="toolbar-label">执行环境：</span>

        <a-select
          v-model="selectedEnvironmentId"
          placeholder="请选择测试环境"
          style="width: 240px"
          allow-clear
        >
          <a-option
            v-for="env in environments"
            :key="env.id"
            :value="env.id"
          >
            {{ env.name }} ({{ env.baseUrl }})
          </a-option>
        </a-select>

        <a-tag color="arcoblue">
          共 {{ cases.length }} 条用例
        </a-tag>
      </a-space>
    </a-card>

    <!-- 用例列表 -->
    <a-table
      :data="cases"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      row-key="id"
      stripe
      :scroll="{ x: 1100 }"
    >
      <template #columns>
        <a-table-column
          title="ID"
          data-index="id"
          :width="70"
        />

        <a-table-column
          title="用例名称"
          data-index="name"
          :width="180"
          ellipsis
          tooltip
        />

        <a-table-column
          title="请求方法"
          data-index="method"
          :width="100"
        >
          <template #cell="{ record }">
            <a-tag
              :color="record.method === 'GET' ? 'green' :
                record.method === 'POST' ? 'arcoblue' :
                record.method === 'DELETE' ? 'red' : 'orange'"
            >
              {{ record.method }}
            </a-tag>
          </template>
        </a-table-column>

        <a-table-column
          title="请求 URL"
          data-index="url"
          ellipsis
          tooltip
        />

        <a-table-column
          title="预期状态码"
          data-index="expectedStatus"
          :width="110"
        />

        <a-table-column
          title="创建时间"
          data-index="createdAt"
          :width="180"
        >
          <template #cell="{ record }">
            {{ record.createdAt?.replace('T', ' ') || '-' }}
          </template>
        </a-table-column>

        <a-table-column
          title="操作"
          :width="230"
          fixed="right"
        >
          <template #cell="{ record }">
            <a-space>
              <a-button
                type="text"
                size="small"
                :loading="executing"
                @click="runCase(record)"
              >
                执行
              </a-button>

              <a-button
                type="text"
                size="small"
                @click="openEditModal(record)"
              >
                编辑
              </a-button>

              <a-popconfirm
                content="确定删除这个用例吗？"
                @ok="removeCase(record.id)"
              >
                <a-button
                  type="text"
                  status="danger"
                  size="small"
                >
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </a-table-column>
      </template>

      <template #empty>
        <a-empty description="当前项目暂无 API 用例" />
      </template>
    </a-table>

    <!-- 新增 / 编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="editingId !== null ? '编辑 API 用例' : '新建 API 用例'"
      :ok-loading="saving"
      :width="720"
      @ok="saveCase"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="用例名称" required>
          <a-input
            v-model="form.name"
            placeholder="例如：查询用户信息"
            :max-length="150"
          />
        </a-form-item>

        <a-form-item label="请求方法" required>
          <a-select v-model="form.method">
            <a-option value="GET">GET</a-option>
            <a-option value="POST">POST</a-option>
            <a-option value="PUT">PUT</a-option>
            <a-option value="PATCH">PATCH</a-option>
            <a-option value="DELETE">DELETE</a-option>
            <a-option value="HEAD">HEAD</a-option>
            <a-option value="OPTIONS">OPTIONS</a-option>
          </a-select>
        </a-form-item>

        <a-form-item label="请求 URL" required>
          <a-input
            v-model="form.url"
            placeholder="例如：/users/{{userId}}"
          />
        </a-form-item>

        <a-form-item label="请求 Headers（JSON 对象）">
          <a-textarea
            v-model="form.headersJson"
            placeholder='例如：{"Accept":"application/json"}'
            :auto-size="{ minRows: 3, maxRows: 6 }"
          />
        </a-form-item>

        <a-form-item label="请求 Params（JSON 对象）">
          <a-textarea
            v-model="form.paramsJson"
            placeholder='例如：{"page":1}'
            :auto-size="{ minRows: 3, maxRows: 6 }"
          />
        </a-form-item>

        <a-form-item label="请求 Body">
          <a-textarea
            v-model="form.body"
            placeholder='例如：{"username":"test"}'
            :auto-size="{ minRows: 4, maxRows: 8 }"
          />
        </a-form-item>

        <a-form-item label="预期状态码" required>
          <a-input-number
            v-model="form.expectedStatus"
            :min="100"
            :max="599"
            :precision="0"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 执行结果弹窗 -->
    <a-modal
      v-model:visible="resultVisible"
      title="API 用例执行结果"
      :footer="false"
      :width="720"
    >
      <div v-if="executionResult" class="result-content">
        <a-result
          :status="executionResult.passed ? 'success' : 'warning'"
          :title="executionResult.passed ? '测试通过' : '测试未通过'"
          :sub-title="executionResult.caseName"
        />

        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="执行记录 ID">
            {{ executionResult.executionRecordId }}
          </a-descriptions-item>

          <a-descriptions-item label="执行环境">
            {{ executionResult.environmentName }}
          </a-descriptions-item>

          <a-descriptions-item label="预期状态码">
            {{ executionResult.expectedStatus }}
          </a-descriptions-item>

          <a-descriptions-item label="实际状态码">
            {{ executionResult.actualStatus ?? '-' }}
          </a-descriptions-item>

          <a-descriptions-item label="执行耗时">
            {{ executionResult.duration ?? '-' }} ms
          </a-descriptions-item>

          <a-descriptions-item label="请求状态">
            {{ executionResult.success ? '成功' : '失败' }}
          </a-descriptions-item>
        </a-descriptions>

        <h3>响应 Body</h3>
        <pre>{{ executionResult.responseBody || '无响应内容' }}</pre>

        <div v-if="executionResult.errorMessage">
          <h3>错误信息</h3>
          <pre class="error-text">{{ executionResult.errorMessage }}</pre>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.api-case-management {
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

.toolbar-card {
  margin-bottom: 16px;
}

.toolbar-label {
  color: var(--color-text-2);
  font-size: 14px;
}

.result-content h3 {
  margin: 20px 0 8px;
  font-size: 14px;
}

.result-content pre {
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

