import request from './request'

// 登录：{account, password, role} -> {token, user}
export function login(data) {
  return request.post('/auth/login', data)
}

// 登录页修改密码（需旧密码）：{role, account, oldPassword, newPassword}
export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}

// 登录页忘记密码（无需旧密码，核验账号+姓名+手机号）：{role, account, name, phone}
export function forgotPassword(data) {
  return request.post('/auth/forgot-password', data)
}

// 个人信息页修改密码：{oldPassword, newPassword}
export function changePassword(data) {
  return request.post('/auth/change-password', data)
}
