<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索课程编号/名称" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openAdd">新增教学班</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="cno" label="课程编号" width="95" />
      <el-table-column prop="tcNo" label="教学班编号" width="105" />
      <el-table-column prop="cname" label="课程名称" width="130" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.courseType)" size="small">{{ row.courseType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="credit" label="学分" width="60" />
      <el-table-column prop="hours" label="学时" width="60" />
      <el-table-column prop="teacherName" label="教师" width="90" />
      <el-table-column prop="semesterName" label="学期" width="120" />
      <el-table-column prop="className" label="绑定班级" width="120" />
      <el-table-column label="人数" width="90">
        <template #default="{ row }">{{ row.selectedCount }}/{{ row.capacity }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" size="small">开设中</el-tag>
          <el-tag v-else type="info" size="small">已停开</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="schedule" label="时间" width="140" />
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager" background
      layout="total, prev, pager, next, sizes"
      :total="total" :current-page="page" :page-size="size" :page-sizes="[10, 20, 50]"
      @current-change="onPageChange" @size-change="onSizeChange"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑教学班' : '新增教学班'" width="520px">
      <el-alert v-if="!form.id" type="info" :closable="false" show-icon style="margin-bottom: 12px">
        一门课只有一个课程编号；填写已存在的课程编号且课程信息一致时，会为该课程新增一个教学班。
      </el-alert>
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程编号">
          <el-input v-model="form.cno" placeholder="如 PK01、ZY01、XK01" />
        </el-form-item>
        <el-form-item v-if="form.id" label="教学班编号">
          <el-input v-model="form.tcNo" disabled />
        </el-form-item>
        <el-form-item label="课程名称"><el-input v-model="form.cname" /></el-form-item>
        <el-form-item label="课程类型">
          <el-select v-model="form.courseType" style="width: 100%">
            <el-option label="公共必修" value="公共必修" />
            <el-option label="专业类" value="专业类" />
            <el-option label="选修类" value="选修类" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.courseType === '专业类' || form.courseType === '公共必修'" label="绑定班级">
          <el-select v-model="form.classId" clearable :placeholder="form.courseType === '专业类' ? '专业类需绑定班级' : '公共必修：留空为合班，选择为分班'" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分"><el-input-number v-model="form.credit" :min="1" :max="10" /></el-form-item>
        <el-form-item label="学时"><el-input-number v-model="form.hours" :min="1" :max="200" /></el-form-item>
        <el-form-item label="授课教师">
          <el-select v-model="form.teacherId" placeholder="请选择教师" style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
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
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageCourses, saveCourse, deleteCourse, listTeachers, listSemesters, listClasses } from '@/api/admin'

const list = ref([])
const teachers = ref([])
const semesters = ref([])
const classes = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageCourses({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [t, s, c] = await Promise.all([listTeachers(), listSemesters(), listClasses()])
  teachers.value = t.data.records
  semesters.value = s.data
  classes.value = c.data
}

function typeTag(type) {
  if (type === '公共必修') return 'info'
  if (type === '专业类') return 'warning'
  return 'success'
}

function openAdd() {
  // 给数值字段默认值，避免为空导致后端计算剩余容量时出错
  form.value = { credit: 3, hours: 48, capacity: 50, courseType: '选修类', classId: null, minStudents: 25 }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSave() {
  const data = { ...form.value }
  if (data.courseType === '选修类') data.classId = null
  await saveCourse(data)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除教学班「${row.tcNo || row.cno} ${row.cname}」吗？该课程若已无其它教学班，课程记录也会一并删除。`,
    '提示', { type: 'warning' }
  )
  await deleteCourse(row.id)
  ElMessage.success('删除成功')
  load()
}

function onPageChange(p) { page.value = p; load() }
function onSizeChange(s) { size.value = s; page.value = 1; load() }

onMounted(() => { load(); loadOptions() })
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
