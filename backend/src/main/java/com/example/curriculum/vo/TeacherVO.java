package com.example.curriculum.vo;

import lombok.Data;

/**
 * 教师信息（不含密码）
 */
@Data
public class TeacherVO {

    private Long id;

    private String tno;

    private String name;

    private String gender;

    private String title;

    private String college;

    private String phone;
}
