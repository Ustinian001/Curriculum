package com.example.curriculum.controller;

import com.example.curriculum.common.Result;
import com.example.curriculum.service.StudentService;
import com.example.curriculum.utils.UserContext;
import com.example.curriculum.vo.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生端接口（当前登录学生 id 从 UserContext 获取）
 */
@RestController
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** 可选课程列表（当前学期、剩余容量、是否已选） */
    @GetMapping("/courses")
    public Result<List<CourseVO>> listCourses() {
        return Result.success(studentService.listSelectableCourses(UserContext.getUserId()));
    }

    /** 选课批次信息（选课中心确认页） */
    @GetMapping("/selection-batch")
    public Result<SelectionBatchVO> selectionBatch() {
        return Result.success(studentService.getSelectionBatch());
    }

    /** 选课 */
    @PostMapping("/course/{courseId}")
    public Result<Void> selectCourse(@PathVariable Long courseId) {
        studentService.selectCourse(UserContext.getUserId(), courseId);
        return Result.success();
    }

    /** 退课 */
    @DeleteMapping("/course/{courseId}")
    public Result<Void> withdrawCourse(@PathVariable Long courseId) {
        studentService.withdrawCourse(UserContext.getUserId(), courseId);
        return Result.success();
    }

    /** 我的课表 */
    @GetMapping("/schedule")
    public Result<List<ScheduleVO>> schedule() {
        return Result.success(studentService.getSchedule(UserContext.getUserId()));
    }

    /** 我的成绩（含学分统计） */
    @GetMapping("/scores")
    public Result<ScoreVO> scores() {
        return Result.success(studentService.getScores(UserContext.getUserId()));
    }

    /** 选课结果：全部已选课程（含自动分配与选修） */
    @GetMapping("/selection-result")
    public Result<List<SelectedCourseVO>> selectionResult() {
        return Result.success(studentService.getSelectionResult(UserContext.getUserId()));
    }

    /** 个人信息 */
    @GetMapping("/profile")
    public Result<StudentVO> profile() {
        return Result.success(studentService.getProfile(UserContext.getUserId()));
    }
}
