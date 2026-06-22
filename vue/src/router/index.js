import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'
import FrontendLayout from '@/components/FrontendLayout.vue'


const BackendRoutes =[
  {
    path: '/back',
    redirect: '/back/dashboard',
  component: BackendLayout,
  children: [
    {path: 'dashboard', component: () => import('@/views/dashboard.vue'), meta: {title: '数据分析', icon: 'PieChart'}},
    {path: 'knowledge', component: () => import('@/views/knowledge.vue'), meta: {title: '知识文章', icon: 'Document'}},
    {path: 'consultations', component: () => import('@/views/consultations.vue'), meta: {title: '咨询记录', icon: 'ChatDotSquare'}},
    {path: 'emotions', component: () => import('@/views/emotions.vue'), meta: {title: '情绪日志', icon: 'User'}}
  ]
  },
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {path: 'login', component: () => import('@/views/login.vue'), meta: {title: '登录', icon: 'Login'}},
      {path: 'register', component: () => import('@/views/register.vue'), meta: {title: '注册', icon: 'UserPlus'}}
    ]
  }
]

const frontendRoutes =[
  {
    path: '/',
    redirect: '/home',
    component: FrontendLayout,
    children: [
      {path: 'home', component: () => import('@/views/home.vue'), meta: {title: '首页', icon: 'Home'}},
      {path: 'consultation', component: () => import('@/views/consultation.vue'), meta: {title: '咨询', icon: 'ChatDotSquare'}},
      {path: 'emotionDiary', component: () => import('@/views/emotionDiary.vue'), meta: {title: '情绪日记', icon: 'User'}},
      {path: 'knowledge', component: () => import('@/views/FrontendKnowledge.vue'), meta: {title: '知识文章', icon: 'Document'}},
      {path: 'knowledge/article/:id', component: () => import('@/views/articleDetail.vue'), props: true, meta: {title: '文章详情'}}
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [...BackendRoutes, ...frontendRoutes]
})

import { useUserStore } from '@/stores/user'

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if(token){
    const userStore = useUserStore()
    if (userStore.isAdmin) {
      if(to.path.startsWith('/back')){
        next()
      } else{
        next('/back/dashboard')
      }
    } else {
      //用户端账号只能访问前台路由
      if(to.path.startsWith('/back' || '/auth')){
        next('/auth/login')
      } else{
        next()
      }
    }
  }else if(to.path.startsWith('/back')){
    //若访问后台页面，且未登录，跳转到登录页
    next('/auth/login')
  } else {
    next()
  }

})

export default router


