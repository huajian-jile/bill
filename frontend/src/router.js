import { createRouter, createWebHashHistory } from 'vue-router'
import Login from './views/Login.vue'
import Layout from './views/Layout.vue'

const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        redirect: () => {
          try {
            const phones = JSON.parse(localStorage.getItem('phones') || '[]')
            const authorities = JSON.parse(localStorage.getItem('authorities') || '[]')
            const isAdmin = authorities.includes('PERM_USER_ADMIN')
            if (phones.length > 0) return '/import'
            if (isAdmin) return '/analytics'
            return '/phones'
          } catch {
            return '/phones'
          }
        }
      },
      { path: 'analytics/group', component: () => import('./views/CounterpartyGroupBoard.vue') },
      { path: 'analytics', component: () => import('./views/AnalyticsHub.vue') },
      { path: 'analytics/day', redirect: '/analytics' },
      { path: 'analytics/month', redirect: '/analytics' },
      { path: 'analytics/type', redirect: '/analytics' },
      { path: 'analytics/real', redirect: '/analytics' },
      { path: 'import', component: () => import('./views/ImportXlsx.vue') },
      { path: 'bills', component: () => import('./views/BillData.vue') },
      { path: 'phones', component: () => import('./views/PhoneBind.vue') },
      {
        path: 'memo',
        component: () => import('./views/PlaceholderPage.vue'),
        props: { title: '备忘录' }
      },
      {
        path: 'tools',
        component: () => import('./views/PlaceholderPage.vue'),
        props: { title: '其他工具' }
      },
      {
        path: 'admin/users',
        component: () => import('./views/AdminUsers.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/phone-binds',
        component: () => import('./views/PhoneBindApprovals.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next('/login')
    return
  }
  if (to.meta.requiresAdmin) {
    try {
      const a = JSON.parse(localStorage.getItem('authorities') || '[]')
      if (!a.includes('PERM_USER_ADMIN')) {
        next('/import')
        return
      }
    } catch {
      next('/import')
      return
    }
  }
  if (token && !to.meta.public) {
    try {
      const authorities = JSON.parse(localStorage.getItem('authorities') || '[]')
      const phones = JSON.parse(localStorage.getItem('phones') || '[]')
      const isAdmin = authorities.includes('PERM_USER_ADMIN')
      // 未绑手机时仍允许进入分析/分组看板（可看空数据或提示）；导入、账单等仍走下方拦截
      const phoneGateExempt =
        to.path === '/phones' || to.path.startsWith('/analytics')
      if (!isAdmin && (!phones || phones.length === 0) && !phoneGateExempt) {
        next('/phones')
        return
      }
    } catch {
      /* ignore */
    }
  }
  next()
})

export default router
