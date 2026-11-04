package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.dto.LoginRequest;
import com.codeagent.dto.RegisterRequest;
import com.codeagent.service.AuthService;
import com.codeagent.service.impl.AuthServiceImpl;
import com.codeagent.vo.LoginVO;
import com.codeagent.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(authService.me());
    }
}
