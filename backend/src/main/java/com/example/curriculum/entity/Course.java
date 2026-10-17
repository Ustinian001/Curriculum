package com.example.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课程实体（课程目录：一门课只有一条记录、一个课程编号）
 * <p>
 * 某学期的具体开课信息（授课教师、上课班级、时间地点、容量等）放在 {@link TeachingClass}，
 * 一门课可以开设多个教学班，供多个学生选择。
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程编号，如 PK01、ZY01、XK01（唯一：一门课只有一个编号） */
    private String cno;

    /** 课程名称 */
    private String cname;

    /** 学分 */
    private Integer credit;

    /** 学时 */
    private Integer hours;

    /** 课程类型：公共必修 / 专业类 / 选修类 */
    private String courseType;

    /** 课程类别（选修类细分小类，如 国学经典/诗词文学/美学艺术 等） */
    private String category;
}
