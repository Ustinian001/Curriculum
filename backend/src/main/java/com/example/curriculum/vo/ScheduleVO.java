package com.example.curriculum.vo;

import lombok.Data;

/**
 * 学生课表单条记录
 */
@Data
public class ScheduleVO {

    /** 教学班 id（选课/名单操作使用；课程编号见 cno，来自课程表） */
    private Long courseId;

    private String cno;

    private String cname;

    private String teacherName;

    private String location;

    private String schedule;

    /** 所属学期名称 */
    private String semesterName;

    private Integer credit;
}
