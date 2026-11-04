package com.codeagent.interceptor;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.exception.BizException;
import com.codeagent.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 纯 Spring MVC 拦截器鉴权，不使用 Spring Security 过滤器链。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // 进入新请求先清一次，防止上一次请求异常时残留。
        UserContext.clear();

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(response);
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return unauthorized(response);
        }

        try {
            UserContext.set(jwtService.parseToken(token));
            return true;
        } catch (BizException e) {
            return unauthorized(response);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理，Tomcat 线程池复用线程时不能把上一个用户带给下一个请求。
        UserContext.clear();
    }

    private boolean unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), R.fail(ErrorCode.UNAUTHORIZED));
        return false;
    }
}
