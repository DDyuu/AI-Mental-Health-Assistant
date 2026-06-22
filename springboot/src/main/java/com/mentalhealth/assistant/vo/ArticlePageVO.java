package com.mentalhealth.assistant.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ArticlePageVO {
    private String id;
    private Integer categoryId;
    private String categoryName;
    private String title;
    private String authorName;
    private String coverImage;
    private Integer readCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status;
}
