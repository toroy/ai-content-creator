package com.aicreator.service;

import com.aicreator.config.DomainProperties;
import com.aicreator.core.DomainRegistry;
import com.aicreator.core.FeedbackEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyOrchestrator {

    private final DomainRegistry domainRegistry;
    private final DomainWorkflowService domainWorkflow;
    private final FeedbackEngine feedbackEngine;

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(cron = "${scheduler.daily-cron}")
    public void scheduledDaily() {
        if (!schedulerEnabled) {
            log.debug("定时任务已禁用，跳过");
            return;
        }
        log.info("⏰ 定时任务触发多领域工作流");
        runAll();
    }

    /**
     * 并行执行所有启用领域
     */
    public void runAll() {
        List<DomainProperties.DomainDefinition> domains = domainRegistry.getEnabled();
        if (domains.isEmpty()) {
            log.warn("没有启用的领域，跳过");
            return;
        }

        log.info("=".repeat(50));
        log.info("🚀 多领域内容工厂启动 - {} 个领域", domains.size());
        domains.forEach(d -> log.info("   ▪ {} (采集: {}, 平台: {})",
                d.getLabel(), d.getCollector(), d.getPlatforms()));
        log.info("=".repeat(50));

        // 并行执行所有领域，等待全部完成
        List<CompletableFuture<Void>> futures = domains.stream()
                .map(d -> CompletableFuture.runAsync(() -> domainWorkflow.execute(d)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("\n🎉 所有领域工作流完成");

        // 检查是否需要触发反馈分析
        feedbackEngine.checkAndAnalyze();
    }

    @Async
    public void runAllAsync() {
        runAll();
    }

    /**
     * CLI 单篇模式：指定领域和主题
     */
    public void runSingle(String domainName, String topic) {
        DomainProperties.DomainDefinition domain = domainRegistry.getByName(domainName)
                .orElseGet(() -> {
                    // 回退：创建临时领域定义
                    DomainProperties.DomainDefinition fallback = new DomainProperties.DomainDefinition();
                    fallback.setLabel(domainName);
                    fallback.setPersona("资深自媒体人");
                    fallback.setPlatforms(List.of("weitoutiao"));
                    fallback.setArticleCount(1);
                    fallback.setCtaType("comment");
                    DomainProperties.LengthRange length = new DomainProperties.LengthRange();
                    length.setMin(100);
                    length.setMax(200);
                    fallback.setLength(length);
                    return fallback;
                });

        log.info("单篇模式: 领域={}, 主题={}", domain.getLabel(), topic);

        // 临时覆盖 articleCount 为 1
        domain.setArticleCount(1);

        // 采集 + 直接以给定 topic 写作
        domainWorkflow.execute(domain);
    }

    public void runReport() {
        // 委托给 Analytics（通过 FeedbackEngine 暴露）
        feedbackEngine.printReport();
    }
}
