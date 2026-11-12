package com.codeagent.ai;

import com.codeagent.common.ErrorCode;
import com.codeagent.config.GoJudgeProperties;
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
 * go-judge REST 客户端。只负责协议转换、超时、重试和结果适配，不包含判题业务规则。
 */
@Slf4j
@Component
public class GoJudgeClient {

    private static final int MAX_ATTEMPTS = 2;
    private static final int COMPILE_ERROR_EXIT = 101;

    private final GoJudgeProperties properties;
    private final GoJudgeCircuitBreaker circuitBreaker;
    private final RestClient restClient;

    /**
     * 创建带连接和读取超时的 RestClient。
     *
     * @param properties     go-judge 配置
     * @param circuitBreaker 连续失败熔断器
     */
    public GoJudgeClient(GoJudgeProperties properties, GoJudgeCircuitBreaker circuitBreaker) {
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(stripTrailingSlash(properties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    /**
     * 查询 go-judge 版本，启动时用于确认服务可用。
     *
     * @return /version 的原始字段
     */
    public Map<String, Object> fetchVersion() {
        return call(() -> restClient.get()
                .uri("/version")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
    }

    /**
     * 提交一段代码到 go-judge。编译和运行在一个 shell 中完成，编译失败通过
     * compile.err 和退出码 101 区分，避免把编译错误误判成运行错误。
     *
     * @param language 业务语言代码 CPP / JAVA
     * @param code     完整源码
     * @param stdin    当前测试用例输入
     * @return 适配后的编译/运行结果
     */
    public GoJudgeExecuteResponse execute(String language, String code, String stdin) {
        circuitBreaker.ensureAvailable();
        GoJudgeProperties.Language target = properties.requireLanguage(language);
        Map<String, Object> request = buildRequest(target, code, stdin);
        try {
            List<GoJudgeResponse> responses = call(() -> restClient.post()
                    .uri("/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GoJudgeResponse>>() {
                    }));
            circuitBreaker.recordSuccess();
            return adapt(responses);
        } catch (BizException e) {
            circuitBreaker.recordFailure();
            throw e;
        }
    }

    /**
     * 组装 go-judge /run 请求。stdout/stderr 使用 collector 收集，compile.err 仅在失败时存在。
     */
    private Map<String, Object> buildRequest(GoJudgeProperties.Language language, String code, String stdin) {
        int runSeconds = Math.max(1, (properties.getRunTimeoutMs() + 999) / 1000);
        String script = "rm -f compile.err; " + language.compileCommand()
                + "; rc=$?; if [ $rc -ne 0 ]; then cat compile.err; exit " + COMPILE_ERROR_EXIT
                + "; fi; rm -f compile.err; ulimit -t " + runSeconds + "; " + language.runCommand();

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("args", List.of("/bin/sh", "-c", script));
        command.put("env", List.of(
                "PATH=" + properties.getPath(),
                "LANG=C.UTF-8",
                "JAVA_HOME=" + properties.getJavaHome()));
        command.put("files", List.of(
                Map.of("content", stdin == null ? "" : stdin),
                Map.of("name", "stdout", "max", properties.getOutputLimit()),
                Map.of("name", "stderr", "max", properties.getOutputLimit())));
        command.put("cpuLimit", (long) (properties.getCompileTimeoutMs() + properties.getRunTimeoutMs() + 1000) * 1_000_000L);
        command.put("clockLimit", (long) (properties.getCompileTimeoutMs() + properties.getRunTimeoutMs() + 3000) * 1_000_000L);
        command.put("memoryLimit", properties.getMemoryLimit());
        command.put("procLimit", properties.getProcLimit());
        command.put("copyIn", Map.of(language.sourceFile(), Map.of("content", code == null ? "" : code)));
        command.put("copyOut", List.of("stdout", "stderr", "compile.err?"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("cmd", List.of(command));
        return request;
    }

    /**
     * 把 go-judge 的单条结果拆成 compile / run 两个阶段。
     */
    private GoJudgeExecuteResponse adapt(List<GoJudgeResponse> responses) {
        if (responses == null || responses.isEmpty() || responses.get(0) == null) {
            throw new BizException(ErrorCode.JUDGE_UNAVAILABLE, "go-judge 未返回执行结果");
        }
        GoJudgeResponse result = responses.get(0);
        Map<String, String> files = result.files() == null ? Map.of() : result.files();
        String stdout = files.getOrDefault("stdout", "");
        String stderr = files.getOrDefault("stderr", "");
        String compileText = files.getOrDefault("compile.err", "");
        int exitCode = result.exitStatus() == null ? -1 : result.exitStatus();
        long timeMs = result.runTime() == null ? 0L : Math.max(result.runTime() / 1_000_000L, 0L);
        String status = result.status() == null ? "" : result.status();
        String sandboxError = result.error() == null ? "" : result.error();
        if (!sandboxError.isBlank()) {
            status = status.isBlank() ? "Internal Error" : status;
            stderr = stderr.isBlank() ? sandboxError : stderr + "\n" + sandboxError;
        }

        boolean compileFailed = exitCode == COMPILE_ERROR_EXIT || !compileText.isBlank();
        if (compileFailed) {
            String text = compileText.isBlank() ? "编译失败（go-judge 未返回编译输出）" : compileText;
            // stdout 留空，避免 JudgeServiceImpl.stageText 把同一段编译器输出拼接两次。
            GoJudgeExecuteResponse.Stage compile = new GoJudgeExecuteResponse.Stage(
                    "", text, text, COMPILE_ERROR_EXIT, null);
            return new GoJudgeExecuteResponse(compile, null, "Compile Error", timeMs);
        }

        // go-judge 对 shell 内的 SIGKILL 可能返回 Signalled / Nonzero Exit Status；
        // 墙钟接近运行上限且不是显式内存/输出超限时，统一归为 TLE。
        boolean nearRunLimit = timeMs >= properties.getRunTimeoutMs() * 0.9;
        if (!"Accepted".equalsIgnoreCase(status)
                && !"Memory Limit Exceeded".equalsIgnoreCase(status)
                && !"Output Limit Exceeded".equalsIgnoreCase(status)
                && nearRunLimit) {
            status = "Time Limit Exceeded";
        }
        GoJudgeExecuteResponse.Stage run = new GoJudgeExecuteResponse.Stage(
                stdout, stderr, stdout + stderr, exitCode, null);
        return new GoJudgeExecuteResponse(null, run, status, timeMs);
    }

    /**
     * 对 5xx 和网络异常重试一次；4xx 不重试。
     */
    private <T> T call(Supplier<T> supplier) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return supplier.get();
            } catch (ResourceAccessException e) {
                last = e;
                log.warn("go-judge 请求超时，第 {} 次尝试失败: {}", attempt + 1, e.getMessage());
            } catch (RestClientResponseException e) {
                if (!e.getStatusCode().is5xxServerError()) {
                    throw translate(e);
                }
                last = e;
                log.warn("go-judge 返回 {}，第 {} 次尝试失败", e.getStatusCode().value(), attempt + 1);
            }
            if (attempt == 0) {
                sleepBeforeRetry();
            }
        }
        log.warn("go-judge 请求最终失败: {}", last == null ? "unknown" : last.getMessage());
        throw new BizException(ErrorCode.JUDGE_UNAVAILABLE, "判题服务超时或暂时不可用");
    }

    /**
     * 把 HTTP 错误转换为统一业务异常。
     */
    private BizException translate(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new BizException(ErrorCode.JUDGE_UNAVAILABLE, "go-judge 拒绝访问，请检查鉴权配置");
        }
        if (status == 404) {
            return new BizException(ErrorCode.JUDGE_UNAVAILABLE, "go-judge 接口不存在，请检查 GOJUDGE_BASE_URL");
        }
        return new BizException(ErrorCode.JUDGE_UNAVAILABLE, "go-judge 调用失败，HTTP " + status);
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
     * 去掉根地址末尾斜杠，避免拼出双斜杠路径。
     */
    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:5050";
        }
        return value.replaceAll("/+$", "");
    }
}
