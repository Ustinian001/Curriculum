package com.example.curriculum.dto;

import lombok.Data;

/**
 * 教师新增/修改请求参数
 */
@Data
public class TeacherDTO {

    private Long id;

    private String tno;

    /** 密码：新增时可空（默认 123456），修改时空表示不修改 */
    private String password;

    private String name;

    private String gender;

    private String title;

    private String college;

    private String phone;
}
