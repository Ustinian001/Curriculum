package com.example.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 教学班实体（课程 + 学期 + 授课教师 + 班级 的一次具体开课）
 * <p>
 * 一门课可以有多个教学班（例如高等数学 PK01 每个行政班一个教学班 PK0101~PK0106），
 * 每个教学班都可以被多个学生选择。
 */
@Data
@TableName("teaching_class")
public class TeachingClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程 id（course.id） */
    private Long courseId;

    /** 教学班编号，如 PK0101 */
    private String tcNo;

    /** 授课教师 id */
    private Long teacherId;

    /** 所属学期 id */
    private Long semesterId;

    /** 绑定班级 id（分班公共必修 / 专业类课程；合班公共必修与选修类为 null） */
    private Long classId;

    /** 教学班状态：1=开设中 0=已停开 */
    private Integer status;

    /** 最低开课人数（选修类低于此人数可停止开设） */
    private Integer minStudents;

    /** 选课容量 */
    private Integer capacity;

    /** 已选人数（并发安全通过原子 SQL 维护） */
    private Integer selectedCount;

    /** 上课地点 */
    private String location;

    /** 上课时间，如 "周一 1-2节,周三 5-6节" */
    private String schedule;
}
