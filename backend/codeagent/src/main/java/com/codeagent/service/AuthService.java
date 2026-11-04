package com.codeagent.service;

import com.codeagent.dto.LoginRequest;
import com.codeagent.dto.RegisterRequest;
import com.codeagent.vo.LoginVO;
import com.codeagent.vo.UserVO;
import jakarta.validation.Valid;

public interface AuthService {
    UserVO register(@Valid RegisterRequest request);

    LoginVO login(@Valid LoginRequest request);

    void logout();

    UserVO me();
}
