<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import axios from 'axios'
import { http } from './api/http'
import { getProjects, type Project } from './api/project'

import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

interface LoginData {
  id: number
  username: string
  nickname: string
  token: string
  tokenType: string
}

interface Health {
  application: string
  status: string
  time: string
}

interface DbHealth {
  database: string
  status: string
}

const router = useRouter()
const route = useRoute()

const token = ref(localStorage.getItem('autotest_token') || '')
const username = ref(localStorage.getItem('autotest_username') || '')

const loginForm = ref({
  username: 'admin',
  password: ''
})

const projects = ref<Project[]>([])
const loading = ref(false)

const selectedProjectId = computed(() => {
  const id = Number(route.params.projectId)
  return Number.isInteger(id) && id > 0 ? id : null
})

const selectedProject = computed(() => {
  return projects.value.find(
    project => project.id === selectedProjectId.value
  ) || null
})

const activeMenu = computed(() => {
  return String(route.meta.menu || 'dashboard')
})

const projectName = computed(() => {
  return selectedProject.value?.name || ''
})

const isLoginPage = computed(() => !token.value)

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message || fallback
  }

  return fallback
}

// 登录
async function login() {
  if (!loginForm.value.username.trim() || !loginForm.value.password) {
    Message.warning('请输入用户名和密码')
    return
  }

  loading.value = true

  try {
    const response = await http.post<ApiResult<LoginData>>(
      '/auth/login',
      loginForm.value
    )

    const user = response.data.data

    token.value = user.token
    username.value = user.username

    localStorage.setItem('autotest_token', user.token)
    localStorage.setItem('autotest_username', user.username)

    loginForm.value.password = ''

    Message.success('登录成功')

    await loadProjects()
    await router.replace('/dashboard')
  } catch (error) {
    Message.error(getErrorMessage(error, '登录失败'))
  } finally {
    loading.value = false
  }
}

// 退出登录
function logout() {
  token.value = ''
  username.value = ''

  localStorage.removeItem('autotest_token')
  localStorage.removeItem('autotest_username')
  localStorage.removeItem('autotest_selected_project')

  projects.value = []

  router.replace('/dashboard')
  Message.success('已退出登录')
}

// 加载项目
async function loadProjects() {
  if (!token.value) return

  loading.value = true

  try {
    const response = await getProjects()
    projects.value = response.data.data

    if (
      selectedProjectId.value !== null &&
      !projects.value.some(
        project => project.id === selectedProjectId.value
      )
    ) {
      await router.replace('/projects')
    }
  } catch (error) {
    Message.error(getErrorMessage(error, '项目列表加载失败'))
  } finally {
    loading.value = false
  }
}

// 导航
function navigate(key: string) {
  const globalRoutes: Record<string, string> = {
    dashboard: '/dashboard',
    projects: '/projects',
    cases: '/cases',
    environments: '/environments',
    scenarios: '/scenarios',
    history: '/history',
    reports: '/reports',
    trends: '/trends'
  }

  const target = globalRoutes[key]
  if (target) {
    router.push(target)
    return
  }
}

// 从工作台或项目管理进入项目
function openProject(project: Project, module: string) {
  localStorage.setItem('autotest_selected_project', String(project.id))
  navigate(module)
}

// 返回工作台
function backHome() {
  router.push('/dashboard')
}

// 后台静默检查后端和数据库
async function checkHealth() {
  try {
    await http.get<ApiResult<Health>>('/health')
    await http.get<ApiResult<DbHealth>>('/health/db')
  } catch (error) {
    console.error('系统健康检查失败：', error)
  }
}

// 记录当前访问项目
watch(
  selectedProjectId,
  id => {
    if (id !== null) {
      localStorage.setItem('autotest_selected_project', String(id))
    }
  }
)

onMounted(async () => {
  await checkHealth()

  if (token.value) {
    await loadProjects()
  }
})
</script>

<template>
  <!-- 登录页面 -->
  <div v-if="isLoginPage" class="login-page">
    <a-card class="login-card">
      <div class="login-brand">
        <div class="login-logo">
          <icon-robot size="32" />
        </div>

        <h1>AutoTest Hub</h1>
        <p>自动化测试管理平台</p>
      </div>

      <a-form
        :model="loginForm"
        layout="vertical"
        @submit-success="login"
      >
        <a-form-item field="username" label="用户名">
          <a-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            allow-clear
          >
            <template #prefix>
              <icon-user />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item field="password" label="密码">
          <a-input-password
            v-model="loginForm.password"
            placeholder="请输入密码"
            @press-enter="login"
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>

        <a-button
          type="primary"
          long
          html-type="submit"
          :loading="loading"
        >
          登 录
        </a-button>
      </a-form>
    </a-card>
  </div>

  <!-- 后台管理布局 -->
  <a-layout v-else class="app-layout">
    <AppSidebar
      :active-menu="activeMenu"
      @navigate="navigate"
    />

    <a-layout class="main-layout">
      <AppHeader
        :username="username"
        :project-name="projectName"
        @logout="logout"
        @back-home="backHome"
      />

      <a-layout-content class="main-content">
        <div class="content-inner">
          <router-view
            :projects="projects"
            :loading="loading"
            @refresh="loadProjects"
            @navigate="navigate"
            @open-project="openProject"
          />
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
/* 整个后台固定在浏览器视口内 */
.app-layout {
  display: flex !important;
  flex-direction: row !important;
  align-items: stretch !important;

  width: 100%;
  height: 100vh;
  height: 100dvh;
  min-height: 0;

  background: #f2f3f5;

  overflow: hidden;
  box-sizing: border-box;
}

/* 右侧整体布局 */
.main-layout {
  display: flex !important;
  flex: 1 1 0 !important;
  flex-direction: column !important;

  width: 0;
  min-width: 0;
  height: 100%;
  min-height: 0;

  overflow: hidden;
}

/* 顶部 Header 不参与滚动 */
.main-layout > :deep(.app-header) {
  flex: 0 0 64px;
}

/* 右侧内容独立滚动 */
.main-content {
  flex: 1 1 0 !important;

  width: 100%;
  min-width: 0;
  min-height: 0;

  padding: 26px 24px;

  overflow-x: auto;
  overflow-y: auto;

  background: #f2f3f5;
  box-sizing: border-box;
}

/* 内容最大宽度 */
.content-inner {
  width: 100%;
  max-width: 1800px;
  min-width: 0;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 登录页面 */
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;

  width: 100%;
  min-height: 100vh;
  padding: 24px;

  box-sizing: border-box;

  background: linear-gradient(
    135deg,
    #e8f3ff 0%,
    #f2f3f5 55%,
    #e8eeff 100%
  );
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(29, 33, 41, 0.08);
}

.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  width: 64px;
  height: 64px;
  margin-bottom: 12px;

  color: #ffffff;
  background: #165dff;
  border-radius: 16px;
}

.login-brand h1 {
  margin: 0;
  color: #1d2129;
  font-size: 26px;
  font-weight: 700;
}

.login-brand p {
  margin-top: 8px;
  color: #86909c;
  font-size: 14px;
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px 12px;
  }
}
</style>