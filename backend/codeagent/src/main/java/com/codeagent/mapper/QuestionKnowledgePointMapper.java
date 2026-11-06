package com.codeagent.mapper;

import com.codeagent.vo.KnowledgePointBriefVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目与知识点的多对多关联。
 */
public interface QuestionKnowledgePointMapper {

    /**
     * 查询题目关联的所有知识点 ID。
     *
     * @param questionId 题目 ID
     * @return 知识点 ID 列表
     */
    @Select("SELECT knowledge_point_id FROM question_knowledge_point WHERE question_id = #{questionId}")
    List<Long> selectIdsByQuestionId(@Param("questionId") Long questionId);

    /**
     * 查询题目关联的知识点摘要，按知识点排序。
     *
     * @param questionId 题目 ID
     * @return 知识点摘要列表
     */
    @Select("""
            SELECT kp.id, kp.code, kp.name
            FROM question_knowledge_point qkp
            JOIN knowledge_point kp ON kp.id = qkp.knowledge_point_id
            WHERE qkp.question_id = #{questionId}
            ORDER BY kp.sort, kp.id
            """)
    List<KnowledgePointBriefVO> selectPointsByQuestionId(@Param("questionId") Long questionId);

    /**
     * 删除题目的全部知识点关联。
     *
     * @param questionId 题目 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM question_knowledge_point WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * 批量写入题目知识点关联。
     *
     * @param questionId 题目 ID
     * @param pointIds   知识点 ID 列表
     * @return 写入行数
     */
    @Insert("""
            <script>
            INSERT INTO question_knowledge_point (question_id, knowledge_point_id)
            VALUES
            <foreach collection="pointIds" item="pointId" separator=",">
                (#{questionId}, #{pointId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("questionId") Long questionId, @Param("pointIds") List<Long> pointIds);
}
