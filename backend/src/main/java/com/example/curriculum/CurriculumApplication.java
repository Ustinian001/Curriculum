package com.example.curriculum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * 学生选课系统启动类
 */
@SpringBootApplication
@MapperScan("com.example.curriculum.mapper")
public class CurriculumApplication {
    public static void main(String[] args) {
        SpringApplication.run(CurriculumApplication.class, args);
    }
}
