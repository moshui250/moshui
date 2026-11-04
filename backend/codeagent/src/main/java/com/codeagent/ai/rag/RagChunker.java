package com.codeagent.ai.rag;

import com.codeagent.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 切块器：先按标题分段，再对超长段做滑动窗口。
 */
@Component
@RequiredArgsConstructor
public class RagChunker {

    private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,6})\\s+(.+?)\\s*$");

    private final RagProperties properties;

    /**
     * 将一篇文档切成 400-600 字左右的块。
     *
     * @param document 待切分文档
     * @return 按原文顺序排列的切块；空文档返回空列表
     */
    public List<RagChunk> split(RagSourceDocument document) {
        if (document == null || document.getContent() == null || document.getContent().isBlank()) {
            return List.of();
        }
        RagProperties.Chunk config = properties.getChunk();
        int size = Math.max(100, config.getSize());
        int overlap = Math.max(0, Math.min(config.getOverlap(), size / 2));
        List<RagChunk> chunks = new ArrayList<>();
        for (Section section : parseSections(document.getContent(), document.getTitle())) {
            String text = section.text().trim();
            if (text.isEmpty()) {
                continue;
            }
            String title = "ERROR_MANUAL".equals(document.getSourceType()) && section.heading() != null
                    ? section.heading()
                    : document.getTitle();
            appendChunks(chunks, text, title, size, overlap);
        }
        return chunks;
    }

    /**
     * 按 Markdown 标题拆分段落，保留标题本身作为段落正文的一部分。
     *
     * @param content       原始 Markdown
     * @param fallbackTitle 没有标题时使用的文档标题
     * @return 段落列表
     */
    private List<Section> parseSections(String content, String fallbackTitle) {
        Matcher matcher = HEADING.matcher(content);
        List<Section> sections = new ArrayList<>();
        String currentHeading = fallbackTitle;
        int sectionStart = 0;
        boolean hasHeading = false;
        while (matcher.find()) {
            if (!hasHeading) {
                if (matcher.start() > 0) {
                    sections.add(new Section(fallbackTitle, content.substring(0, matcher.start())));
                }
                hasHeading = true;
            } else {
                sections.add(new Section(currentHeading, content.substring(sectionStart, matcher.start())));
            }
            currentHeading = matcher.group(2).trim();
            sectionStart = matcher.start();
        }
        if (!hasHeading) {
            sections.add(new Section(fallbackTitle, content));
        } else {
            sections.add(new Section(currentHeading, content.substring(sectionStart)));
        }
        return sections;
    }

    /**
     * 对单个段落追加一个或多个窗口块。
     *
     * @param chunks  结果列表
     * @param text    段落正文
     * @param title   块标题
     * @param size    单块目标长度
     * @param overlap 相邻窗口重叠长度
     */
    private void appendChunks(List<RagChunk> chunks, String text, String title, int size, int overlap) {
        if (text.length() <= size) {
            chunks.add(new RagChunk(text, title, chunks.size()));
            return;
        }
        int step = Math.max(1, size - overlap);
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + size);
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(new RagChunk(piece, title, chunks.size()));
            }
            if (end == text.length()) {
                break;
            }
        }
    }

    /**
     * Markdown 段落。
     *
     * @param heading 段落标题
     * @param text    段落正文（含标题行）
     */
    private record Section(String heading, String text) {
    }
}
