package com.example.curriculum.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.curriculum.common.Result;
import com.example.curriculum.dto.*;
import com.example.curriculum.service.AdminService;
import com.example.curriculum.vo.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员端接口：学生/教师/课程/班级/学期管理
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ==================== 学生管理 ====================

    @GetMapping("/students")
    public Result<Page<StudentVO>> pageStudents(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword) {
        return Result.success(adminService.pageStudents(page, size, keyword));
    }

    @PostMapping("/students")
    public Result<Void> saveStudent(@RequestBody StudentDTO dto) {
        adminService.saveStudent(dto);
        return Result.success();
    }

    @PutMapping("/students")
    public Result<Void> updateStudent(@RequestBody StudentDTO dto) {
        adminService.saveStudent(dto);
        return Result.success();
    }

    @DeleteMapping("/students/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        adminService.deleteStudent(id);
        return Result.success();
    }

    /** 重置学生密码为默认 123456 */
    @PutMapping("/students/{id}/reset-password")
    public Result<Void> resetStudentPassword(@PathVariable Long id) {
        adminService.resetStudentPassword(id);
        return Result.success();
    }

    // ==================== 教师管理 ====================

    @GetMapping("/teachers")
    public Result<Page<TeacherVO>> pageTeachers(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword) {
        return Result.success(adminService.pageTeachers(page, size, keyword));
    }

    @PostMapping("/teachers")
    public Result<Void> saveTeacher(@RequestBody TeacherDTO dto) {
        adminService.saveTeacher(dto);
        return Result.success();
    }

    @PutMapping("/teachers")
    public Result<Void> updateTeacher(@RequestBody TeacherDTO dto) {
        adminService.saveTeacher(dto);
        return Result.success();
    }

    @DeleteMapping("/teachers/{id}")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        adminService.deleteTeacher(id);
        return Result.success();
    }

    @PutMapping("/teachers/{id}/reset-password")
    public Result<Void> resetTeacherPassword(@PathVariable Long id) {
        adminService.resetTeacherPassword(id);
        return Result.success();
    }

    // ==================== 课程管理 ====================

    @GetMapping("/courses")
    public Result<Page<CourseVO>> pageCourses(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(adminService.pageCourses(page, size, keyword));
    }

    @PostMapping("/courses")
    public Result<Void> saveCourse(@RequestBody CourseDTO dto) {
        adminService.saveCourse(dto);
        return Result.success();
    }

    @PutMapping("/courses")
    public Result<Void> updateCourse(@RequestBody CourseDTO dto) {
        adminService.saveCourse(dto);
        return Result.success();
    }

    @DeleteMapping("/courses/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        adminService.deleteCourse(id);
        return Result.success();
    }

    // ==================== 班级管理 ====================

    @GetMapping("/classes")
    public Result<List<ClassVO>> listClasses() {
        return Result.success(adminService.listClasses());
    }

    @PostMapping("/classes")
    public Result<Void> saveClass(@RequestBody ClassDTO dto) {
        adminService.saveClass(dto);
        return Result.success();
    }

    @PutMapping("/classes")
    public Result<Void> updateClass(@RequestBody ClassDTO dto) {
        adminService.saveClass(dto);
        return Result.success();
    }

    @DeleteMapping("/classes/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        adminService.deleteClass(id);
        return Result.success();
    }

    // ==================== 学期管理 ====================

    @GetMapping("/semesters")
    public Result<List<SemesterVO>> listSemesters() {
        return Result.success(adminService.listSemesters());
    }

    /** 查看某学期开设的课程（教学班）列表 */
    @GetMapping("/semesters/{id}/courses")
    public Result<List<CourseVO>> listSemesterCourses(@PathVariable Long id) {
        return Result.success(adminService.listSemesterCourses(id));
    }

    @PostMapping("/semesters")
    public Result<Void> saveSemester(@RequestBody SemesterDTO dto) {
        adminService.saveSemester(dto);
        return Result.success();
    }

    @PutMapping("/semesters")
    public Result<Void> updateSemester(@RequestBody SemesterDTO dto) {
        adminService.saveSemester(dto);
        return Result.success();
    }

    @DeleteMapping("/semesters/{id}")
    public Result<Void> deleteSemester(@PathVariable Long id) {
        adminService.deleteSemester(id);
        return Result.success();
    }

    /** 设为当前学期（全局唯一 isCurrent=1） */
    @PutMapping("/semesters/{id}/current")
    public Result<Void> setCurrentSemester(@PathVariable Long id) {
        adminService.setCurrentSemester(id);
        return Result.success();
    }
}
