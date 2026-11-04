package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private String token;

    /**
     * 有效期，单位秒。
     */
    private long expiresIn;

    private UserVO user;
}
