package com.codeagent.ai;

import com.codeagent.common.ErrorCode;
import com.codeagent.config.PistonProperties;
import com.codeagent.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Piston HTTP 客户端。只负责协议转换、超时和有限重试，不包含判题业务规则。
 */
@Slf4j
@Component
public class PistonClient {

    private static final String RUNTIMES_PATH = "/api/v2/piston/runtimes";
    private static final String EXECUTE_PATH = "/api/v2/piston/execute";
    private static final int MAX_ATTEMPTS = 2;

    private final PistonProperties properties;
    private final RestClient restClient;

    /**
     * 根据配置创建 RestClient，并设置连接、读取超时。
     *
     * @param properties Piston 配置，包含根地址、超时和语言版本
     */
    public PistonClient(PistonProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(stripTrailingSlash(properties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    /**
     * 查询 Piston 可用运行时，用于启动时打印真实语言版本。
     *
     * @return 运行时列表；服务返回空列表时为空列表
     */
    public List<PistonRuntime> fetchRuntimes() {
        return call(() -> restClient.get()
                .uri(RUNTIMES_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PistonRuntime>>() {
                }));
    }

    /**
     * 提交一段代码到 Piston 执行。
     *
     * @param language 业务语言代码 CPP / JAVA
     * @param code     完整源代码
     * @param stdin    当前测试用例的标准输入
     * @return Piston 的编译和运行结果
     */
    public PistonExecuteResponse execute(String language, String code, String stdin) {
        PistonProperties.Language target = properties.requireLanguage(language);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("language", target.getName());
        request.put("version", target.getVersion());
        request.put("files", List.of(Map.of("content", code)));
        request.put("stdin", stdin == null ? "" : stdin);
        request.put("run_timeout", properties.getRunTimeoutMs());
        request.put("compile_timeout", properties.getCompileTimeoutMs());
        request.put("run_memory_limit", properties.getMemoryLimit());
        request.put("compile_memory_limit", properties.getMemoryLimit());
        return call(() -> restClient.post()
                .uri(EXECUTE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PistonExecuteResponse.class));
    }

    /**
     * 对 5xx 和网络超时重试一次；4xx 不重试，直接转成业务异常。
     *
     * @param supplier 实际 HTTP 调用
     * @param <T>      响应类型
     * @return 调用结果
     */
    private <T> T call(Supplier<T> supplier) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return supplier.get();
            } catch (ResourceAccessException e) {
                last = e;
                log.warn("Piston 请求超时，第 {} 次尝试失败: {}", attempt + 1, e.getMessage());
            } catch (RestClientResponseException e) {
                if (!e.getStatusCode().is5xxServerError()) {
                    throw translate(e);
                }
                last = e;
                log.warn("Piston 返回 {}，第 {} 次尝试失败", e.getStatusCode().value(), attempt + 1);
            }
            if (attempt == 0) {
                sleepBeforeRetry();
            }
        }
        log.warn("Piston 请求最终失败: {}", last == null ? "unknown" : last.getMessage());
        throw new BizException(ErrorCode.JUDGE_UNAVAILABLE, "判题服务超时或暂时不可用");
    }

    /**
     * 把 Piston 的 HTTP 错误转换为统一业务异常。
     *
     * @param exception Piston 返回的非 2xx 响应
     * @return 带用户可读消息的业务异常
     */
    private BizException translate(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401) {
            return new BizException(ErrorCode.JUDGE_UNAVAILABLE,
                    "Piston 公共 API 已限制访问（401），请配置可用实例");
        }
        return new BizException(ErrorCode.JUDGE_UNAVAILABLE, "Piston 调用失败，HTTP " + status);
    }

    /**
     * 重试前短暂等待，避免连续打到故障实例。
     */
    private void sleepBeforeRetry() {
        try {
            Thread.sleep(250L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.JUDGE_UNAVAILABLE, "判题请求被中断");
        }
    }

    /**
     * 去掉根地址末尾的斜杠，避免拼出双斜杠路径。
     *
     * @param value 原始根地址
     * @return 无末尾斜杠的地址
     */
    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://emkc.org";
        }
        return value.replaceAll("/+$", "");
    }
}
