package com.codeagent.service;

import com.codeagent.dto.ImageQuestionDraft;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 图片识别草稿的 10 分钟内存缓存。草稿不落库，必须由用户确认后再导入。
 */
@Service
public class ParsedDraftCache {

    private final Cache<String, CachedDraft> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(1000)
            .build();

    /**
     * 写入草稿并生成不可猜测的 parseId。
     *
     * @param userId   当前用户 ID
     * @param draft    识别草稿
     * @param imageUrl 图片地址，可为空
     * @return parseId
     */
    public String put(Long userId, ImageQuestionDraft draft, String imageUrl) {
        String parseId = UUID.randomUUID().toString().replace("-", "");
        cache.put(parseId, new CachedDraft(userId, draft, imageUrl));
        return parseId;
    }

    /**
     * 读取草稿；parseId 不存在或不属于当前用户时返回 null。
     *
     * @param parseId 缓存 ID
     * @param userId  当前用户 ID
     * @return 缓存草稿或 null
     */
    public CachedDraft get(String parseId, Long userId) {
        if (parseId == null || parseId.isBlank() || userId == null) {
            return null;
        }
        CachedDraft cached = cache.getIfPresent(parseId);
        if (cached == null || !userId.equals(cached.userId())) {
            return null;
        }
        return cached;
    }

    /**
     * 导入成功后清理，避免同一草稿重复生成题目。
     *
     * @param parseId 缓存 ID
     */
    public void remove(String parseId) {
        cache.invalidate(parseId);
    }

    /**
     * 缓存中的草稿及其归属。
     *
     * @param userId   用户 ID
     * @param draft    识别草稿
     * @param imageUrl 图片地址
     */
    public record CachedDraft(Long userId, ImageQuestionDraft draft, String imageUrl) {
    }
}
