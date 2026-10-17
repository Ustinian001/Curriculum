package com.example.curriculum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 成绩录入/修改请求参数
 */
@Data
public class ScoreDTO {

    /** 选课记录 id */
    @NotNull(message = "选课记录不能为空")
    private Long scId;

    /** 成绩，0~100 */
    @NotNull(message = "成绩不能为空")
    private Integer score;
}
