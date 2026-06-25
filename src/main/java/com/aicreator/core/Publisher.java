package com.aicreator.core;

import com.aicreator.model.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class Publisher {

    private final Map<String, PlatformPublisher> publishers = new HashMap<>();
    private final String outputDir;
    private final String webhookUrl;

    public Publisher(@Value("${app.publishing.output-dir:${user.home}/.ai-content-creator/output}") String outputDir,
                     @Value("${app.publishing.webhook-url:}") String webhookUrl) {
        this.outputDir = outputDir;
        this.webhookUrl = webhookUrl;
        publishers.put("weitoutiao", new WeitoutiaoPublisher());
        publishers.put("maimai", new MaimaiPublisher());
        publishers.put("zhihu", new ZhihuPublisher());
    }

    public Map<String, Object> publish(Article article, String platform) {
        PlatformPublisher pub = publishers.get(platform);
        if (pub == null) {
            return Map.of("status", "error", "msg", "不支持的平台: " + platform);
        }
        return pub.publish(article);
    }

    private interface PlatformPublisher {
        Map<String, Object> publish(Article article);
    }

    private abstract class BasePublisher implements PlatformPublisher {
        @Override
        public Map<String, Object> publish(Article article) {
            String title = article.getTitles().isEmpty() ? "无标题" : article.getTitles().get(0);
            log.info("[{}] 生成RPA发布文件: {}", getPlatform(), title);

            String fileName = writeArticleFile(article);
            log.info("[{}] 文件已写入: {}", getPlatform(), fileName);

            notifyWebhook(fileName, getPlatform());

            return Map.of("status", "ready", "file", fileName);
        }

        protected abstract String getPlatform();

        private String writeArticleFile(Article article) {
            String platform = getPlatform();
            String title = article.getTitles().isEmpty() ? "无标题" : article.getTitles().get(0);
            String keywords = article.getKeywords() != null ? String.join(", ", article.getKeywords()) : "";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("%s_%s_%s.md", platform, timestamp, UUID.randomUUID().toString().substring(0, 8));

            File dir = new File(outputDir, platform);
            dir.mkdirs();

            StringBuilder sb = new StringBuilder();
            sb.append("---\n");
            sb.append("title: \"").append(title).append("\"\n");
            sb.append("platform: ").append(platform).append("\n");
            sb.append("keywords: ").append(keywords).append("\n");
            sb.append("created: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append("\n");
            sb.append("---\n\n");
            sb.append(article.getContent() != null ? article.getContent() : "");

            File file = new File(dir, fileName);
            try {
                Files.writeString(file.toPath(), sb.toString());
            } catch (IOException e) {
                log.error("[{}] 写入发布文件失败: {}", platform, e.getMessage());
            }
            return file.getAbsolutePath();
        }

        private void notifyWebhook(String fileName, String platform) {
            if (webhookUrl == null || webhookUrl.isBlank()) {
                return;
            }
            try {
                String json = String.format("{\"event\":\"article_ready\",\"platform\":\"%s\",\"file\":\"%s\"}", platform, fileName);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                log.info("[{}] Webhook 通知结果: {} {}", platform, resp.statusCode(), resp.body());
            } catch (Exception e) {
                log.warn("[{}] Webhook 通知失败: {}", platform, e.getMessage());
            }
        }
    }

    private class WeitoutiaoPublisher extends BasePublisher {
        @Override
        protected String getPlatform() { return "weitoutiao"; }
    }

    private class MaimaiPublisher extends BasePublisher {
        @Override
        protected String getPlatform() { return "maimai"; }
    }

    private class ZhihuPublisher extends BasePublisher {
        @Override
        protected String getPlatform() { return "zhihu"; }
    }
}
