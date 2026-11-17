import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/Dashboard.vue'),
      meta: {
        title: '工作台',
        menu: 'dashboard'
      }
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('../views/ProjectManagement.vue'),
      meta: {
        title: '项目管理',
        menu: 'projects'
      }
    },
    {
      path: '/workspace/:projectId/cases',
      name: 'cases',
      component: () => import('../views/ApiCaseManagement.vue'),
      props: route => ({
        projectId: Number(route.params.projectId)
      }),
      meta: {
        title: 'API 用例管理',
        menu: 'cases'
      }
    },
    {
      path: '/workspace/:projectId/environments',
      name: 'environments',
      component: () => import('../views/EnvironmentManagement.vue'),
      props: route => ({
        projectId: Number(route.params.projectId)
      }),
      meta: {
        title: '测试环境管理',
        menu: 'environments'
      }
    },
    {
      path: '/workspace/:projectId/scenarios',
      name: 'scenarios',
      component: () => import('../views/ScenarioManagement.vue'),
      props: route => ({
        projectId: Number(route.params.projectId)
      }),
      meta: {
        title: '测试场景管理',
        menu: 'scenarios'
      }
    },
    {
      path: '/workspace/:projectId/history',
      name: 'history',
      component: () => import('../views/ExecutionHistory.vue'),
      props: route => ({
        projectId: Number(route.params.projectId)
      }),
      meta: {
        title: '执行历史',
        menu: 'history'
      }
    },
    {
      path: '/workspace/:projectId/reports',
      name: 'reports',
      component: () => import('../views/TestReport.vue'),
      props: route => ({
        projectId: Number(route.params.projectId)
      }),
      meta: {
        title: '测试报告',
        menu: 'reports'
      }
    },
    {
      path: '/workspace/:projectId/trends',
      name: 'trends',
      component: () => import('../views/TestTrend.vue'),
      props: route => ({
        projectId: Number(route.params.projectId)
      }),
      meta: {
        title: '趋势分析',
        menu: 'trends'
      }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

export default router