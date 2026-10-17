package com.example.curriculum.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生成绩单条记录
 */
@Data
public class ScoreItemVO {

    /** 教学班 id（课程编号见 cno，来自课程表） */
    private Long courseId;

    private String cno;

    private String cname;

    private Integer credit;

    private String teacherName;

    private String semesterName;

    private String schedule;

    /** 成绩，可能为空（未录入） */
    private Integer score;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime selectTime;
}
