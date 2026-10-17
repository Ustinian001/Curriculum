package com.example.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 选课记录实体（sc）
 * <p>
 * 学生选的是「教学班」，因此一门课可以被多个学生选择（同一门课的不同教学班各自对应多条 sc 记录），
 * 而同一个学生不能重复选同一个教学班（唯一键 student_id + teaching_class_id）。
 */
@Data
@TableName("sc")
public class Sc {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 id */
    private Long studentId;

    /** 教学班 id（teaching_class.id） */
    private Long teachingClassId;

    /** 选课时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime selectTime;

    /** 成绩 0~100，可为空（未录入） */
    private Integer score;
}
