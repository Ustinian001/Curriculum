package com.example.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录页修改密码请求参数（公开接口，通过账号+旧密码校验身份）
 */
@Data
public class ResetPasswordDTO {

    /** 角色：student / teacher */
    @NotBlank(message = "角色不能为空")
    private String role;

    /** 账号：学生为学号，教师为工号 */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
