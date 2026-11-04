package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.service.HealthService;
import com.codeagent.service.impl.HealthServiceImpl;
import com.codeagent.vo.HealthVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    public R<HealthVO> health() {
        return R.ok(healthService.check());
    }
}
