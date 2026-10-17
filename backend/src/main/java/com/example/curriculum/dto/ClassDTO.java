package com.example.curriculum.dto;

import lombok.Data;

/**
 * 班级新增/修改请求参数
 */
@Data
public class ClassDTO {

    private Long id;

    private String className;

    private String grade;

    private String college;

    private String major;
}
