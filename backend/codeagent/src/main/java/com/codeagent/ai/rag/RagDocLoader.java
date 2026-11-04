package com.codeagent.ai.rag;

import com.codeagent.mapper.RagSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 从数据库和内置常量读取全部 RAG 语料。
 */
@Component
@RequiredArgsConstructor
public class RagDocLoader {

    private final RagSourceMapper ragSourceMapper;

    /**
     * 读取四类来源：题解、知识点、内置错误手册、题面。
     *
     * @return 全量逻辑文档，顺序稳定，便于复现实验
     */
    public List<RagSourceDocument> loadAll() {
        List<RagSourceDocument> documents = new ArrayList<>();
        documents.addAll(ragSourceMapper.selectSolutions());
        documents.addAll(ragSourceMapper.selectKnowledgePoints());
        documents.add(RagSourceDocument.builder()
                .sourceType("ERROR_MANUAL")
                .sourceId(0L)
                .title("刷题常见错误手册")
                .knowledgePointId(null)
                .knowledgePointCode("")
                .content(ErrorManual.TEXT)
                .build());
        documents.addAll(ragSourceMapper.selectStatements());
        return documents;
    }
}
