<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-button type="success" @click="openAdd">新增班级</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="className" label="班级名称" width="180" />
      <el-table-column prop="grade" label="年级" width="120" />
      <el-table-column prop="college" label="学院" width="140" />
      <el-table-column prop="major" label="专业" width="180" />
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑班级' : '新增班级'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="班级名称"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" /></el-form-item>
        <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
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
import { listClasses, saveClass, deleteClass } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await listClasses()
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
  dialogVisible.value = true
}

async function handleSave() {
  await saveClass({ ...form.value })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除班级「${row.className}」吗？`, '提示', { type: 'warning' })
  await deleteClass(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; }
</style>
