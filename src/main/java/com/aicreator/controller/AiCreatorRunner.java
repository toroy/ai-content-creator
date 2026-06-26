package com.aicreator.controller;

import com.aicreator.service.DailyOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiCreatorRunner implements CommandLineRunner {

    private final DailyOrchestrator orchestrator;

    @Override
    public void run(String... args) {
        String mode = args.length > 0 ? args[0] : "daily";
        switch (mode) {
            case "daily" -> orchestrator.runAll();
            case "single" -> {
                String domain = args.length > 1 ? args[1] : "default";
                String topic = args.length > 2 ? args[2] : "";
                orchestrator.runSingle(domain, topic);
            }
            case "report" -> orchestrator.runReport();
            default -> log.info("Usage: java -jar ai-content-creator.jar [daily|single <domain> <topic>|report]");
        }
    }
}
