package com.codeagent.service.impl;

import com.codeagent.service.HealthService;
import com.codeagent.vo.HealthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 数据库不可用时也返回 200，前端据此显示服务降级状态。
     */
    @Override
    public HealthVO check() {
        String db = "DOWN";
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                db = "UP";
            }
        } catch (Exception e) {
            log.debug("数据库健康检查失败: {}", e.getMessage());
        }
        return new HealthVO("UP", db, LocalDateTime.now());
    }
}
