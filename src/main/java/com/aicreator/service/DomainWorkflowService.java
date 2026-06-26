package com.aicreator.service;

import com.aicreator.config.DomainProperties;
import com.aicreator.core.*;
import com.aicreator.model.Article;
import com.aicreator.model.HotspotItem;
import com.aicreator.model.OptimizeResult;
import com.aicreator.model.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainWorkflowService {

    private final CollectorFactory collectorFactory;
    private final TopicGenerator topicGenerator;
    private final ArticleWriter articleWriter;
    private final ContentOptimizer contentOptimizer;
    private final Publisher publisher;
    private final Analytics analytics;

    public void execute(DomainProperties.DomainDefinition domain) {
        String label = domain.getLabel() != null ? domain.getLabel() : domain.toString();
        log.info("=".repeat(40));
        log.info("🚀 领域 [{}] 工作流启动", label);
        log.info("=".repeat(40));

        try {
            // 1. 采集内容
            log.info("[{}] 📡 步骤 1: 采集内容...", label);
            ContentCollector collector = collectorFactory.get(domain.getCollector());
            List<HotspotItem> hotList = collector.collect(domain);
            if (hotList.isEmpty()) {
                log.warn("[{}] ⚠️ 内容采集为空，使用历史热门选题兜底", label);
                hotList = List.of(createFallbackItem());
            }

            // 2. 生成选题
            log.info("[{}] 💡 步骤 2: AI 生成选题...", label);
            List<Topic> topics = topicGenerator.generate(hotList, domain);
            for (int i = 0; i < topics.size(); i++) {
                Topic t = topics.get(i);
                log.info("[{}]   {}. {} | 角度: {}", label, i + 1, t.getTopic(), t.getAngle());
            }

            // 3-5. 逐篇写作、优化、发布
            List<String> succeededArticles = new ArrayList<>();
            for (Topic topic : topics) {
                try {
                    log.info("[{}] ✍️ 写作: {}", label, topic.getTopic());
                    Article article = articleWriter.write(topic.getTopic(), domain);

                    log.info("[{}] 🔧 优化: {}", label, topic.getTopic());
                    List<String> domainSensitiveWords = domain.getExcludeKeywords() != null
                            ? domain.getExcludeKeywords() : List.of();
                    OptimizeResult optimized = contentOptimizer.optimize(
                            article.getContent(), domain.getPersona(), domainSensitiveWords);

                    String ctaType = domain.getCtaType() != null ? domain.getCtaType() : "comment";
                    String finalContent = contentOptimizer.addCta(optimized.getFinalContent(), ctaType);

                    // 更新 article 内容
                    article.setContent(finalContent);

                    // 发布到各平台
                    for (String platform : domain.getPlatforms()) {
                        log.info("[{}] 📤 发布到 {}: {}", label, platform, topic.getTopic());
                        Map<String, Object> result = publisher.publish(article, platform, domain.getLabel());
                        log.info("[{}]   发布结果: {}", label, result.get("status"));
                    }

                    // 记录分析
                    analytics.record(domain.getLabel(), domain.getPlatforms().get(0),
                            topic.getTopic(), finalContent.length(), Map.of());

                    succeededArticles.add(topic.getTopic());
                } catch (Exception e) {
                    log.error("[{}] 文章「{}」处理失败: {}", label, topic.getTopic(), e.getMessage());
                }
            }

            log.info("[{}] ✅ 完成，成功 {} 篇/共 {} 篇", label, succeededArticles.size(), topics.size());
        } catch (Exception e) {
            log.error("[{}] ❌ 领域工作流失败: {}", label, e.getMessage(), e);
        }
    }

    private HotspotItem createFallbackItem() {
        HotspotItem item = new HotspotItem();
        item.setTitle("今日热门话题");
        item.setSource("fallback");
        return item;
    }
}
