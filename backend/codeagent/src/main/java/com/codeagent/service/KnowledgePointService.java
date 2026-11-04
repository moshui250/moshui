package com.codeagent.service;

import com.codeagent.vo.KnowledgePointVO;

import java.util.List;

public interface KnowledgePointService {

    /**
     * 查询全部知识点，并统计每个知识点下的公开题数量。
     *
     * @return 按 sort、id 排序的知识点列表
     */
    List<KnowledgePointVO> list();
}
