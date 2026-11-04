package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 20, message = "用户名不能超过 20 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 32, message = "密码不能超过 32 个字符")
    private String password;
}
