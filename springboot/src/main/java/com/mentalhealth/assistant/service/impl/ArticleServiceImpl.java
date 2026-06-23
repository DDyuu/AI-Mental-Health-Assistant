package com.mentalhealth.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mentalhealth.assistant.entity.Article;
import com.mentalhealth.assistant.entity.Category;
import com.mentalhealth.assistant.mapper.ArticleMapper;
import com.mentalhealth.assistant.mapper.CategoryMapper;
import com.mentalhealth.assistant.service.ArticleService;
import com.mentalhealth.assistant.vo.ArticlePageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public IPage<ArticlePageVO> getArticlePage(Map<String, Object> params) {
        // 分页参数
        long currentPage = params.get("currentPage") != null ?
                Long.parseLong(params.get("currentPage").toString()) : 1;
        long pageSize = params.get("pageSize") != null ?
                Long.parseLong(params.get("pageSize").toString()) :
                (params.get("size") != null ? Long.parseLong(params.get("size").toString()) : 10);

        // 构建查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        // 标题模糊搜索
        String title = params.get("title") != null ? params.get("title").toString() : null;
        if (StringUtils.hasText(title)) {
            wrapper.like(Article::getTitle, title);
        }

        // 分类筛选
        String category = params.get("category") != null ? params.get("category").toString() : null;
        if (StringUtils.hasText(category)) {
            wrapper.eq(Article::getCategoryId, category);
        }

        // 状态筛选
        String status = params.get("status") != null ? params.get("status").toString() : null;
        if (StringUtils.hasText(status)) {
            wrapper.eq(Article::getStatus, status);
        }

        // 排序
        String sortField = params.get("sortField") != null ? params.get("sortField").toString() : null;
        String sortDirection = params.get("sortDirection") != null ? params.get("sortDirection").toString() : null;

        if ("readCount".equals(sortField)) {
            if ("asc".equalsIgnoreCase(sortDirection)) {
                wrapper.orderByAsc(Article::getReadCount);
            } else {
                wrapper.orderByDesc(Article::getReadCount);
            }
        } else if ("publice".equals(sortField) || "publish".equals(sortField)) {
            // publice 是前端已知的拼写，按更新时间排序
            if ("asc".equalsIgnoreCase(sortDirection)) {
                wrapper.orderByAsc(Article::getUpdateTime);
            } else {
                wrapper.orderByDesc(Article::getUpdateTime);
            }
        } else {
            // 默认按创建时间倒序
            wrapper.orderByDesc(Article::getCreateTime);
        }

        // 查询字段
        wrapper.select(Article::getId, Article::getCategoryId, Article::getTitle,
                Article::getAuthorName, Article::getCoverImage, Article::getReadCount,
                Article::getCreateTime, Article::getUpdateTime, Article::getStatus);

        // 执行分页查询
        IPage<Article> page = baseMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);

        // 构建分类名映射
        List<Category> allCategories = categoryMapper.selectList(null);
        Map<Integer, String> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getCategoryName, (a, b) -> a));

        // 转换为 VO
        List<ArticlePageVO> voList = page.getRecords().stream().map(a -> {
            ArticlePageVO vo = new ArticlePageVO();
            vo.setId(a.getId());
            vo.setCategoryId(a.getCategoryId());
            vo.setCategoryName(categoryMap.get(a.getCategoryId()));
            vo.setTitle(a.getTitle());
            vo.setAuthorName(a.getAuthorName());
            vo.setCoverImage(a.getCoverImage());
            vo.setReadCount(a.getReadCount());
            vo.setCreateTime(a.getCreateTime());
            vo.setUpdateTime(a.getUpdateTime());
            vo.setStatus(a.getStatus());
            return vo;
        }).collect(Collectors.toList());

        // 构造分页 VO 结果
        Page<ArticlePageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Cacheable(value = "article:detail", key = "#id")
    public Article getArticleDetail(String id) {
        return baseMapper.selectById(id);
    }

    @Override
    @CacheEvict(value = "article:detail", key = "#article.id", condition = "#article.id != null")
    public void saveArticle(Article article) {
        if (article.getId() != null && !article.getId().isEmpty()) {
            // 编辑：更新已有文章
            baseMapper.updateById(article);
        } else {
            // 新增：设置默认值
            article.setId(UUID.randomUUID().toString().replace("-", ""));
            article.setStatus(0);       // 默认草稿
            article.setReadCount(0);    // 默认阅读量 0
            baseMapper.insert(article);
        }
    }
}
