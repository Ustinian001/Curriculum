package com.example.curriculum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.curriculum.common.exception.BusinessException;
import com.example.curriculum.entity.*;
import com.example.curriculum.mapper.*;
import com.example.curriculum.utils.CourseVoAssembler;
import com.example.curriculum.utils.ScheduleUtil;
import com.example.curriculum.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生端服务：选课、退课、课表、成绩、个人信息
 * <p>
 * 学生选的是「教学班」（teaching_class），因此同一门课（同一 course_id）
 * 会有多条选课记录对应不同学生；而同一个学生不能重复选同一个教学班。
 */
@Service
public class StudentService {

    private final CourseMapper courseMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final ScMapper scMapper;
    private final SemesterMapper semesterMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final ClazzMapper clazzMapper;

    public StudentService(CourseMapper courseMapper, TeachingClassMapper teachingClassMapper, ScMapper scMapper,
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

    /** 每学期限选选修课门数 */
    private static final int MAX_ELECTIVE_PER_SEMESTER = 2;

    /**
     * 查询可选课程：仅当前学期、含剩余容量、标记是否已选
     */
    public List<CourseVO> listSelectableCourses(Long studentId) {
        Semester current = getCurrentSemester();
        if (current == null) {
            return Collections.emptyList();
        }
        // 选课中心只展示「选修类 + 开设中」的教学班；公共必修/专业类已按班级自动分配
        List<Long> electiveCourseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getCourseType, "选修类"))
                .stream().map(Course::getId).collect(Collectors.toList());
        if (electiveCourseIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<TeachingClass> classes = teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                .in(TeachingClass::getCourseId, electiveCourseIds)
                .eq(TeachingClass::getSemesterId, current.getId())
                .eq(TeachingClass::getStatus, 1));
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Course> courseMap = loadCourseMap(classes);
        // 该生已选教学班（含自动分配的公共必修/专业类，用于标记已选与冲突判断）
        List<Sc> scList = scMapper.selectList(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getStudentId, studentId));
        Set<Long> selectedIds = scList.stream().map(Sc::getTeachingClassId).collect(Collectors.toSet());
        List<TeachingClass> enrolledClasses = selectedIds.isEmpty() ? Collections.emptyList()
                : teachingClassMapper.selectBatchIds(selectedIds);

        Map<Long, String> teacherNames = loadTeacherNames();
        Map<Long, String> semesterNames = loadSemesterNames();
        return classes.stream().map(t -> {
            CourseVO vo = CourseVoAssembler.build(t, courseMap.get(t.getCourseId()),
                    teacherNames, semesterNames, null);
            if (vo == null) {
                return null;
            }
            vo.setSelected(selectedIds.contains(t.getId()));
            // 冲突判断：与已选教学班（排除自身）上课时间重叠
            boolean conflict = enrolledClasses.stream()
                    .anyMatch(e -> !e.getId().equals(t.getId())
                            && ScheduleUtil.isConflict(t.getSchedule(), e.getSchedule()));
            vo.setConflict(conflict);
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 选课批次信息：选课中心确认页展示的批次配置。
     * 学年学期、选课时间与管理员端「学期管理」保持一致：取当前学期的名称及起止日期。
     */
    public SelectionBatchVO getSelectionBatch() {
        Semester current = getCurrentSemester();
        SelectionBatchVO vo = new SelectionBatchVO();
        vo.setBatchName("公共选修课（非毕业班在校生）");
        if (current != null) {
            vo.setSemester(current.getSemesterName());
            // 选课时间与当前学期起止日期一致：开始日期 00:00 ~ 结束日期 23:59
            String start = current.getStartDate() != null ? current.getStartDate() + " 00:00" : null;
            String end = current.getEndDate() != null ? current.getEndDate() + " 23:59" : null;
            vo.setSelectStartTime(start);
            vo.setSelectEndTime(end);
            vo.setSelectTime((start != null ? start : "") + " ~ " + (end != null ? end : ""));
        }
        vo.setCourseLimit(MAX_ELECTIVE_PER_SEMESTER + "门");
        vo.setLottery("未启用");
        vo.setControl("可选可退");
        vo.setClassLimit("控制（不允许超限选人数）");
        vo.setPreCourseRefund("否");
        vo.setAllowConflict("否");
        vo.setCreditLimit("控制");
        return vo;
    }

    /**
     * 选课：校验学期开放、重复选课、时间冲突、容量，全部通过后原子更新人数并写入记录（事务保证一致）
     *
     * @param teachingClassId 教学班 id
     */
    @Transactional
    public void selectCourse(Long studentId, Long teachingClassId) {
        TeachingClass tc = teachingClassMapper.selectById(teachingClassId);
        if (tc == null) {
            throw new BusinessException("课程不存在");
        }
        Course course = courseMapper.selectById(tc.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        // 1. 校验学期开放：教学班必须属于当前学期
        Semester semester = semesterMapper.selectById(tc.getSemesterId());
        if (semester == null || !Integer.valueOf(1).equals(semester.getIsCurrent())) {
            throw new BusinessException("该课程不在当前学期，无法选课");
        }
        // 2. 校验重复选课
        Long count = scMapper.selectCount(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getStudentId, studentId)
                .eq(Sc::getTeachingClassId, teachingClassId));
        if (count != null && count > 0) {
            throw new BusinessException("请勿重复选课");
        }
        // 3. 加载该生已选教学班及其课程，用于「每学期限选两门选修课」与时间冲突判断
        List<Sc> mySc = scMapper.selectList(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getStudentId, studentId));
        if (!mySc.isEmpty()) {
            Set<Long> myTcIds = mySc.stream().map(Sc::getTeachingClassId).collect(Collectors.toSet());
            List<TeachingClass> myClasses = teachingClassMapper.selectBatchIds(myTcIds);
            Map<Long, Course> myCourseMap = loadCourseMap(myClasses);
            // 3.1 每学期限选两门选修课：统计当前学期已选选修课数量
            long electiveCount = myClasses.stream()
                    .filter(mt -> {
                        Course mc = myCourseMap.get(mt.getCourseId());
                        return mc != null && "选修类".equals(mc.getCourseType())
                                && Objects.equals(mt.getSemesterId(), tc.getSemesterId());
                    })
                    .count();
            if (electiveCount >= MAX_ELECTIVE_PER_SEMESTER) {
                throw new BusinessException("每学期限选 " + MAX_ELECTIVE_PER_SEMESTER + " 门选修课，已达上限");
            }
            // 3.2 校验时间冲突：同一学生已选教学班的 schedule 不能重叠
            for (TeachingClass mt : myClasses) {
                if (ScheduleUtil.isConflict(tc.getSchedule(), mt.getSchedule())) {
                    Course mc = myCourseMap.get(mt.getCourseId());
                    throw new BusinessException("与已选课程《" + (mc == null ? "" : mc.getCname()) + "》上课时间冲突");
                }
            }
        }
        // 4. 原子更新已选人数，返回 0 表示容量已满
        int updated = teachingClassMapper.increaseSelectedCount(teachingClassId);
        if (updated == 0) {
            throw new BusinessException("课程容量已满");
        }
        // 5. 写入选课记录
        Sc sc = new Sc();
        sc.setStudentId(studentId);
        sc.setTeachingClassId(teachingClassId);
        sc.setSelectTime(LocalDateTime.now());
        scMapper.insert(sc);
    }

    /**
     * 退课：删除选课记录并原子减少已选人数
     */
    @Transactional
    public void withdrawCourse(Long studentId, Long teachingClassId) {
        Sc sc = scMapper.selectOne(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getStudentId, studentId)
                .eq(Sc::getTeachingClassId, teachingClassId));
        if (sc == null) {
            throw new BusinessException("未选该课程，无法退课");
        }
        scMapper.deleteById(sc.getId());
        teachingClassMapper.decreaseSelectedCount(teachingClassId);
    }

    /**
     * 我的课表
     */
    public List<ScheduleVO> getSchedule(Long studentId) {
        List<Sc> scList = scMapper.selectList(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getStudentId, studentId));
        if (scList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> tcIds = scList.stream().map(Sc::getTeachingClassId).collect(Collectors.toSet());
        List<TeachingClass> classes = teachingClassMapper.selectBatchIds(tcIds);
        Map<Long, Course> courseMap = loadCourseMap(classes);
        Map<Long, String> teacherNames = loadTeacherNames();
        Map<Long, String> semesterNames = loadSemesterNames();

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
            vo.setTeacherName(teacherNames.get(t.getTeacherId()));
            vo.setLocation(t.getLocation());
            vo.setSchedule(t.getSchedule());
            vo.setCredit(c.getCredit());
            vo.setSemesterName(semesterNames.get(t.getSemesterId()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 我的成绩（含学分统计）
     */
    public ScoreVO getScores(Long studentId) {
        List<Sc> scList = scMapper.selectList(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getStudentId, studentId));
        ScoreVO scoreVO = new ScoreVO();
        scoreVO.setTotalCredit(0);
        scoreVO.setPassedCredit(0);
        if (scList.isEmpty()) {
            scoreVO.setItems(Collections.emptyList());
            return scoreVO;
        }
        Set<Long> tcIds = scList.stream().map(Sc::getTeachingClassId).collect(Collectors.toSet());
        List<TeachingClass> classes = teachingClassMapper.selectBatchIds(tcIds);
        Map<Long, TeachingClass> tcMap = classes.stream()
                .collect(Collectors.toMap(TeachingClass::getId, t -> t));
        Map<Long, Course> courseMap = loadCourseMap(classes);
        Map<Long, String> teacherNames = loadTeacherNames();
        Map<Long, String> semesterNames = loadSemesterNames();

        List<ScoreItemVO> items = new ArrayList<>();
        for (Sc sc : scList) {
            TeachingClass t = tcMap.get(sc.getTeachingClassId());
            if (t == null) {
                continue;
            }
            Course c = courseMap.get(t.getCourseId());
            if (c == null) {
                continue;
            }
            ScoreItemVO item = new ScoreItemVO();
            item.setCourseId(t.getId());
            item.setCno(c.getCno());
            item.setCname(c.getCname());
            item.setCredit(c.getCredit());
            item.setTeacherName(teacherNames.get(t.getTeacherId()));
            item.setSemesterName(semesterNames.get(t.getSemesterId()));
            item.setSchedule(t.getSchedule());
            item.setScore(sc.getScore());
            item.setSelectTime(sc.getSelectTime());
            items.add(item);
            // 学分统计：仅统计已出成绩的课程
            if (sc.getScore() != null) {
                int credit = c.getCredit() == null ? 0 : c.getCredit();
                scoreVO.setTotalCredit(scoreVO.getTotalCredit() + credit);
                if (sc.getScore() >= 60) {
                    scoreVO.setPassedCredit(scoreVO.getPassedCredit() + credit);
                }
            }
        }
        scoreVO.setItems(items);
        return scoreVO;
    }

    /**
     * 选课结果：该生全部已选教学班（含自动分配的公共必修/专业类与自主选修），按课程类型展示
     */
    public List<SelectedCourseVO> getSelectionResult(Long studentId) {
        List<Sc> scList = scMapper.selectList(new LambdaQueryWrapper<Sc>()
                .eq(Sc::getStudentId, studentId));
        if (scList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> tcIds = scList.stream().map(Sc::getTeachingClassId).collect(Collectors.toSet());
        List<TeachingClass> classes = teachingClassMapper.selectBatchIds(tcIds);
        Map<Long, TeachingClass> tcMap = classes.stream()
                .collect(Collectors.toMap(TeachingClass::getId, t -> t));
        Map<Long, Course> courseMap = loadCourseMap(classes);
        Map<Long, String> teacherNames = loadTeacherNames();
        Map<Long, String> semesterNames = loadSemesterNames();

        List<SelectedCourseVO> result = new ArrayList<>();
        for (Sc sc : scList) {
            TeachingClass t = tcMap.get(sc.getTeachingClassId());
            if (t == null) {
                continue;
            }
            Course c = courseMap.get(t.getCourseId());
            if (c == null) {
                continue;
            }
            SelectedCourseVO vo = new SelectedCourseVO();
            vo.setCourseId(t.getId());
            vo.setCno(c.getCno());
            vo.setCname(c.getCname());
            vo.setCredit(c.getCredit());
            vo.setTeacherName(teacherNames.get(t.getTeacherId()));
            vo.setLocation(t.getLocation());
            vo.setSchedule(t.getSchedule());
            vo.setSemesterName(semesterNames.get(t.getSemesterId()));
            vo.setCourseType(c.getCourseType());
            vo.setSelectTime(sc.getSelectTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 个人信息
     */
    public StudentVO getProfile(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) {
            throw new BusinessException("学生不存在");
        }
        StudentVO vo = new StudentVO();
        vo.setId(s.getId());
        vo.setSno(s.getSno());
        vo.setName(s.getName());
        vo.setGender(s.getGender());
        vo.setClassId(s.getClassId());
        vo.setCollege(s.getCollege());
        vo.setMajor(s.getMajor());
        vo.setPhone(s.getPhone());
        if (s.getClassId() != null) {
            Clazz c = clazzMapper.selectById(s.getClassId());
            if (c != null) {
                vo.setClassName(c.getClassName());
            }
        }
        return vo;
    }

    // ---------- 私有辅助方法 ----------

    private Semester getCurrentSemester() {
        return semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1));
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

    private Map<Long, String> loadTeacherNames() {
        return teacherMapper.selectList(null).stream()
                .collect(Collectors.toMap(Teacher::getId, Teacher::getName));
    }

    private Map<Long, String> loadSemesterNames() {
        return semesterMapper.selectList(null).stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getSemesterName));
    }
}
