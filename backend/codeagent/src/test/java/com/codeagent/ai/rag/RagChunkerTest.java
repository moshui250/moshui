package com.codeagent.ai.rag;

import com.codeagent.config.RagProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagChunkerTest {

    @Test
    void errorManualUsesSectionTitleAndRespectsChunkSize() {
        RagProperties properties = new RagProperties();
        properties.getChunk().setSize(500);
        properties.getChunk().setOverlap(80);
        RagChunker chunker = new RagChunker(properties);

        RagSourceDocument manual = RagSourceDocument.builder()
                .sourceType("ERROR_MANUAL")
                .sourceId(0L)
                .title("刷题常见错误手册")
                .knowledgePointCode("")
                .content(ErrorManual.TEXT)
                .build();

        List<RagChunk> chunks = chunker.split(manual);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.title().contains("数组越界怎么办")));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.text().length() <= 500));
    }
}
