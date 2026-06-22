package com.mentalhealth.assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mentalhealth.assistant.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 包含 token 和用户信息的 Map
     */
    Map<String, Object> login(String username, String password);

    /**
     * 用户注册
     * @param registerForm 注册表单数据
     */
    void register(Map<String, String> registerForm);
}
