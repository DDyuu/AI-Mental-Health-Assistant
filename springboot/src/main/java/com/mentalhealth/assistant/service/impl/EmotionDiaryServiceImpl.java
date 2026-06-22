package com.mentalhealth.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mentalhealth.assistant.entity.EmotionDiary;
import com.mentalhealth.assistant.entity.User;
import com.mentalhealth.assistant.mapper.EmotionDiaryMapper;
import com.mentalhealth.assistant.mapper.UserMapper;
import com.mentalhealth.assistant.service.EmotionDiaryService;
import com.mentalhealth.assistant.service.ai.EmotionAnalysisService;
import com.mentalhealth.assistant.vo.EmotionDiaryPageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmotionDiaryServiceImpl extends ServiceImpl<EmotionDiaryMapper, EmotionDiary> implements EmotionDiaryService {

    private static final Logger log = LoggerFactory.getLogger(EmotionDiaryServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmotionAnalysisService emotionAnalysisService;

    @Override
    public IPage<EmotionDiaryPageVO> getEmotionPage(Map<String, Object> params) {
        long currentPage = params.get("currentPage") != null ?
                Long.parseLong(params.get("currentPage").toString()) : 1;
        long pageSize = params.get("size") != null ?
                Long.parseLong(params.get("size").toString()) : 10;

        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();

        String userId = params.get("userId") != null ? params.get("userId").toString() : null;
        if (userId != null && !userId.isEmpty()) {
            wrapper.eq(EmotionDiary::getUserId, userId);
        }

        wrapper.orderByDesc(EmotionDiary::getCreateTime);

        IPage<EmotionDiary> page = baseMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);

        // 收集用户信息
        List<Integer> userIds = page.getRecords().stream()
                .map(EmotionDiary::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, User> userMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<EmotionDiaryPageVO> voList = page.getRecords().stream().map(d -> {
            EmotionDiaryPageVO vo = new EmotionDiaryPageVO();
            vo.setId(d.getId());
            vo.setUserId(d.getUserId());
            User user = userMap.get(d.getUserId());
            vo.setUsername(user != null ? user.getUsername() : null);
            vo.setNickname(user != null ? user.getNickname() : null);
            vo.setDiaryDate(d.getDiaryDate() != null ? d.getDiaryDate().toString() : null);
            vo.setMoodScore(d.getMoodScore());
            vo.setDominantEmotion(d.getDominantEmotion());
            vo.setEmotionTriggers(d.getEmotionTriggers());
            vo.setDiaryContent(d.getDiaryContent());
            vo.setSleepQuality(d.getSleepQuality());
            vo.setStressLevel(d.getStressLevel());
            vo.setAiEmotionAnalysis(d.getAiEmotionAnalysis());
            vo.setCreatedAt(d.getCreateTime());
            vo.setUpdatedAt(d.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<EmotionDiaryPageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void deleteEmotion(Integer id) {
        baseMapper.deleteById(id);
    }

    @Override
    public EmotionDiary addEmotionDiary(EmotionDiary diary) {
        diary.setId(null);

        // AI 分析日记内容
        if (diary.getDiaryContent() != null && !diary.getDiaryContent().isBlank()) {
            try {
                String analysis = emotionAnalysisService.analyzeDiaryContent(diary.getDiaryContent());
                diary.setAiEmotionAnalysis(analysis);
            } catch (Exception e) {
                log.warn("日记AI分析异常", e);
            }
        }

        baseMapper.insert(diary);
        return diary;
    }
}
