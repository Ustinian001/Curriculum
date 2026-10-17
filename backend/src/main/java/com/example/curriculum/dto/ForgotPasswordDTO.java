package com.example.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录页「忘记密码」请求参数（公开接口）
 * <p>
 * 忘记密码时无法提供旧密码，因此改用「账号 + 姓名 + 手机号」三项核验身份，
 * 核验通过后将密码重置为默认密码。
 */
@Data
public class ForgotPasswordDTO {

    /** 角色：student / teacher */
    @NotBlank(message = "角色不能为空")
    private String role;

    /** 账号：学生为学号，教师为工号 */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 姓名（用于核验身份） */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 手机号（用于核验身份） */
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
