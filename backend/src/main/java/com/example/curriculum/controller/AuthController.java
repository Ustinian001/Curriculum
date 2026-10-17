package com.example.curriculum.controller;

import com.example.curriculum.common.Result;
import com.example.curriculum.dto.ChangePasswordDTO;
import com.example.curriculum.dto.ForgotPasswordDTO;
import com.example.curriculum.dto.LoginDTO;
import com.example.curriculum.dto.ResetPasswordDTO;
import com.example.curriculum.service.AuthService;
import com.example.curriculum.utils.UserContext;
import com.example.curriculum.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录：入参 {account, password, role}，返回 token + 用户信息 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 登录页修改密码（公开接口）：入参 {role, account, oldPassword, newPassword}，需校验旧密码 */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.changePasswordByAccount(dto);
        return Result.success();
    }

    /**
     * 登录页忘记密码（公开接口）：入参 {role, account, name, phone}。
     * 忘记密码时无需旧密码，改用账号+姓名+手机号核验身份，通过后重置为默认密码 123456。
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        authService.resetPasswordByVerification(dto);
        return Result.success();
    }

    /** 个人信息页修改密码（已登录）：入参 {oldPassword, newPassword} */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changeOwnPassword(UserContext.getUserId(), UserContext.getRole(), dto);
        return Result.success();
    }
}
