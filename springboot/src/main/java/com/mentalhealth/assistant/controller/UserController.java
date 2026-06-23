package com.mentalhealth.assistant.controller;

import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.common.ResultCode;
import com.mentalhealth.assistant.entity.User;
import com.mentalhealth.assistant.exception.BusinessException;
import com.mentalhealth.assistant.mapper.UserMapper;
import com.mentalhealth.assistant.service.UserService;
import com.mentalhealth.assistant.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader("Token") String token) {
        Claims claims = JwtUtil.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的令牌");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("userType", user.getUserType());
        return Result.success(userInfo);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        if (username == null || username.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空");
        }

        Map<String, Object> data = userService.login(username, password);
        if (data == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }
        return Result.success(data);
    }

    @PostMapping("/add")
    public Result<Void> register(@RequestBody Map<String, String> registerForm) {
        userService.register(registerForm);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
