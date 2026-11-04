package com.codeagent.mapper;

import com.codeagent.entity.QuestionSolution;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionSolutionMapper {

    /**
     * 删除指定题目的全部参考代码，用于编辑时整体替换。
     *
     * @param questionId 题目 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM question_solution WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * 批量插入参考代码，批量 SQL 位于 resources/mapper/QuestionSolutionMapper.xml。
     *
     * @param solutions 参考代码实体列表，questionId 必须已回填
     * @return 插入行数
     */
    int insertBatch(@Param("list") List<QuestionSolution> solutions);

    /**
     * 查询某题指定语言的最新参考代码。
     *
     * @param questionId 题目 ID
     * @param language   语言 CPP / JAVA
     * @return 参考代码；不存在时返回 null
     */
    @Select("SELECT id, question_id, language, content, explanation, create_time " +
            "FROM question_solution WHERE question_id = #{questionId} AND language = #{language} " +
            "ORDER BY id DESC LIMIT 1")
    QuestionSolution selectLatest(@Param("questionId") Long questionId,
                                  @Param("language") String language);
}
