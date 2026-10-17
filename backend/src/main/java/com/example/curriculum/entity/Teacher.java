package com.example.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 教师实体
 */
@Data
@TableName("teacher")
public class Teacher {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工号（登录账号） */
    private String tno;

    /** 密码（BCrypt 加密） */
    private String password;

    private String name;

    /** 性别：男 / 女 */
    private String gender;

    /** 职称，如 教授、讲师 */
    private String title;

    /** 学院，如 计算机学院 */
    private String college;

    private String phone;
}
