package com.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "domain")
public class DomainProperties {
    private Map<String, DomainDefinition> domains;

    @Data
    public static class DomainDefinition {
        private boolean enabled = true;
        private String label;
        private String collector = "hot_filter";
        private String persona = "资深自媒体人";
        private List<String> platforms = List.of("weitoutiao");
        private int articleCount = 3;
        private LengthRange length = new LengthRange();
        private List<SourceConfig> sources = new ArrayList<>();
        private List<String> keywords = new ArrayList<>();
        private List<String> excludeKeywords = new ArrayList<>();
        private StylePrefs stylePrefs = new StylePrefs();
        private String ctaType = "comment";
    }

    @Data
    public static class SourceConfig {
        private String url;
        private String type;
    }

    @Data
    public static class LengthRange {
        private int min = 100;
        private int max = 200;
    }

    @Data
    public static class StylePrefs {
        private double hookWeight = 0.4;
        private double emotionWeight = 0.3;
        private double utilityWeight = 0.3;
    }
}
