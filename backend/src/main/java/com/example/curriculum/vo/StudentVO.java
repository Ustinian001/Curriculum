package com.example.curriculum.vo;

import lombok.Data;

/**
 * 学生信息（不含密码）
 */
@Data
public class StudentVO {

    private Long id;

    private String sno;

    private String name;

    private String gender;

    private Long classId;

    /** 班级名称 */
    private String className;

    /** 学院 */
    private String college;

    /** 专业 */
    private String major;

    private String phone;
}
