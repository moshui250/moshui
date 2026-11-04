package com.codeagent.service;

import com.codeagent.common.ErrorCode;
import com.codeagent.config.DashScopeProperties;
import com.codeagent.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashScope OpenAI 兼容视觉接口客户端。
 */
@Slf4j
@Service
public class DashScopeVisionClient {

    private static final String SYSTEM_PROMPT = """
            你是编程题识别助手。请识别图片中的题目，并且只输出一个 JSON 对象。
            不要输出 Markdown 代码围栏，不要输出解释、前后缀或思考过程。
            JSON 必须包含以下字段：
            {
              "title": "题目标题",
              "description": "完整 Markdown 题面，保留公式和换行",
              "inputDesc": "输入格式",
              "outputDesc": "输出格式",
              "difficulty": 1,
              "knowledgePointCode": "array",
              "knowledgePointName": "数组",
              "samples": [{"input": "样例输入", "output": "样例输出"}],
              "confidence": 0.0
            }
            difficulty 只能是 1、2、3。knowledgePointCode 只能从
            array、string、linked_list、recursion、sort 中选择最接近的一个。
            如果图片中没有某字段，填空字符串；不要编造题目内容。
            样例输入输出必须逐字符保留换行；公式使用 LaTeX。
            """;

    private final DashScopeProperties properties;
    private final RestClient restClient;

    public DashScopeVisionClient(DashScopeProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 把压缩后的图片转成 data URL，调用 qwen-vl-max 并返回原始文本。
     *
     * @param jpeg 压缩后的 JPEG 字节
     * @return 模型返回的 JSON 文本（可能带围栏，交给解析器修复）
     */
    public String extract(byte[] jpeg) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "未配置 DASHSCOPE_API_KEY");
        }
        String dataUrl = "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(jpeg);

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("type", "text");
        text.put("text", "请识别这张图片中的编程题，并按要求只输出 JSON。");

        Map<String, Object> imageUrl = new LinkedHashMap<>();
        imageUrl.put("url", dataUrl);
        Map<String, Object> image = new LinkedHashMap<>();
        image.put("type", "image_url");
        image.put("image_url", imageUrl);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", List.of(text, image));

        try {
            return callModel(properties.getModel(), system, user);
        } catch (BizException e) {
            throw e;
        } catch (RestClientResponseException e) {
            if (isModelUnavailable(e) && hasFallback()) {
                log.warn("DashScope 主模型 {} 不可用，回退到 {}", properties.getModel(), properties.getFallbackModel());
                try {
                    return callModel(properties.getFallbackModel(), system, user);
                } catch (BizException fallbackError) {
                    throw fallbackError;
                } catch (RestClientResponseException fallbackHttp) {
                    log.warn("DashScope 回退模型失败: status={}, body={}",
                            fallbackHttp.getStatusCode(), fallbackHttp.getResponseBodyAsString());
                } catch (ResourceAccessException fallbackNetwork) {
                    log.warn("DashScope 回退模型网络不可用: {}", fallbackNetwork.getMessage());
                }
            }
            log.warn("DashScope 识别失败: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(ErrorCode.IMAGE_PARSE_FAILED, "识别失败，请手动录入");
        } catch (ResourceAccessException e) {
            log.warn("DashScope 网络不可用: {}", e.getMessage());
            throw new BizException(ErrorCode.IMAGE_PARSE_FAILED, "识别失败，请手动录入");
        }
    }

    /**
     * 用指定模型发起一次视觉请求。
     *
     * @param model  模型名
     * @param system system 消息
     * @param user   user 消息
     * @return 模型文本
     */
    private String callModel(String model, Map<String, Object> system, Map<String, Object> user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("max_tokens", 2000);
        if (model.startsWith("qwen3")) {
            body.put("enable_thinking", false);
        }
        body.put("messages", List.of(system, user));

        String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        JsonNode response = restClient.post()
                .uri(endpoint)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        String content = readContent(response);
        if (content == null || content.isBlank()) {
            log.warn("DashScope 模型 {} 返回空内容: {}", model, response);
            throw new BizException(ErrorCode.IMAGE_PARSE_FAILED, "识别失败，请手动录入");
        }
        return content;
    }

    /**
     * 判断 HTTP 错误是否属于模型不存在或无权限，可尝试兼容模型。
     *
     * @param error DashScope HTTP 错误
     * @return 是否可回退
     */
    private boolean isModelUnavailable(RestClientResponseException error) {
        int status = error.getStatusCode().value();
        if (status != 403 && status != 404) {
            return false;
        }
        String body = error.getResponseBodyAsString();
        return body != null && (body.contains("model") || body.contains("access_denied"));
    }

    /**
     * 是否存在可用的兼容模型。
     *
     * @return 配置了且不同于主模型时为 true
     */
    private boolean hasFallback() {
        String fallback = properties.getFallbackModel();
        return fallback != null && !fallback.isBlank() && !fallback.equals(properties.getModel());
    }

    /**
     * 兼容 content 为字符串或 [{type:text,text:...}] 数组两种返回。
     *
     * @param response OpenAI 兼容响应
     * @return 文本内容
     */
    private String readContent(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : content) {
                if (item.hasNonNull("text")) {
                    parts.add(item.get("text").asText());
                }
            }
            return String.join("\n", parts);
        }
        return null;
    }
}
