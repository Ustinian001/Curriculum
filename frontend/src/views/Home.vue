<template>
  <div class="home">
    <el-card>
      <h2>欢迎，{{ store.name }}！</h2>
      <p class="role">当前角色：{{ roleText }}</p>
      <el-divider />
      <p>请从左侧菜单选择功能模块开始使用。</p>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const roleText = computed(() => {
  const map = { student: '学生', teacher: '教师', admin: '管理员' }
  return map[store.role] || ''
})
</script>

<style scoped>
.home {
  max-width: 800px;
}
.role {
  color: #666;
}
</style>
