package com.mentalhealth.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("session")
public class Session {

    @TableId
    private String id;

    @TableField("user_id")
    private Integer userId;

    @TableField("user_nickname")
    private String userNickname;

    @TableField("session_title")
    private String sessionTitle;

    private String status;

    @TableField("last_message_content")
    private String lastMessageContent;

    @TableField("message_count")
    private Integer messageCount;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
