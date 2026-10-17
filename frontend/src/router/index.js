import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/home',
    children: [
      // 首页（所有角色可访问）
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      // 学生端
      { path: 'student/batch', name: 'StudentBatch', component: () => import('@/views/student/SelectionBatch.vue'), meta: { role: 'student', title: '选课中心' } },
      { path: 'student/courses', name: 'StudentCourses', component: () => import('@/views/student/CourseCenter.vue'), meta: { role: 'student', title: '课程列表' } },
      { path: 'student/result', name: 'StudentResult', component: () => import('@/views/student/SelectionResult.vue'), meta: { role: 'student', title: '选课结果' } },
      { path: 'student/schedule', name: 'StudentSchedule', component: () => import('@/views/student/MySchedule.vue'), meta: { role: 'student', title: '我的课表' } },
      { path: 'student/scores', name: 'StudentScores', component: () => import('@/views/student/MyScore.vue'), meta: { role: 'student', title: '我的成绩' } },
      { path: 'student/profile', name: 'StudentProfile', component: () => import('@/views/Profile.vue'), meta: { role: 'student', title: '个人信息' } },
      // 教师端
      { path: 'teacher/courses', name: 'TeacherCourses', component: () => import('@/views/teacher/MyCourses.vue'), meta: { role: 'teacher', title: '我的课程' } },
      { path: 'teacher/schedule', name: 'TeacherSchedule', component: () => import('@/views/teacher/MySchedule.vue'), meta: { role: 'teacher', title: '我的课表' } },
      { path: 'teacher/course/:courseId/students', name: 'TeacherStudents', component: () => import('@/views/teacher/StudentList.vue'), meta: { role: 'teacher', title: '学生名单' } },
      { path: 'teacher/profile', name: 'TeacherProfile', component: () => import('@/views/Profile.vue'), meta: { role: 'teacher', title: '个人信息' } },
      // 管理员端
      { path: 'admin/students', name: 'AdminStudents', component: () => import('@/views/admin/StudentManage.vue'), meta: { role: 'admin', title: '学生管理' } },
      { path: 'admin/teachers', name: 'AdminTeachers', component: () => import('@/views/admin/TeacherManage.vue'), meta: { role: 'admin', title: '教师管理' } },
      { path: 'admin/courses', name: 'AdminCourses', component: () => import('@/views/admin/CourseManage.vue'), meta: { role: 'admin', title: '课程管理' } },
      { path: 'admin/classes', name: 'AdminClasses', component: () => import('@/views/admin/ClassManage.vue'), meta: { role: 'admin', title: '班级管理' } },
      { path: 'admin/semesters', name: 'AdminSemesters', component: () => import('@/views/admin/SemesterManage.vue'), meta: { role: 'admin', title: '学期管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫：未登录跳登录页，按角色控制访问
router.beforeEach((to, from, next) => {
  const store = useUserStore()
  if (to.path === '/login') {
    return next()
  }
  if (!store.token) {
    return next('/login')
  }
  // 若路由声明了角色，则校验当前用户角色
  if (to.meta.role && to.meta.role !== store.role) {
    return next('/home')
  }
  next()
})

export default router
