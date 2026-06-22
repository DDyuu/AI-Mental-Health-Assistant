package com.mentalhealth.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("session_message")
public class SessionMessage {

    @TableId
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("sender_type")
    private Integer senderType;

    private String content;

    @TableField("is_error")
    private Integer isError;

    @TableField("ai_model")
    private String aiModel;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
