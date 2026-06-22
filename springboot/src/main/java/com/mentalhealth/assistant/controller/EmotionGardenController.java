package com.mentalhealth.assistant.controller;

import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.common.ResultCode;
import com.mentalhealth.assistant.entity.SessionEmotion;
import com.mentalhealth.assistant.exception.BusinessException;
import com.mentalhealth.assistant.service.SessionEmotionService;
import com.mentalhealth.assistant.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emotion-garden")
public class EmotionGardenController {

    @Autowired
    private SessionEmotionService sessionEmotionService;

    /**
     * 情绪总览统计
     * GET /api/emotion-garden/overview?days=30
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "30") Integer days) {
        Map<String, Object> data = sessionEmotionService.getEmotionOverview(days);
        return Result.success(data);
    }

    /**
     * 每日情绪趋势
     * GET /api/emotion-garden/trend?days=7
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") Integer days) {
        List<Map<String, Object>> data = sessionEmotionService.getDailyTrend(days);
        return Result.success(data);
    }

    /**
     * 情绪类型分布
     * GET /api/emotion-garden/distribution?days=30
     */
    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> distribution(@RequestParam(defaultValue = "30") Integer days) {
        List<Map<String, Object>> data = sessionEmotionService.getEmotionTypeDistribution(days);
        return Result.success(data);
    }

    /**
     * 获取当前用户的情绪记录列表
     * GET /api/emotion-garden/my-records?days=7
     */
    @GetMapping("/my-records")
    public Result<List<SessionEmotion>> myRecords(
            @RequestParam(required = false) Integer days,
            @RequestHeader("Token") String token) {
        Claims claims = JwtUtil.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的令牌");
        }
        List<SessionEmotion> records = sessionEmotionService.getUserEmotionRecords(userId, days);
        return Result.success(records);
    }
}
