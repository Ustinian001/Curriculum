package com.example.curriculum.vo;

import lombok.Data;

/**
 * 选课批次信息（学生选课中心确认页）
 */
@Data
public class SelectionBatchVO {

    /** 批次名称 */
    private String batchName;

    /** 学年学期 */
    private String semester;

    /** 选课时间（展示字符串，如 2026-01-14 10:00 ~ 2026-01-20 17:00） */
    private String selectTime;

    /** 限选门数 */
    private String courseLimit;

    /** 选课抽签 */
    private String lottery;

    /** 选课控制 */
    private String control;

    /** 教学班选课人数 */
    private String classLimit;

    /** 预置课是否可退 */
    private String preCourseRefund;

    /** 允许冲突选课 */
    private String allowConflict;

    /** 控制选课学分 */
    private String creditLimit;

    /** 选课开始时间（供前端时间校验，格式 yyyy-MM-dd HH:mm） */
    private String selectStartTime;

    /** 选课结束时间（供前端时间校验，格式 yyyy-MM-dd HH:mm） */
    private String selectEndTime;
}
