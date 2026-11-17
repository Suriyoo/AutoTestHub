
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import axios from 'axios'
import {
  createProject,
  updateProject,
  deleteProject,
  type Project
} from '../api/project'

defineProps<{
  projects: Project[]
  loading: boolean
}>()

const emit = defineEmits<{
  refresh: []
  openProject: [project: Project, module: string]
}>()

const modalVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const projectForm = reactive({
  name: '',
  description: ''
})

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message || fallback
  }
  return fallback
}

function openCreateModal() {
  editingId.value = null
  projectForm.name = ''
  projectForm.description = ''
  modalVisible.value = true
}

function openEditModal(project: Project) {
  editingId.value = project.id
  projectForm.name = project.name
  projectForm.description = project.description || ''
  modalVisible.value = true
}

async function saveProject() {
  const name = projectForm.name.trim()
  const description = projectForm.description.trim()

  if (!name) {
    Message.warning('项目名称不能为空')
    return
  }

  if (name.length > 100 || description.length > 500) {
    Message.warning('项目名称最多100个字符，项目描述最多500个字符')
    return
  }

  saving.value = true

  try {
    const data = { name, description }

    if (editingId.value !== null) {
      await updateProject(editingId.value, data)
      Message.success('项目修改成功')
    } else {
      await createProject(data)
      Message.success('项目创建成功')
    }

    modalVisible.value = false
    emit('refresh')
  } catch (error) {
    Message.error(getErrorMessage(error, '保存项目失败'))
  } finally {
    saving.value = false
  }
}

async function removeProject(id: number) {
  try {
    await deleteProject(id)
    Message.success('项目删除成功')
    emit('refresh')
  } catch (error) {
    Message.error(getErrorMessage(error, '删除项目失败'))
  }
}
</script>

<template>
  <div class="project-management">
    <a-card class="project-card">
      <template #title>
        <div class="card-title">
          <span>项目列表</span>
          <a-tag color="arcoblue">共 {{ projects.length }} 个项目</a-tag>
        </div>
      </template>

      <template #extra>
        <a-space>
          <a-button @click="emit('refresh')">
            <template #icon><icon-refresh /></template>
            刷新
          </a-button>
          <a-button type="primary" @click="openCreateModal">
            <template #icon><icon-plus /></template>
            新建项目
          </a-button>
        </a-space>
      </template>

      <a-table
        :data="projects"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        stripe
      >
        <template #columns>
          <a-table-column title="项目 ID" data-index="id" :width="90" />
          <a-table-column title="项目名称" data-index="name" :width="180" />
          <a-table-column title="项目描述" data-index="description" ellipsis tooltip />
          <a-table-column title="创建时间" data-index="createdAt" :width="190">
            <template #cell="{ record }">
              {{ record.createdAt?.replace('T', ' ') || '-' }}
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="190" fixed="right">
            <template #cell="{ record }">
              <a-space>
                <a-button
                  type="primary"
                  size="small"
                  @click="emit('openProject', record, 'scenarios')"
                >
                  进入项目
                </a-button>

                <a-dropdown>
                  <a-button size="small">
                    更多 <icon-down />
                  </a-button>
                  <template #content>
                    <a-doption @click="openEditModal(record)">
                      <icon-edit /> 编辑项目
                    </a-doption>
                    <a-doption @click="emit('openProject', record, 'cases')">
                      <icon-code-square /> API 用例
                    </a-doption>
                    <a-doption @click="emit('openProject', record, 'environments')">
                      <icon-cloud /> 测试环境
                    </a-doption>
                    <a-doption @click="emit('openProject', record, 'scenarios')">
                      <icon-branch /> 测试场景
                    </a-doption>
                    <a-doption @click="emit('openProject', record, 'history')">
                      <icon-history /> 执行历史
                    </a-doption>
                    <a-doption @click="emit('openProject', record, 'reports')">
                      <icon-file /> 测试报告
                    </a-doption>
                    <a-doption @click="emit('openProject', record, 'trends')">
                      <icon-chart-bar /> 趋势分析
                    </a-doption>
                    <a-doption
                      class="danger-option"
                      @click="removeProject(record.id)"
                    >
                      <icon-delete /> 删除项目
                    </a-doption>
                  </template>
                </a-dropdown>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>

      <a-empty
        v-if="!loading && projects.length === 0"
        description="暂无项目，点击新建项目开始使用"
      />
    </a-card>

    <a-modal
      v-model:visible="modalVisible"
      :title="editingId !== null ? '编辑项目' : '新建项目'"
      :ok-loading="saving"
      @ok="saveProject"
    >
      <a-form :model="projectForm" layout="vertical">
        <a-form-item label="项目名称" required>
          <a-input
            v-model="projectForm.name"
            placeholder="请输入项目名称"
            :max-length="100"
            show-word-limit
          />
        </a-form-item>

        <a-form-item label="项目描述">
          <a-textarea
            v-model="projectForm.description"
            placeholder="请输入项目描述"
            :max-length="500"
            show-word-limit
            :auto-size="{ minRows: 3, maxRows: 6 }"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.project-management {
  width: 100%;
}

.project-card {
  border-radius: 9px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.danger-option) {
  color: #f53f3f;
}
</style>
