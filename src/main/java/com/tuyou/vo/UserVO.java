package com.tuyou.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息出参（绝不包含 password）
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private LocalDateTime createTime;
}
