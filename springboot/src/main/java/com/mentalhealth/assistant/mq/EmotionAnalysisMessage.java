package com.mentalhealth.assistant.mq;

import java.io.Serializable;

public class EmotionAnalysisMessage implements Serializable {

    private String sessionId;
    private String conversationContent;

    public EmotionAnalysisMessage() {}

    public EmotionAnalysisMessage(String sessionId, String conversationContent) {
        this.sessionId = sessionId;
        this.conversationContent = conversationContent;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getConversationContent() { return conversationContent; }
    public void setConversationContent(String conversationContent) { this.conversationContent = conversationContent; }
}
