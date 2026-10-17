<template>
  <el-card class="batch-card" v-loading="loading">
    <!-- 主标题：居中显示批次名称 -->
    <div class="batch-title">{{ batch.batchName }}</div>

    <!-- 批次信息：三行排版，左边标签、右边数值 -->
    <div class="info-table">
      <!-- 第一行 -->
      <div class="info-row">
        <div class="info-cell">
          <span class="cell-label">学年学期</span>
          <span class="cell-value">{{ batch.semester }}</span>
        </div>
        <div class="info-cell">
          <span class="cell-label">选课时间</span>
          <span class="cell-value">{{ batch.selectTime }}</span>
        </div>
        <div class="info-cell">
          <span class="cell-label">限选门数</span>
          <span class="cell-value">{{ batch.courseLimit }}</span>
        </div>
      </div>
      <!-- 第二行 -->
      <div class="info-row">
        <div class="info-cell">
          <span class="cell-label">选课抽签</span>
          <span class="cell-value">{{ batch.lottery }}</span>
        </div>
        <div class="info-cell">
          <span class="cell-label">选课控制</span>
          <span class="cell-value">{{ batch.control }}</span>
        </div>
        <div class="info-cell">
          <span class="cell-label">教学班选课人数</span>
          <span class="cell-value">{{ batch.classLimit }}</span>
        </div>
      </div>
      <!-- 第三行 -->
      <div class="info-row">
        <div class="info-cell">
          <span class="cell-label">预置课是否可退</span>
          <span class="cell-value">{{ batch.preCourseRefund }}</span>
        </div>
        <div class="info-cell">
          <span class="cell-label">允许冲突选课</span>
          <span class="cell-value">{{ batch.allowConflict }}</span>
        </div>
        <div class="info-cell">
          <span class="cell-label">控制选课学分</span>
          <span class="cell-value">{{ batch.creditLimit }}</span>
        </div>
      </div>
    </div>

    <!-- 底部操作区 -->
    <div class="btn-area">
      <el-button type="primary" size="large" :disabled="countdown > 0" @click="handleEnter">
        {{ countdown > 0 ? `进入选课（${countdown}秒）` : '进入选课' }}
      </el-button>
      <el-button size="large" @click="handleBack">返回</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getSelectionBatch } from '@/api/student'

const router = useRouter()
const batch = ref({})
const loading = ref(false)
// 进入选课前需阅读倒计时（秒）
const READ_SECONDS = 3
const countdown = ref(READ_SECONDS)
let timer = null

async function load() {
  loading.value = true
  try {
    const res = await getSelectionBatch()
    batch.value = res.data
  } finally {
    loading.value = false
  }
}

// 将后端 "yyyy-MM-dd HH:mm" 解析为本地 Date
function parseTime(str) {
  if (!str) return null
  return new Date(str.replace(' ', 'T'))
}

// 进入选课：先做选课时间校验，通过后跳转课程列表
function handleEnter() {
  const start = parseTime(batch.value.selectStartTime)
  const end = parseTime(batch.value.selectEndTime)
  const now = new Date()
  if (start && now < start) {
    ElMessageBox.alert(`选课时间未开始，请于 ${batch.value.selectStartTime} 开始后再试`, '提示')
    return
  }
  if (end && now > end) {
    ElMessageBox.alert('选课已结束', '提示')
    return
  }
  router.push('/student/courses')
}

function handleBack() {
  router.back()
}

// 阅读倒计时：3 秒后才能进入选课
function startCountdown() {
  countdown.value = READ_SECONDS
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      countdown.value = 0
      clearInterval(timer)
    }
  }, 1000)
}

onMounted(() => {
  load()
  startCountdown()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.batch-title {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
}
.info-table {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.info-row {
  display: flex;
}
.info-cell {
  flex: 1;
  display: flex;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}
.info-cell:last-child {
  border-right: none;
}
.info-row:last-child .info-cell {
  border-bottom: none;
}
.cell-label {
  flex-shrink: 0;
  background: #fafafa;
  color: #606266;
  padding: 10px 12px;
  white-space: nowrap;
}
.cell-value {
  flex: 1;
  color: #303133;
  padding: 10px 12px;
  word-break: break-all;
}
.btn-area {
  margin-top: 24px;
  text-align: center;
}
</style>
