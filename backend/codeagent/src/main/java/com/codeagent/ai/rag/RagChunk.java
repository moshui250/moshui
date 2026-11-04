package com.codeagent.ai.rag;

/**
 * 切块结果。
 *
 * @param text       切块正文（Markdown）
 * @param title      该块用于展示的标题
 * @param chunkIndex 在所属文档内的顺序号，从 0 开始
 */
public record RagChunk(String text, String title, int chunkIndex) {
}
