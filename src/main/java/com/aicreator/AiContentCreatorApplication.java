package com.aicreator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiContentCreatorApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(AiContentCreatorApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 AI Content Creator 已启动");
        System.out.println("Usage: java -jar ai-content-creator.jar [daily|single|report]");
    }
}
