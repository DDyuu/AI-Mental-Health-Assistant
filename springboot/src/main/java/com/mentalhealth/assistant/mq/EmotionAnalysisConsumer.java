package com.mentalhealth.assistant.mq;

import com.mentalhealth.assistant.config.RabbitMQConfig;
import com.mentalhealth.assistant.service.ai.EmotionAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class EmotionAnalysisConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmotionAnalysisConsumer.class);

    @Autowired
    private EmotionAnalysisService emotionAnalysisService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMOTION_ANALYSIS)
    public void handleEmotionAnalysis(EmotionAnalysisMessage message) {
        log.info("收到情绪分析消息 sessionId={}", message.getSessionId());
        try {
            emotionAnalysisService.analyzeAndSave(
                    message.getSessionId(),
                    message.getConversationContent()
            );
            log.info("情绪分析完成 sessionId={}", message.getSessionId());
        } catch (Exception e) {
            log.error("情绪分析失败 sessionId={}", message.getSessionId(), e);
        }
    }
}
