package com.mentalhealth.assistant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.common.ResultCode;
import com.mentalhealth.assistant.entity.Session;
import com.mentalhealth.assistant.entity.SessionEmotion;
import com.mentalhealth.assistant.entity.SessionMessage;
import com.mentalhealth.assistant.exception.BusinessException;
import com.mentalhealth.assistant.mapper.SessionMapper;
import com.mentalhealth.assistant.mapper.SessionMessageMapper;
import com.mentalhealth.assistant.mapper.SessionEmotionMapper;
import com.mentalhealth.assistant.mapper.UserMapper;
import com.mentalhealth.assistant.service.SessionService;
import com.mentalhealth.assistant.service.ai.DeepSeekService;
import com.mentalhealth.assistant.service.ai.EmotionAnalysisService;
import com.mentalhealth.assistant.service.ai.PromptConfig;
import com.mentalhealth.assistant.util.JwtUtil;
import com.mentalhealth.assistant.vo.SessionPageVO;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ConsultationController {

    private static final Logger log = LoggerFactory.getLogger(ConsultationController.class);

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionMessageMapper sessionMessageMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SessionEmotionMapper sessionEmotionMapper;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private EmotionAnalysisService emotionAnalysisService;

    @Value("${deepseek.model}")
    private String aiModel;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping("/psychological-chat/sessions")
    public Result<IPage<SessionPageVO>> getConsultationPage(@RequestParam Map<String, Object> params) {
        IPage<SessionPageVO> page = sessionService.getSessionPage(params);
        return Result.success(page);
    }

    @GetMapping("/psychological-chat/my-sessions")
    public Result<IPage<SessionPageVO>> getMySessions(
            @RequestParam Map<String, Object> params,
            @RequestHeader("Token") String token) {
        // 解析 token 获取用户信息
        Claims claims = JwtUtil.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的令牌");
        }
        IPage<SessionPageVO> page = sessionService.getUserSessions(userId, params);
        return Result.success(page);
    }

    @GetMapping("/psychological-chat/sessions/{sessionId}/messages")
    public Result<List<SessionMessage>> getSessionDetail(
            @PathVariable String sessionId,
            @RequestHeader("Token") String token) {
        // 解析 token 获取用户信息
        Claims claims = JwtUtil.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的令牌");
        }

        boolean isAdmin = isAdminUser(claims, userId);

        // 校验会话是否属于当前用户（管理员可以访问所有会话）
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该会话");
        }

        LambdaQueryWrapper<SessionMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionMessage::getSessionId, sessionId);
        wrapper.orderByAsc(SessionMessage::getCreatedAt);

        List<SessionMessage> messages = sessionMessageMapper.selectList(wrapper);
        return Result.success(messages);
    }

    @DeleteMapping("/psychological-chat/session/{sessionId}")
    public Result<Void> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader("Token") String token) {
        // 解析 token 获取用户信息
        Claims claims = JwtUtil.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的令牌");
        }

        boolean isAdmin = isAdminUser(claims, userId);
        sessionService.deleteUserSession(userId, sessionId, isAdmin);
        return Result.success();
    }

    /**
     * 流式 AI 聊天接口
     * POST /api/psychological-chat/session/chat
     * Body: { "sessionId": "xxx", "message": "xxx" }
     * 返回 SSE 流
     */
    @PostMapping(value = "/psychological-chat/session/chat", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter streamChat(
            @RequestBody Map<String, String> body,
            @RequestHeader("Token") String token,
            HttpServletResponse response) {

        // 禁用缓存，确保流式输出不被缓冲
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        // 先创建 Emitter，确保响应类型已确定
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                // 解析 token
                Claims claims;
                try {
                    claims = JwtUtil.parseToken(token);
                } catch (Exception e) {
                    sendErrorEvent(emitter, "登录已过期，请重新登录");
                    return;
                }

                Integer userId = claims.get("userId", Integer.class);
                String username = claims.getSubject();
                if (userId == null) {
                    sendErrorEvent(emitter, "无效的令牌");
                    return;
                }

                String sessionId = body.get("sessionId");
                String userMessage = body.get("message");
                String sessionTitle = body.get("sessionTitle");

                if (userMessage == null || userMessage.isBlank()) {
                    sendErrorEvent(emitter, "消息不能为空");
                    return;
                }

                // 如果是临时会话或新会话，先创建会话记录
                if (sessionId == null || sessionId.isBlank() || sessionId.startsWith("temp_")) {
                    sessionId = "session_" + UUID.randomUUID().toString().replace("-", "");
                    Session newSession = new Session();
                    newSession.setId(sessionId);
                    newSession.setUserId(userId);
                    newSession.setUserNickname(username);
                    newSession.setSessionTitle(sessionTitle != null && !sessionTitle.isBlank()
                            ? sessionTitle : "新对话 - " + LocalDateTime.now().toString().substring(0, 16));
                    newSession.setStatus("ACTIVE");
                    newSession.setStartedAt(LocalDateTime.now());
                    newSession.setMessageCount(0);
                    newSession.setLastMessageContent(userMessage);
                    sessionMapper.insert(newSession);
                }

                final String finalSessionId = sessionId;

                // 保存用户消息
                SessionMessage userMsg = new SessionMessage();
                userMsg.setSessionId(finalSessionId);
                userMsg.setSenderType(1);
                userMsg.setContent(userMessage);
                userMsg.setCreatedAt(LocalDateTime.now());
                sessionMessageMapper.insert(userMsg);

                // 更新会话
                Session session = sessionMapper.selectById(finalSessionId);
                if (session != null) {
                    session.setLastMessageContent(userMessage);
                    session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
                    sessionMapper.updateById(session);
                }

                // 发送 sessionId（使用 SESSION: 前缀以便前端识别）
                emitter.send(SseEmitter.event().data("SESSION:" + finalSessionId));

                // 调用 DeepSeek 流式 API
                deepSeekService.streamChat(PromptConfig.SYSTEM_PROMPT, userMessage, emitter,
                        fullResponse -> {
                            if (fullResponse != null && !fullResponse.isEmpty()) {
                                SessionMessage aiMsg = new SessionMessage();
                                aiMsg.setSessionId(finalSessionId);
                                aiMsg.setSenderType(2);
                                aiMsg.setContent(fullResponse);
                                aiMsg.setAiModel(aiModel);
                                aiMsg.setCreatedAt(LocalDateTime.now());
                                sessionMessageMapper.insert(aiMsg);

                                Session s = sessionMapper.selectById(finalSessionId);
                                if (s != null) {
                                    s.setLastMessageContent(fullResponse);
                                    s.setMessageCount((s.getMessageCount() == null ? 0 : s.getMessageCount()) + 1);
                                    sessionMapper.updateById(s);
                                }

                                // 异步分析情绪（不阻塞流结束）
                                try {
                                    String conversationText = "用户说：" + userMessage + "\nAI回复：" + fullResponse;
                                    emotionAnalysisService.analyzeAndSave(finalSessionId, conversationText);
                                } catch (Exception ex) {
                                    log.warn("情绪分析异常 sessionId={}", finalSessionId, ex);
                                }
                            }
                        });

            } catch (Exception e) {
                log.error("流式聊天处理异常", e);
                sendErrorEvent(emitter, "处理请求时出错: " + e.getMessage());
            }
        });

        return emitter;
    }

    /**
     * 获取会话的情绪分析结果
     * GET /api/psychological-chat/sessions/{sessionId}/emotion
     */
    @GetMapping("/psychological-chat/sessions/{sessionId}/emotion")
    public Result<Map<String, Object>> getSessionEmotion(@PathVariable String sessionId) {
        LambdaQueryWrapper<SessionEmotion> query = new LambdaQueryWrapper<>();
        query.eq(SessionEmotion::getSessionId, sessionId);
        SessionEmotion emotion = sessionEmotionMapper.selectOne(query);

        Map<String, Object> result = new HashMap<>();
        if (emotion == null) {
            result.put("primaryEmotion", "中性");
            result.put("emotionScore", 50);
            result.put("isNegative", false);
            result.put("riskLevel", 0);
            result.put("suggestion", "情绪状态平稳");
            result.put("improvementSuggestions", new ArrayList<>());
            result.put("riskDescription", "");
            return Result.success(result);
        }

        result.put("primaryEmotion", emotion.getPrimaryEmotionName() != null ? emotion.getPrimaryEmotionName() : "中性");
        // 数据库1-10分 → 前端需要的1-100分
        result.put("emotionScore", emotion.getEmotionScore() != null ? emotion.getEmotionScore() * 10 : 50);
        result.put("isNegative", emotion.getIsNegative() != null && emotion.getIsNegative() == 1);
        result.put("riskLevel", emotion.getRiskLevel() != null ? emotion.getRiskLevel() : 0);
        result.put("suggestion", emotion.getSuggestion() != null ? emotion.getSuggestion() : "情绪状态平稳");

        // improvement_suggestions 逗号分隔 → 数组
        List<String> improvements = new ArrayList<>();
        if (emotion.getImprovementSuggestions() != null && !emotion.getImprovementSuggestions().isBlank()) {
            String[] parts = emotion.getImprovementSuggestions().split(",");
            for (String part : parts) {
                improvements.add(part.trim());
            }
        }
        result.put("improvementSuggestions", improvements);
        result.put("riskDescription", emotion.getRiskDescription() != null ? emotion.getRiskDescription() : "");

        return Result.success(result);
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data("error:" + message));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }

    /**
     * 判断用户是否为管理员（优先从 JWT claims 判断，兼容旧 token 则查数据库）
     */
    private boolean isAdminUser(Claims claims, Integer userId) {
        // 先看 token 中是否有 userType
        Integer userType = claims.get("userType", Integer.class);
        if (userType != null) {
            return userType == 2;
        }
        // 旧 token 没有 userType，从数据库查询（兼容性 fallback）
        if (userId != null) {
            com.mentalhealth.assistant.entity.User user = userMapper.selectById(userId);
            if (user != null && user.getUserType() != null) {
                return user.getUserType() == 2;
            }
        }
        return false;
    }
}
