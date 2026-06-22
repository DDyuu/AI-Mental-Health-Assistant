package com.mentalhealth.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId
    private String id;

    private String title;

    private String content;

    @TableField("cover_image")
    private String coverImage;

    @TableField("category_id")
    private Integer categoryId;

    private String summary;

    private String tags;

    private Integer status;

    @TableField("author_name")
    private String authorName;

    @TableField("read_count")
    private Integer readCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
