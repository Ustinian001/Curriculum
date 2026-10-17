<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>我的课程</span>
        <div class="toolbar-right">
          <el-select v-model="classFilter" size="small" style="width: 200px">
            <el-option label="全部课程" value="" />
            <el-option label="未绑定班级（合班/选修）" value="unbound" />
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="String(c.id)" />
          </el-select>
          <el-button type="success" @click="openAdd">开设课程</el-button>
        </div>
      </div>
    </template>

    <el-table :data="filteredCourses" v-loading="loading" border stripe>
      <el-table-column prop="cno" label="课程编号" width="95" />
      <el-table-column prop="tcNo" label="教学班编号" width="105" />
      <el-table-column prop="cname" label="课程名称" width="140" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.courseType)" size="small">{{ row.courseType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="班级" width="120">
        <template #default="{ row }">
          <span>{{ row.className || '全部班级' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="credit" label="学分" width="70" />
      <el-table-column prop="semesterName" label="学期" width="130" />
      <el-table-column prop="location" label="地点" width="110" />
      <el-table-column prop="schedule" label="时间" width="150" />
      <el-table-column label="选课人数" width="140">
        <template #default="{ row }">
          {{ row.selectedCount }}/{{ row.capacity }}
          <span v-if="row.minStudents" class="min">（最低 {{ row.minStudents }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" size="small">开设中</el-tag>
          <el-tag v-else type="info" size="small">已停开</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="200">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="goStudents(row)">学生名单</el-button>
          <el-button
            v-if="canStop(row)"
            type="danger" size="small"
            @click="handleStop(row)"
          >停止开设</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 开设课程弹窗 -->
    <el-dialog v-model="dialogVisible" title="开设课程" width="560px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        一门课只有一个课程编号。填写已存在的课程编号且课程信息一致时，将视为为该课程增开一个教学班。
      </el-alert>
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程编号"><el-input v-model="form.cno" placeholder="如 PK01、XK13" /></el-form-item>
        <el-form-item label="课程名称"><el-input v-model="form.cname" /></el-form-item>
        <el-form-item label="课程类型">
          <el-select v-model="form.courseType" style="width: 100%">
            <el-option label="选修类" value="选修类" />
            <el-option label="专业类" value="专业类" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.courseType === '专业类'" label="绑定班级">
          <el-select v-model="form.classId" placeholder="专业类课程需绑定班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分"><el-input-number v-model="form.credit" :min="1" :max="10" /></el-form-item>
        <el-form-item label="学时"><el-input-number v-model="form.hours" :min="1" :max="200" /></el-form-item>
        <el-form-item label="所属学期">
          <el-select v-model="form.semesterId" placeholder="请选择学期" style="width: 100%">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="1" :max="500" /></el-form-item>
        <el-form-item label="最低人数"><el-input-number v-model="form.minStudents" :min="0" :max="200" /></el-form-item>
        <el-form-item label="上课地点"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="上课时间">
          <el-input v-model="form.schedule" placeholder="如：周一 1-2节,周三 5-6节" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCourses, createCourse, stopCourse, listSemesters, listClasses } from '@/api/teacher'

const router = useRouter()
const courses = ref([])
const semesters = ref([])
const classes = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})

// 班级筛选：'' = 全部课程，'unbound' = 全部班级（公共/选修），其余为具体班级 id
const classFilter = ref('')
const filteredCourses = computed(() => {
  if (!classFilter.value) return courses.value
  if (classFilter.value === 'unbound') return courses.value.filter(c => c.classId == null)
  const id = Number(classFilter.value)
  return courses.value.filter(c => c.classId === id)
})

function typeTag(type) {
  if (type === '公共必修') return 'info'
  if (type === '专业类') return 'warning'
  return 'success'
}

// 仅选修类且选课人数低于最低人数时可停止开设
function canStop(row) {
  return row.courseType === '选修类' && row.status === 1 && row.selectedCount < row.minStudents
}

async function load() {
  loading.value = true
  try {
    const res = await listCourses()
    courses.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [s, c] = await Promise.all([listSemesters(), listClasses()])
  semesters.value = s.data
  classes.value = c.data
}

function openAdd() {
  form.value = {
    cno: '', cname: '', courseType: '选修类', classId: null,
    credit: 3, hours: 48, capacity: 40, minStudents: 25,
    semesterId: null, location: '', schedule: ''
  }
  // 默认选中当前学期
  const current = semesters.value.find(s => s.isCurrent === 1) || semesters.value[0]
  if (current) form.value.semesterId = current.id
  dialogVisible.value = true
}

async function handleCreate() {
  const data = { ...form.value }
  if (data.courseType !== '专业类') data.classId = null
  saving.value = true
  try {
    await createCourse(data)
    ElMessage.success('开设课程成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleStop(row) {
  await ElMessageBox.confirm(
    `确定停止开设《${row.cname}》（教学班 ${row.tcNo || ''}）吗？停止后将清空该教学班的全部选课记录。`,
    '提示', { type: 'warning' }
  )
  await stopCourse(row.id)
  ElMessage.success('已停止开设')
  load()
}

function goStudents(row) {
  router.push(`/teacher/course/${row.id}/students`)
}

onMounted(() => { load(); loadOptions() })
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.min {
  color: #999;
  font-size: 12px;
}
</style>
