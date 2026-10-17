package com.example.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.curriculum.entity.TeachingClass;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 教学班 Mapper
 */
public interface TeachingClassMapper extends BaseMapper<TeachingClass> {

    /**
     * 原子增加已选人数（并发安全）：
     * 只有当 selected_count < capacity 时才 +1，返回受影响行数。
     * 返回 0 说明教学班已满，从而避免超卖。
     */
    @Update("UPDATE teaching_class SET selected_count = selected_count + 1 " +
            "WHERE id = #{teachingClassId} AND selected_count < capacity")
    int increaseSelectedCount(@Param("teachingClassId") Long teachingClassId);

    /**
     * 原子减少已选人数，仅当已选人数 > 0 时 -1
     */
    @Update("UPDATE teaching_class SET selected_count = selected_count - 1 " +
            "WHERE id = #{teachingClassId} AND selected_count > 0")
    int decreaseSelectedCount(@Param("teachingClassId") Long teachingClassId);
}
