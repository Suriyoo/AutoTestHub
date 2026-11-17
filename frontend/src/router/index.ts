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
      path: '/cases',
      name: 'cases',
      component: () => import('../views/ApiCaseManagement.vue'),
      meta: { title: 'API 用例管理', menu: 'cases' }
    },
    {
      path: '/environments',
      name: 'environments',
      component: () => import('../views/EnvironmentManagement.vue'),
      meta: { title: '测试环境管理', menu: 'environments' }
    },
    {
      path: '/scenarios',
      name: 'scenarios',
      component: () => import('../views/ScenarioManagement.vue'),
      meta: { title: '测试场景管理', menu: 'scenarios' }
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('../views/ExecutionHistory.vue'),
      meta: { title: '执行历史', menu: 'history' }
    },
    {
      path: '/reports',
      name: 'reports',
      component: () => import('../views/TestReport.vue'),
      meta: { title: '测试报告', menu: 'reports' }
    },
    {
      path: '/trends',
      name: 'trends',
      component: () => import('../views/TestTrend.vue'),
      meta: { title: '趋势分析', menu: 'trends' }
    },
    {
      path: '/workspace/:projectId/:module(cases|environments|scenarios|history|reports|trends)',
      redirect: to => {
        const module = String(to.params.module)
        return module === 'history' ? '/history' : `/${module}`
      }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

export default router