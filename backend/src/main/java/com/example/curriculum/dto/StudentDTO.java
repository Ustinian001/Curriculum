package com.example.curriculum.dto;

import lombok.Data;

/**
 * 学生新增/修改请求参数
 */
@Data
public class StudentDTO {

    private Long id;

    private String sno;

    /** 密码：新增时可空（默认 123456），修改时空表示不修改 */
    private String password;

    private String name;

    private String gender;

    private Long classId;

    /** 学院，如 计算机学院 */
    private String college;

    /** 专业，如 软件工程 */
    private String major;

    private String phone;
}
