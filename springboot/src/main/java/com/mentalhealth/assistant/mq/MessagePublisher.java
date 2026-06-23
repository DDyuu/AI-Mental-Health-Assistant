package com.mentalhealth.assistant.mq;

import com.mentalhealth.assistant.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** 发送情绪分析消息 */
    public void sendEmotionAnalysis(String sessionId, String conversationContent) {
        EmotionAnalysisMessage msg = new EmotionAnalysisMessage(sessionId, conversationContent);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_EMOTION_ANALYSIS, msg);
        log.info("已发送情绪分析消息 sessionId={}", sessionId);
    }

    /** 发送阅读量更新消息 */
    public void sendReadCount(String articleId) {
        ReadCountMessage msg = new ReadCountMessage(articleId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_READ_COUNT, msg);
    }
}
