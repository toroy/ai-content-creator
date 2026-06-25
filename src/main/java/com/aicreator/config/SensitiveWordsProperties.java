package com.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "sensitive-words")
public class SensitiveWordsProperties {
    private List<String> words = new ArrayList<>();
}
