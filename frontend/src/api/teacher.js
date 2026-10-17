import request from './request'

// 我的课程
export function listCourses() {
  return request.get('/teacher/courses')
}

// 课程学生名单
export function listStudents(courseId) {
  return request.get(`/teacher/course/${courseId}/students`)
}

// 可增加学生列表
export function listAddableStudents(courseId) {
  return request.get(`/teacher/course/${courseId}/students/addable`)
}

// 增加学生
export function addStudent(courseId, studentId) {
  return request.post(`/teacher/course/${courseId}/students/${studentId}`)
}

// 删除学生
export function removeStudent(courseId, studentId) {
  return request.delete(`/teacher/course/${courseId}/students/${studentId}`)
}

// 开设课程
export function createCourse(data) {
  return request.post('/teacher/course', data)
}

// 停止开设课程
export function stopCourse(courseId) {
  return request.put(`/teacher/course/${courseId}/stop`)
}

// 开设课程表单：学期下拉
export function listSemesters() {
  return request.get('/teacher/semesters')
}

// 开设课程表单：班级下拉
export function listClasses() {
  return request.get('/teacher/classes')
}

// 录入/修改成绩
export function updateScore(data) {
  return request.put('/teacher/score', data)
}

// 我的课表
export function getSchedule() {
  return request.get('/teacher/schedule')
}

// 个人信息
export function getProfile() {
  return request.get('/teacher/profile')
}
