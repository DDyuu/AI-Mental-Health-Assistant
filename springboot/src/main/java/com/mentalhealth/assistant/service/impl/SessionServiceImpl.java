package com.mentalhealth.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mentalhealth.assistant.common.ResultCode;
import com.mentalhealth.assistant.entity.Session;
import com.mentalhealth.assistant.entity.SessionMessage;
import com.mentalhealth.assistant.exception.BusinessException;
import com.mentalhealth.assistant.mapper.SessionMapper;
import com.mentalhealth.assistant.mapper.SessionMessageMapper;
import com.mentalhealth.assistant.service.SessionService;
import com.mentalhealth.assistant.vo.SessionPageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    @Autowired
    private SessionMessageMapper sessionMessageMapper;

    @Override
    public IPage<SessionPageVO> getUserSessions(Integer userId, Map<String, Object> params) {
        long currentPage = params.get("currentPage") != null ?
                Long.parseLong(params.get("currentPage").toString()) : 1;
        long pageSize = params.get("size") != null ?
                Long.parseLong(params.get("size").toString()) : 10;

        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getUserId, userId);
        wrapper.orderByDesc(Session::getCreateTime);

        IPage<Session> page = baseMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);

        List<SessionPageVO> voList = page.getRecords().stream().map(s -> {
            SessionPageVO vo = new SessionPageVO();
            vo.setId(s.getId());
            vo.setUserNickname(s.getUserNickname());
            vo.setSessionTitle(s.getSessionTitle());
            vo.setLastMessageContent(s.getLastMessageContent());
            vo.setMessageCount(s.getMessageCount());
            vo.setLastMessageTime(s.getStartedAt());
            return vo;
        }).collect(Collectors.toList());

        Page<SessionPageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<SessionPageVO> getSessionPage(Map<String, Object> params) {
        long currentPage = params.get("currentPage") != null ?
                Long.parseLong(params.get("currentPage").toString()) : 1;
        long pageSize = params.get("size") != null ?
                Long.parseLong(params.get("size").toString()) : 10;

        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Session::getCreateTime);

        IPage<Session> page = baseMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);

        List<SessionPageVO> voList = page.getRecords().stream().map(s -> {
            SessionPageVO vo = new SessionPageVO();
            vo.setId(s.getId());
            vo.setUserNickname(s.getUserNickname());
            vo.setSessionTitle(s.getSessionTitle());
            vo.setLastMessageContent(s.getLastMessageContent());
            vo.setMessageCount(s.getMessageCount());
            vo.setLastMessageTime(s.getStartedAt());
            return vo;
        }).collect(Collectors.toList());

        Page<SessionPageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void deleteUserSession(Integer userId, String sessionId, boolean isAdmin) {
        // 校验会话是否存在并属于当前用户（管理员可以删除任何会话）
        Session session = baseMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该会话");
        }

        // 删除会话下的所有消息
        LambdaQueryWrapper<SessionMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(SessionMessage::getSessionId, sessionId);
        sessionMessageMapper.delete(messageWrapper);

        // 删除会话
        baseMapper.deleteById(sessionId);
    }
}
