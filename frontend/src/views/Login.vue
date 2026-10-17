<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">学生选课系统</h2>
      <!-- 角色 Tab 切换 -->
      <el-tabs v-model="form.role" stretch>
        <el-tab-pane label="学生" name="student" />
        <el-tab-pane label="教师" name="teacher" />
        <el-tab-pane label="管理员" name="admin" />
      </el-tabs>

      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.account" :placeholder="accountPlaceholder" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="tip">演示账号：学生 2024001 / 教师 T001 / 管理员 admin，密码均为 123456</div>
      <div class="extra">
        <el-link type="primary" @click="openReset">修改密码</el-link>
        <el-divider direction="vertical" />
        <el-link type="warning" @click="openForgot">忘记密码</el-link>
      </div>
    </el-card>

    <!-- 修改密码弹窗：记得旧密码时使用 -->
    <el-dialog v-model="resetVisible" title="修改密码" width="400px">
      <el-form :model="resetForm" label-width="80px">
        <el-form-item label="角色">
          <el-select v-model="resetForm.role" style="width: 100%">
            <el-option label="学生" value="student" />
            <el-option label="教师" value="teacher" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="resetForm.account" :placeholder="resetForm.role === 'student' ? '学号' : '工号'" />
        </el-form-item>
        <el-form-item label="旧密码">
          <el-input v-model="resetForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="handleReset">确定修改</el-button>
      </template>
    </el-dialog>

    <!-- 忘记密码弹窗：不需要旧密码，改用账号 + 姓名 + 手机号核验身份 -->
    <el-dialog v-model="forgotVisible" title="忘记密码" width="420px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        忘记密码时无需填写旧密码。请填写本人姓名与预留手机号进行身份核验，
        核验通过后密码将重置为 <b>123456</b>，登录后请及时修改。
      </el-alert>
      <el-form :model="forgotForm" label-width="80px">
        <el-form-item label="角色">
          <el-select v-model="forgotForm.role" style="width: 100%">
            <el-option label="学生" value="student" />
            <el-option label="教师" value="teacher" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="forgotForm.account" :placeholder="forgotForm.role === 'student' ? '学号' : '工号'" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="forgotForm.name" placeholder="本人姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="forgotForm.phone" placeholder="预留手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="warning" :loading="forgotLoading" @click="handleForgot">核验并重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login, resetPassword, forgotPassword } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const store = useUserStore()

const form = ref({ account: '', password: '', role: 'student' })
const loading = ref(false)

// 修改密码（需旧密码）
const resetVisible = ref(false)
const resetLoading = ref(false)
const resetForm = ref({ role: 'student', account: '', oldPassword: '', newPassword: '' })

function openReset() {
  resetForm.value = { role: 'student', account: '', oldPassword: '', newPassword: '' }
  resetVisible.value = true
}

async function handleReset() {
  const { role, account, oldPassword, newPassword } = resetForm.value
  if (!account || !oldPassword || !newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  resetLoading.value = true
  try {
    await resetPassword({ role, account, oldPassword, newPassword })
    ElMessage.success('密码修改成功，请使用新密码登录')
    resetVisible.value = false
  } finally {
    resetLoading.value = false
  }
}

// 忘记密码（账号 + 姓名 + 手机号核验）
const forgotVisible = ref(false)
const forgotLoading = ref(false)
const forgotForm = ref({ role: 'student', account: '', name: '', phone: '' })

function openForgot() {
  forgotForm.value = { role: 'student', account: '', name: '', phone: '' }
  forgotVisible.value = true
}

async function handleForgot() {
  const { role, account, name, phone } = forgotForm.value
  if (!account || !name || !phone) {
    ElMessage.warning('请填写完整信息')
    return
  }
  forgotLoading.value = true
  try {
    await forgotPassword({ role, account, name, phone })
    ElMessage.success('核验通过，密码已重置为 123456，请用新密码登录')
    forgotVisible.value = false
  } finally {
    forgotLoading.value = false
  }
}

const accountPlaceholder = computed(() => {
  return form.value.role === 'student' ? '学号' : form.value.role === 'teacher' ? '工号' : '管理员账号'
})

// 登录成功后的首页
const homePath = { student: '/student/profile', teacher: '/teacher/profile', admin: '/admin/students' }

async function handleLogin() {
  if (!form.value.account || !form.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ ...form.value })
    // 保存 token 与用户信息
    store.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push(homePath[form.value.role] || '/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6feb 0%, #6a5cff 100%);
}
.login-card {
  width: 380px;
  padding: 10px 20px;
}
.title {
  text-align: center;
  margin-bottom: 10px;
}
.tip {
  color: #999;
  font-size: 12px;
  text-align: center;
}
.extra {
  text-align: center;
  margin-top: 10px;
}
</style>
