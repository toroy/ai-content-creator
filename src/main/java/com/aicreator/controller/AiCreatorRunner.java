package com.aicreator.controller;

import com.aicreator.service.DailyWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiCreatorRunner implements CommandLineRunner {

    private final DailyWorkflowService workflowService;

    @Override
    public void run(String... args) {
        String mode = args.length > 0 ? args[0] : "daily";
        switch (mode) {
            case "daily" -> workflowService.runDaily();
            case "single" -> {
                String topic = args.length > 1 ? args[1] : "";
                workflowService.runSingle(topic);
            }
            case "report" -> workflowService.runReport();
            default -> log.info("Usage: java -jar ai-content-creator.jar [daily|single <topic>|report]");
        }
    }
}
