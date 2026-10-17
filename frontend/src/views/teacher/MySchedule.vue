<template>
  <el-card>
    <template #header><span>我的课表</span></template>
    <table class="grid" v-loading="loading">
      <thead>
        <tr>
          <th class="period">节次</th>
          <th v-for="d in days" :key="d">{{ d }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in periods" :key="p">
          <td class="period">{{ p }}</td>
          <template v-for="d in 7" :key="d">
            <td v-if="cell(p - 1, d - 1) && cell(p - 1, d - 1) !== 'covered'" :rowspan="cell(p - 1, d - 1).rowspan" class="course">
              <div class="cname">{{ cell(p - 1, d - 1).cname }}</div>
              <div class="loc">{{ cell(p - 1, d - 1).location }}</div>
              <div class="sem">{{ cell(p - 1, d - 1).semesterName }}</div>
            </td>
            <td v-else-if="!cell(p - 1, d - 1)" class="empty"></td>
          </template>
        </tr>
      </tbody>
    </table>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSchedule } from '@/api/teacher'

const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const periods = [1, 2, 3, 4, 5, 6, 7, 8]

const scheduleList = ref([])
const loading = ref(false)

// 解析 "周一 1-2节" 为 [{day, start, end}]
function parseSchedule(s) {
  if (!s) return []
  const dayMap = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 日: 7 }
  const list = []
  for (const seg of s.split(/[,，]/)) {
    const m = seg.match(/周([一二三四五六日])\s*(\d+)-(\d+)/)
    if (m) list.push({ day: dayMap[m[1]], start: +m[2], end: +m[3] })
  }
  return list
}

// 生成 8(节次) x 7(星期) 的课表格子；'covered' 表示被上面课程 rowspan 覆盖
const grid = computed(() => {
  const g = Array.from({ length: 8 }, () => Array(7).fill(null))
  for (const c of scheduleList.value) {
    for (const seg of parseSchedule(c.schedule)) {
      const day = seg.day - 1
      const start = seg.start - 1
      const end = seg.end - 1
      if (start < 0 || end >= 8) continue
      g[start][day] = { ...c, rowspan: end - start + 1 }
      for (let p = start + 1; p <= end; p++) g[p][day] = 'covered'
    }
  }
  return g
})

function cell(p, d) {
  return grid.value[p][d]
}

async function load() {
  loading.value = true
  try {
    const res = await getSchedule()
    scheduleList.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.grid th, .grid td {
  border: 1px solid #dcdfe6;
  text-align: center;
  height: 52px;
  padding: 4px;
  font-size: 13px;
  overflow-wrap: break-word;
  word-break: break-all;
}
.grid th {
  background: #f5f7fa;
}
.grid .period {
  width: 60px;
  background: #f5f7fa;
  color: #606266;
}
.grid .course {
  background: #ecf5ff;
  color: #409eff;
}
.grid .cname {
  font-weight: bold;
}
.grid .loc {
  font-size: 12px;
  color: #909399;
}
.grid .sem {
  font-size: 12px;
  color: #b0b3b8;
}
.grid .empty {
  background: #fff;
}
</style>
