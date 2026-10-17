<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>学生名单（成绩录入）</span>
        <div class="header-actions">
          <el-button type="success" @click="openAdd">增加学生</el-button>
          <el-button type="primary" :loading="saving" @click="handleBatchSave">批量保存成绩</el-button>
        </div>
      </div>
    </template>
    <el-table :data="students" v-loading="loading" border stripe>
      <el-table-column prop="sno" label="学号" width="110" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="className" label="班级" width="140" />
      <el-table-column label="成绩（0~100）">
        <template #default="{ row }">
          <el-input-number v-model="row.score" :min="0" :max="100" controls-position="right" style="width: 160px" />
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="100">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 增加学生弹窗 -->
    <el-dialog v-model="dialogVisible" title="增加学生" width="480px">
      <el-select v-model="selectedStudentId" filterable placeholder="搜索学号/姓名选择学生" style="width: 100%">
        <el-option
          v-for="s in addableStudents"
          :key="s.id"
          :label="`${s.sno} ${s.name}（${s.className || '未分班'}）`"
          :value="s.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">确定增加</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listStudents, listAddableStudents, addStudent, removeStudent, updateScore } from '@/api/teacher'

const route = useRoute()
const courseId = route.params.courseId

const students = ref([])
const loading = ref(false)
const saving = ref(false)
const original = ref({}) // scId -> 原始成绩，用于判断是否有修改

const dialogVisible = ref(false)
const adding = ref(false)
const addableStudents = ref([])
const selectedStudentId = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listStudents(courseId)
    students.value = res.data
    original.value = {}
    for (const s of students.value) {
      original.value[s.scId] = s.score
    }
  } finally {
    loading.value = false
  }
}

// 批量保存：仅提交发生变化的成绩
async function handleBatchSave() {
  const changed = students.value.filter(s => s.score !== original.value[s.scId] && s.score != null)
  if (changed.length === 0) {
    ElMessage.info('没有需要保存的修改')
    return
  }
  saving.value = true
  try {
    await Promise.all(changed.map(s => updateScore({ scId: s.scId, score: s.score })))
    ElMessage.success(`已保存 ${changed.length} 条成绩`)
    load()
  } finally {
    saving.value = false
  }
}

async function openAdd() {
  selectedStudentId.value = null
  const res = await listAddableStudents(courseId)
  addableStudents.value = res.data
  if (addableStudents.value.length === 0) {
    ElMessage.info('没有可增加的学生')
    return
  }
  dialogVisible.value = true
}

async function handleAdd() {
  if (!selectedStudentId.value) {
    ElMessage.warning('请选择要增加的学生')
    return
  }
  adding.value = true
  try {
    await addStudent(courseId, selectedStudentId.value)
    ElMessage.success('增加学生成功')
    dialogVisible.value = false
    load()
  } finally {
    adding.value = false
  }
}

async function handleRemove(row) {
  await ElMessageBox.confirm(`确定将「${row.name}（${row.sno}）」移出该课程吗？其成绩记录将一并删除。`, '提示', { type: 'warning' })
  await removeStudent(courseId, row.studentId)
  ElMessage.success('已删除学生')
  load()
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-actions {
  display: flex;
  gap: 8px;
}
</style>
