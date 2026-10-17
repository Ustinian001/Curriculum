package com.example.curriculum.dto;

import lombok.Data;

/**
 * 课程新增/修改请求参数
 */
@Data
public class CourseDTO {

    private Long id;

    private String cno;

    private String cname;

    private Integer credit;

    private Integer hours;

    private Long teacherId;

    private Long semesterId;

    /** 课程类型：公共必修 / 专业类 / 选修类 */
    private String courseType;

    /** 课程类别（选修类细分小类） */
    private String category;

    /** 绑定班级 id（分班公共必修 / 专业类课程） */
    private Long classId;

    /** 最低开课人数 */
    private Integer minStudents;

    private Integer capacity;

    private String location;

    /** 上课时间，如 "周一 1-2节,周三 5-6节" */
    private String schedule;
}
