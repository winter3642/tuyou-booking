package com.tuyou.service;

import com.tuyou.dto.LoginDTO;
import com.tuyou.dto.RegisterDTO;
import com.tuyou.vo.LoginVO;
import com.tuyou.vo.UserVO;

public interface UserService {

    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    UserVO getCurrentUser(Long userId);
}
