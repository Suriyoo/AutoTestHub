
<script setup lang="ts">
import { ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import axios from 'axios'
import {
  getEnvironments,
  createEnvironment,
  updateEnvironment,
  deleteEnvironment,
  type Environment,
  type EnvironmentRequest
} from '../api/environment'

const props = defineProps<{
  projectId: number
}>()

// 环境列表
const environments = ref<Environment[]>([])
const loading = ref(false)
const saving = ref(false)

// 弹窗状态
const modalVisible = ref(false)
const editingId = ref<number | null>(null)

// 表单数据
const form = ref({
  name: '',
  baseUrl: '',
  variablesJson: ''
})

// 获取错误信息
function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message || fallback
  }

  return fallback
}

// 加载环境列表
async function loadEnvironments() {
  if (!props.projectId) {
    environments.value = []
    return
  }

  loading.value = true

  try {
    const response = await getEnvironments(props.projectId)
    environments.value = response.data.data || []
  } catch (error) {
    Message.error(getErrorMessage(error, '环境列表加载失败'))
  } finally {
    loading.value = false
  }
}

// 重置表单
function resetForm() {
  form.value.name = ''
  form.value.baseUrl = ''
  form.value.variablesJson = ''
}

// 打开新增弹窗
function openCreateModal() {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
function openEditModal(item: Environment) {
  editingId.value = item.id

  form.value.name = item.name
  form.value.baseUrl = item.baseUrl
  form.value.variablesJson = item.variablesJson || ''

  modalVisible.value = true
}

// 校验环境变量 JSON
function validateVariablesJson(value: string) {
  if (!value.trim()) {
    return true
  }

  try {
    const parsed = JSON.parse(value)

    if (
      parsed === null ||
      Array.isArray(parsed) ||
      typeof parsed !== 'object'
    ) {
      Message.warning('环境变量必须是 JSON 对象')
      return false
    }

    return true
  } catch {
    Message.warning('环境变量 JSON 格式不正确')
    return false
  }
}

// 保存环境
async function saveEnvironment() {
  const name = form.value.name.trim()
  const baseUrl = form.value.baseUrl.trim()
  const variablesJson = form.value.variablesJson.trim()

  if (!name) {
    Message.warning('请输入环境名称')
    return
  }

  if (name.length > 64) {
    Message.warning('环境名称不能超过64个字符')
    return
  }

  if (!baseUrl) {
    Message.warning('请输入 Base URL')
    return
  }

  if (baseUrl.length > 500) {
    Message.warning('Base URL 不能超过500个字符')
    return
  }

  if (!validateVariablesJson(variablesJson)) {
    return
  }

  const data: EnvironmentRequest = {
    projectId: props.projectId,
    name,
    baseUrl,
    variablesJson: variablesJson || null
  }

  saving.value = true

  try {
    if (editingId.value !== null) {
      await updateEnvironment(editingId.value, data)
      Message.success('环境修改成功')
    } else {
      await createEnvironment(data)
      Message.success('环境创建成功')
    }

    modalVisible.value = false
    await loadEnvironments()
  } catch (error) {
    Message.error(getErrorMessage(error, '保存环境失败'))
  } finally {
    saving.value = false
  }
}

// 删除环境
async function removeEnvironment(id: number) {
  try {
    await deleteEnvironment(id)
    Message.success('环境删除成功')
    await loadEnvironments()
  } catch (error) {
    Message.error(getErrorMessage(error, '删除环境失败'))
  }
}

// 项目切换时重新加载环境
watch(
  () => props.projectId,
  () => {
    loadEnvironments()
  },
  { immediate: true }
)
</script>

<template>
  <div class="environment-management">
    <!-- 页面标题和操作 -->
    <div class="page-header">
      <div>
        <h2>环境管理</h2>
        <p>配置 API 自动化测试使用的环境地址和变量</p>
      </div>

      <a-space>
        <a-button
          :loading="loading"
          @click="loadEnvironments"
        >
          <template #icon>
            <icon-refresh />
          </template>
          刷新
        </a-button>

        <a-button
          type="primary"
          @click="openCreateModal"
        >
          + 新建环境
        </a-button>
      </a-space>
    </div>

    <!-- 环境列表 -->
    <a-table
      :data="environments"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      row-key="id"
      stripe
      :scroll="{ x: 900 }"
    >
      <template #columns>
        <a-table-column
          title="环境 ID"
          data-index="id"
          :width="90"
        />

        <a-table-column
          title="环境名称"
          data-index="name"
          :width="160"
        />

        <a-table-column
          title="Base URL"
          data-index="baseUrl"
          ellipsis
          tooltip
        />

        <a-table-column
          title="环境变量"
          :width="120"
        >
          <template #cell="{ record }">
            <a-tag
              :color="record.variablesJson ? 'green' : 'gray'"
            >
              {{ record.variablesJson ? '已配置' : '未配置' }}
            </a-tag>
          </template>
        </a-table-column>

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
          :width="160"
          fixed="right"
        >
          <template #cell="{ record }">
            <a-space>
              <a-button
                type="text"
                size="small"
                @click="openEditModal(record)"
              >
                编辑
              </a-button>

              <a-popconfirm
                content="确定删除这个环境吗？"
                @ok="removeEnvironment(record.id)"
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
        <a-empty description="当前项目暂无测试环境" />
      </template>
    </a-table>

    <!-- 新增 / 编辑环境弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="editingId !== null ? '编辑测试环境' : '新建测试环境'"
      :ok-loading="saving"
      :width="650"
      @ok="saveEnvironment"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="环境名称" required>
          <a-input
            v-model="form.name"
            placeholder="例如：测试环境、生产环境"
            :max-length="64"
          />
        </a-form-item>

        <a-form-item label="Base URL" required>
          <a-input
            v-model="form.baseUrl"
            placeholder="例如：https://jsonplaceholder.typicode.com"
          />
        </a-form-item>

        <a-form-item label="环境变量（JSON 对象）">
          <a-textarea
            v-model="form.variablesJson"
            placeholder='例如：{"userId":"1","version":"v1"}'
            :auto-size="{ minRows: 5, maxRows: 10 }"
          />

          <div class="field-tip">
            在 API 用例中使用 &#123;&#123;变量名&#125;&#125; 引用环境变量。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.environment-management {
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

.field-tip {
  margin-top: 8px;
  color: var(--color-text-3);
  font-size: 12px;
}
</style>
