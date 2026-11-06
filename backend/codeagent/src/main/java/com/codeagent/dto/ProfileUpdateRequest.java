package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料修改请求。登录用户名不可修改，只允许改昵称与头像配色。
 */
@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 32, message = "昵称需为 1-32 个字符")
    private String nickname;

    @NotBlank(message = "请选择头像")
    @Pattern(regexp = "^[a-z]{3,16}$", message = "头像格式不正确")
    private String avatar;
}
