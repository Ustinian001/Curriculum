package com.example.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 班级实体
 */
@Data
@TableName("`class`") // class 是 MySQL 保留字，需用反引号包裹
public class Clazz {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称，如 软件工程2401 */
    private String className;

    /** 年级，如 2024 */
    private String grade;

    /** 学院，如 计算机学院 */
    private String college;

    /** 专业，如 软件工程 */
    private String major;
}
