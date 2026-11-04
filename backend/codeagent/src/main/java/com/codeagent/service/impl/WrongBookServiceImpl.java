package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.PageResult;
import com.codeagent.common.UserContext;
import com.codeagent.dto.WrongBookItemRow;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.UserWrongQuestionMapper;
import com.codeagent.service.ProfileStatService;
import com.codeagent.service.WrongBookService;
import com.codeagent.vo.KnowledgePointBriefVO;
import com.codeagent.vo.WrongBookItemVO;
import com.codeagent.vo.WrongBookLastSubmitVO;
import com.codeagent.vo.WrongBookQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 错题本服务实现。
 */
@Service
@RequiredArgsConstructor
public class WrongBookServiceImpl implements WrongBookService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final ProfileStatService profileStatService;

    /**
     * 使用 MySQL upsert 保证同一用户同一题只有一条错题记录。
     *
     * @param userId     用户 ID
     * @param questionId 题目 ID
     * @param submitId   最近一次未通过的提交 ID
     */
    @Override
    @Transactional
    public void recordWrong(Long userId, Long questionId, Long submitId) {
        userWrongQuestionMapper.upsertWrong(userId, questionId, submitId);
        profileStatService.invalidate(userId);
    }

    /**
     * 分页查询当前用户的错题本，并把扁平行组装成嵌套响应。
     *
     * @param page             页码，小于 1 时按 1 处理
     * @param size             每页条数，小于 1 用默认 10，超过 50 按 50
     * @param mastered         是否已掌握；null 或 false 查未掌握，true 查已掌握
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @return 分页错题列表；无数据时返回空页
     */
    @Override
    public PageResult<WrongBookItemVO> list(long page, long size, Boolean mastered, Long knowledgePointId) {
        Long userId = requireUserId();
        long safePage = Math.max(page, 1L);
        long safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int masteredValue = Boolean.TRUE.equals(mastered) ? 1 : 0;

        long total = userWrongQuestionMapper.countList(userId, masteredValue, knowledgePointId);
        if (total == 0) {
            return PageResult.empty(safePage, safeSize);
        }
        long offset = (safePage - 1L) * safeSize;
        if (offset < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "页码过大");
        }

        List<WrongBookItemRow> rows = userWrongQuestionMapper.selectList(
                userId, masteredValue, knowledgePointId, offset, safeSize);
        List<WrongBookItemVO> records = new ArrayList<>();
        if (rows != null) {
            for (WrongBookItemRow row : rows) {
                records.add(toVO(row));
            }
        }
        return PageResult.of(records, total, safePage, safeSize);
    }

    /**
     * 更新当前用户某道错题的掌握状态。
     *
     * @param questionId 题目 ID
     * @param mastered   目标掌握状态
     */
    @Override
    @Transactional
    public void updateMastered(Long questionId, boolean mastered) {
        if (questionId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题目 ID 不能为空");
        }
        Long userId = requireUserId();
        int updated = userWrongQuestionMapper.updateMastered(
                userId, questionId, mastered ? 1 : 0);
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "错题记录不存在");
        }
        profileStatService.invalidate(userId);
    }

    /**
     * 从 ThreadLocal 取当前登录用户 ID，未登录时抛 40100。
     *
     * @return 当前用户 ID
     */
    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 将扁平查询行转换为嵌套错题项。
     *
     * @param row 查询结果行
     * @return 错题项响应
     */
    private WrongBookItemVO toVO(WrongBookItemRow row) {
        WrongBookItemVO vo = new WrongBookItemVO();
        vo.setId(row.getId());
        vo.setWrongCount(row.getWrongCount());
        vo.setMastered(Integer.valueOf(1).equals(row.getMastered()));
        vo.setPassed(Integer.valueOf(1).equals(row.getPassed()));
        vo.setLastWrongTime(row.getLastWrongTime());

        WrongBookQuestionVO question = new WrongBookQuestionVO();
        question.setId(row.getQuestionId());
        question.setTitle(row.getTitle());
        question.setDifficulty(row.getDifficulty());
        if (row.getKnowledgePointId() != null) {
            KnowledgePointBriefVO knowledgePoint = new KnowledgePointBriefVO();
            knowledgePoint.setId(row.getKnowledgePointId());
            knowledgePoint.setCode(row.getKnowledgePointCode());
            knowledgePoint.setName(row.getKnowledgePointName());
            question.setKnowledgePoint(knowledgePoint);
        }
        vo.setQuestion(question);

        if (row.getLastSubmitId() != null) {
            WrongBookLastSubmitVO lastSubmit = new WrongBookLastSubmitVO();
            lastSubmit.setId(row.getLastSubmitId());
            lastSubmit.setStatus(row.getLastSubmitStatus());
            lastSubmit.setLanguage(row.getLastSubmitLanguage());
            lastSubmit.setCodeSnippet(row.getLastSubmitCode());
            lastSubmit.setCode(row.getLastSubmitCodeFull());
            vo.setLastSubmit(lastSubmit);
        }
        return vo;
    }
}
