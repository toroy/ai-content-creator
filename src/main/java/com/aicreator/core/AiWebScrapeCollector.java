package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;
import com.aicreator.util.AiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiWebScrapeCollector implements ContentCollector {

    private final AiClient ai;
    private final ObjectMapper mapper;

    @Override
    public List<HotspotItem> collect(DomainProperties.DomainDefinition domain) {
        List<HotspotItem> allItems = new ArrayList<>();
        List<DomainProperties.SourceConfig> sources = domain.getSources();
        if (sources == null || sources.isEmpty()) {
            log.warn("[{}] 未配置网页源", domain.getLabel());
            return allItems;
        }

        for (DomainProperties.SourceConfig source : sources) {
            try {
                List<HotspotItem> items = scrapeOne(source);
                allItems.addAll(items);
                log.info("[{}] {} 采集到 {} 条", domain.getLabel(), source.getUrl(), items.size());
            } catch (Exception e) {
                log.error("[{}] 采集失败 {}: {}", domain.getLabel(), source.getUrl(), e.getMessage());
            }
        }
        log.info("[{}] AI网页采集共获取 {} 条", domain.getLabel(), allItems.size());
        return allItems;
    }

    private List<HotspotItem> scrapeOne(DomainProperties.SourceConfig source) {
        WebClient client = WebClient.create();
        String html = client.get().uri(source.getUrl())
                .header("User-Agent", "Mozilla/5.0 (compatible; AiContentCreator/1.0)")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (html == null || html.isBlank()) {
            return List.of();
        }

        // 去掉 script/style 标签，保留文本
        String cleaned = html.replaceAll("(?s)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?s)<style[^>]*>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // 截取前 8000 字符
        String truncated = cleaned.length() > 8000 ? cleaned.substring(0, 8000) : cleaned;

        String prompt = String.format("""
            从以下网页文本内容中，提取所有新闻/文章条目的标题和链接（如果有）。
            输出 JSON 数组格式：
            [{"title":"标题","url":"链接","excerpt":"摘要(最多100字)"}]
            只提取新闻类条目，忽略导航、广告、版权声明等。
            网页内容：
            %s
            """, truncated);

        String result = ai.chat(prompt, 0.2, 2000);
        try {
            return mapper.readValue(result, new TypeReference<List<HotspotItem>>() {});
        } catch (Exception e) {
            log.error("AI 网页提取 JSON 解析失败: {}", e.getMessage());
            return List.of();
        }
    }
}
