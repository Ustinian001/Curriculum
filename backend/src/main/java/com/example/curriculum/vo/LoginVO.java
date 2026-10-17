package com.example.curriculum.vo;

import lombok.Data;

/**
 * 登录返回结果：token + 用户信息
 */
@Data
public class LoginVO {

    private String token;

    private UserVO user;
}
