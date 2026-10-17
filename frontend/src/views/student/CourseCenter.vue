<template>
  <div class="course-center">
    <!-- 选课中心 -->
    <el-card>
      <template #header>
        <span>选课中心（当前学期可选课程，每学期限选 {{ MAX_ELECTIVE }} 门选修课）</span>
      </template>
      <!-- 课程查询/筛选栏 -->
      <div class="filter-bar">
        <el-input v-model="filter.keyword" placeholder="编号/名称" clearable size="small" style="width: 120px" />
        <el-select v-model="filter.category" placeholder="课程类别" clearable size="small" style="width: 105px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input v-model="filter.teacher" placeholder="上课老师" clearable size="small" style="width: 95px" />
        <el-select v-model="filter.semester" placeholder="学期" clearable size="small" style="width: 125px">
          <el-option v-for="s in semesters" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="filter.section" placeholder="节次" clearable size="small" style="width: 90px" />
        <el-checkbox v-model="filter.hideFull" size="small">过滤已满课程</el-checkbox>
        <el-checkbox v-model="filter.hideConflict" size="small">过滤冲突课程</el-checkbox>
        <el-checkbox v-model="filter.hideUnselectable" size="small">过滤限选课程</el-checkbox>
        <el-button size="small" @click="resetFilter">重置</el-button>
      </div>
      <el-table :data="filteredCourses" v-loading="loading" border stripe>
        <el-table-column prop="cno" label="课程编号" width="100" />
        <el-table-column prop="cname" label="课程名称" width="140" />
        <el-table-column label="课程类别" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="credit" label="学分" width="70" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="location" label="地点" width="110" />
        <el-table-column prop="schedule" label="时间" width="150" />
        <el-table-column label="容量" width="120">
          <template #default="{ row }">
            {{ row.selectedCount }}/{{ row.capacity }}
          </template>
        </el-table-column>
        <el-table-column label="剩余" width="80">
          <template #default="{ row }">
            <el-tag :type="row.remaining > 0 ? 'success' : 'danger'" size="small">{{ row.remaining }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="140">
          <template #default="{ row }">
            <el-button v-if="row.selected" type="danger" size="small" @click="handleWithdraw(row)">退课</el-button>
            <el-button v-else type="primary" size="small" :disabled="row.remaining <= 0 || reachedLimit" @click="handleSelect(row)">
              {{ row.remaining <= 0 ? '已满' : (reachedLimit ? '已达上限' : '选课') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 选课结果 -->
    <el-card class="result-card">
      <template #header>
        <span>选课结果（已选 {{ selectedCourses.length }}/{{ MAX_ELECTIVE }} 门 / {{ selectedCredit }} 学分）</span>
      </template>
      <el-table :data="selectedCourses" v-loading="loading" border stripe>
        <el-table-column prop="cno" label="课程编号" width="100" />
        <el-table-column prop="cname" label="课程名称" width="140" />
        <el-table-column prop="credit" label="学分" width="70" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="location" label="地点" width="110" />
        <el-table-column prop="schedule" label="时间" width="150" />
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleWithdraw(row)">退课</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无选课结果，请从上方选课中心选课" :image-size="80" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCourses, selectCourse, withdrawCourse } from '@/api/student'

const courses = ref([])
const loading = ref(false)
// 筛选条件（输入即筛，实时生效）
const filter = reactive({
  keyword: '', category: '', teacher: '', semester: '', section: '',
  hideFull: false, hideConflict: false, hideUnselectable: false
})

// 下拉选项（从数据中提取去重）
const categories = computed(() => [...new Set(courses.value.map(c => c.category).filter(Boolean))])
const semesters = computed(() => [...new Set(courses.value.map(c => c.semesterName).filter(Boolean))])

// 筛选后的可选课程：编号/名称 + 类别 + 老师 + 学期 + 节次 + 已满/冲突/限选
const filteredCourses = computed(() => {
  const kw = filter.keyword.trim().toLowerCase()
  const t = filter.teacher.trim().toLowerCase()
  const sec = filter.section.trim()
  return courses.value.filter(c => {
    if (kw && !(c.cno || '').toLowerCase().includes(kw) && !(c.cname || '').toLowerCase().includes(kw)) return false
    if (filter.category && c.category !== filter.category) return false
    if (t && !(c.teacherName || '').toLowerCase().includes(t)) return false
    if (filter.semester && c.semesterName !== filter.semester) return false
    if (sec && !(c.schedule || '').includes(sec)) return false
    if (filter.hideFull && c.remaining <= 0) return false
    if (filter.hideConflict && c.conflict) return false
    if (filter.hideUnselectable && (c.selected || c.remaining <= 0 || c.conflict || reachedLimit.value)) return false
    return true
  })
})

function resetFilter() {
  Object.assign(filter, { keyword: '', category: '', teacher: '', semester: '', section: '', hideFull: false, hideConflict: false, hideUnselectable: false })
}

// 每学期限选选修课门数（与后端一致）
const MAX_ELECTIVE = 2
// 选课结果：已选课程（row.selected 为 true）
const selectedCourses = computed(() => courses.value.filter(c => c.selected))
const selectedCredit = computed(() => selectedCourses.value.reduce((sum, c) => sum + (c.credit || 0), 0))
// 是否已达每学期限选上限
const reachedLimit = computed(() => selectedCourses.value.length >= MAX_ELECTIVE)

async function load() {
  loading.value = true
  try {
    const res = await listCourses()
    courses.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleSelect(row) {
  try {
    await selectCourse(row.id)
    ElMessage.success('选课成功')
    load() // 刷新剩余容量与选课结果
  } catch (e) { /* 错误提示已在拦截器处理 */ }
}

async function handleWithdraw(row) {
  await ElMessageBox.confirm(`确定退选《${row.cname}》吗？`, '提示', { type: 'warning' })
  await withdrawCourse(row.id)
  ElMessage.success('退课成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}
.result-card {
  margin-top: 16px;
}
</style>
