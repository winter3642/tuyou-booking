package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.common.BizException;
import com.tuyou.common.ResultCode;
import com.tuyou.dto.LoginDTO;
import com.tuyou.dto.RegisterDTO;
import com.tuyou.entity.User;
import com.tuyou.mapper.UserMapper;
import com.tuyou.service.UserService;
import com.tuyou.utils.JwtUtil;
import com.tuyou.vo.LoginVO;
import com.tuyou.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(RegisterDTO dto) {
        // 用户名唯一校验
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "用户名已存在");
        }
        // 手机号唯一校验（选了才校验）
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            Long phoneCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
            if (phoneCount > 0) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "手机号已注册");
            }
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // BCrypt 加密
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        // 统一提示"用户名或密码错误"，不暴露哪个错了（安全习惯）
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(toVO(user));
        return vo;
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo; // VO 里没有 password 字段，天然不泄露
    }
}
