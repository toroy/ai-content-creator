package com.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "hotspot")
public class HotspotProperties {
    private Map<String, SourceConfig> sources;

    @Data
    public static class SourceConfig {
        private String url;
        private boolean enabled;
    }
}
