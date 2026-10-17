<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openAdd">新增学生</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="sno" label="学号" width="110" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="className" label="班级" width="140" />
      <el-table-column prop="college" label="学院" width="120" />
      <el-table-column prop="major" label="专业" width="140" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="操作" fixed="right" width="220">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="warning" size="small" @click="handleReset(row)">重置密码</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next, sizes"
      :total="total"
      :current-page="page"
      :page-size="size"
      :page-sizes="[10, 20, 50]"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学生' : '新增学生'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="学号"><el-input v-model="form.sno" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%" @change="onClassChange">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" :placeholder="form.id ? '留空则不修改' : '留空默认 123456'" show-password />
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
import { pageStudents, saveStudent, deleteStudent, resetStudentPassword, listClasses } from '@/api/admin'

const list = ref([])
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
    const res = await pageStudents({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadClasses() {
  const res = await listClasses()
  classes.value = res.data
}

function openAdd() {
  form.value = { gender: '男' }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

function onClassChange(id) {
  const c = classes.value.find(x => x.id === id)
  if (c) {
    form.value.college = c.college
    form.value.major = c.major
  }
}

async function handleSave() {
  await saveStudent({ ...form.value })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除学生「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteStudent(row.id)
  ElMessage.success('删除成功')
  load()
}

async function handleReset(row) {
  await ElMessageBox.confirm(`确定将「${row.name}」的密码重置为 123456 吗？`, '提示', { type: 'warning' })
  await resetStudentPassword(row.id)
  ElMessage.success('密码已重置为 123456')
}

function onPageChange(p) { page.value = p; load() }
function onSizeChange(s) { size.value = s; page.value = 1; load() }

onMounted(() => { load(); loadClasses() })
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
