package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 对外用户信息，绝不包含 password。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String role;
}
