import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// Configure NProgress
NProgress.configure({ showSpinner: false })

// Route definitions
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  // FIXED: P0-FE-ROUTES - 添加注册路由
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { requiresAuth: true, title: '首页', icon: 'HomeFilled' }
      },
      {
        path: '/books',
        name: 'Books',
        component: () => import('@/views/book/BookList.vue'),
        meta: { requiresAuth: true, title: '图书管理', icon: 'Collection' }
      },
      // FIXED: P0-FE-ROUTES - 添加图书新增路由
      {
        path: '/books/add',
        name: 'BookAdd',
        component: () => import('@/views/book/BookAdd.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '新增图书', hidden: true }
      },
      // FIXED: P0-FE-ROUTES - 添加图书详情路由
      {
        path: '/books/:id',
        name: 'BookDetail',
        component: () => import('@/views/book/BookDetail.vue'),
        meta: { requiresAuth: true, title: '图书详情', hidden: true }
      },
      {
        path: '/readers',
        name: 'Readers',
        component: () => import('@/views/ReaderList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '读者管理', icon: 'UserFilled' }
      },
      {
        path: '/borrows',
        name: 'Borrows',
        component: () => import('@/views/borrow/BorrowList.vue'),
        meta: { requiresAuth: true, title: '借阅管理', icon: 'Reading' }
      },
      // FIXED: P0-FE-ROUTES - 添加借阅页面路由
      {
        path: '/borrows/page',
        name: 'BorrowPage',
        component: () => import('@/views/borrow/BorrowPage.vue'),
        meta: { requiresAuth: true, title: '借阅图书', hidden: true }
      },
      {
        path: '/seats',
        name: 'Seats',
        component: () => import('@/views/SeatList.vue'),
        meta: { requiresAuth: true, title: '座位预约', icon: 'OfficeBuilding' }
      },
      // FIXED: P0-FE-ROUTES - 添加座位预约路由
      {
        path: '/seats/reserve',
        name: 'SeatReserve',
        component: () => import('@/views/seat/SeatReserve.vue'),
        meta: { requiresAuth: true, title: '预约座位', hidden: true }
      },
      // FIXED: P0-FE-ROUTES - 添加座位地图路由
      {
        path: '/seats/map',
        name: 'SeatMap',
        component: () => import('@/views/seat/SeatMap.vue'),
        meta: { requiresAuth: true, title: '座位地图', hidden: true }
      },
      {
        path: '/announcements',
        name: 'Announcements',
        component: () => import('@/views/AnnouncementList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '公告管理', icon: 'BellFilled' }
      },
      {
        path: '/volunteers',
        name: 'Volunteers',
        component: () => import('@/views/VolunteerList.vue'),
        meta: { requiresAuth: true, title: '志愿服务', icon: 'Service' }
      },
      {
        path: '/statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '统计分析', icon: 'TrendCharts' }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { requiresAuth: true, title: '个人中心', hidden: true }
      },
      // FIXED: FE-004 - 积分详情路由
      {
        path: '/profile/credit',
        name: 'CreditDetail',
        component: () => import('@/views/profile/CreditView.vue'),
        meta: { requiresAuth: true, title: '积分详情', hidden: true }
      },
      {
        path: '/credit',
        name: 'Credit',
        component: () => import('@/views/profile/CreditView.vue'),
        meta: { requiresAuth: true, title: '信用积分', icon: 'Coin' }
      },
      {
        path: '/compensations',
        name: 'Compensations',
        component: () => import('@/views/compensation/CompensationList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '赔偿管理', icon: 'Warning' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// Navigation guards
// FIXED: P1-FE-03 - 补充权限校验和页面访问控制
router.beforeEach(async (to, from, next) => {
  NProgress.start()
  
  // Set page title
  document.title = to.meta.title ? `${to.meta.title} - 图书馆管理系统` : '图书馆管理系统'
  
  const userStore = useUserStore()
  
  // Check if route requires authentication
  if (!to.meta.public && to.meta.requiresAuth !== false) {
    if (!userStore.token) {
      next('/login')
      return
    }
    
    // Fetch user info if not loaded
    // FIXED: PERF-02 增加fetching锁防止并发请求
    if (!userStore.userInfo && !userStore._fetchingUserInfo) {
      try {
        userStore._fetchingUserInfo = true
        await userStore.fetchUserInfo()
      } catch {
        // fetchUserInfo内部已处理，如果已有userInfo不会登出
        if (!userStore.userInfo) {
          userStore.logout()
          next('/login')
          return
        }
      } finally {
        userStore._fetchingUserInfo = false
      }
    }
    
    // FIXED: P1-FE-03 - 权限校验：检查用户角色是否允许访问该页面
    if (to.meta.roles && to.meta.roles.length > 0) {
      const userRole = userStore.userInfo?.role
      const hasPermission = to.meta.roles.includes(userRole)
      if (!hasPermission) {
        ElMessage.error('您没有权限访问该页面')
        // FIXED: QUAL-04 当来源页是公开页面时，跳转dashboard
        const fromIsPublic = from.meta?.public || false
        next(fromIsPublic ? '/dashboard' : (from.path || '/dashboard'))
        return
      }
    }
  }
  
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
