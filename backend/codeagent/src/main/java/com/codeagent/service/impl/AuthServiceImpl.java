package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.UserContext;
import com.codeagent.dto.LoginRequest;
import com.codeagent.dto.RegisterRequest;
import com.codeagent.entity.SysUser;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.SysUserMapper;
import com.codeagent.service.AuthService;
import com.codeagent.utils.JwtUtils;
import com.codeagent.vo.LoginVO;
import com.codeagent.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtService;
    @Override
    public UserVO register(RegisterRequest request) {
        long exists = userMapper.countByUsername(request.getUsername());
        if (exists > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : request.getUsername());
        user.setRole(DEFAULT_ROLE);
        user.setStatus(1);
        user.setDeleted(0);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时唯一索引兜底。
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return toVO(user);
    }
    @Override
    public LoginVO login(LoginRequest request) {
        SysUser user = userMapper.selectByUsername(request.getUsername());

        // 用户不存在与密码错误返回同一错误码和文案，避免账号枚举。
        if (user == null
                || !Integer.valueOf(1).equals(user.getStatus())
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtService.createToken(user);
        return new LoginVO(token, jwtService.getExpireSeconds(), toVO(user));
    }

    @Override
    public void logout() {
        // JWT 无状态：服务端不维护黑名单，前端收到成功响应后删除本地 token。
        log.info("用户登出（无状态）: userId={}", UserContext.getUserId());
    }
    @Override
    public UserVO me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return toVO(user);
    }

    private UserVO toVO(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }
}
