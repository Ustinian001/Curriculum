package com.example.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学期实体
 */
@Data
@TableName("semester")
public class Semester {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学期名称，如 2025-2026-1 */
    private String semesterName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 是否当前学期：1=是 0=否（全局只有一个为 1） */
    private Integer isCurrent;
}
