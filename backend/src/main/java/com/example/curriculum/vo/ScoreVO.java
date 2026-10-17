package com.example.curriculum.vo;

import lombok.Data;

import java.util.List;

/**
 * 学生成绩总览（含学分统计）
 */
@Data
public class ScoreVO {

    private List<ScoreItemVO> items;

    /** 已修总学分（已出成绩课程的学分之和） */
    private Integer totalCredit;

    /** 已通过学分（成绩 >= 60 的课程学分之和） */
    private Integer passedCredit;
}
