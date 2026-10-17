<template>
  <div>
    <!-- 学分统计 -->
    <el-row :gutter="16" class="stats">
      <el-col :span="8">
        <el-card><div class="stat-num">{{ totalCredit }}</div><div class="stat-label">已修总学分</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="stat-num">{{ passedCredit }}</div><div class="stat-label">已通过学分</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="stat-num">{{ items.length }}</div><div class="stat-label">总课程门数</div></el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header><span>我的成绩</span></template>
      <el-table :data="items" v-loading="loading" border stripe>
        <el-table-column prop="cno" label="课程编号" width="100" />
        <el-table-column prop="cname" label="课程名称" width="140" />
        <el-table-column prop="credit" label="学分" width="70" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="semesterName" label="学期" width="120" />
        <el-table-column prop="schedule" label="时间" width="150" />
        <el-table-column label="成绩" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.score != null" :type="row.score >= 60 ? 'success' : 'danger'">{{ row.score }}</el-tag>
            <el-tag v-else type="info">未录入</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getScores } from '@/api/student'

const items = ref([])
const totalCredit = ref(0)
const passedCredit = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getScores()
    items.value = res.data.items || []
    totalCredit.value = res.data.totalCredit || 0
    passedCredit.value = res.data.passedCredit || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stats {
  margin-bottom: 16px;
}
.stat-num {
  font-size: 30px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
}
.stat-label {
  text-align: center;
  color: #909399;
  margin-top: 6px;
}
</style>
