package com.example.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生实体
 */
@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号（登录账号） */
    private String sno;

    /** 密码（BCrypt 加密，永不返回给前端） */
    private String password;

    private String name;

    /** 性别：男 / 女 */
    private String gender;

    /** 所属班级 id */
    private Long classId;

    /** 学院，如 计算机学院 */
    private String college;

    /** 专业，如 软件工程 */
    private String major;

    private String phone;
}
