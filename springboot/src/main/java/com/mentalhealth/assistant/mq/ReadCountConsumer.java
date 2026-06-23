package com.mentalhealth.assistant.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mentalhealth.assistant.config.RabbitMQConfig;
import com.mentalhealth.assistant.entity.Article;
import com.mentalhealth.assistant.mapper.ArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class ReadCountConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReadCountConsumer.class);

    @Autowired
    private ArticleMapper articleMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_READ_COUNT)
    public void handleReadCount(ReadCountMessage message) {
        log.debug("收到阅读量更新消息 articleId={}", message.getArticleId());
        try {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, message.getArticleId())
                    .setSql("read_count = read_count + 1")
            );
        } catch (Exception e) {
            log.error("阅读量更新失败 articleId={}", message.getArticleId(), e);
        }
    }
}
