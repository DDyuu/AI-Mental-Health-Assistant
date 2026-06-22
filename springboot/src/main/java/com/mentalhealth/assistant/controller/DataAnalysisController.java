package com.mentalhealth.assistant.controller;

import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.entity.EmotionDiary;
import com.mentalhealth.assistant.entity.Session;
import com.mentalhealth.assistant.entity.User;
import com.mentalhealth.assistant.mapper.EmotionDiaryMapper;
import com.mentalhealth.assistant.mapper.SessionMapper;
import com.mentalhealth.assistant.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/data-analysis")
public class DataAnalysisController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @GetMapping("/analytics/overview")
    public Result<Map<String, Object>> getAnalyticsOverview() {
        Map<String, Object> result = new HashMap<>();

        // 1. System Overview
        Map<String, Object> systemOverview = buildSystemOverview();
        result.put("systemOverview", systemOverview);

        // 2. Emotion Trend (最近 7 天)
        List<Map<String, Object>> emotionTrend = buildEmotionTrend();
        result.put("emotionTrend", emotionTrend);

        // 3. Consultation Stats
        Map<String, Object> consultationStats = buildConsultationStats();
        result.put("consultationStats", consultationStats);

        // 4. User Activity (最近 7 天)
        List<Map<String, Object>> userActivity = buildUserActivity();
        result.put("userActivity", userActivity);

        return Result.success(result);
    }

    private Map<String, Object> buildSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 总用户数
        Long totalUsers = userMapper.selectCount(null);
        overview.put("totalUsers", totalUsers);

        // 活跃用户（最近 7 天有会话或日记的用户）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeUsers = sessionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                        .ge(Session::getCreateTime, sevenDaysAgo)
        );
        // 取日记用户去重
        List<EmotionDiary> recentDiaries = emotionDiaryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmotionDiary>()
                        .ge(EmotionDiary::getCreateTime, sevenDaysAgo)
                        .select(EmotionDiary::getUserId)
        );
        long diaryUserCount = recentDiaries.stream()
                .map(EmotionDiary::getUserId)
                .distinct()
                .count();
        // 活跃用户 = max(会话活跃, 日记用户)
        overview.put("activeUsers", Math.max(activeUsers, diaryUserCount) > 0 ? Math.max(activeUsers, diaryUserCount) : 0);

        // 总情绪日志数
        Long totalDiaries = emotionDiaryMapper.selectCount(null);
        overview.put("totalDiaries", totalDiaries);

        // 今日新增情绪日志
        LocalDate today = LocalDate.now();
        long todayNewDiaries = emotionDiaryMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmotionDiary>()
                        .ge(EmotionDiary::getCreateTime, today.atStartOfDay())
        );
        overview.put("todayNewDiaries", todayNewDiaries);

        // 总咨询会话数
        Long totalSessions = sessionMapper.selectCount(null);
        overview.put("totalSessions", totalSessions);

        // 今日新增会话
        long todayNewSessions = sessionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                        .ge(Session::getCreateTime, today.atStartOfDay())
        );
        overview.put("todayNewSessions", todayNewSessions);

        // 平均情绪评分（所有日记的平均 moodScore）
        List<EmotionDiary> allDiariesWithScore = emotionDiaryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmotionDiary>()
                        .isNotNull(EmotionDiary::getMoodScore)
                        .select(EmotionDiary::getMoodScore)
        );
        double avgMoodScore = allDiariesWithScore.stream()
                .mapToInt(EmotionDiary::getMoodScore)
                .average()
                .orElse(0.0);
        overview.put("avgMoodScore", BigDecimal.valueOf(avgMoodScore)
                .setScale(1, RoundingMode.HALF_UP).doubleValue());

        return overview;
    }

    private List<Map<String, Object>> buildEmotionTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 最近 7 天
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<EmotionDiary> dayDiaries = emotionDiaryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmotionDiary>()
                            .ge(EmotionDiary::getCreateTime, dayStart)
                            .lt(EmotionDiary::getCreateTime, dayEnd)
                            .isNotNull(EmotionDiary::getMoodScore)
                            .select(EmotionDiary::getMoodScore)
            );

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            dayData.put("recordCount", dayDiaries.size());
            dayData.put("avgMoodScore", dayDiaries.stream()
                    .mapToInt(EmotionDiary::getMoodScore)
                    .average()
                    .orElse(0.0));

            trend.add(dayData);
        }

        return trend;
    }

    private Map<String, Object> buildConsultationStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总会话数
        Long totalSessions = sessionMapper.selectCount(null);
        stats.put("totalSessions", totalSessions);

        // 平均时长（分钟）
        List<Session> sessionsWithDuration = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                        .isNotNull(Session::getDurationMinutes)
                        .select(Session::getDurationMinutes)
        );
        double avgDuration = sessionsWithDuration.stream()
                .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .average()
                .orElse(0.0);
        stats.put("avgDurationMinutes", BigDecimal.valueOf(avgDuration)
                .setScale(1, RoundingMode.HALF_UP).doubleValue());

        // 每日趋势（最近 7 天）
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<Session> daySessions = sessionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                            .ge(Session::getCreateTime, dayStart)
                            .lt(Session::getCreateTime, dayEnd)
                            .select(Session::getId, Session::getUserId)
            );

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            dayData.put("sessionCount", daySessions.size());
            dayData.put("userCount", daySessions.stream()
                    .map(Session::getUserId)
                    .distinct()
                    .count());

            dailyTrend.add(dayData);
        }
        stats.put("dailyTrend", dailyTrend);

        return stats;
    }

    private List<Map<String, Object>> buildUserActivity() {
        List<Map<String, Object>> activity = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            // 当天新增用户
            Long newUsers = userMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .ge(User::getCreateTime, dayStart)
                            .lt(User::getCreateTime, dayEnd)
            );

            // 当天有日记的用户数
            List<EmotionDiary> dayDiaries = emotionDiaryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmotionDiary>()
                            .ge(EmotionDiary::getCreateTime, dayStart)
                            .lt(EmotionDiary::getCreateTime, dayEnd)
                            .select(EmotionDiary::getUserId)
            );
            long diaryUsers = dayDiaries.stream()
                    .map(EmotionDiary::getUserId)
                    .distinct()
                    .count();

            // 当天有咨询的用户数
            List<Session> daySessions = sessionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Session>()
                            .ge(Session::getCreateTime, dayStart)
                            .lt(Session::getCreateTime, dayEnd)
                            .select(Session::getUserId)
            );
            long consultationUsers = daySessions.stream()
                    .map(Session::getUserId)
                    .distinct()
                    .count();

            // 活跃用户 = 当天有日记或咨询的用户数
            Set<Integer> activeUserSet = new HashSet<>();
            dayDiaries.stream().map(EmotionDiary::getUserId).forEach(activeUserSet::add);
            daySessions.stream().map(Session::getUserId).forEach(activeUserSet::add);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            dayData.put("activeUsers", (long) activeUserSet.size());
            dayData.put("newUsers", newUsers);
            dayData.put("diaryUsers", diaryUsers);
            dayData.put("consultationUsers", consultationUsers);

            activity.add(dayData);
        }

        return activity;
    }
}
