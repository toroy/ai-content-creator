package com.aicreator.core;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorFactory {

    private final AiWebScrapeCollector aiWebScrapeCollector;
    private final RssCollector rssCollector;
    private final HotFilterCollector hotFilterCollector;

    private final Map<String, ContentCollector> collectors = new HashMap<>();

    @PostConstruct
    void init() {
        collectors.put("ai_web_scrape", aiWebScrapeCollector);
        collectors.put("rss_feed", rssCollector);
        collectors.put("hot_filter", hotFilterCollector);
    }

    public ContentCollector get(String collectorType) {
        ContentCollector c = collectors.get(collectorType);
        if (c == null) {
            log.warn("未知采集器类型: {}，回退到 hot_filter", collectorType);
            return hotFilterCollector;
        }
        return c;
    }
}
