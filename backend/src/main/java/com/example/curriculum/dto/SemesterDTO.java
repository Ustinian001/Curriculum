package com.example.curriculum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学期新增/修改请求参数
 */
@Data
public class SemesterDTO {

    private Long id;

    private String semesterName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 是否当前学期 */
    private Integer isCurrent;
}
