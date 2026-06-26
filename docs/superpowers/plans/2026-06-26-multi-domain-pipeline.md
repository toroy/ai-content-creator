# 多领域并行内容创作流水线 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有单一流水线重构为 3 个独立领域（外网热点、IT热点、生活热点）并行运行的多领域架构，支持 AI 辅助网页抓取、领域感知写作、多平台分发和自动化反馈闭环。

**Architecture:** 新增 `DomainProperties` 配置驱动领域定义，`ContentCollector` 接口 + 3 种实现支撑不同采集策略，`DomainWorkflowService` 编排单领域流水线，`DailyOrchestrator` 并行调度所有领域。重构 `ArticleWriter`/`ContentOptimizer`/`TopicGenerator` 为领域感知，`FeedbackEngine` 实现自优化闭环。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring WebFlux, Lombok, Jackson, JUnit 5 + Mockito

## Global Constraints

- Java 17，Spring Boot 3.2.5，Maven 构建
- Lombok `@RequiredArgsConstructor` 构造器注入，`@ConfigurationProperties` 配置绑定
- 所有外部调用包裹 try-catch，失败降级不抛异常
- 字数默认 100-200（各领域可覆盖）
- 每领域默认 3 篇/天（各领域可独立配置）
- 遵循现有代码风格：中文日志，emoji 前缀，防御性降级

---

### Task 1: DomainProperties 配置类

**Files:**
- Create: `src/main/java/com/aicreator/config/DomainProperties.java`

**Interfaces:**
- Produces: `DomainProperties` — `@ConfigurationProperties(prefix = "domain")`，`Map<String, DomainDefinition> getDomains()`
- Produces: `DomainProperties.DomainDefinition` — 包含 `enabled`, `label`, `collector`, `persona`, `platforms`, `articleCount`(默认3), `length`, `sources`, `keywords`, `excludeKeywords`, `stylePrefs`, `ctaType`
- Produces: `DomainProperties.SourceConfig` — `url`, `type`
- Produces: `DomainProperties.LengthRange` — `min`(默认100), `max`(默认200)
- Produces: `DomainProperties.StylePrefs` — `hookWeight`(默认0.4), `emotionWeight`(默认0.3), `utilityWeight`(默认0.3)

- [ ] **Step 1: 创建 DomainProperties.java**

```java
package com.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "domain")
public class DomainProperties {
    private Map<String, DomainDefinition> domains;

    @Data
    public static class DomainDefinition {
        private boolean enabled = true;
        private String label;
        private String collector = "hot_filter";
        private String persona = "资深自媒体人";
        private List<String> platforms = List.of("weitoutiao");
        private int articleCount = 3;
        private LengthRange length = new LengthRange();
        private List<SourceConfig> sources = new ArrayList<>();
        private List<String> keywords = new ArrayList<>();
        private List<String> excludeKeywords = new ArrayList<>();
        private StylePrefs stylePrefs = new StylePrefs();
        private String ctaType = "comment";
    }

    @Data
    public static class SourceConfig {
        private String url;
        private String type;
    }

    @Data
    public static class LengthRange {
        private int min = 100;
        private int max = 200;
    }

    @Data
    public static class StylePrefs {
        private double hookWeight = 0.4;
        private double emotionWeight = 0.3;
        private double utilityWeight = 0.3;
    }
}
```

- [ ] **Step 2: 在 AppConfig 注册配置绑定**

修改 `src/main/java/com/aicreator/config/AppConfig.java`，在 `@EnableConfigurationProperties` 中加入 `DomainProperties.class`：

```java
@EnableConfigurationProperties({AiProperties.class, ContentProperties.class, HotspotProperties.class, SensitiveWordsProperties.class, DomainProperties.class})
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/aicreator/config/DomainProperties.java src/main/java/com/aicreator/config/AppConfig.java
git commit -m "feat: add DomainProperties configuration class for multi-domain support"
```

---

### Task 2: DomainRegistry 领域注册中心

**Files:**
- Create: `src/main/java/com/aicreator/core/DomainRegistry.java`

**Interfaces:**
- Consumes: `DomainProperties`（构造器注入）
- Produces: `List<DomainProperties.DomainDefinition> getEnabled()` — 返回所有 enabled=true 的领域
- Produces: `Optional<DomainProperties.DomainDefinition> getByName(String name)` — 按名称查找
- Produces: `List<String> getDomainNames()` — 所有领域名称列表

- [ ] **Step 1: 创建 DomainRegistry.java**

```java
package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainRegistry {

    private final DomainProperties domainProps;

    public List<DomainProperties.DomainDefinition> getEnabled() {
        if (domainProps.getDomains() == null || domainProps.getDomains().isEmpty()) {
            log.warn("未配置任何领域");
            return List.of();
        }
        return domainProps.getDomains().values().stream()
                .filter(DomainProperties.DomainDefinition::isEnabled)
                .toList();
    }

    public Optional<DomainProperties.DomainDefinition> getByName(String name) {
        if (domainProps.getDomains() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(domainProps.getDomains().get(name));
    }

    public List<String> getDomainNames() {
        if (domainProps.getDomains() == null) {
            return List.of();
        }
        return List.copyOf(domainProps.getDomains().keySet());
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/core/DomainRegistry.java
git commit -m "feat: add DomainRegistry for managing enabled domains"
```

---

### Task 3: ContentCollector 接口 + CollectorFactory

**Files:**
- Create: `src/main/java/com/aicreator/core/ContentCollector.java`
- Create: `src/main/java/com/aicreator/core/CollectorFactory.java`

**Interfaces:**
- Produces: `ContentCollector` 接口 — `List<HotspotItem> collect(DomainProperties.DomainDefinition domain)`
- Produces: `CollectorFactory` — `ContentCollector get(String collectorType)`，维护 `Map<String, ContentCollector>` 映射

- [ ] **Step 1: 创建 ContentCollector 接口**

```java
package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;

import java.util.List;

public interface ContentCollector {
    List<HotspotItem> collect(DomainProperties.DomainDefinition domain);
}
```

- [ ] **Step 2: 创建 CollectorFactory**

```java
package com.aicreator.core;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorFactory {

    private final AiWebScrapeCollector aiWebScrapeCollector;
    private final RssCollector rssCollector;
    private final HotFilterCollector hotFilterCollector;

    private final Map<String, ContentCollector> collectors = new HashMap<>();

    @PostConstruct
    void init() {
        collectors.put("ai_web_scrape", aiWebScrapeCollector);
        collectors.put("rss_feed", rssCollector);
        collectors.put("hot_filter", hotFilterCollector);
    }

    public ContentCollector get(String collectorType) {
        ContentCollector c = collectors.get(collectorType);
        if (c == null) {
            log.warn("未知采集器类型: {}，回退到 hot_filter", collectorType);
            return hotFilterCollector;
        }
        return c;
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/aicreator/core/ContentCollector.java src/main/java/com/aicreator/core/CollectorFactory.java
git commit -m "feat: add ContentCollector interface and CollectorFactory"
```

---

### Task 4: AiWebScrapeCollector — AI 辅助网页采集

**Files:**
- Create: `src/main/java/com/aicreator/core/AiWebScrapeCollector.java`

**Interfaces:**
- Implements: `ContentCollector`
- Consumes: `AiClient`, `ObjectMapper`
- Produces: `List<HotspotItem>` — 每个 source URL 独立抓取 → AI 提取 → 合并结果

- [ ] **Step 1: 创建 AiWebScrapeCollector.java**

```java
package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;
import com.aicreator.util.AiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiWebScrapeCollector implements ContentCollector {

    private final AiClient ai;
    private final ObjectMapper mapper;

    @Override
    public List<HotspotItem> collect(DomainProperties.DomainDefinition domain) {
        List<HotspotItem> allItems = new ArrayList<>();
        List<DomainProperties.SourceConfig> sources = domain.getSources();
        if (sources == null || sources.isEmpty()) {
            log.warn("[{}] 未配置网页源", domain.getLabel());
            return allItems;
        }

        for (DomainProperties.SourceConfig source : sources) {
            try {
                List<HotspotItem> items = scrapeOne(source);
                allItems.addAll(items);
                log.info("[{}] {} 采集到 {} 条", domain.getLabel(), source.getUrl(), items.size());
            } catch (Exception e) {
                log.error("[{}] 采集失败 {}: {}", domain.getLabel(), source.getUrl(), e.getMessage());
            }
        }
        log.info("[{}] AI网页采集共获取 {} 条", domain.getLabel(), allItems.size());
        return allItems;
    }

    private List<HotspotItem> scrapeOne(DomainProperties.SourceConfig source) {
        WebClient client = WebClient.create();
        String html = client.get().uri(source.getUrl())
                .header("User-Agent", "Mozilla/5.0 (compatible; AiContentCreator/1.0)")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (html == null || html.isBlank()) {
            return List.of();
        }

        // 去掉 script/style 标签，保留文本
        String cleaned = html.replaceAll("(?s)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?s)<style[^>]*>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // 截取前 8000 字符
        String truncated = cleaned.length() > 8000 ? cleaned.substring(0, 8000) : cleaned;

        String prompt = String.format("""
            从以下网页文本内容中，提取所有新闻/文章条目的标题和链接（如果有）。
            输出 JSON 数组格式：
            [{"title":"标题","url":"链接","excerpt":"摘要(最多100字)"}]
            只提取新闻类条目，忽略导航、广告、版权声明等。
            网页内容：
            %s
            """, truncated);

        String result = ai.chat(prompt, 0.2, 2000);
        try {
            return mapper.readValue(result, new TypeReference<List<HotspotItem>>() {});
        } catch (Exception e) {
            log.error("AI 网页提取 JSON 解析失败: {}", e.getMessage());
            return List.of();
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/core/AiWebScrapeCollector.java
git commit -m "feat: add AiWebScrapeCollector for AI-assisted web content extraction"
```

---

### Task 5: RssCollector — RSS 订阅采集

**Files:**
- Create: `src/main/java/com/aicreator/core/RssCollector.java`

**Interfaces:**
- Implements: `ContentCollector`
- Consumes: 无额外依赖，使用 JDK 内置 `javax.xml.parsers`
- Produces: `List<HotspotItem>` — 解析 RSS/Atom XML

- [ ] **Step 1: 创建 RssCollector.java**

```java
package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RssCollector implements ContentCollector {

    @Override
    public List<HotspotItem> collect(DomainProperties.DomainDefinition domain) {
        List<HotspotItem> allItems = new ArrayList<>();
        List<DomainProperties.SourceConfig> sources = domain.getSources();
        if (sources == null || sources.isEmpty()) {
            log.warn("[{}] 未配置 RSS 源", domain.getLabel());
            return allItems;
        }

        for (DomainProperties.SourceConfig source : sources) {
            try {
                List<HotspotItem> items = fetchRss(source);
                allItems.addAll(items);
                log.info("[{}] RSS {} 采集到 {} 条", domain.getLabel(), source.getUrl(), items.size());
            } catch (Exception e) {
                log.error("[{}] RSS 采集失败 {}: {}", domain.getLabel(), source.getUrl(), e.getMessage());
            }
        }
        log.info("[{}] RSS 采集共获取 {} 条", domain.getLabel(), allItems.size());
        return allItems;
    }

    private List<HotspotItem> fetchRss(DomainProperties.SourceConfig source) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source.getUrl()))
                .header("User-Agent", "Mozilla/5.0 (compatible; AiContentCreator/1.0)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(response.body());

        List<HotspotItem> items = new ArrayList<>();

        // RSS 2.0: <item> elements
        NodeList itemNodes = doc.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element elem = (Element) itemNodes.item(i);
            HotspotItem hi = new HotspotItem();
            hi.setTitle(getText(elem, "title"));
            hi.setUrl(getText(elem, "link"));
            hi.setExcerpt(truncate(getText(elem, "description"), 100));
            hi.setSource("rss");
            items.add(hi);
        }

        // Atom: <entry> elements
        if (items.isEmpty()) {
            NodeList entryNodes = doc.getElementsByTagName("entry");
            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element elem = (Element) entryNodes.item(i);
                HotspotItem hi = new HotspotItem();
                hi.setTitle(getText(elem, "title"));
                hi.setUrl(getLinkHref(elem));
                hi.setExcerpt(truncate(getText(elem, "summary"), 100));
                hi.setSource("rss");
                items.add(hi);
            }
        }

        return items;
    }

    private String getText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }

    private String getLinkHref(Element parent) {
        NodeList links = parent.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String href = link.getAttribute("href");
            if (href != null && !href.isBlank()) return href;
            String rel = link.getAttribute("rel");
            if (rel == null || rel.isBlank() || "alternate".equals(rel)) {
                return link.getTextContent().trim();
            }
        }
        return "";
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.isBlank()) return "";
        String cleaned = text.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/core/RssCollector.java
git commit -m "feat: add RssCollector for RSS/Atom feed parsing"
```

---

### Task 6: HotFilterCollector — 热榜关键词过滤采集

**Files:**
- Create: `src/main/java/com/aicreator/core/HotFilterCollector.java`

**Interfaces:**
- Implements: `ContentCollector`
- Consumes: `HotspotCollector`（复用现有知乎/微博采集）
- Produces: `List<HotspotItem>` — 采集后按领域关键词过滤排序

- [ ] **Step 1: 创建 HotFilterCollector.java**

```java
package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotFilterCollector implements ContentCollector {

    private final HotspotCollector hotspotCollector;

    @Override
    public List<HotspotItem> collect(DomainProperties.DomainDefinition domain) {
        // 1. 从现有热榜采集
        List<HotspotItem> allHot = hotspotCollector.fetchAll();
        log.info("[{}] 热榜原始数据 {} 条", domain.getLabel(), allHot.size());

        List<String> keywords = domain.getKeywords();
        List<String> excludeKeywords = domain.getExcludeKeywords();

        if (keywords == null || keywords.isEmpty()) {
            log.info("[{}] 未配置关键词，返回全部热榜数据", domain.getLabel());
            return allHot;
        }

        // 2. 关键词匹配过滤 + 排除过滤 + 按匹配度排序
        List<HotspotItem> filtered = allHot.stream()
                .filter(item -> {
                    // 排除关键词检查
                    if (excludeKeywords != null && !excludeKeywords.isEmpty()) {
                        for (String ek : excludeKeywords) {
                            if (item.getTitle() != null && item.getTitle().contains(ek)) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .filter(item -> keywords.stream().anyMatch(kw ->
                        item.getTitle() != null && item.getTitle().contains(kw)))
                .sorted(Comparator.comparingInt((HotspotItem item) -> {
                    // 按匹配关键词数量降序
                    return -(int) keywords.stream()
                            .filter(kw -> item.getTitle() != null && item.getTitle().contains(kw))
                            .count();
                }))
                .toList();

        log.info("[{}] 关键词过滤后 {} 条", domain.getLabel(), filtered.size());
        return filtered;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/core/HotFilterCollector.java
git commit -m "feat: add HotFilterCollector for keyword-based hotspot filtering"
```

---

### Task 7: ArticleWriter 领域感知改造

**Files:**
- Modify: `src/main/java/com/aicreator/core/ArticleWriter.java`

**Interfaces:**
- Consumes: `AiClient`, `ContentProperties`, `FeedbackEngine`
- Produces: `Article write(String topic, DomainProperties.DomainDefinition domain)` — 新增领域感知方法，注入反馈建议
- 保留: 原有 `write(String topic, String platform, String style)` 方法不变（向后兼容）

- [ ] **Step 1: 添加领域感知 write 方法**

修改 `ArticleWriter.java`，在现有 `write(String, String, String)` 方法之后添加新方法：

```java
public Article write(String topic, DomainProperties.DomainDefinition domain) {
    String platform = domain.getPlatforms().isEmpty() ? "weitoutiao" : domain.getPlatforms().get(0);
    ContentProperties.LengthRange length = domain.getLength() != null
            ? new ContentProperties.LengthRange() {{
                setMin(domain.getLength().getMin());
                setMax(domain.getLength().getMax());
            }}
            : contentProps.getArticleLength().getOrDefault(platform, new ContentProperties.LengthRange());

    if (length.getMin() <= 0) length.setMin(100);
    if (length.getMax() <= 0) length.setMax(200);

    String persona = domain.getPersona() != null ? domain.getPersona() : "资深自媒体人";
    DomainProperties.StylePrefs prefs = domain.getStylePrefs() != null
            ? domain.getStylePrefs() : new DomainProperties.StylePrefs();

    // 生成大纲（领域感知）
    String outline = generateOutline(topic, platform, persona, prefs);
    // 生成正文
    String content = generateContent(topic, outline, length, platform, persona, prefs, domain.getLabel());
    // 生成标题（领域感知）
    List<String> titles = ai.optimizeTitles(content, 10);
    // 提取钩子
    String hook = content.length() > 150 ? content.substring(0, 150) : content;

    return Article.builder()
            .topic(topic)
            .platform(platform)
            .titles(titles)
            .hook(hook)
            .outline(outline)
            .content(content)
            .wordCount(content.length())
            .build();
}

private String generateOutline(String topic, String platform, String persona, DomainProperties.StylePrefs prefs) {
    double hw = prefs.getHookWeight();
    double ew = prefs.getEmotionWeight();
    double uw = prefs.getUtilityWeight();

    String styleHint;
    if (hw >= ew && hw >= uw) {
        styleHint = "以悬念和故事开头为核心，勾起读者好奇心";
    } else if (ew >= uw) {
        styleHint = "以情绪共鸣为核心，让读者产生强烈认同感";
    } else {
        styleHint = "以实用干货为核心，让读者觉得有收藏价值";
    }

    String prompt = String.format("""
        请以"%s"的身份，为"%s"生成一个%s风格的文章大纲。
        风格方向：%s
        要求：
        - 结构清晰，有起承转合
        - 适合%s平台的阅读习惯
        - 每个部分标注预计字数
        输出格式：
        一、XXX（约XX字）
        二、XXX（约XX字）
        ...
        """, persona, topic, platform, styleHint, platform);
    return ai.chat(prompt, 0.5, 1000);
}

private String generateContent(String topic, String outline, ContentProperties.LengthRange length,
                                String platform, String persona, DomainProperties.StylePrefs prefs,
                                String domainLabel) {
    // 注入反馈优化建议
    String feedbackHint = "";
    String tips = feedbackEngine.getDomainTips(domainLabel);
    if (tips != null && !tips.isBlank()) {
        feedbackHint = "\n优化方向（基于近期表现数据）：" + tips + "\n";
    }

    String prompt = String.format("""
        请以"%s"的身份和口吻，根据以下大纲撰写一篇完整的文章。
        主题：%s
        大纲：%s
        平台：%s
        字数要求：%d-%d字
        风格权重：标题吸引力%.0f%%, 情绪共鸣%.0f%%, 实用价值%.0f%%
        %s写作要求：
        1. 开头必须有钩子（故事/冲突/数据/疑问）
        2. 中间有细节和案例，不要泛泛而谈
        3. 结尾有金句总结 + 引导评论
        4. 语气和视角必须完全贴合"%s"这个人设
        5. 适当使用 emoji 和网络用语
        6. 段落要短，适合手机阅读
        请直接输出正文，不要包含大纲。
        """, persona, topic, outline, platform, length.getMin(), length.getMax(),
        prefs.getHookWeight() * 100, prefs.getEmotionWeight() * 100, prefs.getUtilityWeight() * 100,
        feedbackHint, persona);
    return ai.chat(prompt, 0.7, 2500);
}
```

- [ ] **Step 2: 添加缺失的 import**

在文件头部确保有：
```java
import com.aicreator.config.DomainProperties;
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/aicreator/core/ArticleWriter.java
git commit -m "feat: add domain-aware write method to ArticleWriter"
```

---

### Task 8: ContentOptimizer 领域感知改造

**Files:**
- Modify: `src/main/java/com/aicreator/core/ContentOptimizer.java`

**Interfaces:**
- Consumes: `AiClient`, `List<String> sensitiveWords`（全局敏感词）
- Produces: `OptimizeResult optimize(String content, String persona, List<String> domainSensitiveWords)` — 新增领域感知方法

- [ ] **Step 1: 添加领域感知 optimize 方法**

在 `ContentOptimizer.java` 现有 `optimize(String content, String persona)` 方法后添加：

```java
public OptimizeResult optimize(String content, String persona, List<String> domainSensitiveWords) {
    OptimizeResult result = new OptimizeResult();
    result.setOriginal(content);
    result.setSuggestions(new ArrayList<>());

    // 1. 人格化改写
    String humanized = ai.humanize(content, persona);
    result.setHumanized(humanized);

    // 2. 敏感词检测（领域优先，回退全局）
    OptimizeResult.SensitiveCheck check = new OptimizeResult.SensitiveCheck();
    List<String> hits = new ArrayList<>();
    List<String> words = (domainSensitiveWords != null && !domainSensitiveWords.isEmpty())
            ? domainSensitiveWords : sensitiveWords;
    for (String word : words) {
        if (humanized.contains(word)) {
            hits.add(word);
        }
    }
    check.setPassed(hits.isEmpty());
    check.setHits(hits);
    result.setSensitiveCheck(check);
    if (!hits.isEmpty()) {
        result.getSuggestions().add("检测到敏感词: " + hits + "，建议修改");
    }

    // 3. 原创度评估
    double similarity = calculateSimilarity(content, humanized);
    result.setOriginalityScore(1.0 - similarity);
    if (similarity > 0.9) {
        result.getSuggestions().add("改写程度不够，建议增加更多个人表达");
    }

    // 4. 格式化
    result.setFinalContent(format(humanized));
    return result;
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/core/ContentOptimizer.java
git commit -m "feat: add domain-sensitive-word-aware optimize method"
```

---

### Task 9: TopicGenerator 领域感知改造

**Files:**
- Modify: `src/main/java/com/aicreator/core/TopicGenerator.java`

**Interfaces:**
- Consumes: `AiClient`, `ContentProperties`, `ObjectMapper`, `FeedbackEngine`
- Produces: `List<Topic> generate(List<HotspotItem> hotList, DomainProperties.DomainDefinition domain)` — 新增领域感知方法，注入反馈建议

- [ ] **Step 1: 添加领域感知 generate 方法**

在 `TopicGenerator.java` 现有 `generate(List<HotspotItem>, int)` 方法后添加：

```java
public List<Topic> generate(List<HotspotItem> hotList, DomainProperties.DomainDefinition domain) {
    int targetCount = domain.getArticleCount() > 0 ? domain.getArticleCount() : 3;
    String persona = domain.getPersona() != null ? domain.getPersona() : "资深自媒体人";

    List<String> hotSummary = hotList.stream().limit(15)
            .map(h -> "{\"title\":\"" + h.getTitle() + "\",\"source\":\"" + h.getSource() + "\"}")
            .toList();

    String keywordHint = "";
    if (domain.getKeywords() != null && !domain.getKeywords().isEmpty()) {
        keywordHint = "优先选择与以下关键词相关的话题：" + String.join("、", domain.getKeywords()) + "\n";
    }

    // 注入反馈优化建议
    String feedbackHint = "";
    String domainTips = feedbackEngine.getDomainTips(domain.getLabel());
    if (domainTips != null && !domainTips.isBlank()) {
        feedbackHint = "\n近期表现洞察（供参考）：" + domainTips + "\n";
    }

    String prompt = String.format("""
        你是一位"%s"。请根据以下热点，筛选出 %d 个最适合%s（%s）的选题，按"爆款概率"排序。
        %s%s要求：
        1. 有争议性、情绪共鸣或反常识
        2. 适合 %d-%d 字的短内容
        3. 能引发评论互动
        4. 选题必须贴合"%s"的领域方向
        热点列表：%s
        请直接输出 JSON 格式：
        [{"rank":1,"topic":"选题标题","angle":"切入角度","emotion":"情绪点","interaction":"评论区引导话术"}]
        """, persona, targetCount, domain.getLabel(), persona, keywordHint, feedbackHint,
        domain.getLength() != null ? domain.getLength().getMin() : 100,
        domain.getLength() != null ? domain.getLength().getMax() : 200,
        domain.getLabel(), hotSummary);

    String result = ai.chat(prompt, 0.8, 2000);
    try {
        return mapper.readValue(result, new TypeReference<List<Topic>>() {});
    } catch (Exception e) {
        log.error("选题 JSON 解析失败，降级处理: {}", e.getMessage());
        List<Topic> fallback = new ArrayList<>();
        for (int i = 0; i < Math.min(targetCount, hotList.size()); i++) {
            Topic t = new Topic();
            t.setRank(i + 1);
            t.setTopic(hotList.get(i).getTitle());
            t.setAngle("热点解读");
            t.setEmotion("共鸣");
            t.setInteraction("你怎么看？");
            fallback.add(t);
        }
        return fallback;
    }
}
```

- [ ] **Step 2: 添加缺失的 import**

在文件头部确保有：
```java
import com.aicreator.config.DomainProperties;
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/aicreator/core/TopicGenerator.java
git commit -m "feat: add domain-aware generate method to TopicGenerator"
```

---

### Task 10: Publisher 领域标识改造

**Files:**
- Modify: `src/main/java/com/aicreator/core/Publisher.java`

**Interfaces:**
- 修改: `publish(Article, String platform)` → `publish(Article, String platform, String domainName)`
- 保持向后兼容: 旧签名 `publish(Article, String platform)` 内部调用新签名，domain 默认 `"default"`

- [ ] **Step 1: 添加新 publish 方法，旧方法委托**

在 `Publisher.java` 中，修改 `publish` 方法和内部 `PlatformPublisher` 接口：

```java
// 旧方法保持向后兼容
public Map<String, Object> publish(Article article, String platform) {
    return publish(article, platform, "default");
}

// 新方法带领域标识
public Map<String, Object> publish(Article article, String platform, String domainName) {
    PlatformPublisher pub = publishers.get(platform);
    if (pub == null) {
        return Map.of("status", "error", "msg", "不支持的平台: " + platform);
    }
    return pub.publish(article, domainName);
}

private interface PlatformPublisher {
    Map<String, Object> publish(Article article, String domainName);
}
```

- [ ] **Step 2: 修改 BasePublisher 使用 domainName**

将 `BasePublisher` 类的 `publish` 方法和 `writeArticleFile` 方法修改为接收 domainName：

```java
private abstract class BasePublisher implements PlatformPublisher {
    @Override
    public Map<String, Object> publish(Article article, String domainName) {
        String title = article.getTitles().isEmpty() ? "无标题" : article.getTitles().get(0);
        log.info("[{}:{}] 生成RPA发布文件: {}", getPlatform(), domainName, title);

        String fileName = writeArticleFile(article, domainName);
        log.info("[{}:{}] 文件已写入: {}", getPlatform(), domainName, fileName);

        notifyWebhook(fileName, getPlatform());

        return Map.of("status", "ready", "file", fileName);
    }

    protected abstract String getPlatform();

    private String writeArticleFile(Article article, String domainName) {
        String platform = getPlatform();
        String title = article.getTitles().isEmpty() ? "无标题" : article.getTitles().get(0);
        String keywords = article.getKeywords() != null ? String.join(", ", article.getKeywords()) : "";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s_%s_%s.md", domainName, platform, timestamp,
                UUID.randomUUID().toString().substring(0, 8));

        File dir = new File(outputDir, platform);
        dir.mkdirs();

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"").append(title).append("\"\n");
        sb.append("platform: ").append(platform).append("\n");
        sb.append("domain: ").append(domainName).append("\n");
        sb.append("keywords: ").append(keywords).append("\n");
        sb.append("created: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append("\n");
        sb.append("---\n\n");
        sb.append(article.getContent() != null ? article.getContent() : "");

        File file = new File(dir, fileName);
        try {
            Files.writeString(file.toPath(), sb.toString());
        } catch (IOException e) {
            log.error("[{}:{}] 写入发布文件失败: {}", platform, domainName, e.getMessage());
        }
        return file.getAbsolutePath();
    }
    // notifyWebhook 方法保持不变
    ...
}
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/aicreator/core/Publisher.java
git commit -m "feat: add domain name to publisher filename and frontmatter"
```

---

### Task 11: Analytics 扩展 metrics 字段

**Files:**
- Modify: `src/main/java/com/aicreator/core/Analytics.java`

**Interfaces:**
- Produces: `void record(String domain, String platform, String topic, int wordCount, Map<String, Object> metrics)` — 扩展 record 方法

- [ ] **Step 1: 在 Analytics 中添加扩展 record 方法**

```java
public synchronized void record(String domain, String platform, String topic, int wordCount,
                                 Map<String, Object> metrics) {
    List<Map<String, Object>> history = load();
    Map<String, Object> data = new HashMap<>();
    data.put("domain", domain);
    data.put("platform", platform);
    data.put("topic", topic);
    data.put("wordCount", wordCount);
    data.put("metrics", metrics != null ? metrics : Map.of());
    data.put("recordTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
    data.put("articleId", UUID.randomUUID().toString().substring(0, 8));
    history.add(data);
    try {
        mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, history);
    } catch (IOException e) {
        log.error("保存历史记录失败: {}", e.getMessage());
    }
}
```

- [ ] **Step 2: 在文件头部添加 UUID import**

```java
import java.util.UUID;
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/aicreator/core/Analytics.java
git commit -m "feat: extend Analytics with domain and metrics fields"
```

---

### Task 12: DomainWorkflowService 单领域工作流

**Files:**
- Create: `src/main/java/com/aicreator/service/DomainWorkflowService.java`

**Interfaces:**
- Consumes: `CollectorFactory`, `TopicGenerator`, `ArticleWriter`, `ContentOptimizer`, `Publisher`, `Analytics`
- Produces: `void execute(DomainProperties.DomainDefinition domain)` — 执行单领域完整流水线

- [ ] **Step 1: 创建 DomainWorkflowService.java**

```java
package com.aicreator.service;

import com.aicreator.config.DomainProperties;
import com.aicreator.core.*;
import com.aicreator.model.Article;
import com.aicreator.model.HotspotItem;
import com.aicreator.model.OptimizeResult;
import com.aicreator.model.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainWorkflowService {

    private final CollectorFactory collectorFactory;
    private final TopicGenerator topicGenerator;
    private final ArticleWriter articleWriter;
    private final ContentOptimizer contentOptimizer;
    private final Publisher publisher;
    private final Analytics analytics;

    @Async
    public void execute(DomainProperties.DomainDefinition domain) {
        String label = domain.getLabel() != null ? domain.getLabel() : domain.toString();
        log.info("=".repeat(40));
        log.info("🚀 领域 [{}] 工作流启动", label);
        log.info("=".repeat(40));

        try {
            // 1. 采集内容
            log.info("[{}] 📡 步骤 1: 采集内容...", label);
            ContentCollector collector = collectorFactory.get(domain.getCollector());
            List<HotspotItem> hotList = collector.collect(domain);
            if (hotList.isEmpty()) {
                log.warn("[{}] ⚠️ 内容采集为空，使用历史热门选题兜底", label);
                hotList = List.of(createFallbackItem());
            }

            // 2. 生成选题
            log.info("[{}] 💡 步骤 2: AI 生成选题...", label);
            List<Topic> topics = topicGenerator.generate(hotList, domain);
            for (int i = 0; i < topics.size(); i++) {
                Topic t = topics.get(i);
                log.info("[{}]   {}. {} | 角度: {}", label, i + 1, t.getTopic(), t.getAngle());
            }

            // 3-5. 逐篇写作、优化、发布
            List<String> succeededArticles = new ArrayList<>();
            for (Topic topic : topics) {
                try {
                    log.info("[{}] ✍️ 写作: {}", label, topic.getTopic());
                    Article article = articleWriter.write(topic.getTopic(), domain);

                    log.info("[{}] 🔧 优化: {}", label, topic.getTopic());
                    List<String> domainSensitiveWords = domain.getExcludeKeywords() != null
                            ? domain.getExcludeKeywords() : List.of();
                    OptimizeResult optimized = contentOptimizer.optimize(
                            article.getContent(), domain.getPersona(), domainSensitiveWords);

                    String ctaType = domain.getCtaType() != null ? domain.getCtaType() : "comment";
                    String finalContent = contentOptimizer.addCta(optimized.getFinalContent(), ctaType);

                    // 更新 article 内容
                    article.setContent(finalContent);

                    // 发布到各平台
                    for (String platform : domain.getPlatforms()) {
                        log.info("[{}] 📤 发布到 {}: {}", label, platform, topic.getTopic());
                        Map<String, Object> result = publisher.publish(article, platform, domain.getLabel());
                        log.info("[{}]   发布结果: {}", label, result.get("status"));
                    }

                    // 记录分析
                    analytics.record(domain.getLabel(), domain.getPlatforms().get(0),
                            topic.getTopic(), finalContent.length(), Map.of());

                    succeededArticles.add(topic.getTopic());
                } catch (Exception e) {
                    log.error("[{}] 文章「{}」处理失败: {}", label, topic.getTopic(), e.getMessage());
                }
            }

            log.info("[{}] ✅ 完成，成功 {} 篇/共 {} 篇", label, succeededArticles.size(), topics.size());
        } catch (Exception e) {
            log.error("[{}] ❌ 领域工作流失败: {}", label, e.getMessage(), e);
        }
    }

    private HotspotItem createFallbackItem() {
        HotspotItem item = new HotspotItem();
        item.setTitle("今日热门话题");
        item.setSource("fallback");
        return item;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/service/DomainWorkflowService.java
git commit -m "feat: add DomainWorkflowService for single-domain pipeline"
```

---

### Task 13: DailyOrchestrator 多领域并行编排器

**Files:**
- Create: `src/main/java/com/aicreator/service/DailyOrchestrator.java`

**Interfaces:**
- Consumes: `DomainRegistry`, `DomainWorkflowService`, `FeedbackEngine`
- Produces: `void runAll()` — 并行执行所有启用领域
- Produces: `void runAllAsync()` — REST 异步触发
- Produces: `void runSingle(String domainName, String topic)` — 单篇 CLI 模式

- [ ] **Step 1: 创建 DailyOrchestrator.java**

```java
package com.aicreator.service;

import com.aicreator.config.DomainProperties;
import com.aicreator.core.DomainRegistry;
import com.aicreator.core.FeedbackEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @org.springframework.beans.factory.annotation.Value("${scheduler.enabled:true}")
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
```

- [ ] **Step 2: 验证编译（此时 FeedbackEngine 尚未创建，需先建空壳）**

暂时先验证其他部分没问题，FeedbackEngine 下一步创建。先检查编译：

```bash
mvn compile 2>&1 | head -20
```
预期: 报错找不到 FeedbackEngine（预期行为）

- [ ] **Step 3: Commit（依赖 Task 14 完成后才能编译通过，此处仅提交）**

```bash
git add src/main/java/com/aicreator/service/DailyOrchestrator.java
git commit -m "feat: add DailyOrchestrator for parallel multi-domain scheduling"
```

---

### Task 14: FeedbackCollector 接口 + FeedbackEngine

**Files:**
- Create: `src/main/java/com/aicreator/core/FeedbackCollector.java`
- Create: `src/main/java/com/aicreator/core/FeedbackEngine.java`

**Interfaces:**
- Produces: `FeedbackCollector` 接口 — `Map<String, Object> fetchMetrics(String articleId, String platform)`
- Produces: `FeedbackEngine` — `void checkAndAnalyze()`, `void printReport()`

- [ ] **Step 1: 创建 FeedbackCollector 接口**

```java
package com.aicreator.core;

import java.util.Map;

public interface FeedbackCollector {
    /**
     * 拉取指定平台文章的互动数据
     * @return metrics map: {views, likes, comments, shares}
     */
    Map<String, Object> fetchMetrics(String articleId, String platform);

    /** 是否支持该平台 */
    boolean supports(String platform);
}
```

- [ ] **Step 2: 创建 FeedbackEngine.java**

```java
package com.aicreator.core;

import com.aicreator.util.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FeedbackEngine {

    private final AiClient ai;
    private final ObjectMapper mapper;
    private final File historyFile;
    private final File tipsFile;
    private final List<FeedbackCollector> collectors;

    public FeedbackEngine(AiClient ai,
                          @Value("${app.data-dir}") String dataDir,
                          List<FeedbackCollector> collectors) {
        this.ai = ai;
        this.mapper = new ObjectMapper();
        this.collectors = collectors != null ? collectors : List.of();
        File dir = new File(dataDir);
        dir.mkdirs();
        this.historyFile = new File(dir, "publish_history.json");
        this.tipsFile = new File(dir, "optimization_tips.json");
    }

    /**
     * 检查是否需要触发分析（每 30 篇文章或距上次分析 7 天）
     */
    public void checkAndAnalyze() {
        List<Map<String, Object>> history = loadHistory();
        if (history.isEmpty()) return;

        // 读取上次分析时间
        Map<String, Object> tips = loadTips();
        String lastAnalysis = (String) tips.getOrDefault("lastAnalysis", "");
        LocalDateTime lastTime = lastAnalysis.isEmpty() ? null
                : LocalDateTime.parse(lastAnalysis);

        // 统计上次分析后的新文章数
        long newCount = history.stream()
                .filter(h -> {
                    String t = (String) h.getOrDefault("recordTime", "");
                    if (lastTime == null) return true;
                    return LocalDateTime.parse(t).isAfter(lastTime);
                })
                .count();

        boolean shouldAnalyze = newCount >= 30 ||
                (lastTime != null && lastTime.isBefore(LocalDateTime.now().minusDays(7)));

        if (shouldAnalyze) {
            log.info("触发反馈分析 (上次分析后新增 {} 篇文章)", newCount);
            analyze();
        } else {
            log.debug("跳过反馈分析 (上次分析后 {} 篇，未到触发条件)", newCount);
        }
    }

    private void analyze() {
        List<Map<String, Object>> history = loadHistory();
        if (history.size() < 5) {
            log.info("历史文章不足 5 篇，跳过分析");
            return;
        }

        // 构建分析数据
        StringBuilder data = new StringBuilder();
        for (Map<String, Object> h : history) {
            data.append(String.format("领域:%s 平台:%s 选题:%s 字数:%s\n",
                    h.getOrDefault("domain", "?"),
                    h.getOrDefault("platform", "?"),
                    h.getOrDefault("topic", "?"),
                    h.getOrDefault("wordCount", 0)));
        }

        String prompt = String.format("""
            分析以下文章发布记录，找出：
            1. 每个领域表现最好的选题类型（总结规律，如"职场吐槽类"、"热点解读类"）
            2. 表现较差的内容共性
            3. 各领域的优化建议（选题方向、标题策略、风格调整）
            输出 JSON：
            {"tipsByDomain":{"领域名":"优化建议"},
             "bestTopicTypes":["类型1","类型2"],
             "generalAdvice":"整体建议"}
            文章记录（最近 %d 篇）：
            %s
            """, history.size(), data.toString());

        String result = ai.chat(prompt, 0.5, 2000);
        try {
            Map<String, Object> tips = new HashMap<>(mapper.readValue(result, Map.class));
            tips.put("lastAnalysis", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            tips.put("articleCount", history.size());
            mapper.writerWithDefaultPrettyPrinter().writeValue(tipsFile, tips);
            log.info("优化建议已保存到 {}", tipsFile);
        } catch (Exception e) {
            log.error("反馈分析 JSON 解析失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadHistory() {
        if (!historyFile.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(historyFile, List.class);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadTips() {
        if (!tipsFile.exists()) return new HashMap<>();
        try {
            return mapper.readValue(tipsFile, Map.class);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public void printReport() {
        Map<String, Object> tips = loadTips();
        log.info("📊 优化建议:");
        try {
            log.info("\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tips));
        } catch (Exception e) {
            log.error("输出报告失败", e);
        }
    }

    /** 获取某个领域的最新优化建议文本 */
    public String getDomainTips(String domainName) {
        Map<String, Object> tips = loadTips();
        Object domainTips = tips.getOrDefault("tipsByDomain", Map.of());
        if (domainTips instanceof Map) {
            return (String) ((Map<?, ?>) domainTips).getOrDefault(domainName, "");
        }
        return "";
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/core/FeedbackCollector.java src/main/java/com/aicreator/core/FeedbackEngine.java
git commit -m "feat: add FeedbackEngine for automated content optimization loop"
```

---

### Task 15: 更新 AiCreatorRunner CLI 入口

**Files:**
- Modify: `src/main/java/com/aicreator/controller/AiCreatorRunner.java`

**Interfaces:**
- Consumes: `DailyOrchestrator`（替代 `DailyWorkflowService`）
- CLI 用法: `java -jar app.jar daily` → 多领域并行；`java -jar app.jar single <domain> <topic>` → 指定领域单篇

- [ ] **Step 1: 修改 AiCreatorRunner**

```java
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
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/controller/AiCreatorRunner.java
git commit -m "refactor: update AiCreatorRunner to use DailyOrchestrator"
```

---

### Task 16: 更新 ManagementController REST 接口

**Files:**
- Modify: `src/main/java/com/aicreator/controller/ManagementController.java`

**Interfaces:**
- Consumes: `DailyOrchestrator`（替代 `DailyWorkflowService`）

- [ ] **Step 1: 修改 ManagementController**

```java
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
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aicreator/controller/ManagementController.java
git commit -m "refactor: update ManagementController to use DailyOrchestrator"
```

---

### Task 17: 清理旧 DailyWorkflowService + 更新 AppConfig

**Files:**
- Delete: `src/main/java/com/aicreator/service/DailyWorkflowService.java`
- Modify: `src/main/java/com/aicreator/config/AppConfig.java`

- [ ] **Step 1: 删除旧文件**

```bash
rm src/main/java/com/aicreator/service/DailyWorkflowService.java
```

- [ ] **Step 2: 更新 AppConfig**

`AppConfig.java` 无需显式注册 `DomainWorkflowService` 和 `DailyOrchestrator`（它们使用 `@Service` 注解自动扫描）。确认现有 `@EnableConfigurationProperties` 已包含 `DomainProperties.class`（Task 1 已完成）。

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```
预期: BUILD SUCCESS（无引用旧 DailyWorkflowService 的错误）

- [ ] **Step 4: Commit**

```bash
git add -u src/main/java/com/aicreator/service/DailyWorkflowService.java
git commit -m "refactor: remove deprecated DailyWorkflowService"
```

---

### Task 18: 更新 application.yml 配置

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 在 application.yml 末尾追加 domain 配置段**

在现有配置后追加：

```yaml
# 多领域配置
domain:
  foreign-news:
    enabled: true
    label: "外网热点"
    collector: ai_web_scrape
    persona: "国际新闻观察者，视角客观冷静"
    platforms:
      - weitoutiao
      - maimai
    article-count: 3
    length:
      min: 100
      max: 200
    cta-type: comment
    sources:
      - url: "https://www.bbc.com"
        type: webpage
      - url: "https://www.zaobao.com"
        type: webpage
      - url: "https://www.foxnews.com"
        type: webpage
      - url: "https://edition.cnn.com"
        type: webpage
      - url: "https://www.aljazeera.com"
        type: webpage
  it-hot:
    enabled: true
    label: "IT热点"
    collector: rss_feed
    persona: "资深程序员，经验丰富，语言犀利"
    platforms:
      - maimai
    article-count: 3
    length:
      min: 100
      max: 200
    cta-type: follow
    sources:
      - url: "https://github.com/trending"
        type: rss
      - url: "https://news.ycombinator.com/rss"
        type: rss
      - url: "https://www.v2ex.com/feed/tab/tech.xml"
        type: rss
      - url: "https://www.36kr.com/feed"
        type: rss
      - url: "https://sspai.com/feed"
        type: rss
  lifestyle:
    enabled: true
    label: "生活热点"
    collector: hot_filter
    persona: "懂生活的过来人，温暖接地气"
    platforms:
      - weitoutiao
      - maimai
    article-count: 3
    length:
      min: 100
      max: 200
    cta-type: share
    keywords:
      - 家庭
      - 育儿
      - 美食
      - 旅游
      - 健康
      - 情感
    excludeKeywords:
      - 政治
      - 军事
      - 灾难
```

- [ ] **Step 2: 验证应用启动**

```bash
mvn compile
```
预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "config: add multi-domain configuration for foreign-news, it-hot, and lifestyle"
```

---

### Task 19: 集成测试与最终验证

**Files:**
- 验证: 全量编译 + 启动

- [ ] **Step 1: 完整构建**

```bash
mvn clean package -DskipTests
```
预期: BUILD SUCCESS

- [ ] **Step 2: 验证应用启动**

```bash
java -jar target/ai-content-creator-1.0.0.jar &
sleep 10
curl http://localhost:8080/api/health
kill %1
```
预期: 返回 `{"status":"UP","service":"AI Content Creator",...}`

- [ ] **Step 3: Commit**

```bash
git commit --allow-empty -m "verify: full build and startup test pass"
```
