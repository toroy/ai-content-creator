package com.aicreator.config;

import com.aicreator.core.ContentOptimizer;
import com.aicreator.util.AiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties({AiProperties.class, ContentProperties.class, HotspotProperties.class})
public class AppConfig {

    @Bean
    public ContentOptimizer contentOptimizer(AiClient ai, org.springframework.core.env.Environment env) {
        String words = env.getProperty("sensitive-words", "");
        List<String> sensitiveWords = List.of(words.split(","));
        return new ContentOptimizer(ai, sensitiveWords);
    }
}
