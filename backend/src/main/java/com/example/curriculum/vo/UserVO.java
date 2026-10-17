package com.example.curriculum.vo;

import lombok.Data;

/**
 * 登录返回的用户信息（不含密码）
 */
@Data
public class UserVO {

    private Long id;

    /** 账号：学号/工号/admin */
    private String account;

    private String name;

    /** 角色：student / teacher / admin */
    private String role;
}
