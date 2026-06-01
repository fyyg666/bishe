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
        path: 'borrows/:id',
        name: 'BorrowDetail',
        component: () => import('@/views/borrow/BorrowDetail.vue'),
        meta: { requiresAuth: true, title: '借阅详情', hidden: true }
      },
      {
        path: '/borrow-rules',
        name: 'BorrowRules',
        component: () => import('@/views/borrow/BorrowRuleList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '借阅规则', icon: 'List' }
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
        redirect: '/profile/credit'
      },
      {
        path: '/compensations',
        name: 'Compensations',
        component: () => import('@/views/compensation/CompensationList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '赔偿管理', icon: 'Warning' }
      },
      {
        path: '/budget-funds',
        name: 'BudgetFunds',
        component: () => import('@/views/budget/BudgetList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '预算管理', icon: 'Wallet' }
      },
      {
        path: '/suggestions',
        name: 'Suggestions',
        component: () => import('@/views/suggestion/SuggestionList.vue'),
        meta: { requiresAuth: true, title: '荐购管理', icon: 'ShoppingCart' }
      },
      {
        path: '/marc',
        name: 'MarcRecordList',
        component: () => import('@/views/marc/MarcRecordList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: 'MARC编目', icon: 'Document' }
      },
      {
        path: '/marc/create',
        name: 'MarcCreate',
        component: () => import('@/views/marc/MarcEditor.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '新建MARC记录', hidden: true }
      },
      {
        path: '/marc/:id/edit',
        name: 'MarcEdit',
        component: () => import('@/views/marc/MarcEditor.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '编辑MARC记录', hidden: true }
      },
      {
        path: '/z3950',
        name: 'Z3950Search',
        component: () => import('@/views/marc/Z3950Search.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: 'Z39.50联机编目', icon: 'Connection' }
      },
      {
        path: '/vendors',
        name: 'Vendors',
        component: () => import('@/views/acquisition/VendorList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '供应商管理', icon: 'Shop' }
      },
      {
        path: '/purchase-orders',
        name: 'PurchaseOrders',
        component: () => import('@/views/acquisition/PurchaseOrderList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '采购管理', icon: 'ShoppingCart' }
      },
      {
        path: '/purchase-orders/:id',
        name: 'PurchaseOrderDetail',
        component: () => import('@/views/acquisition/PurchaseOrderDetail.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '采购订单详情', hidden: true }
      },
      {
        path: '/digital-resources',
        name: 'DigitalResources',
        component: () => import('@/views/digital/DigitalResourceList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '数字资源管理', icon: 'Monitor' }
      },
      {
        path: '/unified-search',
        name: 'UnifiedSearch',
        component: () => import('@/views/search/UnifiedSearch.vue'),
        meta: { requiresAuth: true, title: '统一检索', icon: 'Search' }
      },
      {
        path: '/serial/subscriptions',
        name: 'SerialSubscriptions',
        component: () => import('@/views/serial/SerialSubscriptionList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '期刊管理', icon: 'Notebook' }
      },
      {
        path: '/serial/issues',
        name: 'SerialIssues',
        component: () => import('@/views/serial/SerialIssueList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '期刊到刊', hidden: true }
      },
      {
        path: '/serial/claims',
        name: 'SerialClaims',
        component: () => import('@/views/serial/SerialClaimList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '催缺管理', hidden: true }
      },
      {
        path: '/serial/routings',
        name: 'SerialRoutings',
        component: () => import('@/views/serial/SerialRoutingList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '期刊路由分发', icon: 'Guide' }
      },
      {
        path: '/branches',
        name: 'Branches',
        component: () => import('@/views/branch/BranchList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '分馆管理', icon: 'School' }
      },
      {
        path: '/reports',
        name: 'Reports',
        component: () => import('@/views/report/ReportList.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LIBRARIAN'], title: '自定义报表', icon: 'DataLine' }
      }
    ]
  },
  {
    path: '/digital/read/:id',
    name: 'DigitalReader',
    component: () => import('@/views/digital/DigitalReader.vue'),
    meta: { requiresAuth: true, title: '在线阅读' }
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
  
  const isPublic = to.meta.public === true
  const requiresAuth = to.meta.requiresAuth !== false
  if (!isPublic && requiresAuth) {
    if (!userStore.token) {
      next('/login')
      return
    }
    
    // Fetch user info if not loaded
    // FIXED: PERF-02 增加fetching锁防止并发请求
    if (!userStore.userInfo && !userStore.fetchingUserInfo) {
      try {
        userStore.fetchingUserInfo = true
        await userStore.fetchUserInfo()
      } catch {
        // fetchUserInfo内部已处理，如果已有userInfo不会登出
        if (!userStore.userInfo) {
          userStore.logout()
          next('/login')
          return
        }
      } finally {
        userStore.fetchingUserInfo = false
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
