package com.codeagent.mapper;

import com.codeagent.dto.ProfileStatRow;
import com.codeagent.vo.RecentSubmitVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学情看板统计查询。统计主查询只访问一次数据库，按知识点 GROUP BY。
 */
public interface ProfileStatMapper {

    /**
     * 按知识点聚合当前用户的提交、AC 与已通过题目数，并附带未掌握错题数。
     * 知识点使用 LEFT JOIN，保证没有提交的知识点也返回一行，供前端显示“—”。
     *
     * @param userId 当前用户 ID
     * @return 每个知识点一行；没有知识点时为空列表
     */
    @Select("""
            SELECT kp.id AS knowledge_point_id,
                   kp.name AS knowledge_point_name,
                   COUNT(sr.id) AS submit_count,
                   CAST(COALESCE(SUM(CASE WHEN sr.status = 'AC' THEN 1 ELSE 0 END), 0) AS SIGNED) AS ac_count,
                   COUNT(DISTINCT CASE WHEN sr.status = 'AC' THEN sr.question_id END) AS solved_count,
                   (SELECT COUNT(*) FROM user_wrong_question w
                     WHERE w.user_id = #{userId} AND w.mastered = 0) AS wrong_count
            FROM knowledge_point kp
            LEFT JOIN question q ON q.knowledge_point_id = kp.id
            LEFT JOIN submit_record sr ON sr.question_id = q.id AND sr.user_id = #{userId}
            GROUP BY kp.id, kp.name, kp.sort
            ORDER BY kp.sort, kp.id
            """)
    List<ProfileStatRow> selectStats(@Param("userId") Long userId);

    /**
     * 统计未掌握错题数，仅在知识点表为空时作为兜底。
     *
     * @param userId 当前用户 ID
     * @return 未掌握错题数
     */
    @Select("""
            SELECT COUNT(*) FROM user_wrong_question
            WHERE user_id = #{userId} AND mastered = 0
            """)
    long countWrong(@Param("userId") Long userId);

    /**
     * 查询最近若干次提交，关联题目标题，供看板最近提交流水展示。
     *
     * @param userId 当前用户 ID
     * @param limit  返回条数
     * @return 按提交时间倒序的提交列表
     */
    @Select("""
            SELECT sr.id,
                   sr.question_id,
                   q.title AS question_title,
                   sr.language,
                   sr.status,
                   sr.run_time_ms,
                   sr.create_time
            FROM submit_record sr
            LEFT JOIN question q ON q.id = sr.question_id
            WHERE sr.user_id = #{userId}
            ORDER BY sr.create_time DESC, sr.id DESC
            LIMIT #{limit}
            """)
    List<RecentSubmitVO> selectRecentSubmits(@Param("userId") Long userId,
                                             @Param("limit") int limit);
}
