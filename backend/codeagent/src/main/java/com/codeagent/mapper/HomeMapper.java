package com.codeagent.mapper;

import com.codeagent.dto.HomeDailyRow;
import com.codeagent.dto.HomeTotalsRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 首页公开统计查询。只读 submit_record / question / knowledge_point，不修改数据。
 */
public interface HomeMapper {

    /**
     * 查询全站提交、通过、公共题目和知识点总数。
     *
     * @return 首页总览聚合结果
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM submit_record) AS total_submits,
              (SELECT COUNT(*) FROM submit_record WHERE status = 'AC') AS ac_submits,
              (SELECT COUNT(*) FROM question WHERE deleted = 0 AND type = 'PUBLIC') AS public_questions,
              (SELECT COUNT(*) FROM knowledge_point) AS knowledge_points
            """)
    HomeTotalsRow selectTotals();

    /**
     * 查询指定日期区间内每天提交和通过数量。
     *
     * @param start 区间开始日期（含）
     * @param end   区间结束日期（不含）
     * @return 按日期升序的每日统计
     */
    @Select("""
            SELECT DATE(create_time) AS day,
                   COUNT(*) AS submit_count,
                   CAST(SUM(CASE WHEN status = 'AC' THEN 1 ELSE 0 END) AS SIGNED) AS ac_count
            FROM submit_record
            WHERE create_time >= #{start} AND create_time < #{end}
            GROUP BY DATE(create_time)
            ORDER BY day
            """)
    List<HomeDailyRow> selectWeek(@Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    /**
     * 查询指定用户在某周内每天提交和通过数量。
     *
     * @param userId 当前登录用户 ID
     * @param start  区间开始日期（含）
     * @param end    区间结束日期（不含）
     * @return 按日期升序的每日统计
     */
    @Select("""
            SELECT DATE(create_time) AS day,
                   COUNT(*) AS submit_count,
                   CAST(SUM(CASE WHEN status = 'AC' THEN 1 ELSE 0 END) AS SIGNED) AS ac_count
            FROM submit_record
            WHERE user_id = #{userId}
              AND create_time >= #{start} AND create_time < #{end}
            GROUP BY DATE(create_time)
            ORDER BY day
            """)
    List<HomeDailyRow> selectWeekByUser(@Param("userId") Long userId,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);
}
