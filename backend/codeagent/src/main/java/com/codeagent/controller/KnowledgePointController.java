package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.service.KnowledgePointService;
import com.codeagent.vo.KnowledgePointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-point")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    /**
     * 查询全部知识点，并附带每个知识点下的公开题数量。
     *
     * @return 按 sort、id 排序的知识点列表
     */
    @GetMapping("/list")
    public R<List<KnowledgePointVO>> list() {
        return R.ok(knowledgePointService.list());
    }
}
