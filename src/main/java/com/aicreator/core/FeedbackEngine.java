package com.aicreator.core;

import com.aicreator.util.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FeedbackEngine {

    private final AiClient ai;
    private final ObjectMapper mapper;
    private final File historyFile;
    private final File tipsFile;
    private final List<FeedbackCollector> collectors;

    public FeedbackEngine(AiClient ai,
                          @Value("${app.data-dir}") String dataDir,
                          List<FeedbackCollector> collectors) {
        this.ai = ai;
        this.mapper = new ObjectMapper();
        this.collectors = collectors != null ? collectors : List.of();
        File dir = new File(dataDir);
        dir.mkdirs();
        this.historyFile = new File(dir, "publish_history.json");
        this.tipsFile = new File(dir, "optimization_tips.json");
    }

    /**
     * 检查是否需要触发分析（每 30 篇文章或距上次分析 7 天）
     */
    public void checkAndAnalyze() {
        List<Map<String, Object>> history = loadHistory();
        if (history.isEmpty()) return;

        // 读取上次分析时间
        Map<String, Object> tips = loadTips();
        String lastAnalysis = (String) tips.getOrDefault("lastAnalysis", "");
        LocalDateTime lastTime = lastAnalysis.isEmpty() ? null
                : LocalDateTime.parse(lastAnalysis);

        // 统计上次分析后的新文章数
        long newCount = history.stream()
                .filter(h -> {
                    String t = (String) h.getOrDefault("recordTime", "");
                    if (lastTime == null) return true;
                    return LocalDateTime.parse(t).isAfter(lastTime);
                })
                .count();

        boolean shouldAnalyze = newCount >= 30 ||
                (lastTime != null && lastTime.isBefore(LocalDateTime.now().minusDays(7)));

        if (shouldAnalyze) {
            log.info("触发反馈分析 (上次分析后新增 {} 篇文章)", newCount);
            analyze();
        } else {
            log.debug("跳过反馈分析 (上次分析后 {} 篇，未到触发条件)", newCount);
        }
    }

    @SuppressWarnings("unchecked")
    private void analyze() {
        List<Map<String, Object>> history = loadHistory();
        if (history.size() < 5) {
            log.info("历史文章不足 5 篇，跳过分析");
            return;
        }

        // 构建分析数据
        StringBuilder data = new StringBuilder();
        for (Map<String, Object> h : history) {
            data.append(String.format("领域:%s 平台:%s 选题:%s 字数:%s\n",
                    h.getOrDefault("domain", "?"),
                    h.getOrDefault("platform", "?"),
                    h.getOrDefault("topic", "?"),
                    h.getOrDefault("wordCount", 0)));
        }

        String prompt = String.format("""
                分析以下文章发布记录，找出：
                1. 每个领域表现最好的选题类型（总结规律，如"职场吐槽类"、"热点解读类"）
                2. 表现较差的内容共性
                3. 各领域的优化建议（选题方向、标题策略、风格调整）
                输出 JSON：
                {"tipsByDomain":{"领域名":"优化建议"},
                 "bestTopicTypes":["类型1","类型2"],
                 "generalAdvice":"整体建议"}
                文章记录（最近 %d 篇）：
                %s
                """, history.size(), data.toString());

        String result = ai.chat(prompt, 0.5, 2000);
        try {
            Map<String, Object> tips = new HashMap<>(mapper.readValue(result, Map.class));
            tips.put("lastAnalysis", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            tips.put("articleCount", history.size());
            mapper.writerWithDefaultPrettyPrinter().writeValue(tipsFile, tips);
            log.info("优化建议已保存到 {}", tipsFile);
        } catch (Exception e) {
            log.error("反馈分析 JSON 解析失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadHistory() {
        if (!historyFile.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(historyFile, List.class);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadTips() {
        if (!tipsFile.exists()) return new HashMap<>();
        try {
            return mapper.readValue(tipsFile, Map.class);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public void printReport() {
        Map<String, Object> tips = loadTips();
        log.info("📊 优化建议:");
        try {
            log.info("\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tips));
        } catch (Exception e) {
            log.error("输出报告失败", e);
        }
    }

    /** 获取某个领域的最新优化建议文本 */
    @SuppressWarnings("unchecked")
    public String getDomainTips(String domainName) {
        Map<String, Object> tips = loadTips();
        Object domainTips = tips.getOrDefault("tipsByDomain", Map.of());
        if (domainTips instanceof Map) {
            Map<String, Object> tipsMap = (Map<String, Object>) domainTips;
            Object value = tipsMap.get(domainName);
            return value instanceof String ? (String) value : "";
        }
        return "";
    }
}
