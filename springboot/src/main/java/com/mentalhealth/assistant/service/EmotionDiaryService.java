package com.mentalhealth.assistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mentalhealth.assistant.entity.EmotionDiary;
import com.mentalhealth.assistant.vo.EmotionDiaryPageVO;

import java.util.Map;

public interface EmotionDiaryService extends IService<EmotionDiary> {

    IPage<EmotionDiaryPageVO> getEmotionPage(Map<String, Object> params);

    void deleteEmotion(Integer id);

    EmotionDiary addEmotionDiary(EmotionDiary diary);
}
