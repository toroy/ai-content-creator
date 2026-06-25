package com.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "content")
public class ContentProperties {
    private String platform = "weitoutiao";
    private String targetAudience = "30-50岁男性，互联网从业者";
    private int dailyTopicCount = 5;
    private Map<String, LengthRange> articleLength;

    @Data
    public static class LengthRange {
        private int min;
        private int max;
    }
}
