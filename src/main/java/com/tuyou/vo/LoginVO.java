package com.tuyou.vo;

import lombok.Data;

/**
 * 登录出参：token + 用户信息
 */
@Data
public class LoginVO {
    private String token;
    private UserVO user;
}
