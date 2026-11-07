package com.codeagent.mapper;

import com.codeagent.dto.ContestParticipantRow;
import com.codeagent.dto.ContestProblemRow;
import com.codeagent.dto.ContestRow;
import com.codeagent.dto.ContestSubmitRow;
import com.codeagent.entity.Contest;
import com.codeagent.entity.ContestProblem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 竞赛查询与报名。
 */
public interface ContestMapper {

    /**
     * 查询竞赛详情，同时计算状态、题数、报名人数和当前用户是否报名。
     *
     * @param id     竞赛 ID
     * @param userId 当前登录用户 ID，可为 null（访客）
     * @return 竞赛行；不存在返回 null
     */
    ContestRow selectById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 分页查询竞赛列表，可按状态过滤。
     *
     * @param status  UPCOMING/RUNNING/ENDED，null 表示全部
     * @param keyword 标题/说明关键词，可为空
     * @param userId  当前登录用户 ID，可为 null
     * @param offset  分页偏移
     * @param size    每页条数
     * @return 竞赛列表
     */
    List<ContestRow> selectList(@Param("status") String status,
                                @Param("keyword") String keyword,
                                @Param("userId") Long userId,
                                @Param("offset") long offset,
                                @Param("size") long size);

    /**
     * 统计竞赛列表总数，条件与 selectList 一致。
     *
     * @param status  UPCOMING/RUNNING/ENDED，null 表示全部
     * @param keyword 标题/说明关键词，可为空
     * @return 总条数
     */
    long countList(@Param("status") String status, @Param("keyword") String keyword);

    /**
     * 查询竞赛题单，按题号排序。
     *
     * @param contestId 竞赛 ID
     * @return 题单项
     */
    List<ContestProblemRow> selectProblems(@Param("contestId") Long contestId);

    /**
     * 查询已报名用户，用于排名初始化。
     *
     * @param contestId 竞赛 ID
     * @return 报名用户
     */
    List<ContestParticipantRow> selectParticipants(@Param("contestId") Long contestId);

    /**
     * 查询竞赛内所有提交，供排名按时间计算罚时。
     *
     * @param contestId 竞赛 ID
     * @return 提交行，按时间升序
     */
    List<ContestSubmitRow> selectSubmissions(@Param("contestId") Long contestId);

    /**
     * 统计报名人数。
     *
     * @param contestId 竞赛 ID
     * @return 报名人数
     */
    long countParticipants(@Param("contestId") Long contestId);

    /**
     * 查询用户是否已报名。
     *
     * @param contestId 竞赛 ID
     * @param userId    用户 ID
     * @return 1 表示已报名
     */
    int selectRegistration(@Param("contestId") Long contestId, @Param("userId") Long userId);

    /**
     * 报名；唯一键冲突时忽略，重复报名仍返回成功。
     *
     * @param contestId 竞赛 ID
     * @param userId    用户 ID
     * @return 新增行数
     */
    int insertRegistration(@Param("contestId") Long contestId, @Param("userId") Long userId);

    /**
     * 新建竞赛。
     *
     * @param contest 已填充标题、说明和起止时间的竞赛实体
     * @return 新增行数
     */
    int insertContest(Contest contest);

    /**
     * 向竞赛题单添加一道题。
     *
     * @param problem 已填充竞赛、题目、标签和分值的题单项
     * @return 新增行数
     */
    int insertProblem(ContestProblem problem);

    /**
     * 查询用户是否报名了包含指定题目的竞赛。
     * 用于允许参赛者读取竞赛专属的私有题。
     *
     * @param questionId 题目 ID
     * @param userId     用户 ID
     * @return 报名记录数
     */
    int countRegisteredQuestion(@Param("questionId") Long questionId, @Param("userId") Long userId);
}
