package com.mentalhealth.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("category_name")
    private String categoryName;

    private String description;

    @TableField("parent_id")
    private Integer parentId;

    private Integer sort;

    private Integer status;

    @TableField("article_count")
    private Integer articleCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Category> children;
}
