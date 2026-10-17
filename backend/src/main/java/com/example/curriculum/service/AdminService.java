package com.example.curriculum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.curriculum.common.exception.BusinessException;
import com.example.curriculum.dto.*;
import com.example.curriculum.entity.*;
import com.example.curriculum.mapper.*;
import com.example.curriculum.utils.CourseVoAssembler;
import com.example.curriculum.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员服务：学生/教师/课程/班级/学期 CRUD、设置当前学期、重置密码
 * <p>
 * 课程模型：course（课程，一门课只有一个课程编号）+ teaching_class（教学班，一门课可开多个）。
 * 管理员列表里的一行 = 一个教学班；{@code id} 即教学班 id。
 */
@Service
public class AdminService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final ClazzMapper clazzMapper;
    private final SemesterMapper semesterMapper;
    private final ScMapper scMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminService(StudentMapper studentMapper, TeacherMapper teacherMapper, CourseMapper courseMapper,
                        TeachingClassMapper teachingClassMapper, ClazzMapper clazzMapper,
                        SemesterMapper semesterMapper, ScMapper scMapper,
                        BCryptPasswordEncoder passwordEncoder) {
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.courseMapper = courseMapper;
        this.teachingClassMapper = teachingClassMapper;
        this.clazzMapper = clazzMapper;
        this.semesterMapper = semesterMapper;
        this.scMapper = scMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== 学生管理 ====================

    public Page<StudentVO> pageStudents(long page, long size, String keyword) {
        Page<Student> p = new Page<>(page, size);
        LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Student::getSno, keyword).or().like(Student::getName, keyword));
        }
        qw.orderByAsc(Student::getSno);
        Page<Student> sp = studentMapper.selectPage(p, qw);

        Map<Long, String> classMap = clazzMapper.selectList(null).stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));

        Page<StudentVO> voPage = new Page<>(sp.getCurrent(), sp.getSize(), sp.getTotal());
        voPage.setRecords(sp.getRecords().stream().map(s -> {
            StudentVO vo = new StudentVO();
            vo.setId(s.getId());
            vo.setSno(s.getSno());
            vo.setName(s.getName());
            vo.setGender(s.getGender());
            vo.setClassId(s.getClassId());
            vo.setClassName(classMap.get(s.getClassId()));
            vo.setCollege(s.getCollege());
            vo.setMajor(s.getMajor());
            vo.setPhone(s.getPhone());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Transactional
    public void saveStudent(StudentDTO dto) {
        // 校验学号唯一
        checkSnoUnique(dto.getSno(), dto.getId());
        Student s = new Student();
        BeanUtils.copyProperties(dto, s, "password");
        if (dto.getId() == null) {
            // 新增：密码为空则用默认密码，BCrypt 加密
            String raw = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD;
            s.setPassword(passwordEncoder.encode(raw));
            studentMapper.insert(s);
            // 新增学生自动录入已有公共必修 + 本班专业类课程
            autoEnrollStudent(s);
        } else {
            // 修改：密码为空则不修改
            if (StringUtils.hasText(dto.getPassword())) {
                s.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            studentMapper.updateById(s);
        }
    }

    /**
     * 新增学生自动录入：合班「公共必修」（未绑定班级）+ 本班「分班公共必修 / 专业类」教学班（均为开设中）。
     */
    private void autoEnrollStudent(Student student) {
        List<TeachingClass> targets = new ArrayList<>();
        // 合班公共必修：未绑定班级，全员统一上课
        List<Long> publicCourseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getCourseType, "公共必修"))
                .stream().map(Course::getId).collect(Collectors.toList());
        if (!publicCourseIds.isEmpty()) {
            targets.addAll(teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                    .in(TeachingClass::getCourseId, publicCourseIds)
                    .isNull(TeachingClass::getClassId)
                    .eq(TeachingClass::getStatus, 1)));
            if (student.getClassId() != null) {
                // 分班公共必修：按班级绑定
                targets.addAll(teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                        .in(TeachingClass::getCourseId, publicCourseIds)
                        .eq(TeachingClass::getClassId, student.getClassId())
                        .eq(TeachingClass::getStatus, 1)));
            }
        }
        // 专业类：按班级绑定
        if (student.getClassId() != null) {
            List<Long> majorCourseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getCourseType, "专业类"))
                    .stream().map(Course::getId).collect(Collectors.toList());
            if (!majorCourseIds.isEmpty()) {
                targets.addAll(teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                        .in(TeachingClass::getCourseId, majorCourseIds)
                        .eq(TeachingClass::getClassId, student.getClassId())
                        .eq(TeachingClass::getStatus, 1)));
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (TeachingClass t : targets) {
            Sc sc = new Sc();
            sc.setStudentId(student.getId());
            sc.setTeachingClassId(t.getId());
            sc.setSelectTime(now);
            scMapper.insert(sc);
            t.setSelectedCount((t.getSelectedCount() == null ? 0 : t.getSelectedCount()) + 1);
            teachingClassMapper.updateById(t);
        }
    }

    @Transactional
    public void deleteStudent(Long id) {
        // 删除学生的选课记录
        scMapper.delete(new LambdaQueryWrapper<Sc>().eq(Sc::getStudentId, id));
        studentMapper.deleteById(id);
    }

    public void resetStudentPassword(Long id) {
        Student s = studentMapper.selectById(id);
        if (s == null) {
            throw new BusinessException("学生不存在");
        }
        s.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        studentMapper.updateById(s);
    }

    // ==================== 教师管理 ====================

    public Page<TeacherVO> pageTeachers(long page, long size, String keyword) {
        Page<Teacher> p = new Page<>(page, size);
        LambdaQueryWrapper<Teacher> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Teacher::getTno, keyword).or().like(Teacher::getName, keyword));
        }
        qw.orderByAsc(Teacher::getTno);
        Page<Teacher> tp = teacherMapper.selectPage(p, qw);

        Page<TeacherVO> voPage = new Page<>(tp.getCurrent(), tp.getSize(), tp.getTotal());
        voPage.setRecords(tp.getRecords().stream().map(t -> {
            TeacherVO vo = new TeacherVO();
            vo.setId(t.getId());
            vo.setTno(t.getTno());
            vo.setName(t.getName());
            vo.setGender(t.getGender());
            vo.setTitle(t.getTitle());
            vo.setCollege(t.getCollege());
            vo.setPhone(t.getPhone());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Transactional
    public void saveTeacher(TeacherDTO dto) {
        checkTnoUnique(dto.getTno(), dto.getId());
        Teacher t = new Teacher();
        BeanUtils.copyProperties(dto, t, "password");
        if (dto.getId() == null) {
            String raw = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD;
            t.setPassword(passwordEncoder.encode(raw));
            teacherMapper.insert(t);
        } else {
            if (StringUtils.hasText(dto.getPassword())) {
                t.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            teacherMapper.updateById(t);
        }
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Long count = teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getTeacherId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该教师名下存在课程，无法删除");
        }
        teacherMapper.deleteById(id);
    }

    public void resetTeacherPassword(Long id) {
        Teacher t = teacherMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("教师不存在");
        }
        t.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        teacherMapper.updateById(t);
    }

    // ==================== 课程管理（列表里一行 = 一个教学班） ====================

    public Page<CourseVO> pageCourses(long page, long size, String keyword) {
        LambdaQueryWrapper<TeachingClass> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            // 课程编号/名称在课程表上，先查出匹配的课程
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .like(Course::getCno, keyword).or().like(Course::getCname, keyword))
                    .stream().map(Course::getId).collect(Collectors.toList());
            if (courseIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            qw.in(TeachingClass::getCourseId, courseIds);
        }
        qw.orderByAsc(TeachingClass::getCourseId).orderByAsc(TeachingClass::getId);
        Page<TeachingClass> tp = teachingClassMapper.selectPage(new Page<>(page, size), qw);

        Map<Long, Course> courseMap = loadCourseMap(tp.getRecords());
        Map<Long, String> teacherNames = teacherMapper.selectList(null).stream()
                .collect(Collectors.toMap(Teacher::getId, Teacher::getName));
        Map<Long, String> semesterNames = semesterMapper.selectList(null).stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getSemesterName));
        Map<Long, String> classNames = clazzMapper.selectList(null).stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));

        Page<CourseVO> voPage = new Page<>(tp.getCurrent(), tp.getSize(), tp.getTotal());
        voPage.setRecords(tp.getRecords().stream()
                .map(t -> CourseVoAssembler.build(t, courseMap.get(t.getCourseId()),
                        teacherNames, semesterNames, classNames))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 某学期开设的课程（教学班）列表
     */
    public List<CourseVO> listSemesterCourses(Long semesterId) {
        if (semesterMapper.selectById(semesterId) == null) {
            throw new BusinessException("学期不存在");
        }
        List<TeachingClass> classes = teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getSemesterId, semesterId)
                .orderByAsc(TeachingClass::getCourseId)
                .orderByAsc(TeachingClass::getId));
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Course> courseMap = loadCourseMap(classes);
        Map<Long, String> teacherNames = teacherMapper.selectList(null).stream()
                .collect(Collectors.toMap(Teacher::getId, Teacher::getName));
        Map<Long, String> semesterNames = semesterMapper.selectList(null).stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getSemesterName));
        Map<Long, String> classNames = clazzMapper.selectList(null).stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));
        return classes.stream()
                .map(t -> CourseVoAssembler.build(t, courseMap.get(t.getCourseId()),
                        teacherNames, semesterNames, classNames))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveCourse(CourseDTO dto) {
        if (!StringUtils.hasText(dto.getCno())) {
            throw new BusinessException("课程编号不能为空");
        }
        if (dto.getId() == null) {
            // ---------- 新增：按课程编号复用/新建课程，再新增一个教学班 ----------
            Course course = courseMapper.selectOne(new LambdaQueryWrapper<Course>()
                    .eq(Course::getCno, dto.getCno()));
            if (course == null) {
                course = new Course();
                course.setCno(dto.getCno());
                course.setCname(dto.getCname());
                course.setCredit(dto.getCredit());
                course.setHours(dto.getHours());
                course.setCourseType(dto.getCourseType());
                course.setCategory(dto.getCategory());
                courseMapper.insert(course);
            } else {
                // 该编号已属于某门课：允许继续为它开设教学班，但课程信息必须一致
                checkCourseInfoConsistent(course, dto);
            }
            TeachingClass tc = new TeachingClass();
            tc.setCourseId(course.getId());
            tc.setTcNo(nextTcNo(course));
            tc.setTeacherId(dto.getTeacherId());
            tc.setSemesterId(dto.getSemesterId());
            tc.setClassId("选修类".equals(course.getCourseType()) ? null : dto.getClassId());
            tc.setStatus(1);
            tc.setSelectedCount(0);
            tc.setMinStudents(dto.getMinStudents() != null ? dto.getMinStudents()
                    : ("选修类".equals(course.getCourseType()) ? 25 : null));
            tc.setCapacity(dto.getCapacity());
            tc.setLocation(dto.getLocation());
            tc.setSchedule(dto.getSchedule());
            teachingClassMapper.insert(tc);
            // 公共必修/专业类教学班新增时自动分配学生
            autoEnrollStudents(tc, course);
        } else {
            // ---------- 修改：更新教学班，并同步该课程的公共信息 ----------
            TeachingClass tc = teachingClassMapper.selectById(dto.getId());
            if (tc == null) {
                throw new BusinessException("教学班不存在");
            }
            Course course = courseMapper.selectById(tc.getCourseId());
            if (course == null) {
                throw new BusinessException("课程不存在");
            }
            checkCnoUnique(dto.getCno(), course.getId());
            course.setCno(dto.getCno());
            course.setCname(dto.getCname());
            course.setCredit(dto.getCredit());
            course.setHours(dto.getHours());
            course.setCourseType(dto.getCourseType());
            course.setCategory(dto.getCategory());
            courseMapper.updateById(course);

            tc.setTeacherId(dto.getTeacherId());
            tc.setSemesterId(dto.getSemesterId());
            tc.setClassId("选修类".equals(course.getCourseType()) ? null : dto.getClassId());
            tc.setMinStudents(dto.getMinStudents());
            tc.setCapacity(dto.getCapacity());
            tc.setLocation(dto.getLocation());
            tc.setSchedule(dto.getSchedule());
            teachingClassMapper.updateById(tc);
        }
    }

    /**
     * 自动分配：
     * 分班课程（公共必修/专业类 且绑定了班级）-> 该班级学生；
     * 合班公共必修（未绑定班级）-> 全部学生；
     * 选修类 -> 不分配。
     */
    private void autoEnrollStudents(TeachingClass tc, Course course) {
        List<Long> studentIds = new ArrayList<>();
        if (tc.getClassId() != null
                && ("公共必修".equals(course.getCourseType()) || "专业类".equals(course.getCourseType()))) {
            // 分班课程：只分配给绑定班级的学生
            studentIds = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .eq(Student::getClassId, tc.getClassId()))
                    .stream().map(Student::getId).collect(Collectors.toList());
        } else if ("公共必修".equals(course.getCourseType())) {
            // 合班公共必修：分配给全部学生
            studentIds = studentMapper.selectList(null).stream()
                    .map(Student::getId).collect(Collectors.toList());
        } else {
            return; // 选修类不自动分配
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long sid : studentIds) {
            Sc sc = new Sc();
            sc.setStudentId(sid);
            sc.setTeachingClassId(tc.getId());
            sc.setSelectTime(now);
            scMapper.insert(sc);
        }
        tc.setSelectedCount(studentIds.size());
        teachingClassMapper.updateById(tc);
    }

    @Transactional
    public void deleteCourse(Long id) {
        // id 为教学班 id
        TeachingClass tc = teachingClassMapper.selectById(id);
        if (tc == null) {
            return;
        }
        // 删除该教学班的选课记录
        scMapper.delete(new LambdaQueryWrapper<Sc>().eq(Sc::getTeachingClassId, id));
        teachingClassMapper.deleteById(id);
        // 该课程已没有任何教学班时，一并删除课程，避免留下孤立课程
        Long remain = teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getCourseId, tc.getCourseId()));
        if (remain == null || remain == 0) {
            courseMapper.deleteById(tc.getCourseId());
        }
    }

    /**
     * 生成教学班编号：课程编号 + 两位序号（如 PK01 -> PK0101）
     */
    private String nextTcNo(Course course) {
        Long count = teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getCourseId, course.getId()));
        int seq = (count == null ? 0 : count.intValue()) + 1;
        String tcNo = course.getCno() + String.format("%02d", seq);
        while (teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getTcNo, tcNo)) > 0) {
            seq++;
            tcNo = course.getCno() + String.format("%02d", seq);
        }
        return tcNo;
    }

    // ==================== 班级管理 ====================

    public List<ClassVO> listClasses() {
        return clazzMapper.selectList(null).stream().map(c -> {
            ClassVO vo = new ClassVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    public void saveClass(ClassDTO dto) {
        Clazz c = new Clazz();
        BeanUtils.copyProperties(dto, c);
        if (dto.getId() == null) {
            clazzMapper.insert(c);
        } else {
            clazzMapper.updateById(c);
        }
    }

    public void deleteClass(Long id) {
        Long count = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getClassId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该班级下存在学生，无法删除");
        }
        clazzMapper.deleteById(id);
    }

    // ==================== 学期管理 ====================

    public List<SemesterVO> listSemesters() {
        return semesterMapper.selectList(null).stream().map(s -> {
            SemesterVO vo = new SemesterVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void saveSemester(SemesterDTO dto) {
        Semester s = new Semester();
        BeanUtils.copyProperties(dto, s);
        if (dto.getId() == null) {
            s.setIsCurrent(0); // 新增学期默认不是当前学期
            semesterMapper.insert(s);
        } else {
            semesterMapper.updateById(s);
        }
    }

    public void deleteSemester(Long id) {
        Long count = teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getSemesterId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该学期下存在课程，无法删除");
        }
        semesterMapper.deleteById(id);
    }

    /**
     * 设置当前学期：先取消所有 isCurrent，再置目标学期为 1，保证全局唯一
     */
    @Transactional
    public void setCurrentSemester(Long id) {
        Semester s = semesterMapper.selectById(id);
        if (s == null) {
            throw new BusinessException("学期不存在");
        }
        semesterMapper.update(null, new LambdaUpdateWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1)
                .set(Semester::getIsCurrent, 0));
        s.setIsCurrent(1);
        semesterMapper.updateById(s);
    }

    // ==================== 私有校验/辅助 ====================

    private Map<Long, Course> loadCourseMap(List<TeachingClass> list) {
        Set<Long> ids = list.stream().map(TeachingClass::getCourseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return courseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
    }

    private void checkSnoUnique(String sno, Long excludeId) {
        if (!StringUtils.hasText(sno)) {
            throw new BusinessException("学号不能为空");
        }
        Long count = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getSno, sno)
                .ne(excludeId != null, Student::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("学号已存在");
        }
    }

    private void checkTnoUnique(String tno, Long excludeId) {
        if (!StringUtils.hasText(tno)) {
            throw new BusinessException("工号不能为空");
        }
        Long count = teacherMapper.selectCount(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTno, tno)
                .ne(excludeId != null, Teacher::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("工号已存在");
        }
    }

    /**
     * 课程编号唯一：一门课只能有一个课程编号
     */
    private void checkCnoUnique(String cno, Long excludeCourseId) {
        if (!StringUtils.hasText(cno)) {
            throw new BusinessException("课程编号不能为空");
        }
        Long count = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getCno, cno)
                .ne(excludeCourseId != null, Course::getId, excludeCourseId));
        if (count != null && count > 0) {
            throw new BusinessException("课程编号 " + cno + " 已存在（一门课只能有一个课程编号）");
        }
    }

    /**
     * 同一门课的课程信息必须一致，避免同编号对应不同课程
     */
    private void checkCourseInfoConsistent(Course course, CourseDTO dto) {
        if (!Objects.equals(course.getCname(), dto.getCname())
                || !Objects.equals(course.getCredit(), dto.getCredit())
                || !Objects.equals(course.getHours(), dto.getHours())
                || !Objects.equals(course.getCourseType(), dto.getCourseType())) {
            throw new BusinessException("课程编号 " + course.getCno() + " 已是《" + course.getCname()
                    + "》，一门课只有一个课程编号；如需为它新增教学班，请填写相同的课程信息");
        }
    }
}
