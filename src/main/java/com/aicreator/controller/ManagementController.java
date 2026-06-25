package com.aicreator.controller;

import com.aicreator.service.DailyWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ManagementController {

    private final DailyWorkflowService workflowService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AI Content Creator",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/workflow/daily")
    public ResponseEntity<Map<String, String>> triggerDaily() {
        workflowService.runDailyAsync();
        return ResponseEntity.ok(Map.of("status", "accepted", "message", "每日工作流已触发"));
    }

    @PostMapping("/workflow/single")
    public ResponseEntity<Map<String, String>> triggerSingle(@RequestParam(defaultValue = "") String topic) {
        workflowService.runSingleAsync(topic);
        return ResponseEntity.ok(Map.of("status", "accepted", "message", "单篇文章已触发"));
    }

    @GetMapping("/workflow/report")
    public ResponseEntity<Map<String, Object>> report() {
        return ResponseEntity.ok(workflowService.getReport());
    }
}
