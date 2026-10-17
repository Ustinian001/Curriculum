<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>选课结果</span>
        <span class="stat">已选 {{ list.length }} 门 / {{ totalCredit }} 学分</span>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="cno" label="课程编号" width="100" />
      <el-table-column prop="cname" label="课程名称" width="150" />
      <el-table-column label="课程类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.courseType)" size="small">{{ row.courseType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="credit" label="学分" width="70" />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="location" label="地点" width="110" />
      <el-table-column prop="schedule" label="时间" width="150" />
      <el-table-column prop="semesterName" label="学期" width="130" />
      <el-table-column prop="selectTime" label="选课时间" width="170" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSelectionResult } from '@/api/student'

const list = ref([])
const loading = ref(false)

const totalCredit = computed(() => list.value.reduce((sum, c) => sum + (c.credit || 0), 0))

// 根据课程类型返回标签颜色
function typeTag(type) {
  if (type === '公共必修') return 'info'
  if (type === '专业类') return 'warning'
  return 'success' // 选修类
}

async function load() {
  loading.value = true
  try {
    const res = await getSelectionResult()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat {
  color: #666;
  font-size: 14px;
}
</style>
