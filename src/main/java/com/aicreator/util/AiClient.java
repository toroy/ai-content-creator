package com.aicreator.util;

import com.aicreator.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final AiProperties props;
    private final ObjectMapper mapper;

    private WebClient getClient() {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String chat(String prompt, double temperature, int maxTokens) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", props.getModel());
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);

            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", prompt);

            String response = getClient().post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = mapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("[AiClient] 请求失败: {}", e.getMessage());
            return "";
        }
    }

    public String chat(String prompt) {
        return chat(prompt, props.getTemperature(), props.getMaxTokens());
    }

    public List<String> optimizeTitles(String content, int count) {
        String prompt = String.format("""
            为以下微头条内容生成 %d 个标题：
            内容摘要：%s
            要求：
            - 前3个：悬念型（不说透，勾好奇心）
            - 中3个：数字型（带具体数据）
            - 后3个：情绪型（愤怒/共鸣/焦虑）
            - 最后1个：反问型
            只输出标题列表，每行一个。
            """, count, content.substring(0, Math.min(content.length(), 500)));

        String result = chat(prompt, 0.9, 1000);
        return result.lines().filter(line -> !line.isBlank()).toList();
    }

    public String humanize(String aiContent, String persona) {
        String prompt = String.format("""
            请将以下 AI 生成的内容，改写成一位"%s"的口语化表达：
            改写要求：
            1. 加入个人经历（如"我前公司..."、"我去年..."）
            2. 加入口头禅（如"说实话"、"说白了"、"你品品"）
            3. 加入情绪词（如"离谱"、"扎心"、"绝了"）
            4. 长短句交错，不要太工整，允许语法不严谨
            5. 适当使用 emoji 和网络用语
            6. 保留核心信息，但表达方式要随意、真实
            原文：
            %s
            """, persona, aiContent);
        return chat(prompt, 0.8, props.getMaxTokens());
    }
}
