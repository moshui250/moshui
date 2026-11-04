package com.codeagent.mapper;

import com.codeagent.dto.WrongBookItemRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserWrongQuestionMapper {

    /**
     * 记录一次错题：首次插入，之后累加次数并刷新最近提交时间。
     * 依赖 uk_user_q 唯一键保证同一用户同一题只有一行。
     * MySQL 专用 upsert SQL 位于 resources/mapper/UserWrongQuestionMapper.xml。
     *
     * @param userId     用户 ID
     * @param questionId 题目 ID
     * @param submitId   最近一次未通过的提交 ID
     * @return 受影响行数
     */
    int upsertWrong(@Param("userId") Long userId,
                    @Param("questionId") Long questionId,
                    @Param("submitId") Long submitId);

    /**
     * 分页查询错题列表，联表取出题目、知识点和最近一次错误提交。
     *
     * @param userId           当前用户 ID
     * @param mastered         掌握状态：1 已掌握，0 未掌握
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @param offset           分页偏移量
     * @param size             每页条数
     * @return 错题列表查询行
     */
    List<WrongBookItemRow> selectList(@Param("userId") Long userId,
                                      @Param("mastered") int mastered,
                                      @Param("knowledgePointId") Long knowledgePointId,
                                      @Param("offset") long offset,
                                      @Param("size") long size);

    /**
     * 统计当前用户错题总数，筛选条件与 selectList 保持一致。
     *
     * @param userId           当前用户 ID
     * @param mastered         掌握状态：1 已掌握，0 未掌握
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @return 符合条件的错题总数
     */
    long countList(@Param("userId") Long userId,
                   @Param("mastered") int mastered,
                   @Param("knowledgePointId") Long knowledgePointId);

    /**
     * 更新错题的掌握状态。
     *
     * @param userId     当前用户 ID
     * @param questionId 题目 ID
     * @param mastered   掌握状态：1 已掌握，0 未掌握
     * @return 受影响行数，0 表示错题记录不存在
     */
    int updateMastered(@Param("userId") Long userId,
                       @Param("questionId") Long questionId,
                       @Param("mastered") int mastered);
}
