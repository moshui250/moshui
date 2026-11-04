package com.codeagent.service;

import com.codeagent.common.PageResult;
import com.codeagent.dto.QuestionSaveRequest;
import com.codeagent.vo.QuestionDetailVO;
import com.codeagent.vo.QuestionListItemVO;
import com.codeagent.vo.QuestionRecommendVO;

import java.util.List;

public interface QuestionService {

    /**
     * 分页查询题目列表。PUBLIC 返回公开题，PERSONAL 只返回当前用户的个人题。
     *
     * @param page             页码，从 1 开始
     * @param size             每页条数，服务端限制为 1~50
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @param difficulty       难度 1/2/3，null 表示不限
     * @param type             题目类型 PUBLIC/PERSONAL
     * @param keyword          标题关键字，null 或空串表示不限
     * @return 分页后的题目列表
     */
    PageResult<QuestionListItemVO> list(long page, long size, Long knowledgePointId,
                                        Integer difficulty, String type, String keyword);

    /**
     * 查询题目详情。只返回样例用例，隐藏用例不会进入响应。
     *
     * @param id 题目 ID
     * @return 题目详情，包含知识点、模板、样例和当前用户做题状态
     */
    QuestionDetailVO detail(Long id);

    /**
     * 创建个人题，题目归属当前登录用户。
     *
     * @param request 题目基本信息、用例和可选参考代码
     * @return 新建题目 ID
     */
    Long create(QuestionSaveRequest request);

    /**
     * 修改当前用户创建的个人题。
     *
     * @param id      题目 ID
     * @param request 新的题目信息；用例/参考代码不传时保持原值
     */
    void update(Long id, QuestionSaveRequest request);

    /**
     * 逻辑删除当前用户创建的个人题。
     *
     * @param id 题目 ID
     */
    void delete(Long id);

    /**
     * 推荐同知识点或难度接近的公开题。
     *
     * @param questionId 当前题 ID，用于定位知识点、难度并排除自身
     * @param limit      推荐数量，null 时默认 3
     * @return 推荐题目列表
     */
    List<QuestionRecommendVO> recommend(Long questionId, Integer limit);
}
