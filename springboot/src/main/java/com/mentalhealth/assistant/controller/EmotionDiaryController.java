package com.mentalhealth.assistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.common.ResultCode;
import com.mentalhealth.assistant.entity.EmotionDiary;
import com.mentalhealth.assistant.exception.BusinessException;
import com.mentalhealth.assistant.service.EmotionDiaryService;
import com.mentalhealth.assistant.util.JwtUtil;
import com.mentalhealth.assistant.vo.EmotionDiaryPageVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmotionDiaryController {

    @Autowired
    private EmotionDiaryService emotionDiaryService;

    @GetMapping("/emotion-diary/admin/page")
    public Result<IPage<EmotionDiaryPageVO>> getEmotionPage(@RequestParam Map<String, Object> params) {
        IPage<EmotionDiaryPageVO> page = emotionDiaryService.getEmotionPage(params);
        return Result.success(page);
    }

    @DeleteMapping("/emotion-diary/admin/{id}")
    public Result<Void> deleteEmotion(@PathVariable Integer id) {
        emotionDiaryService.deleteEmotion(id);
        return Result.success();
    }

    @PostMapping("/emotion-diary")
    public Result<EmotionDiary> addEmotionDiary(
            @RequestBody EmotionDiary diary,
            @RequestHeader("Token") String token) {
        Claims claims = JwtUtil.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的令牌");
        }
        diary.setUserId(userId);
        EmotionDiary saved = emotionDiaryService.addEmotionDiary(diary);
        return Result.success(saved);
    }
}
