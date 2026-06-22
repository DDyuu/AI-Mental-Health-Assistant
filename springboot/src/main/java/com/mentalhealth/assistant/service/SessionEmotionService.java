package com.mentalhealth.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mentalhealth.assistant.entity.SessionEmotion;
import com.mentalhealth.assistant.mapper.SessionEmotionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SessionEmotionService extends ServiceImpl<SessionEmotionMapper, SessionEmotion> {

    @Autowired
    private SessionEmotionMapper sessionEmotionMapper;

    /**
     * 获取情绪总览统计
     */
    public Map<String, Object> getEmotionOverview(Integer days) {
        String startDate = LocalDate.now().minusDays(days).toString();
        return sessionEmotionMapper.emotionOverview(startDate);
    }

    /**
     * 获取每日情绪趋势
     */
    public List<Map<String, Object>> getDailyTrend(Integer days) {
        String startDate = LocalDate.now().minusDays(days).toString();
        return sessionEmotionMapper.dailyEmotionTrend(startDate);
    }

    /**
     * 获取情绪类型分布
     */
    public List<Map<String, Object>> getEmotionTypeDistribution(Integer days) {
        String startDate = LocalDate.now().minusDays(days).toString();
        return sessionEmotionMapper.countByEmotionType(startDate);
    }

    /**
     * 获取用户的情绪花园记录
     */
    public List<SessionEmotion> getUserEmotionRecords(Integer userId, Integer days) {
        LambdaQueryWrapper<SessionEmotion> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(SessionEmotion::getSessionId,
                "SELECT id FROM session WHERE user_id = " + userId);
        if (days != null && days > 0) {
            wrapper.ge(SessionEmotion::getCreateTime,
                    LocalDate.now().minusDays(days).atStartOfDay());
        }
        wrapper.orderByDesc(SessionEmotion::getCreateTime);
        return sessionEmotionMapper.selectList(wrapper);
    }
}
