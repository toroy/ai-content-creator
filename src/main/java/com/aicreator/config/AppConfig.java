package com.aicreator.config;

import com.aicreator.core.ContentOptimizer;
import com.aicreator.util.AiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AiProperties.class, ContentProperties.class, HotspotProperties.class, SensitiveWordsProperties.class})
public class AppConfig {

    @Bean
    public ContentOptimizer contentOptimizer(AiClient ai, SensitiveWordsProperties sensitiveWordsProps) {
        return new ContentOptimizer(ai, sensitiveWordsProps.getWords());
    }
}
