package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotFilterCollector implements ContentCollector {

    private final HotspotCollector hotspotCollector;

    @Override
    public List<HotspotItem> collect(DomainProperties.DomainDefinition domain) {
        // 1. 从现有热榜采集
        List<HotspotItem> allHot = hotspotCollector.fetchAll();
        log.info("[{}] 热榜原始数据 {} 条", domain.getLabel(), allHot.size());

        List<String> keywords = domain.getKeywords();
        List<String> excludeKeywords = domain.getExcludeKeywords();

        if (keywords == null || keywords.isEmpty()) {
            log.info("[{}] 未配置关键词，返回全部热榜数据", domain.getLabel());
            return allHot;
        }

        // 2. 关键词匹配过滤 + 排除过滤 + 按匹配度排序
        List<HotspotItem> filtered = allHot.stream()
                .filter(item -> {
                    // 排除关键词检查
                    if (excludeKeywords != null && !excludeKeywords.isEmpty()) {
                        for (String ek : excludeKeywords) {
                            if (item.getTitle() != null && item.getTitle().contains(ek)) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .filter(item -> keywords.stream().anyMatch(kw ->
                        item.getTitle() != null && item.getTitle().contains(kw)))
                .sorted(Comparator.comparingInt((HotspotItem item) -> {
                    // 按匹配关键词数量降序
                    return -(int) keywords.stream()
                            .filter(kw -> item.getTitle() != null && item.getTitle().contains(kw))
                            .count();
                }))
                .toList();

        log.info("[{}] 关键词过滤后 {} 条", domain.getLabel(), filtered.size());
        return filtered;
    }
}
