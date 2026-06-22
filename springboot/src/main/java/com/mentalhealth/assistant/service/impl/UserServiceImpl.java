package com.mentalhealth.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mentalhealth.assistant.common.ResultCode;
import com.mentalhealth.assistant.entity.User;
import com.mentalhealth.assistant.exception.BusinessException;
import com.mentalhealth.assistant.mapper.UserMapper;
import com.mentalhealth.assistant.service.UserService;
import com.mentalhealth.assistant.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Map<String, Object> login(String username, String password) {
        // 根据用户名查询用户
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );

        // 用户不存在或密码不匹配
        if (user == null || !user.getPassword().equals(password)) {
            return null;
        }

        // 用户被禁用
        if (user.getStatus() != null && user.getStatus() == 0) {
            return null;
        }

        // 生成 token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());

        // 构建返回的用户信息（脱敏）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("userType", user.getUserType());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return result;
    }

    @Override
    public void register(Map<String, String> registerForm) {
        String username = registerForm.get("username");
        String email = registerForm.get("email");
        String password = registerForm.get("password");
        String confirmPassword = registerForm.get("confirmPassword");
        String nickname = registerForm.get("nickname");
        String phone = registerForm.get("phone");

        // 校验必填字段
        if (username == null || username.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        if (email == null || email.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "确认密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        Long usernameCount = baseMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
        if (usernameCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }

        // 检查邮箱是否已存在
        Long emailCount = baseMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
        );
        if (emailCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        user.setPhone(phone);
        user.setUserType(1); // 1: 普通用户
        user.setStatus(1);   // 1: 启用

        baseMapper.insert(user);
    }
}
