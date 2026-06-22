package com.mentalhealth.assistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mentalhealth.assistant.entity.Article;
import com.mentalhealth.assistant.vo.ArticlePageVO;

import java.util.Map;

public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章
     * @param params 查询参数（currentPage, pageSize, title, category, status）
     * @return 分页结果（仅含 id, categoryId, categoryName, title, authorName）
     */
    IPage<ArticlePageVO> getArticlePage(Map<String, Object> params);

    /**
     * 保存或更新文章
     * 若 article.id 存在则更新，否则新增
     */
    void saveArticle(Article article);
}
