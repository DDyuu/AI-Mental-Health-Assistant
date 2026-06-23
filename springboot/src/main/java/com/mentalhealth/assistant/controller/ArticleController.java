package com.mentalhealth.assistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.entity.Article;
import com.mentalhealth.assistant.entity.Category;
import com.mentalhealth.assistant.mapper.CategoryMapper;
import com.mentalhealth.assistant.service.ArticleService;
import com.mentalhealth.assistant.util.JwtUtil;
import com.mentalhealth.assistant.mq.MessagePublisher;
import com.mentalhealth.assistant.vo.ArticlePageVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired(required = false)
    private MessagePublisher messagePublisher;

    @GetMapping("/article/page")
    public Result<IPage<ArticlePageVO>> getArticlePage(@RequestParam Map<String, Object> params) {
        IPage<ArticlePageVO> page = articleService.getArticlePage(params);
        return Result.success(page);
    }

    @PostMapping("/article")
    public Result<String> saveArticle(@RequestBody Article article,
                                       @RequestHeader("Token") String token) {
        // 从 Token 解析当前用户
        Claims claims = JwtUtil.parseToken(token);
        String username = claims.getSubject();
        article.setAuthorName(username);

        articleService.saveArticle(article);
        return Result.success(article.getId());
    }

    @GetMapping("/article/{id}")
    public Result<Map<String, Object>> getArticleDetail(@PathVariable String id) {
        Article article = articleService.getArticleDetail(id);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            return Result.error(404, "文章不存在或已下架");
        }

        // 阅读量 +1（通过 RabbitMQ 异步更新，不可用时直接更新）
        if (messagePublisher != null) {
            messagePublisher.sendReadCount(id);
        } else {
            article.setReadCount(article.getReadCount() == null ? 1 : article.getReadCount() + 1);
            articleService.updateById(article);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", article.getId());
        result.put("title", article.getTitle());
        result.put("content", article.getContent());
        result.put("coverImage", article.getCoverImage());
        result.put("categoryId", article.getCategoryId());
        // 查询分类名称
        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            result.put("categoryName", category != null ? category.getCategoryName() : null);
        } else {
            result.put("categoryName", null);
        }
        result.put("summary", article.getSummary());
        result.put("tags", article.getTags());
        result.put("status", article.getStatus());
        result.put("createTime", article.getCreateTime());
        result.put("updateTime", article.getUpdateTime());
        result.put("authorName", article.getAuthorName());
        result.put("readCount", article.getReadCount());

        // tags 逗号分隔字符串转数组（给前端 tagArray 使用）
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            result.put("tagArray", Arrays.asList(article.getTags().split(",")));
        } else {
            result.put("tagArray", List.of());
        }

        return Result.success(result);
    }

    @PutMapping("/article/{id}/status")
    @CacheEvict(value = "article:detail", key = "#id")
    public Result<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.error(400, "状态不能为空");
        }
        Article article = new Article();
        article.setId(id);
        article.setStatus(status);
        articleService.updateById(article);
        return Result.success();
    }

    @DeleteMapping("/article/{id}")
    @CacheEvict(value = "article:detail", key = "#id")
    public Result<Void> deleteArticle(@PathVariable String id) {
        articleService.removeById(id);
        return Result.success();
    }
}
