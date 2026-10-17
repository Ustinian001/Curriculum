package com.example.curriculum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.curriculum.common.exception.BusinessException;
import com.example.curriculum.dto.CourseDTO;
import com.example.curriculum.dto.ScoreDTO;
import com.example.curriculum.entity.*;
import com.example.curriculum.mapper.*;
import com.example.curriculum.utils.CourseVoAssembler;
import com.example.curriculum.vo.ClassVO;
import com.example.curriculum.vo.CourseStudentVO;
import com.example.curriculum.vo.CourseVO;
import com.example.curriculum.vo.ScheduleVO;
import com.example.curriculum.vo.SemesterVO;
import com.example.curriculum.vo.StudentVO;
import com.example.curriculum.vo.TeacherVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端服务：我的课程、学生名单、成绩录入、个人信息
 * <p>
 * 教师「开设课程」= 为某门课程（course，一门课一个编号）开设一个教学班（teaching_class）。
 */
@Service
public class TeacherService {

    private final CourseMapper courseMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final ScMapper scMapper;
    private final SemesterMapper semesterMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final ClazzMapper clazzMapper;

    public TeacherService(CourseMapper courseMapper, TeachingClassMapper teachingClassMapper, ScMapper scMapper,
                          SemesterMapper semesterMapper, TeacherMapper teacherMapper, StudentMapper studentMapper,
                          ClazzMapper clazzMapper) {
        this.courseMapper = courseMapper;
        this.teachingClassMapper = teachingClassMapper;
        this.scMapper = scMapper;
        this.semesterMapper = semesterMapper;
        this.teacherMapper = teacherMapper;
        this.studentMapper = studentMapper;
        this.clazzMapper = clazzMapper;
    }

    /**
     * 我的课程（我授课的教学班）
     */
    public List<CourseVO> listMyCourses(Long teacherId) {
        List<TeachingClass> classes = teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getTeacherId, teacherId)
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

    /**
     * 教师开设课程：教师作为授课教师，为某门课程开设一个教学班。
     * 若课程编号已存在，则视为「为同一门课增开教学班」，要求课程信息一致。
     * 分班课程（专业类 且绑定班级）自动分配给该班级学生；选修类由学生自由选课。
     */
    @Transactional
    public void createCourse(Long teacherId, CourseDTO dto) {
        if (!StringUtils.hasText(dto.getCno())) {
            throw new BusinessException("课程编号不能为空");
        }
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
        } else if (!Objects.equals(course.getCname(), dto.getCname())
                || !Objects.equals(course.getCredit(), dto.getCredit())
                || !Objects.equals(course.getHours(), dto.getHours())
                || !Objects.equals(course.getCourseType(), dto.getCourseType())) {
            throw new BusinessException("课程编号 " + course.getCno() + " 已存在（《" + course.getCname()
                    + "》），一门课只有一个课程编号；如需为它增开教学班，请填写相同的课程信息");
        }
        TeachingClass tc = new TeachingClass();
        tc.setCourseId(course.getId());
        tc.setTcNo(nextTcNo(course));
        tc.setTeacherId(teacherId); // 当前登录教师
        tc.setSemesterId(dto.getSemesterId());
        tc.setClassId("专业类".equals(course.getCourseType()) ? dto.getClassId() : null);
        tc.setStatus(1);
        tc.setSelectedCount(0);
        tc.setMinStudents(dto.getMinStudents() != null ? dto.getMinStudents()
                : ("选修类".equals(course.getCourseType()) ? 25 : null));
        tc.setCapacity(dto.getCapacity());
        tc.setLocation(dto.getLocation());
        tc.setSchedule(dto.getSchedule());
        teachingClassMapper.insert(tc);
        autoEnrollStudents(tc, course);
    }

    /**
     * 停止开设：仅选修类课程在选课人数低于最低要求时可停开，停开后清空选课记录。
     *
     * @param teachingClassId 教学班 id
     */
    @Transactional
    public void stopCourse(Long teacherId, Long teachingClassId) {
        TeachingClass tc = teachingClassMapper.selectById(teachingClassId);
        if (tc == null || !Objects.equals(tc.getTeacherId(), teacherId)) {
            throw new BusinessException("无权操作该课程");
        }
        Course course = courseMapper.selectById(tc.getCourseId());
        if (course == null || !"选修类".equals(course.getCourseType())) {
            throw new BusinessException("仅选修类课程可停止开设");
        }
        if (!Integer.valueOf(1).equals(tc.getStatus())) {
            throw new BusinessException("该课程已停止开设");
        }
        int selected = tc.getSelectedCount() == null ? 0 : tc.getSelectedCount();
        int min = tc.getMinStudents() == null ? 25 : tc.getMinStudents();
        if (selected >= min) {
            throw new BusinessException("选课人数已达最低要求，无需停止开设");
        }
        // 清空选课记录并标记停开
        scMapper.delete(new LambdaQueryWrapper<Sc>().eq(Sc::getTeachingClassId, teachingClassId));
        tc.setStatus(0);
        tc.setSelectedCount(0);
        teachingClassMapper.updateById(tc);
    }

    /**
     * 自动分配：
     * 分班课程（专业类 且绑定了班级）-> 该班级学生；
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

    /**
     * 开设课程表单所需的学期下拉数据
     */
    public List<SemesterVO> listSemesters() {
        return semesterMapper.selectList(null).stream().map(s -> {
            SemesterVO vo = new SemesterVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 开设课程表单所需的班级下拉数据（专业类课程需绑定班级）
     */
    public List<ClassVO> listClasses() {
        return clazzMapper.selectList(null).stream().map(c -> {
            ClassVO vo = new ClassVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 课程学生名单（含成绩）
     */
    public List<CourseStudentVO> listCourseStudents(Long teacherId, Long teachingClassId) {
        TeachingClass tc = requireOwnedClass(teacherId, teachingClassId);
        List<Sc> scList = scMapper.selectList(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getTeachingClassId, tc.getId()));
        if (scList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> studentIds = scList.stream().map(Sc::getStudentId).collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, String> classMap = clazzMapper.selectList(null).stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));

        return scList.stream().map(sc -> {
            Student s = studentMap.get(sc.getStudentId());
            CourseStudentVO vo = new CourseStudentVO();
            vo.setScId(sc.getId());
            vo.setStudentId(sc.getStudentId());
            if (s != null) {
                vo.setSno(s.getSno());
                vo.setName(s.getName());
                vo.setGender(s.getGender());
                vo.setClassName(classMap.get(s.getClassId()));
            }
            vo.setScore(sc.getScore());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 可增加学生列表：尚未选修该教学班的学生。
     * 若教学班绑定班级（分班公共必修/专业类），只返回该班学生；否则返回全部未选学生。
     */
    public List<StudentVO> listAddableStudents(Long teacherId, Long teachingClassId) {
        TeachingClass tc = requireOwnedClass(teacherId, teachingClassId);
        // 已选该教学班的学生 id
        Set<Long> enrolledIds = scMapper.selectList(new LambdaQueryWrapper<Sc>()
                        .eq(Sc::getTeachingClassId, tc.getId()))
                .stream().map(Sc::getStudentId).collect(Collectors.toSet());
        LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<>();
        if (tc.getClassId() != null) {
            qw.eq(Student::getClassId, tc.getClassId());
        }
        List<Student> students = studentMapper.selectList(qw);
        Map<Long, String> classMap = clazzMapper.selectList(null).stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));
        return students.stream()
                .filter(s -> !enrolledIds.contains(s.getId()))
                .map(s -> {
                    StudentVO vo = new StudentVO();
                    vo.setId(s.getId());
                    vo.setSno(s.getSno());
                    vo.setName(s.getName());
                    vo.setGender(s.getGender());
                    vo.setClassId(s.getClassId());
                    vo.setClassName(classMap.get(s.getClassId()));
                    vo.setPhone(s.getPhone());
                    return vo;
                }).collect(Collectors.toList());
    }

    /**
     * 增加学生：教师手动将学生加入教学班名单（校验归属、重复、容量）。
     */
    @Transactional
    public void addStudent(Long teacherId, Long teachingClassId, Long studentId) {
        requireOwnedClass(teacherId, teachingClassId);
        if (studentMapper.selectById(studentId) == null) {
            throw new BusinessException("学生不存在");
        }
        Long count = scMapper.selectCount(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getTeachingClassId, teachingClassId)
                .eq(Sc::getStudentId, studentId));
        if (count != null && count > 0) {
            throw new BusinessException("该学生已在课程名单中");
        }
        // 原子增加人数，返回 0 表示容量已满
        int updated = teachingClassMapper.increaseSelectedCount(teachingClassId);
        if (updated == 0) {
            throw new BusinessException("课程容量已满");
        }
        Sc sc = new Sc();
        sc.setTeachingClassId(teachingClassId);
        sc.setStudentId(studentId);
        sc.setSelectTime(LocalDateTime.now());
        scMapper.insert(sc);
    }

    /**
     * 删除学生：教师将学生移出教学班名单（同时删除其成绩记录并减少人数）。
     */
    @Transactional
    public void removeStudent(Long teacherId, Long teachingClassId, Long studentId) {
        requireOwnedClass(teacherId, teachingClassId);
        Sc sc = scMapper.selectOne(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getTeachingClassId, teachingClassId)
                .eq(Sc::getStudentId, studentId));
        if (sc == null) {
            throw new BusinessException("该学生不在课程名单中");
        }
        scMapper.deleteById(sc.getId());
        teachingClassMapper.decreaseSelectedCount(teachingClassId);
    }

    /**
     * 录入/修改成绩：校验教师归属与成绩范围
     */
    public void updateScore(Long teacherId, ScoreDTO dto) {
        Integer score = dto.getScore();
        if (score == null || score < 0 || score > 100) {
            throw new BusinessException("成绩必须在 0~100 之间");
        }
        Sc sc = scMapper.selectById(dto.getScId());
        if (sc == null) {
            throw new BusinessException("选课记录不存在");
        }
        TeachingClass tc = teachingClassMapper.selectById(sc.getTeachingClassId());
        if (tc == null || !Objects.equals(tc.getTeacherId(), teacherId)) {
            throw new BusinessException("无权修改该课程成绩");
        }
        sc.setScore(score);
        scMapper.updateById(sc);
    }

    /**
     * 个人信息
     */
    public TeacherVO getProfile(Long teacherId) {
        Teacher t = teacherMapper.selectById(teacherId);
        if (t == null) {
            throw new BusinessException("教师不存在");
        }
        TeacherVO vo = new TeacherVO();
        vo.setId(t.getId());
        vo.setTno(t.getTno());
        vo.setName(t.getName());
        vo.setGender(t.getGender());
        vo.setTitle(t.getTitle());
        vo.setCollege(t.getCollege());
        vo.setPhone(t.getPhone());
        return vo;
    }

    /**
     * 我的课表：本人教授的教学班
     */
    public List<ScheduleVO> getSchedule(Long teacherId) {
        List<TeachingClass> classes = teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getTeacherId, teacherId));
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Course> courseMap = loadCourseMap(classes);
        Map<Long, String> semesterNames = semesterMapper.selectList(null).stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getSemesterName));
        List<ScheduleVO> result = new ArrayList<>();
        for (TeachingClass t : classes) {
            Course c = courseMap.get(t.getCourseId());
            if (c == null) {
                continue;
            }
            ScheduleVO vo = new ScheduleVO();
            vo.setCourseId(t.getId());
            vo.setCno(c.getCno());
            vo.setCname(c.getCname());
            vo.setLocation(t.getLocation());
            vo.setSchedule(t.getSchedule());
            vo.setCredit(c.getCredit());
            vo.setSemesterName(semesterNames.get(t.getSemesterId()));
            result.add(vo);
        }
        return result;
    }

    // ---------- 私有辅助方法 ----------

    private TeachingClass requireOwnedClass(Long teacherId, Long teachingClassId) {
        TeachingClass tc = teachingClassMapper.selectById(teachingClassId);
        if (tc == null || !Objects.equals(tc.getTeacherId(), teacherId)) {
            throw new BusinessException("无权操作该课程");
        }
        return tc;
    }

    private Map<Long, Course> loadCourseMap(List<TeachingClass> list) {
        Set<Long> ids = list.stream().map(TeachingClass::getCourseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return courseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
    }
}
