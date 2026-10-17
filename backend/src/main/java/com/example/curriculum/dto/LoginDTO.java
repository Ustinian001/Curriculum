package com.example.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginDTO {

    /** 账号：学生为学号，教师为工号，管理员为 admin */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 角色：student / teacher / admin */
    @NotBlank(message = "角色不能为空")
    private String role;
}
