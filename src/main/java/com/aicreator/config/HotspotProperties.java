package com.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "hotspot")
public class HotspotProperties {
    private Map<String, SourceConfig> sources;

    @Data
    public static class SourceConfig {
        private String url;
        private boolean enabled;
    }
}
