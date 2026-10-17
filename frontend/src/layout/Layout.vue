<template>
  <el-container class="layout">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="logo">🎓 学生选课系统</div>
      <div class="user">
        <span class="name">{{ store.name }}（{{ roleText }}）</span>
        <el-button link type="primary" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container class="body">
      <!-- 左侧菜单 -->
      <el-aside width="200px" class="aside">
        <el-menu :default-active="$route.path" router class="menu">
          <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon>
            <span>{{ m.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <!-- 主内容 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const router = useRouter()

const roleText = computed(() => {
  const map = { student: '学生', teacher: '教师', admin: '管理员' }
  return map[store.role] || ''
})

// 根据角色生成左侧菜单
const menus = computed(() => {
  const all = {
    student: [
      { path: '/student/profile', title: '个人信息', icon: 'User' },
      { path: '/student/batch', title: '选课中心', icon: 'Search' },
      { path: '/student/result', title: '选课结果', icon: 'Finished' },
      { path: '/student/schedule', title: '我的课表', icon: 'Calendar' },
      { path: '/student/scores', title: '我的成绩', icon: 'DataAnalysis' }
    ],
    teacher: [
      { path: '/teacher/profile', title: '个人信息', icon: 'User' },
      { path: '/teacher/courses', title: '我的课程', icon: 'Collection' },
      { path: '/teacher/schedule', title: '我的课表', icon: 'Calendar' }
    ],
    admin: [
      { path: '/admin/students', title: '学生管理', icon: 'User' },
      { path: '/admin/teachers', title: '教师管理', icon: 'Avatar' },
      { path: '/admin/courses', title: '课程管理', icon: 'Collection' },
      { path: '/admin/classes', title: '班级管理', icon: 'School' },
      { path: '/admin/semesters', title: '学期管理', icon: 'Calendar' }
    ]
  }
  return all[store.role] || []
})

function handleLogout() {
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #001529;
  color: #fff;
}
.logo {
  font-size: 20px;
  font-weight: bold;
}
.user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.name {
  color: #ddd;
}
.body {
  height: calc(100% - 60px);
}
.aside {
  background: #fff;
  border-right: 1px solid #eee;
}
.menu {
  border-right: none;
}
.main {
  background: #f5f6f8;
}
</style>
