package com.mentalhealth.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mentalhealth.assistant.entity.SessionEmotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SessionEmotionMapper extends BaseMapper<SessionEmotion> {

    @Select("SELECT primary_emotion_name as emotionType, COUNT(*) as count " +
            "FROM session_emotion WHERE create_time >= #{startDate} " +
            "GROUP BY primary_emotion_name ORDER BY count DESC")
    List<Map<String, Object>> countByEmotionType(@Param("startDate") String startDate);

    @Select("SELECT DATE(create_time) as date, " +
            "AVG(emotion_score) as avgScore, " +
            "COUNT(*) as count " +
            "FROM session_emotion " +
            "WHERE create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> dailyEmotionTrend(@Param("startDate") String startDate);

    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN is_negative = 1 THEN 1 ELSE 0 END) as negativeCount, " +
            "SUM(CASE WHEN is_negative = 0 THEN 1 ELSE 0 END) as positiveCount " +
            "FROM session_emotion " +
            "WHERE create_time >= #{startDate}")
    Map<String, Object> emotionOverview(@Param("startDate") String startDate);
}
