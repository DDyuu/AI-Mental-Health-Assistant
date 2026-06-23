package com.mentalhealth.assistant.mq;

import java.io.Serializable;

public class ReadCountMessage implements Serializable {

    private String articleId;

    public ReadCountMessage() {}

    public ReadCountMessage(String articleId) {
        this.articleId = articleId;
    }

    public String getArticleId() { return articleId; }
    public void setArticleId(String articleId) { this.articleId = articleId; }
}
