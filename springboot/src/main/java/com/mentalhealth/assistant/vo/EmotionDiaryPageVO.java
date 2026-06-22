package com.mentalhealth.assistant.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmotionDiaryPageVO {
    private Integer id;
    private Integer userId;
    private String username;
    private String nickname;
    private String diaryDate;
    private Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    private Integer sleepQuality;
    private Integer stressLevel;
    private String aiEmotionAnalysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
