package com.example.curriculum.utils;

import com.example.curriculum.entity.Course;
import com.example.curriculum.entity.TeachingClass;
import com.example.curriculum.vo.CourseVO;

import java.util.Map;

/**
 * 「教学班 + 课程」-> 前端课程视图对象
 * <p>
 * 统一在此组装，保证各端返回的字段一致；对外字段与拆分前完全兼容，
 * 因此前端页面无需改动即可继续工作。
 */
public final class CourseVoAssembler {

    private CourseVoAssembler() {
    }

    /**
     * @param t            教学班
     * @param c            课程
     * @param teacherNames teacherId -> 教师姓名，可为 null
     * @param semesterNames semesterId -> 学期名称，可为 null
     * @param classNames   classId -> 班级名称，可为 null
     */
    public static CourseVO build(TeachingClass t, Course c,
                                 Map<Long, String> teacherNames,
                                 Map<Long, String> semesterNames,
                                 Map<Long, String> classNames) {
        if (t == null || c == null) {
            return null;
        }
        CourseVO vo = new CourseVO();
        // id 仍是教学班 id
        vo.setId(t.getId());
        vo.setCourseId(c.getId());
        // 以下来自课程表（一门课一个编号）
        vo.setCno(c.getCno());
        vo.setCname(c.getCname());
        vo.setCredit(c.getCredit());
        vo.setHours(c.getHours());
        vo.setCourseType(c.getCourseType());
        vo.setCategory(c.getCategory());
        // 以下来自教学班
        vo.setTcNo(t.getTcNo());
        vo.setTeacherId(t.getTeacherId());
        vo.setTeacherName(teacherNames == null ? null : teacherNames.get(t.getTeacherId()));
        vo.setSemesterId(t.getSemesterId());
        vo.setSemesterName(semesterNames == null ? null : semesterNames.get(t.getSemesterId()));
        vo.setClassId(t.getClassId());
        vo.setClassName(classNames == null ? null : classNames.get(t.getClassId()));
        vo.setStatus(t.getStatus());
        vo.setMinStudents(t.getMinStudents());
        vo.setCapacity(t.getCapacity());
        vo.setSelectedCount(t.getSelectedCount());
        int capacity = t.getCapacity() == null ? 0 : t.getCapacity();
        int selected = t.getSelectedCount() == null ? 0 : t.getSelectedCount();
        vo.setRemaining(capacity - selected);
        vo.setLocation(t.getLocation());
        vo.setSchedule(t.getSchedule());
        return vo;
    }
}
