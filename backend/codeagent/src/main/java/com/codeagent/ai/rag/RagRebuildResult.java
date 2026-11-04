package com.codeagent.ai.rag;

/**
 * 全量重建统计。
 *
 * @param indexed 本次实际写入向量库的文档数
 * @param skipped 内容哈希未变而跳过的文档数
 * @param failed  处理失败的文档数
 * @param chunks  本次写入或跳过的总块数
 */
public record RagRebuildResult(int indexed, int skipped, int failed, int chunks) {

    /**
     * 是否全部成功。
     *
     * @return 没有失败文档时返回 true
     */
    public boolean success() {
        return failed == 0;
    }
}
