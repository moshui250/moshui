package com.codeagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=codeagent-test-secret-2026-at-least-32-bytes")
class CodeAgentApplicationTests {

    @Test
    void contextLoads() {
    }
}
