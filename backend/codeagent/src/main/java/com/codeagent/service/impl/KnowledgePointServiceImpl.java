package com.codeagent.service.impl;

import com.codeagent.mapper.KnowledgePointMapper;
import com.codeagent.service.KnowledgePointService;
import com.codeagent.vo.KnowledgePointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;

    /**
     * 查询全部知识点，并附带每个知识点下的公开题数量。
     *
     * @return 按 sort、id 排序的知识点列表
     */
    @Override
    public List<KnowledgePointVO> list() {
        return knowledgePointMapper.selectListWithQuestionCount();
    }
}
