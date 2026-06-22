package com.mentalhealth.assistant.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentalhealth.assistant.entity.SessionEmotion;
import com.mentalhealth.assistant.mapper.SessionEmotionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmotionAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EmotionAnalysisService.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    @Autowired
    private SessionEmotionMapper sessionEmotionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final String ANALYSIS_PROMPT = """
            你是一位专业的情绪分析专家。请分析以下对话内容，识别用户的情绪状态。
            请严格按照 JSON 格式返回分析结果，不要包含任何其他文字：

            {
                "primaryEmotion": "主要情绪名称（如：焦虑、悲伤、快乐、愤怒、平静、困惑、孤独、压力等）",
                "emotionScore": "情绪评分，1-10分，分数越低越负面",
                "multiEmotions": "检测到的多种情绪，用中文逗号分隔（如：焦虑,压力,孤独）",
                "isNegative": "是否为负面情绪，1是0否",
                "riskLevel": "风险等级，0无风险 1低风险 2中风险 3高风险",
                "suggestion": "给用户的简短建议（一句话，温暖鼓励的语气）",
                "improvementSuggestions": "3条改善情绪的具体行动建议，用中文逗号分隔（如：冥想5分钟,散步20分钟,听音乐放松）",
                "riskDescription": "如果存在风险，描述风险情况，无风险则为空字符串"
            }
            """;

    private static final String DIARY_ANALYSIS_PROMPT = """
            你是一位专业的情绪数据分析师。请分析用户的情绪日记内容，以 JSON 格式输出结构化分析结果，供后台管理员查看。
            请严格按照 JSON 格式返回，不要包含任何其他文字：

            {
                "primaryEmotion": "核心情绪分类（如：焦虑、抑郁、快乐、愤怒、平静、悲伤、压力、迷茫等）",
                "emotionScore": "情绪强度分数，1-100分，分数越高情绪越强烈",
                "riskLevel": "风险等级，0无风险 1低风险 2中风险 3高风险",
                "isNegative": "是否为负面情绪，true是 false否",
                "suggestion": "针对该情绪的专业建议（一句话，面向管理员提供干预方向）",
                "riskDescription": "如果存在风险，详细描述风险情况；无风险则为空字符串",
                "improvementSuggestions": "3条改善建议，用JSON数组格式（如：[\"冥想5分钟\",\"散步20分钟\",\"听音乐放松\"]）"
            }
            """;

    /**
     * 分析对话情绪并保存到数据库
     */
    public SessionEmotion analyzeAndSave(String sessionId, String conversationContent) {
        try {
            // 调用 DeepSeek 分析
            String analysisResult = callDeepSeek(conversationContent);

            // 解析 JSON
            JsonNode json = objectMapper.readTree(analysisResult);

            // 构建实体
            SessionEmotion emotion = new SessionEmotion();
            emotion.setSessionId(sessionId);
            emotion.setPrimaryEmotionName(getText(json, "primaryEmotion"));
            emotion.setEmotionScore(getInt(json, "emotionScore"));
            emotion.setMultiEmotions(getText(json, "multiEmotions"));
            emotion.setIsNegative(getInt(json, "isNegative"));
            emotion.setRiskLevel(getInt(json, "riskLevel"));
            emotion.setSuggestion(getText(json, "suggestion"));
            emotion.setImprovementSuggestions(getText(json, "improvementSuggestions"));
            emotion.setRiskDescription(getText(json, "riskDescription"));

            // 保存到数据库（如果已存在则更新）
            LambdaQueryWrapper<SessionEmotion> query = new LambdaQueryWrapper<>();
            query.eq(SessionEmotion::getSessionId, sessionId);
            SessionEmotion existing = sessionEmotionMapper.selectOne(query);
            if (existing != null) {
                emotion.setId(existing.getId());
                sessionEmotionMapper.updateById(emotion);
            } else {
                sessionEmotionMapper.insert(emotion);
            }

            return emotion;

        } catch (Exception e) {
            log.error("情绪分析失败 sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 分析情绪日记内容，返回结构化 JSON 分析结果（供管理员后台查看）
     */
    public String analyzeDiaryContent(String diaryContent) {
        try {
            String requestBody = String.format("""
                    {
                        "model": "%s",
                        "messages": [
                            {"role": "system", "content": "%s"},
                            {"role": "user", "content": "请分析我的情绪日记：\\n\\n%s"}
                        ],
                        "stream": false,
                        "temperature": 0.3,
                        "max_tokens": 256
                    }
                    """, model, escapeJson(DIARY_ANALYSIS_PROMPT), escapeJson(truncateContent(diaryContent)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("DeepSeek API error: " + response.statusCode());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            String content = responseJson.get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

            return extractJson(content);

        } catch (Exception e) {
            log.error("日记AI分析失败", e);
            return """
                    {"primaryEmotion":"未知","emotionScore":50,"riskLevel":0,"isNegative":false,"suggestion":"AI分析暂时不可用","riskDescription":"","improvementSuggestions":["保持良好心态","适当运动","规律作息"]}
                    """;
        }
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private Integer getInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asInt() : null;
    }

    private String callDeepSeek(String conversationContent) throws Exception {
        String requestBody = String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "请分析以下对话内容中用户的情绪：\\n\\n%s"}
                    ],
                    "stream": false,
                    "temperature": 0.3,
                    "max_tokens": 512
                }
                """, model, escapeJson(ANALYSIS_PROMPT), escapeJson(truncateContent(conversationContent)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API error: " + response.statusCode() + " " + response.body());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson.get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText();

        // 提取 JSON 部分（AI 可能返回 markdown 代码块）
        return extractJson(content);
    }

    private String extractJson(String text) {
        // 尝试提取 ```json ... ``` 块
        int start = text.indexOf("```json");
        if (start != -1) {
            start += 7;
            int end = text.indexOf("```", start);
            if (end != -1) {
                return text.substring(start, end).trim();
            }
        }
        // 尝试提取 ``` ... ``` 块
        start = text.indexOf("```");
        if (start != -1) {
            start += 3;
            int end = text.indexOf("```", start);
            if (end != -1) {
                return text.substring(start, end).trim();
            }
        }
        // 直接返回
        return text.trim();
    }

    private String truncateContent(String content) {
        // 限制分析内容长度（取最近 2000 字）
        if (content.length() > 2000) {
            return "..." + content.substring(content.length() - 2000);
        }
        return content;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
