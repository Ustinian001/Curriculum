package com.example.curriculum.vo;

import lombok.Data;

/**
 * 教师端：课程学生名单（含成绩）
 */
@Data
public class CourseStudentVO {

    /** 选课记录 id，用于提交成绩 */
    private Long scId;

    private Long studentId;

    private String sno;

    private String name;

    private String gender;

    private String className;

    /** 成绩，可能为空 */
    private Integer score;
}
