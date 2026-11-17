package com.autotesthub.system.service;

import com.autotesthub.system.dto.LoginRequest;
import com.autotesthub.system.entity.SysUser;
import com.autotesthub.system.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录验证
     */
    public SysUser login(LoginRequest request) {

        // 1. 根据用户名查询用户
        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query
                        .LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername())
        );

        // 2. 用户不存在
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }

        // 4. 验证 BCrypt 密码
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 5. 返回用户信息
        return user;
    }
}