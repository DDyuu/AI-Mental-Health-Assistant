package com.mentalhealth.assistant.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 流式调用 DeepSeek API，推送结果给前端 emitter
     */
    public void streamChat(String systemPrompt, String userMessage,
                           SseEmitter emitter, Consumer<String> onComplete) {
        try {
            String requestBody = buildRequestBody(systemPrompt, userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            log.error("DeepSeek API 错误: status={}", response.statusCode());
                            sendData(emitter, "error:AI服务暂时不可用 (状态码: " + response.statusCode() + ")");
                            return;
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.body(), "UTF-8"))) {

                            String line;
                            StringBuilder fullContent = new StringBuilder();

                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();

                                    if ("[DONE]".equals(data)) {
                                        break;
                                    }

                                    String content = extractContent(data);
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        // 直接发送 data: 行，不使用 event 命名
                                        sendData(emitter, content);
                                    }
                                }
                            }

                            String fullText = fullContent.toString();
                            if (onComplete != null) {
                                onComplete.accept(fullText);
                            }

                            // 发送完成标记
                            sendData(emitter, "[DONE]");
                            emitter.complete();

                        } catch (IOException e) {
                            log.error("读取流响应失败", e);
                            sendData(emitter, "error:AI响应读取失败");
                        }
                    })
                    .exceptionally(e -> {
                        log.error("调用 DeepSeek API 异常", e);
                        sendData(emitter, "error:AI服务调用失败，请检查API密钥配置");
                        return null;
                    });

        } catch (Exception e) {
            log.error("发起 DeepSeek 请求失败", e);
            sendData(emitter, "error:请求失败，请稍后重试");
        }
    }

    private void sendData(SseEmitter emitter, @NonNull String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException e) {
            log.debug("发送SSE数据失败: {}", e.getMessage());
        }
    }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        return String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                    ],
                    "stream": true,
                    "temperature": 0.8,
                    "max_tokens": 2048
                }
                """, model, escapeJson(systemPrompt), escapeJson(userMessage));
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    String extractContent(String jsonData) {
        try {
            String searchKey = "\"content\":\"";
            int start = jsonData.indexOf(searchKey);
            if (start == -1) return "";
            start += searchKey.length();
            StringBuilder content = new StringBuilder();
            int end = start;
            while (end < jsonData.length()) {
                char c = jsonData.charAt(end);
                if (c == '\\') {
                    if (end + 1 < jsonData.length()) {
                        char next = jsonData.charAt(end + 1);
                        if (next == 'n') content.append('\n');
                        else if (next == 'r') content.append('\r');
                        else if (next == 't') content.append('\t');
                        else if (next == '"') content.append('"');
                        else if (next == '\\') content.append('\\');
                        else content.append(next);
                        end += 2;
                    } else {
                        end++;
                    }
                } else if (c == '"') {
                    break;
                } else {
                    content.append(c);
                    end++;
                }
            }
            return content.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
