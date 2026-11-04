package com.codeagent.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每个 HTTP 请求生成 traceId，写入 MDC 和响应头。
 * 异常日志通过日志格式自动携带该 traceId，便于一次请求串起全部日志。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 响应头名称，排查问题时可直接从浏览器/响应中拿到 traceId。 */
    public static final String TRACE_HEADER = "X-Trace-Id";

    private static final String TRACE_KEY = "traceId";

    /**
     * 生成或复用 traceId，并保证请求结束后清理 MDC。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param chain    后续过滤器链
     * @throws ServletException Servlet 处理异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = normalize(request.getHeader(TRACE_HEADER));
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_KEY);
        }
    }

    /**
     * 只接受 8-64 位安全字符，避免把用户输入原样写进日志/响应头。
     *
     * @param value 原始请求头
     * @return 合法 traceId 或 null
     */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 8 || trimmed.length() > 64) {
            return null;
        }
        return trimmed.matches("[A-Za-z0-9._-]+") ? trimmed : null;
    }
}
