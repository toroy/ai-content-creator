package com.aicreator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class Analytics {

    private final ObjectMapper mapper = new ObjectMapper();
    private final File historyFile;

    public Analytics() {
        String dir = System.getProperty("user.dir") + "/src/main/resources/history";
        new File(dir).mkdirs();
        this.historyFile = new File(dir, "publish_history.json");
    }

    public void record(Map<String, Object> data) {
        List<Map<String, Object>> history = load();
        data.put("recordTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        history.add(data);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, history);
        } catch (IOException e) {
            log.error("保存历史记录失败: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> load() {
        if (!historyFile.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(historyFile, List.class);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public Map<String, Object> dailyReport(int days) {
        List<Map<String, Object>> history = load();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Map<String, Object>> recent = history.stream()
                .filter(h -> {
                    String time = (String) h.getOrDefault("recordTime", "");
                    return LocalDateTime.parse(time).isAfter(cutoff);
                }).toList();

        Map<String, Object> report = new HashMap<>();
        report.put("period", days + "天");
        report.put("totalArticles", recent.size());
        report.put("platforms", countByKey(recent, "platform"));
        report.put("topics", recent.stream().limit(5).map(h -> h.getOrDefault("topic", "")).toList());
        report.put("avgWordCount", recent.stream().mapToInt(h -> (int) h.getOrDefault("wordCount", 0)).average().orElse(0));
        return report;
    }

    private Map<String, Integer> countByKey(List<Map<String, Object>> list, String key) {
        Map<String, Integer> counts = new HashMap<>();
        for (Map<String, Object> item : list) {
            String val = (String) item.getOrDefault(key, "unknown");
            counts.put(val, counts.getOrDefault(val, 0) + 1);
        }
        return counts;
    }

    public List<String> suggestTopics() {
        Map<String, Object> report = dailyReport(7);
        Map<String, Integer> platforms = (Map<String, Integer>) report.getOrDefault("platforms", Map.of());
        List<String> suggestions = new ArrayList<>();
        platforms.forEach((p, c) -> suggestions.add(p + " 平台已发布 " + c + " 篇，建议尝试新角度"));
        return suggestions;
    }
}
