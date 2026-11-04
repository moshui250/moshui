package com.codeagent.mapper;

import com.codeagent.entity.QuestionTestCase;
import com.codeagent.vo.TestCaseSampleVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionTestCaseMapper {

    /**
     * 详情接口唯一允许使用的用例查询：只取 is_sample=1。
     *
     * @param questionId 题目 ID
     * @return 公开样例列表，按 sort_no、id 排序
     */
    @Select("SELECT input_data, expected_output, sort_no " +
            "FROM question_test_case WHERE question_id = #{questionId} AND is_sample = 1 " +
            "ORDER BY sort_no ASC, id ASC")
    List<TestCaseSampleVO> selectSamples(@Param("questionId") Long questionId);

    /**
     * 判题使用：取出题目的全部用例，包含隐藏用例。
     * 该结果只能留在服务端，不得直接返回给前端。
     *
     * @param questionId 题目 ID
     * @return 全部用例，按 sort_no、id 排序
     */
    @Select("SELECT id, question_id, input_data, expected_output, is_sample, score, sort_no " +
            "FROM question_test_case WHERE question_id = #{questionId} " +
            "ORDER BY sort_no ASC, id ASC")
    List<QuestionTestCase> selectAllByQuestionId(@Param("questionId") Long questionId);

    /**
     * 删除指定题目的全部用例，用于编辑时整体替换。
     *
     * @param questionId 题目 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM question_test_case WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * 批量插入用例，批量 SQL 位于 resources/mapper/QuestionTestCaseMapper.xml。
     *
     * @param cases 用例实体列表，questionId 必须已回填
     * @return 插入行数
     */
    int insertBatch(@Param("list") List<QuestionTestCase> cases);
}
