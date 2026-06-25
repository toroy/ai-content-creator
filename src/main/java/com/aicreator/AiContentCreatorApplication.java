package com.aicreator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AiContentCreatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiContentCreatorApplication.class, args);
        System.out.println("AI Content Creator 已启动");
        System.out.println("Usage: java -jar ai-content-creator.jar [daily|single|report]");
    }
}
