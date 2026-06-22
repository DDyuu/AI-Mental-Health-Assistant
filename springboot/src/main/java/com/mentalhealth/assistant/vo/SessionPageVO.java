package com.mentalhealth.assistant.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionPageVO {
    private String id;
    private String userNickname;
    private String sessionTitle;
    private String lastMessageContent;
    private Integer messageCount;
    private LocalDateTime lastMessageTime;
}
