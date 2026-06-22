package com.mentalhealth.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("session_emotion")
public class SessionEmotion {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("session_id")
    private String sessionId;

    @TableField("primary_emotion_name")
    private String primaryEmotionName;

    @TableField("emotion_score")
    private Integer emotionScore;

    @TableField("multi_emotions")
    private String multiEmotions;

    @TableField("is_negative")
    private Integer isNegative;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("suggestion")
    private String suggestion;

    @TableField("improvement_suggestions")
    private String improvementSuggestions;

    @TableField("risk_description")
    private String riskDescription;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
