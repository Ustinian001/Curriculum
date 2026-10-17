<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-button type="success" @click="openAdd">新增学期</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="semesterName" label="学期名称" width="180" />
      <el-table-column prop="startDate" label="开始日期" width="130" />
      <el-table-column prop="endDate" label="结束日期" width="130" />
      <el-table-column label="当前学期" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.isCurrent === 1" type="success">当前学期</el-tag>
          <el-tag v-else type="info">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="390">
        <template #default="{ row }">
          <div class="op">
            <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" size="small" @click="openCourses(row)">查看课程</el-button>
            <el-button type="success" size="small" :disabled="row.isCurrent === 1" @click="handleSetCurrent(row)">设为当前学期</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学期' : '新增学期'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学期名称">
          <el-select v-model="form.semesterName" placeholder="请选择学期" style="width: 100%">
            <el-option v-for="s in semesterOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看某学期开设的课程 -->
    <el-dialog v-model="coursesVisible" :title="`${coursesSemester.semesterName || ''} 开设的课程`" width="960px">
      <div class="course-summary">
        共 <b>{{ courses.length }}</b> 个教学班，涉及 <b>{{ courseCount }}</b> 门课程
        <span class="summary-hint">（同一门课可有多个教学班，课程编号唯一）</span>
      </div>
      <el-table :data="courses" v-loading="coursesLoading" border stripe max-height="440">
        <el-table-column prop="cno" label="课程编号" width="95" />
        <el-table-column prop="tcNo" label="教学班编号" width="105" />
        <el-table-column prop="cname" label="课程名称" width="150" />
        <el-table-column label="类型" width="95">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.courseType)" size="small">{{ row.courseType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="credit" label="学分" width="65" />
        <el-table-column prop="hours" label="学时" width="65" />
        <el-table-column prop="teacherName" label="教师" width="90" />
        <el-table-column prop="className" label="绑定班级" width="130">
          <template #default="{ row }">
            <span>{{ row.className || '全部班级' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="人数" width="90">
          <template #default="{ row }">{{ row.selectedCount }}/{{ row.capacity }}</template>
        </el-table-column>
        <el-table-column prop="schedule" label="时间" width="140" />
        <el-table-column prop="location" label="地点" width="100" />
      </el-table>
      <template #footer>
        <el-button @click="coursesVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSemesters, saveSemester, deleteSemester, setCurrentSemester, listSemesterCourses } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({})
const semesterOptions = ref([])

// 查看某学期开设的课程
const coursesVisible = ref(false)
const coursesLoading = ref(false)
const courses = ref([])
const coursesSemester = ref({})
// 涉及多少门课程（同一门课的多个教学班只算一门）
const courseCount = computed(() => new Set(courses.value.map(c => c.courseId)).size)

function typeTag(type) {
  if (type === '公共必修') return 'info'
  if (type === '专业类') return 'warning'
  return 'success'
}

// 生成可选的学期名称（如 2025-2026-1 / 2025-2026-2），覆盖当前学年前后若干年
function buildSemesterOptions() {
  const now = new Date()
  const year = now.getFullYear()
  // 9 月及以后视为新学年开始（如 2026-09 属于 2026-2027 学年）
  const academicStart = now.getMonth() >= 8 ? year : year - 1
  const options = []
  for (let y = academicStart - 3; y <= academicStart + 3; y++) {
    options.push({ value: `${y}-${y + 1}-1`, label: `${y}-${y + 1}-1（第一学期）` })
    options.push({ value: `${y}-${y + 1}-2`, label: `${y}-${y + 1}-2（第二学期）` })
  }
  semesterOptions.value = options
}

async function load() {
  loading.value = true
  try {
    const res = await listSemesters()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openAdd() {
  form.value = {}
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  // 已存在的学期名称若不在生成范围内，补进下拉选项，避免回显为空
  if (form.value.semesterName && !semesterOptions.value.some(s => s.value === form.value.semesterName)) {
    semesterOptions.value.unshift({ value: form.value.semesterName, label: form.value.semesterName })
  }
  dialogVisible.value = true
}

// 查看该学期开设的课程
async function openCourses(row) {
  coursesSemester.value = { ...row }
  courses.value = []
  coursesVisible.value = true
  coursesLoading.value = true
  try {
    const res = await listSemesterCourses(row.id)
    courses.value = res.data || []
  } finally {
    coursesLoading.value = false
  }
}

async function handleSave() {
  await saveSemester({ ...form.value })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleSetCurrent(row) {
  await ElMessageBox.confirm(`确定将「${row.semesterName}」设为当前学期吗？`, '提示', { type: 'warning' })
  await setCurrentSemester(row.id)
  ElMessage.success('设置成功')
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除学期「${row.semesterName}」吗？`, '提示', { type: 'warning' })
  await deleteSemester(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  buildSemesterOptions()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; }
.op { white-space: nowrap; }
.course-summary {
  margin-bottom: 12px;
  color: #303133;
}
.course-summary b {
  color: #409eff;
  font-size: 16px;
}
.summary-hint {
  color: #909399;
  font-size: 12px;
}
</style>
