package com.codeagent.mapper;

import com.codeagent.dto.QuestionUserState;
import com.codeagent.entity.SubmitRecord;
import com.codeagent.vo.JudgeHistoryVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface SubmitRecordMapper {

    /**
     * 详情页状态：最近一次提交 id + 当前用户是否 AC 过。一次 SQL 完成。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID
     * @return 最近提交 ID 和是否 AC；无提交记录时返回对象中的字段为 null/false
     */
    @Select("SELECT " +
            "(SELECT id FROM submit_record WHERE user_id = #{userId} AND question_id = #{questionId} " +
            " ORDER BY id DESC LIMIT 1) AS last_submit_id, " +
            "EXISTS(SELECT 1 FROM submit_record WHERE user_id = #{userId} AND question_id = #{questionId} " +
            " AND status = 'AC') AS solved")
    QuestionUserState selectUserState(@Param("userId") Long userId,
                                      @Param("questionId") Long questionId);

    /**
     * 新建提交记录，初始状态为 PENDING，并回填自增主键。
     *
     * @param record 提交记录，totalCount 为用例总数
     * @return 插入行数
     */
    @Insert("INSERT INTO submit_record " +
            "(user_id, question_id, language, code, status, passed_count, total_count, " +
            " run_time_ms, create_time) " +
            "VALUES (#{userId}, #{questionId}, #{language}, #{code}, #{status}, #{passedCount}, " +
            " #{totalCount}, #{runTimeMs}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SubmitRecord record);

    /**
     * 把排队中的提交标记为 JUDGING，防止重复处理。
     *
     * @param id 提交记录 ID
     * @return 受影响行数；0 表示已被其他线程处理
     */
    @Update("UPDATE submit_record SET status = 'JUDGING' " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int markJudging(@Param("id") Long id);

    /**
     * 在不覆盖判题结果的情况下记录失败状态，用于线程池拒绝等前置失败。
     *
     * @param id           提交记录 ID
     * @param status       目标状态，例如 SE
     * @param errorMessage 面向用户的错误摘要
     * @return 受影响行数
     */
    @Update("UPDATE submit_record SET status = #{status}, error_message = #{errorMessage} " +
            "WHERE id = #{id}")
    int markFailed(@Param("id") Long id,
                   @Param("status") String status,
                   @Param("errorMessage") String errorMessage);

    /**
     * 写回判题结果，包括通过数、耗时、编译输出和逐用例 JSON。
     *
     * @param record 包含 id 和完整判题结果的提交记录
     * @return 受影响行数
     */
    @Update("UPDATE submit_record SET status = #{status}, passed_count = #{passedCount}, " +
            "total_count = #{totalCount}, run_time_ms = #{runTimeMs}, " +
            "compile_output = #{compileOutput}, error_message = #{errorMessage}, " +
            "case_result_json = #{caseResultJson} WHERE id = #{id}")
    int updateResult(SubmitRecord record);

    /**
     * 按 ID 查询提交记录，包含代码和逐用例 JSON，仅供服务端判题/鉴权使用。
     *
     * @param id 提交记录 ID
     * @return 提交记录；不存在时返回 null
     */
    @Select("SELECT id, user_id, question_id, language, code, status, passed_count, total_count, " +
            "run_time_ms, compile_output, error_message, case_result_json, create_time " +
            "FROM submit_record WHERE id = #{id} LIMIT 1")
    SubmitRecord selectById(@Param("id") Long id);

    /**
     * 分页查询当前用户的提交历史；questionId 为空时查询全部题目。
     * 动态条件 SQL 位于 resources/mapper/SubmitRecordMapper.xml。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID，null 表示不限
     * @param offset     分页偏移量
     * @param size       每页条数
     * @return 提交历史列表
     */
    List<JudgeHistoryVO> selectHistory(@Param("userId") Long userId,
                                       @Param("questionId") Long questionId,
                                       @Param("offset") long offset,
                                       @Param("size") long size);

    /**
     * 统计当前用户提交历史总数，筛选条件与 selectHistory 一致。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID，null 表示不限
     * @return 总记录数
     */
    long countHistory(@Param("userId") Long userId,
                      @Param("questionId") Long questionId);
}
