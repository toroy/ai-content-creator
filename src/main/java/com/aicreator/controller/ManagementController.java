package com.aicreator.controller;

import com.aicreator.service.DailyOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ManagementController {

    private final DailyOrchestrator orchestrator;

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
        orchestrator.runAllAsync();
        return ResponseEntity.ok(Map.of("status", "accepted", "message", "多领域每日工作流已触发"));
    }

    @PostMapping("/workflow/single")
    public ResponseEntity<Map<String, String>> triggerSingle(
            @RequestParam(defaultValue = "default") String domain,
            @RequestParam(defaultValue = "") String topic) {
        orchestrator.runSingle(domain, topic);
        return ResponseEntity.ok(Map.of("status", "accepted", "message", "单篇文章已触发"));
    }

    @GetMapping("/workflow/report")
    public ResponseEntity<Map<String, String>> report() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "报告请查看控制台输出"));
    }
}
