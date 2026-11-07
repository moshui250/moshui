package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.PageResult;
import com.codeagent.dto.AdminContestRequest;
import com.codeagent.dto.ContestParticipantRow;
import com.codeagent.dto.ContestProblemRow;
import com.codeagent.dto.ContestRow;
import com.codeagent.dto.ContestSubmitRow;
import com.codeagent.dto.QuestionSaveRequest;
import com.codeagent.entity.Contest;
import com.codeagent.entity.ContestProblem;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.ContestMapper;
import com.codeagent.mapper.QuestionMapper;
import com.codeagent.service.ContestService;
import com.codeagent.service.QuestionService;
import com.codeagent.vo.ContestDetailVO;
import com.codeagent.vo.ContestListItemVO;
import com.codeagent.vo.ContestProblemCellVO;
import com.codeagent.vo.ContestProblemVO;
import com.codeagent.vo.ContestRankVO;
import com.codeagent.vo.ContestRegisterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 竞赛服务实现：列表/详情、报名、基于提交记录的实时排名。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestServiceImpl implements ContestService {

    private static final Set<String> STATUSES = Set.of("UPCOMING", "RUNNING", "ENDED");
    private static final Set<String> WRONG_STATUSES = Set.of("WA", "TLE", "MLE", "RE", "CE");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final long WRONG_PENALTY_SECONDS = 20L * 60L;

    private final ContestMapper contestMapper;
    private final QuestionService questionService;
    private final QuestionMapper questionMapper;

    @Override
    public PageResult<ContestListItemVO> list(Long userId, String status, String keyword,
                                               long page, long size) {
        long safePage = Math.max(page, 1L);
        long safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        long total = contestMapper.countList(normalizedStatus, normalizedKeyword);
        if (total == 0) {
            return PageResult.empty(safePage, safeSize);
        }
        long offset = (safePage - 1L) * safeSize;
        List<ContestRow> rows = contestMapper.selectList(
                normalizedStatus, normalizedKeyword, userId, offset, safeSize);
        List<ContestListItemVO> records = rows == null ? List.of() : rows.stream()
                .map(this::toListItem)
                .toList();
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    public ContestDetailVO detail(Long contestId, Long userId) {
        if (contestId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "竞赛 ID 不能为空");
        }
        ContestRow row = contestMapper.selectById(contestId, userId);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "竞赛不存在");
        }
        ContestDetailVO vo = new ContestDetailVO();
        vo.setId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setDescription(row.getDescription());
        vo.setStartTime(row.getStartTime());
        vo.setEndTime(row.getEndTime());
        vo.setStatus(row.getStatus());
        vo.setServerTime(LocalDateTime.now());
        vo.setProblemCount(row.getProblemCount());
        vo.setParticipantCount(row.getParticipantCount());
        vo.setRegistered(Boolean.TRUE.equals(row.getRegistered()));
        List<ContestProblemRow> problems = contestMapper.selectProblems(contestId);
        vo.setProblems(problems == null ? List.of() : problems.stream()
                .map(this::toProblem)
                .toList());
        return vo;
    }

    @Override
    @Transactional
    public ContestRegisterVO register(Long contestId, Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        ContestRow row = contestMapper.selectById(contestId, userId);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "竞赛不存在");
        }
        if ("ENDED".equals(row.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "竞赛已结束，不能报名");
        }
        contestMapper.insertRegistration(contestId, userId);
        long count = contestMapper.countParticipants(contestId);
        log.info("[monitor] type=contest-register contestId={} userId={} participants={}",
                contestId, userId, count);
        return new ContestRegisterVO(true, Math.toIntExact(count));
    }

    @Override
    public PageResult<ContestRankVO> rank(Long contestId, Long userId, long page, long size) {
        if (contestId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "竞赛 ID 不能为空");
        }
        ContestRow contestRow = contestMapper.selectById(contestId, userId);
        if (contestRow == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "竞赛不存在");
        }
        long safePage = Math.max(page, 1L);
        long safeSize = size < 1 ? 20L : Math.min(size, MAX_PAGE_SIZE);

        Map<Long, Integer> problemScores = new HashMap<>();
        List<ContestProblemRow> problems = contestMapper.selectProblems(contestId);
        if (problems == null) {
            problems = List.of();
        }
        for (ContestProblemRow problem : problems) {
            problemScores.put(problem.getQuestionId(), problem.getScore() == null ? 100 : problem.getScore());
        }

        Map<Long, RankAccumulator> participants = new HashMap<>();
        List<ContestParticipantRow> registered = contestMapper.selectParticipants(contestId);
        if (registered != null) {
            for (ContestParticipantRow participant : registered) {
                RankAccumulator item = new RankAccumulator();
                item.userId = participant.getUserId();
                item.username = participant.getUsername();
                item.nickname = participant.getNickname();
                participants.put(item.userId, item);
            }
        }

        List<ContestSubmitRow> submissions = contestMapper.selectSubmissions(contestId);
        if (submissions != null) {
            for (ContestSubmitRow submission : submissions) {
                RankAccumulator item = participants.get(submission.getUserId());
                if (item == null) {
                    continue;
                }
                Long questionId = submission.getQuestionId();
                ContestCell cell = item.cells.computeIfAbsent(questionId, key -> new ContestCell());
                if ("AC".equals(submission.getStatus())) {
                    if (!cell.accepted) {
                        cell.accepted = true;
                        cell.acSeconds = Math.max(0L,
                                Duration.between(contestRow.getStartTime(), submission.getCreateTime()).getSeconds());
                    }
                    if (!item.solved.contains(questionId)) {
                        item.solved.add(questionId);
                        item.solvedCount++;
                        item.score += problemScores.getOrDefault(questionId, 0);
                        long baseSeconds = Math.max(0L,
                                Duration.between(contestRow.getStartTime(), submission.getCreateTime()).getSeconds());
                        int wrong = item.wrong.getOrDefault(questionId, 0);
                        item.penaltySeconds += baseSeconds + wrong * WRONG_PENALTY_SECONDS;
                        if (item.lastAcTime == null || submission.getCreateTime().isAfter(item.lastAcTime)) {
                            item.lastAcTime = submission.getCreateTime();
                        }
                    }
                } else if (WRONG_STATUSES.contains(submission.getStatus()) && !cell.accepted) {
                    cell.attempts++;
                    if (!item.solved.contains(questionId)) {
                        item.wrong.merge(questionId, 1, Integer::sum);
                    }
                }
            }
        }

        List<RankAccumulator> ordered = new ArrayList<>(participants.values());
        ordered.sort(Comparator
                .comparingInt((RankAccumulator item) -> item.score).reversed()
                .thenComparing(Comparator.comparingInt((RankAccumulator item) -> item.solvedCount).reversed())
                .thenComparingLong(item -> item.penaltySeconds)
                .thenComparingLong(item -> item.userId));

        long total = ordered.size();
        long offset = (safePage - 1L) * safeSize;
        if (offset >= total) {
            return PageResult.empty(safePage, safeSize);
        }
        int from = (int) offset;
        int to = (int) Math.min(offset + safeSize, total);
        List<ContestRankVO> records = new ArrayList<>();
        for (int i = from; i < to; i++) {
            RankAccumulator item = ordered.get(i);
            ContestRankVO vo = new ContestRankVO();
            vo.setRank((long) (i + 1));
            vo.setUserId(item.userId);
            vo.setUsername(item.username);
            vo.setNickname(item.nickname == null || item.nickname.isBlank() ? item.username : item.nickname);
            vo.setSolvedCount(item.solvedCount);
            vo.setScore(item.score);
            vo.setPenaltyMinutes(item.penaltySeconds / 60L);
            vo.setLastAcTime(item.lastAcTime);
            vo.setMe(userId != null && userId.equals(item.userId));
            List<ContestProblemCellVO> cells = new ArrayList<>();
            for (ContestProblemRow problem : problems) {
                ContestCell cell = item.cells.get(problem.getQuestionId());
                ContestProblemCellVO cellVO = new ContestProblemCellVO();
                cellVO.setQuestionId(problem.getQuestionId());
                cellVO.setAttempts(cell == null ? 0 : cell.attempts);
                if (cell != null && cell.accepted) {
                    cellVO.setStatus("AC");
                    cellVO.setAcTimeSeconds(cell.acSeconds);
                } else if (cell != null && cell.attempts > 0) {
                    cellVO.setStatus("WA");
                } else {
                    cellVO.setStatus("UNSOLVED");
                }
                cells.add(cellVO);
            }
            vo.setCells(cells);
            records.add(vo);
        }
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    @Transactional
    public Long createContest(AdminContestRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }

        Contest contest = new Contest();
        contest.setTitle(request.getTitle().trim());
        contest.setDescription(request.getDescription());
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        contestMapper.insertContest(contest);

        int order = 0;
        Set<Long> seen = new HashSet<>();
        if (request.getQuestionIds() != null) {
            for (Long questionId : request.getQuestionIds()) {
                if (questionId == null || !seen.add(questionId)) {
                    continue;
                }
                if (questionMapper.selectDetailRow(questionId) == null) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "题目不存在：" + questionId);
                }
                order = addContestProblem(contest.getId(), questionId, order);
            }
        }
        if (request.getNewQuestions() != null) {
            for (QuestionSaveRequest draft : request.getNewQuestions()) {
                if (draft == null) {
                    continue;
                }
                Long questionId = questionService.create(draft);
                order = addContestProblem(contest.getId(), questionId, order);
            }
        }
        if (order == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "竞赛至少需要一道题");
        }
        log.info("[monitor] type=contest-create contestId={} problems={}", contest.getId(), order);
        return contest.getId();
    }

    /**
     * 把一道题挂到竞赛题单，自动分配 A/B/C 标签和 100 分。
     *
     * @param contestId 竞赛 ID
     * @param questionId 题目 ID
     * @param order 当前题目序号，从 0 开始
     * @return 下一道题的序号
     */
    private int addContestProblem(Long contestId, Long questionId, int order) {
        if (order >= 26) {
            throw new BizException(ErrorCode.PARAM_ERROR, "单场竞赛最多 26 道题");
        }
        ContestProblem problem = new ContestProblem();
        problem.setContestId(contestId);
        problem.setQuestionId(questionId);
        problem.setLabel(String.valueOf((char) ('A' + order)));
        problem.setSortNo(order + 1);
        problem.setScore(100);
        contestMapper.insertProblem(problem);
        return order + 1;
    }

    @Override
    public void validateSubmission(Long contestId, Long questionId, Long userId) {
        if (contestId == null) {
            return;
        }
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        ContestRow contest = contestMapper.selectById(contestId, userId);
        if (contest == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "竞赛不存在");
        }
        if (!"RUNNING".equals(contest.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "竞赛不在进行中");
        }
        if (!Boolean.TRUE.equals(contest.getRegistered())) {
            throw new BizException(ErrorCode.FORBIDDEN, "请先报名竞赛");
        }
        List<ContestProblemRow> problems = contestMapper.selectProblems(contestId);
        boolean exists = problems != null && problems.stream()
                .anyMatch(problem -> questionId.equals(problem.getQuestionId()));
        if (!exists) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题目不属于该竞赛");
        }
    }

    private ContestListItemVO toListItem(ContestRow row) {
        ContestListItemVO vo = new ContestListItemVO();
        vo.setId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setDescription(row.getDescription());
        vo.setStartTime(row.getStartTime());
        vo.setEndTime(row.getEndTime());
        vo.setStatus(row.getStatus());
        vo.setProblemCount(row.getProblemCount());
        vo.setParticipantCount(row.getParticipantCount());
        vo.setRegistered(Boolean.TRUE.equals(row.getRegistered()));
        return vo;
    }

    private ContestProblemVO toProblem(ContestProblemRow row) {
        ContestProblemVO vo = new ContestProblemVO();
        vo.setLabel(row.getLabel());
        vo.setQuestionId(row.getQuestionId());
        vo.setTitle(row.getTitle());
        vo.setDifficulty(row.getDifficulty());
        vo.setKnowledgePoint(row.getKnowledgePointName());
        vo.setScore(row.getScore());
        return vo;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "竞赛状态不合法");
        }
        return value;
    }

    private static final class RankAccumulator {
        private Long userId;
        private String username;
        private String nickname;
        private int solvedCount;
        private int score;
        private long penaltySeconds;
        private LocalDateTime lastAcTime;
        private final Set<Long> solved = new HashSet<>();
        private final Map<Long, Integer> wrong = new HashMap<>();
        private final Map<Long, ContestCell> cells = new HashMap<>();
    }

    private static final class ContestCell {
        private boolean accepted;
        private int attempts;
        private long acSeconds;
    }
}
