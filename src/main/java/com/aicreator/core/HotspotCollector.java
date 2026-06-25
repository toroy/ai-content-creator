package com.aicreator.core;

import com.aicreator.config.HotspotProperties;
import com.aicreator.model.HotspotItem;
import com.fasterxml.jackson.databind.JsonNode;
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
public class HotspotCollector {

    private final HotspotProperties props;
    private final ObjectMapper mapper;
    private final WebClient webClient = WebClient.create();

    public List<HotspotItem> fetchAll() {
        List<HotspotItem> all = new ArrayList<>();
        if (props.getSources().get("zhihu") != null && props.getSources().get("zhihu").isEnabled()) {
            all.addAll(fetchZhihu());
        }
        if (props.getSources().get("weibo") != null && props.getSources().get("weibo").isEnabled()) {
            all.addAll(fetchWeibo());
        }
        log.info("[HotspotCollector] 共采集 {} 条热点", all.size());
        return all;
    }

    private List<HotspotItem> fetchZhihu() {
        List<HotspotItem> items = new ArrayList<>();
        try {
            String url = props.getSources().get("zhihu").getUrl();
            String resp = webClient.get().uri(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode root = mapper.readTree(resp);
            for (JsonNode item : root.path("data")) {
                JsonNode target = item.path("target");
                HotspotItem hi = new HotspotItem();
                hi.setSource("zhihu");
                hi.setTitle(target.path("title").asText());
                hi.setUrl(target.path("url").asText());
                hi.setHeat(target.path("metrics").asText());
                hi.setExcerpt(target.path("excerpt").asText("").substring(0, Math.min(100, target.path("excerpt").asText("").length())));
                items.add(hi);
            }
        } catch (Exception e) {
            log.error("[知乎热榜] 抓取失败: {}", e.getMessage());
        }
        return items;
    }

    private List<HotspotItem> fetchWeibo() {
        List<HotspotItem> items = new ArrayList<>();
        try {
            String url = props.getSources().get("weibo").getUrl();
            String resp = webClient.get().uri(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://weibo.com/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode root = mapper.readTree(resp);
            for (JsonNode item : root.path("data").path("realtime")) {
                HotspotItem hi = new HotspotItem();
                hi.setSource("weibo");
                hi.setTitle(item.path("word").asText());
                hi.setHeat(item.path("num").asText());
                hi.setCategory(item.path("category").asText());
                items.add(hi);
            }
        } catch (Exception e) {
            log.error("[微博热搜] 抓取失败: {}", e.getMessage());
        }
        return items;
    }

    public List<HotspotItem> filterByKeywords(List<HotspotItem> list, List<String> keywords) {
        return list.stream()
                .filter(item -> keywords.stream().anyMatch(kw -> item.getTitle().contains(kw)))
                .toList();
    }
}
