package com.example.curriculum.vo;

import lombok.Data;

/**
 * 课程信息（含教师名、学期名、是否已选、剩余容量）
 * <p>
 * 说明：本对象描述的是「教学班 + 课程」的组合视图。
 * {@code id} 仍然表示教学班 id（选课、退课、名单、停开、课表跳转都用它），
 * {@code courseId} 才是课程 id；{@code cno}/{@code cname}/{@code credit}/{@code hours}/{@code courseType}/{@code category}
 * 来自课程表（一门课一个课程编号），其余字段来自教学班。
 */
@Data
public class CourseVO {

    /** 教学班 id */
    private Long id;

    /** 课程 id */
    private Long courseId;

    /** 课程编号（唯一，一门课一个） */
    private String cno;

    private String cname;

    private Integer credit;

    private Integer hours;

    /** 教学班编号，如 PK0101 */
    private String tcNo;

    private Long teacherId;

    private String teacherName;

    private Long semesterId;

    private String semesterName;

    /** 课程类型：公共必修 / 专业类 / 选修类 */
    private String courseType;

    /** 课程类别（选修类细分小类） */
    private String category;

    /** 绑定班级 id（分班公共必修 / 专业类课程） */
    private Long classId;

    /** 绑定班级名称（分班公共必修 / 专业类课程） */
    private String className;

    /** 教学班状态：1=开设中 0=已停开 */
    private Integer status;

    /** 最低开课人数 */
    private Integer minStudents;

    private Integer capacity;

    private Integer selectedCount;

    /** 剩余容量 */
    private Integer remaining;

    private String location;

    private String schedule;

    /** 当前学生是否已选该教学班（仅学生端查询可选课程时有意义） */
    private Boolean selected;

    /** 是否与该生已选课程时间冲突（仅学生端查询可选课程时有意义） */
    private Boolean conflict;
}
