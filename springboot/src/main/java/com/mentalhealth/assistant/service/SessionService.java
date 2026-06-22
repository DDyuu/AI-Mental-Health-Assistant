package com.mentalhealth.assistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mentalhealth.assistant.entity.Session;
import com.mentalhealth.assistant.vo.SessionPageVO;

import java.util.Map;

public interface SessionService extends IService<Session> {

    IPage<SessionPageVO> getSessionPage(Map<String, Object> params);

    IPage<SessionPageVO> getUserSessions(Integer userId, Map<String, Object> params);

    void deleteUserSession(Integer userId, String sessionId, boolean isAdmin);
}
