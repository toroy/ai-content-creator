package com.aicreator.core;

import com.aicreator.model.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class Publisher {

    private final Map<String, PlatformPublisher> publishers = new HashMap<>();

    public Publisher() {
        publishers.put("weitoutiao", new WeitoutiaoPublisher());
        publishers.put("maimai", new MaimaiPublisher());
        publishers.put("zhihu", new ZhihuPublisher());
    }

    public Map<String, Object> publish(Article article, String platform) {
        PlatformPublisher pub = publishers.get(platform);
        if (pub == null) {
            return Map.of("status", "error", "msg", "不支持的平台: " + platform);
        }
        return pub.publish(article);
    }

    private interface PlatformPublisher {
        Map<String, Object> publish(Article article);
    }

    private static class WeitoutiaoPublisher implements PlatformPublisher {
        @Override
        public Map<String, Object> publish(Article article) {
            log.info("[微头条] 模拟发布: {}", article.getTitles().isEmpty() ? "无标题" : article.getTitles().get(0));
            return Map.of("status", "simulated", "url", "https://example.com/weitoutiao/123");
        }
    }

    private static class MaimaiPublisher implements PlatformPublisher {
        @Override
        public Map<String, Object> publish(Article article) {
            log.info("[脉脉] 模拟发布: {}", article.getTitles().isEmpty() ? "无标题" : article.getTitles().get(0));
            return Map.of("status", "simulated", "url", "https://example.com/maimai/456");
        }
    }

    private static class ZhihuPublisher implements PlatformPublisher {
        @Override
        public Map<String, Object> publish(Article article) {
            log.info("[知乎] 模拟发布: {}", article.getTitles().isEmpty() ? "无标题" : article.getTitles().get(0));
            return Map.of("status", "simulated", "url", "https://example.com/zhihu/789");
        }
    }
}
