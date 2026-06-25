package com.aicreator.service;

import com.aicreator.config.AiProperties;
import com.aicreator.config.ContentProperties;
import com.aicreator.core.*;
import com.aicreator.model.Article;
import com.aicreator.model.HotspotItem;
import com.aicreator.model.OptimizeResult;
import com.aicreator.model.Topic;
import com.aicreator.util.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyWorkflowService {

    private final AiClient ai;
    private final HotspotCollector hotspotCollector;
    private final TopicGenerator topicGenerator;
    private final ArticleWriter articleWriter;
    private final ContentOptimizer contentOptimizer;
    private final Publisher publisher;
    private final Analytics analytics;
    private final ContentProperties contentProps;
    private final AiProperties aiProps;

    public void runDaily() {
        log.info("=".repeat(50));
        log.info("🚀 AI 自媒体内容工厂 - 每日工作流");
        log.info("=".repeat(50));

        // 1. 采集热点
        log.info("\n📡 步骤 1: 采集热点...");
        List<HotspotItem> hotList = hotspotCollector.fetchAll();
        if (hotList.isEmpty()) {
            log.warn("⚠️ 热点采集为空，使用默认选题");
            hotList = List.of(new HotspotItem() {{ setTitle("35岁程序员的出路在哪里"); setSource("default"); }});
        }

        // 2. 生成选题
        log.info("\n💡 步骤 2: AI 生成选题...");
        List<Topic> topics = topicGenerator.generate(hotList, contentProps.getDailyTopicCount());
        for (int i = 0; i < topics.size(); i++) {
            Topic t = topics.get(i);
            log.info("  {}. {} | 角度: {}", i + 1, t.getTopic(), t.getAngle());
        }

        // 3. 选择最佳选题并写作
        Topic bestTopic = topics.isEmpty() ? new Topic() : topics.get(0);
        String topicStr = bestTopic.getTopic() != null ? bestTopic.getTopic() : "默认选题";
        log.info("\n✍️ 步骤 3: 写作选题 - {}", topicStr);

        Article article = articleWriter.write(topicStr, "weitoutiao", "story");
        log.info("  标题候选: {}", article.getTitles().isEmpty() ? "无" : article.getTitles().subList(0, Math.min(3, article.getTitles().size())));
        log.info("  字数: {}", article.getWordCount());

        // 4. 内容优化
        log.info("\n🔧 步骤 4: 内容优化...");
        OptimizeResult optimized = contentOptimizer.optimize(article.getContent(), "40岁互联网老兵");
        log.info("  敏感词检测: {}", optimized.getSensitiveCheck().isPassed() ? "通过" : "未通过");
        log.info("  改写相似度: {:.2f}", optimized.getOriginalityScore());

        String finalContent = contentOptimizer.addCta(optimized.getFinalContent(), "comment");
        List<String> keywords = contentOptimizer.extractKeywords(finalContent);

        // 5. 输出预览
        log.info("\n📄 最终文章预览:");
        log.info("-".repeat(50));
        log.info("{}", finalContent.length() > 500 ? finalContent.substring(0, 500) + "..." : finalContent);
        log.info("-".repeat(50));
        log.info("\n🏷️ 推荐标签: {}", String.join(", ", keywords));

        // 6. 模拟发布
        log.info("\n📤 步骤 5: 发布（模拟）...");
        Map<String, Object> result = publisher.publish(article, "weitoutiao");
        log.info("  发布结果: {}", result.get("status"));

        // 7. 记录分析
        analytics.record(Map.of(
                "topic", topicStr,
                "platform", "weitoutiao",
                "wordCount", finalContent.length()
        ));

        log.info("\n✅ 工作流完成！");
    }

    public void runSingle(String topic) {
        if (topic == null || topic.isBlank()) {
            List<HotspotItem> hotList = hotspotCollector.fetchAll();
            topic = hotList.isEmpty() ? "默认选题" : hotList.get(0).getTitle();
            log.info("使用热点: {}", topic);
        }
        Article article = articleWriter.write(topic, "weitoutiao", "story");
        OptimizeResult optimized = contentOptimizer.optimize(article.getContent(), "40岁互联网老兵");
        String finalContent = contentOptimizer.addCta(optimized.getFinalContent(), "comment");
        log.info("\n{}\n", finalContent);
    }

    public void runReport() {
        Map<String, Object> report = analytics.dailyReport(7);
        log.info("📊 近7日数据复盘:");
        try {
            log.info("\n{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
        } catch (Exception e) {
            log.error("输出报告失败", e);
        }
        List<String> suggestions = analytics.suggestTopics();
        log.info("\n💡 选题建议:");
        suggestions.forEach(s -> log.info("  - {}", s));
    }
}
