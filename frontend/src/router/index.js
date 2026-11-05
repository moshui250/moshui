import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/contests',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'contest-list',
        component: () => import('@/views/ContestListView.vue'),
        meta: { title: '竞赛' }
      },
      {
        path: ':id',
        name: 'contest-detail',
        component: () => import('@/views/ContestDetailView.vue'),
        meta: { title: '竞赛详情' }
      },
      {
        path: ':id/solve/:questionId',
        name: 'contest-solve',
        component: () => import('@/views/QuestionSolveView.vue'),
        meta: { title: '竞赛答题' }
      }
    ]
  },
  {
    path: '/dashboard',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '学习看板' }
      }
    ]
  },
  {
    path: '/wrong-book',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'wrong-book',
        component: () => import('@/views/WrongBookView.vue'),
        meta: { title: '错题本' }
      }
    ]
  },
  {
    path: '/question',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'question-list',
        component: () => import('@/views/QuestionListView.vue'),
        meta: { title: '题库' }
      },
      {
        path: ':id(\\d+)',
        name: 'question-detail',
        component: () => import('@/views/QuestionDetailView.vue'),
        meta: { title: '题目详情' }
      },
      {
        path: ':id(\\d+)/solve',
        name: 'question-solve',
        component: () => import('@/views/QuestionSolveView.vue'),
        meta: { title: '答题' }
      }
    ]
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页', public: true, layout: 'home' }
      },
      {
        path: 'question/import',
        name: 'question-import',
        component: () => import('@/views/QuestionImportView.vue'),
        meta: { title: '图片录题', section: '练习' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心', section: '账户' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/question'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

function loginLocation(fullPath) {
  return { name: 'login', query: { redirect: fullPath } }
}

function isAuthError(error) {
  return error?.code === 40100 || error?.status === 401
}

function safeRedirect(raw) {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//')) {
    return ''
  }
  if (raw === '/login' || raw.startsWith('/login?') || raw.startsWith('/login/')) {
    return ''
  }
  if (raw === '/register' || raw.startsWith('/register?') || raw.startsWith('/register/')) {
    return ''
  }
  return raw
}

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · CodeAgent` : 'CodeAgent · 智码刷题'

  const userStore = useUserStore()

  // 首页始终公开；已登录用户也可以停留在首页。
  if (to.name === 'home') return true

  // 登录/注册页：已登录则直接进入目标页；token 失效则允许重新登录。
  if (to.meta.public) {
    if (!userStore.token) return true

    if (!userStore.user) {
      try {
        await userStore.fetchMe()
      } catch (error) {
        if (isAuthError(error)) {
          userStore.clearAuth()
        }
        return true
      }
    }

    if (!userStore.user) return true

    const target = safeRedirect(to.query.redirect) || '/question'
    return target === to.fullPath ? true : target
  }

  // 业务页：没有 token 直接跳登录，并保留原始地址。
  if (!userStore.token) {
    return loginLocation(to.fullPath)
  }

  // 刷新页面后 store 里只有 token，没有 user，必须先拉取 /auth/me。
  if (!userStore.user) {
    try {
      await userStore.fetchMe()
    } catch (error) {
      if (isAuthError(error)) {
        userStore.clearAuth()
        return loginLocation(to.fullPath)
      }

      // 网络异常不直接清空本地 token，避免后端短暂不可用时误退出。
      ElMessage.error('无法校验登录状态，请检查后端服务')
      return true
    }
  }

  return true
})

export default router
