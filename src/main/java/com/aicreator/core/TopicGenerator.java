package com.aicreator.core;

import com.aicreator.config.ContentProperties;
import com.aicreator.model.HotspotItem;
import com.aicreator.model.Topic;
import com.aicreator.util.AiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicGenerator {

    private final AiClient ai;
    private final ContentProperties contentProps;
    private final ObjectMapper mapper;

    public List<Topic> generate(List<HotspotItem> hotList, int count) {
        int targetCount = count > 0 ? count : contentProps.getDailyTopicCount();
        List<String> hotSummary = hotList.stream().limit(15)
                .map(h -> "{"title":"" + h.getTitle() + "","source":"" + h.getSource() + ""}")
                .toList();

        String prompt = String.format("""
            你是一位资深自媒体主编。根据以下热点，结合"%s"的偏好，
            筛选出 %d 个最适合微头条的选题，按"爆款概率"排序。
            要求：
            1. 有争议性、情绪共鸣或反常识
            2. 适合 300-800 字的短内容
            3. 能引发评论互动
            4. 优先选择职场、技术、社会观察类话题
            热点列表：%s
            请直接输出 JSON 格式：
            [{"rank":1,"topic":"选题标题","angle":"切入角度","emotion":"情绪点","interaction":"评论区引导话术"}]
            """, contentProps.getTargetAudience(), targetCount, hotSummary);

        String result = ai.chat(prompt, 0.8, 2000);
        try {
            return mapper.readValue(result, new TypeReference<List<Topic>>() {});
        } catch (Exception e) {
            log.error("选题 JSON 解析失败，降级处理: {}", e.getMessage());
            List<Topic> fallback = new ArrayList<>();
            for (int i = 0; i < Math.min(targetCount, hotList.size()); i++) {
                Topic t = new Topic();
                t.setRank(i + 1);
                t.setTopic(hotList.get(i).getTitle());
                t.setAngle("热点解读");
                t.setEmotion("共鸣");
                t.setInteraction("你怎么看？");
                fallback.add(t);
            }
            return fallback;
        }
    }
}
