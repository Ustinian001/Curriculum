import request from './request'

// ============ 学生管理 ============
export function pageStudents(params) {
  return request.get('/admin/students', { params })
}
export function saveStudent(data) {
  return data.id ? request.put('/admin/students', data) : request.post('/admin/students', data)
}
export function deleteStudent(id) {
  return request.delete(`/admin/students/${id}`)
}
export function resetStudentPassword(id) {
  return request.put(`/admin/students/${id}/reset-password`)
}

// ============ 教师管理 ============
export function pageTeachers(params) {
  return request.get('/admin/teachers', { params })
}
export function saveTeacher(data) {
  return data.id ? request.put('/admin/teachers', data) : request.post('/admin/teachers', data)
}
export function deleteTeacher(id) {
  return request.delete(`/admin/teachers/${id}`)
}
export function resetTeacherPassword(id) {
  return request.put(`/admin/teachers/${id}/reset-password`)
}

// ============ 课程管理 ============
export function pageCourses(params) {
  return request.get('/admin/courses', { params })
}
export function saveCourse(data) {
  return data.id ? request.put('/admin/courses', data) : request.post('/admin/courses', data)
}
export function deleteCourse(id) {
  return request.delete(`/admin/courses/${id}`)
}
// 课程表单所需的下拉数据
export function listTeachers() {
  return request.get('/admin/teachers', { params: { page: 1, size: 1000 } })
}
export function listSemesters() {
  return request.get('/admin/semesters')
}

// ============ 班级管理 ============
export function listClasses() {
  return request.get('/admin/classes')
}
export function saveClass(data) {
  return data.id ? request.put('/admin/classes', data) : request.post('/admin/classes', data)
}
export function deleteClass(id) {
  return request.delete(`/admin/classes/${id}`)
}

// ============ 学期管理 ============
export function saveSemester(data) {
  return data.id ? request.put('/admin/semesters', data) : request.post('/admin/semesters', data)
}
export function deleteSemester(id) {
  return request.delete(`/admin/semesters/${id}`)
}
export function setCurrentSemester(id) {
  return request.put(`/admin/semesters/${id}/current`)
}
// 某学期开设的课程（教学班）列表
export function listSemesterCourses(id) {
  return request.get(`/admin/semesters/${id}/courses`)
}
