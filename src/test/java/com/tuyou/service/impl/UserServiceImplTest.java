package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.common.BizException;
import com.tuyou.dto.LoginDTO;
import com.tuyou.dto.RegisterDTO;
import com.tuyou.entity.User;
import com.tuyou.mapper.UserMapper;
import com.tuyou.vo.LoginVO;
import com.tuyou.vo.UserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务单元测试（Mockito mock Mapper）
 * 验证：注册加密、唯一校验、登录统一错误提示、禁用账号、当前用户查询
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("注册成功：密码 BCrypt 加密后入库，非明文")
    void registerSuccess() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("123456");
        userService.register(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals("newuser", saved.getUsername());
        assertNotEquals("123456", saved.getPassword());
        assertTrue(encoder.matches("123456", saved.getPassword()));
    }

    @Test
    @DisplayName("用户名重复：抛参数错误且不落库")
    void registerDuplicateUsername() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("test01");
        dto.setPassword("123456");
        BizException ex = assertThrows(BizException.class, () -> userService.register(dto));
        assertEquals("用户名已存在", ex.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("登录成功：返回 token 与用户信息")
    void loginSuccess() {
        User user = new User();
        user.setId(1001L);
        user.setUsername("test01");
        user.setPassword(encoder.encode("123456"));
        user.setStatus(1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("test01");
        dto.setPassword("123456");
        LoginVO vo = userService.login(dto);

        assertNotNull(vo.getToken());
        assertFalse(vo.getToken().isEmpty());
        assertEquals(1001L, vo.getUser().getId());
        assertEquals("test01", vo.getUser().getUsername());
    }

    @Test
    @DisplayName("密码错误：统一提示用户名或密码错误")
    void loginWrongPassword() {
        User user = new User();
        user.setId(1001L);
        user.setUsername("test01");
        user.setPassword(encoder.encode("correct-stored-password"));
        user.setStatus(1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("test01");
        dto.setPassword("wrong-input");
        BizException ex = assertThrows(BizException.class, () -> userService.login(dto));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("用户不存在：同样提示统一文案（不暴露账号是否存在）")
    void loginUserNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("nobody");
        dto.setPassword("123456");
        BizException ex = assertThrows(BizException.class, () -> userService.login(dto));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("禁用账号（status=0）禁止登录，返回 403")
    void loginDisabledUser() {
        User user = new User();
        user.setId(1001L);
        user.setUsername("test01");
        user.setPassword(encoder.encode("123456"));
        user.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("test01");
        dto.setPassword("123456");
        BizException ex = assertThrows(BizException.class, () -> userService.login(dto));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("查询当前用户：不存在抛 404")
    void getCurrentUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> userService.getCurrentUser(999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("查询当前用户成功：VO 不包含 password 字段")
    void getCurrentUserSuccess() {
        User user = new User();
        user.setId(1001L);
        user.setUsername("test01");
        user.setPassword("encrypted");
        user.setPhone("13800000000");
        user.setStatus(1);
        when(userMapper.selectById(1001L)).thenReturn(user);

        UserVO vo = userService.getCurrentUser(1001L);
        assertEquals("test01", vo.getUsername());

        boolean hasPassword = java.util.Arrays.stream(UserVO.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equals("password"));
        assertFalse(hasPassword);
    }
}
