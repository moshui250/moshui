package com.codeagent.ai.rag;

import com.codeagent.service.RagIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * init profile 下的全量 RAG 重建入口。
 */
@Slf4j
@Component
@Profile("init")
@RequiredArgsConstructor
public class RagInitRunner implements CommandLineRunner {

    private final RagIndexService ragIndexService;

    /**
     * 启动后执行全量重建，并打印入库文档数与块数。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        RagRebuildResult result = ragIndexService.rebuild();
        System.out.printf("入库 %d 篇 / %d 块%n", result.indexed(), result.chunks());
        log.info("RAG 全量重建完成：入库 {} 篇 / {} 块，跳过 {} 篇，失败 {} 篇",
                result.indexed(), result.chunks(), result.skipped(), result.failed());
    }
}
