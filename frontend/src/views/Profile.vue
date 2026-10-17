<template>
  <el-card class="profile" v-loading="loading">
    <template #header>
      <div class="header">
        <span>个人信息</span>
        <el-button type="primary" size="small" @click="openChange">修改密码</el-button>
      </div>
    </template>
    <!-- 学生信息 -->
    <el-descriptions v-if="isStudent" :column="1" border>
      <el-descriptions-item label="学号">{{ student.sno }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ student.name }}</el-descriptions-item>
      <el-descriptions-item label="性别">{{ student.gender }}</el-descriptions-item>
      <el-descriptions-item label="班级">{{ student.className }}</el-descriptions-item>
      <el-descriptions-item label="学院">{{ student.college }}</el-descriptions-item>
      <el-descriptions-item label="专业">{{ student.major }}</el-descriptions-item>
      <el-descriptions-item label="电话">{{ student.phone }}</el-descriptions-item>
    </el-descriptions>
    <!-- 教师信息 -->
    <el-descriptions v-else :column="1" border>
      <el-descriptions-item label="工号">{{ teacher.tno }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ teacher.name }}</el-descriptions-item>
      <el-descriptions-item label="性别">{{ teacher.gender }}</el-descriptions-item>
      <el-descriptions-item label="职称">{{ teacher.title }}</el-descriptions-item>
      <el-descriptions-item label="学院">{{ teacher.college }}</el-descriptions-item>
      <el-descriptions-item label="电话">{{ teacher.phone }}</el-descriptions-item>
    </el-descriptions>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="380px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChange">确定修改</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getProfile as getStudentProfile } from '@/api/student'
import { getProfile as getTeacherProfile } from '@/api/teacher'
import { changePassword } from '@/api/auth'

const store = useUserStore()
const student = ref({})
const teacher = ref({})
const loading = ref(false)

// 修改密码
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '' })

const isStudent = computed(() => store.role === 'student')

function openChange() {
  pwdForm.value = { oldPassword: '', newPassword: '' }
  pwdVisible.value = true
}

async function handleChange() {
  const { oldPassword, newPassword } = pwdForm.value
  if (!oldPassword || !newPassword) {
    ElMessage.warning('请填写旧密码和新密码')
    return
  }
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword, newPassword })
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
  } finally {
    pwdLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    if (isStudent.value) {
      const res = await getStudentProfile()
      student.value = res.data
    } else {
      const res = await getTeacherProfile()
      teacher.value = res.data
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile {
  max-width: 500px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
