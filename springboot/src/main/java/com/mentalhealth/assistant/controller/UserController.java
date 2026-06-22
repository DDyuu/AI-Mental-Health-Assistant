package com.mentalhealth.assistant.controller;

import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.common.ResultCode;
import com.mentalhealth.assistant.exception.BusinessException;
import com.mentalhealth.assistant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

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
