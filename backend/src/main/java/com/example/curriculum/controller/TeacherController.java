package com.example.curriculum.controller;

import com.example.curriculum.common.Result;
import com.example.curriculum.dto.CourseDTO;
import com.example.curriculum.dto.ScoreDTO;
import com.example.curriculum.service.TeacherService;
import com.example.curriculum.utils.UserContext;
import com.example.curriculum.vo.ClassVO;
import com.example.curriculum.vo.CourseStudentVO;
import com.example.curriculum.vo.CourseVO;
import com.example.curriculum.vo.ScheduleVO;
import com.example.curriculum.vo.SemesterVO;
import com.example.curriculum.vo.StudentVO;
import com.example.curriculum.vo.TeacherVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端接口
 */
@RestController
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /** 我的课程 */
    @GetMapping("/courses")
    public Result<List<CourseVO>> courses() {
        return Result.success(teacherService.listMyCourses(UserContext.getUserId()));
    }

    /** 课程学生名单（含成绩） */
    @GetMapping("/course/{courseId}/students")
    public Result<List<CourseStudentVO>> students(@PathVariable Long courseId) {
        return Result.success(teacherService.listCourseStudents(UserContext.getUserId(), courseId));
    }

    /** 可增加学生列表（尚未选该课程的学生） */
    @GetMapping("/course/{courseId}/students/addable")
    public Result<List<StudentVO>> addableStudents(@PathVariable Long courseId) {
        return Result.success(teacherService.listAddableStudents(UserContext.getUserId(), courseId));
    }

    /** 增加学生 */
    @PostMapping("/course/{courseId}/students/{studentId}")
    public Result<Void> addStudent(@PathVariable Long courseId, @PathVariable Long studentId) {
        teacherService.addStudent(UserContext.getUserId(), courseId, studentId);
        return Result.success();
    }

    /** 删除学生 */
    @DeleteMapping("/course/{courseId}/students/{studentId}")
    public Result<Void> removeStudent(@PathVariable Long courseId, @PathVariable Long studentId) {
        teacherService.removeStudent(UserContext.getUserId(), courseId, studentId);
        return Result.success();
    }

    /** 开设课程（教师作为授课教师） */
    @PostMapping("/course")
    public Result<Void> createCourse(@RequestBody CourseDTO dto) {
        teacherService.createCourse(UserContext.getUserId(), dto);
        return Result.success();
    }

    /** 开设课程表单：学期下拉 */
    @GetMapping("/semesters")
    public Result<List<SemesterVO>> semesters() {
        return Result.success(teacherService.listSemesters());
    }

    /** 开设课程表单：班级下拉 */
    @GetMapping("/classes")
    public Result<List<ClassVO>> classes() {
        return Result.success(teacherService.listClasses());
    }

    /** 停止开设课程（选课人数不达标时） */
    @PutMapping("/course/{courseId}/stop")
    public Result<Void> stopCourse(@PathVariable Long courseId) {
        teacherService.stopCourse(UserContext.getUserId(), courseId);
        return Result.success();
    }

    /** 录入/修改成绩 */
    @PutMapping("/score")
    public Result<Void> updateScore(@Valid @RequestBody ScoreDTO dto) {
        teacherService.updateScore(UserContext.getUserId(), dto);
        return Result.success();
    }

    /** 我的课表 */
    @GetMapping("/schedule")
    public Result<List<ScheduleVO>> schedule() {
        return Result.success(teacherService.getSchedule(UserContext.getUserId()));
    }

    /** 个人信息 */
    @GetMapping("/profile")
    public Result<TeacherVO> profile() {
        return Result.success(teacherService.getProfile(UserContext.getUserId()));
    }
}
