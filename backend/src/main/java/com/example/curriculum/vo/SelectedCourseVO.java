package com.example.curriculum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生选课结果单条记录（含课程类型）
 */
@Data
public class SelectedCourseVO {

    /** 教学班 id（课程编号见 cno，来自课程表） */
    private Long courseId;

    private String cno;

    private String cname;

    private Integer credit;

    private String teacherName;

    private String location;

    private String schedule;

    /** 所属学期名称 */
    private String semesterName;

    /** 课程类型：公共必修 / 专业类 / 选修类 */
    private String courseType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime selectTime;
}
