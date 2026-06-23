package com.mentalhealth.assistant.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

    /** 交换机名称 */
    public static final String EXCHANGE = "ai.mental.health.direct";

    /** 情绪分析队列 */
    public static final String QUEUE_EMOTION_ANALYSIS = "emotion.analysis.queue";
    public static final String ROUTING_EMOTION_ANALYSIS = "emotion.analysis";

    /** 阅读量更新队列 */
    public static final String QUEUE_READ_COUNT = "read.count.queue";
    public static final String ROUTING_READ_COUNT = "read.count";

    @Bean
    public DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue emotionAnalysisQueue() {
        return QueueBuilder.durable(QUEUE_EMOTION_ANALYSIS).build();
    }

    @Bean
    public Queue readCountQueue() {
        return QueueBuilder.durable(QUEUE_READ_COUNT).build();
    }

    @Bean
    public Binding emotionAnalysisBinding(Queue emotionAnalysisQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(emotionAnalysisQueue)
                .to(directExchange)
                .with(ROUTING_EMOTION_ANALYSIS);
    }

    @Bean
    public Binding readCountBinding(Queue readCountQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(readCountQueue)
                .to(directExchange)
                .with(ROUTING_READ_COUNT);
    }
}
