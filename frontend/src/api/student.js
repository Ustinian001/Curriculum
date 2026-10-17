import request from './request'

// 选课批次信息（选课中心确认页）
export function getSelectionBatch() {
  return request.get('/student/selection-batch')
}

// 可选课程列表
export function listCourses() {
  return request.get('/student/courses')
}

// 选课
export function selectCourse(courseId) {
  return request.post(`/student/course/${courseId}`)
}

// 退课
export function withdrawCourse(courseId) {
  return request.delete(`/student/course/${courseId}`)
}

// 我的课表
export function getSchedule() {
  return request.get('/student/schedule')
}

// 我的成绩
export function getScores() {
  return request.get('/student/scores')
}

// 选课结果（全部已选课程，含课程类型）
export function getSelectionResult() {
  return request.get('/student/selection-result')
}

// 个人信息
export function getProfile() {
  return request.get('/student/profile')
}
