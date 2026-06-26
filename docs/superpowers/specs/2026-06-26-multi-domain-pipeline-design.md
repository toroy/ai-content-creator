# 多领域并行内容创作流水线 设计文档

**日期**: 2026-06-26
**状态**: 设计完成，待实现

---

## 1. 目标

将现有的单一通用热点流水线重构为多领域并行架构，支持：

- 3 个独立领域（外网热点、IT热点、生活热点）并行运行，预留扩展
- AI 辅助网页内容提取，从指定新闻/技术网站采集
- 领域感知的写作风格（人格、标题策略、CTA）
- 多平台分发（微头条、脉脉），领域→平台可配置映射
- 自动化反馈闭环：平台数据回读 → AI 分析 → 优化下次写作
- 所有领域配置化（application.yml），支持运行时增减

---

## 2. 架构概览

```
                      DailyOrchestrator (定时触发)
                       /        |        \
                      /         |         \
          DomainWorkflow   DomainWorkflow   DomainWorkflow
          (外网热点)       (IT热点)         (生活热点)
               │               │               │
          ┌────┴────┐    ┌────┴────┐    ┌────┴────┐
          │Collector│    │Collector│    │Collector│
          │TopicGen │    │TopicGen │    │TopicGen │
          │Writer   │    │Writer   │    │Writer   │
          │Optimizer│    │Optimizer│    │Optimizer│
          │Publisher│    │Publisher│    │Publisher│
          └─────────┘    └─────────┘    └─────────┘
               │               │               │
               └───────────────┴───────────────┘
                               │
                        Analytics + FeedbackEngine
```

- `DailyOrchestrator` 替代现有 `DailyWorkflowService` 的调度职责
- 每个领域在独立线程中异步执行，互不阻塞
- 某个领域失败不影响其他领域

---

## 3. 领域模型

新增 `DomainDefinition`，一个领域即一份完整配置：

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 领域标识，如 `foreign-news` |
| label | String | 中文显示名，如 `外网热点` |
| enabled | boolean | 是否启用 |
| collectorType | String | 采集器类型：`ai_web_scrape` / `rss_feed` / `hot_filter` |
| sources | List\<SourceConfig\> | 内容源列表 |
| persona | String | 写作人格描述 |
| platforms | List\<String\> | 目标发布平台 |
| articleCount | int | 每日生成篇数，默认 3 |
| length | LengthRange | 字数范围，默认 {min: 100, max: 200} |
| keywords | List\<String\> | 领域关键词（hot_filter 类型使用） |
| excludeKeywords | List\<String\> | 排除关键词 |
| stylePrefs | StylePrefs | 风格偏好权重 |

所有领域定义从 `application.yml` 绑定，支持增减无需改代码。

---

## 4. 内容采集层

定义 `ContentCollector` 接口：

```java
interface ContentCollector {
    List<HotspotItem> collect(List<SourceConfig> sources);
}
```

### 4.1 AiWebScrapeCollector（外网热点）

流程：`HTTP GET 网页 → 去标签/脚本 → 截取前 8000 字符 → AI 提取结构化内容`

- 每个 source URL 独立抓取，AI 调用可并行
- AI 提取温度 0.2，确保 JSON 输出稳定
- 单源失败跳过，其他源继续
- 目标源：zaobao.com、foxnews.com、bbc.com、cnn.com、aljazeera.com

### 4.2 RssCollector（IT热点）

解析技术网站的 RSS/Atom feed，标准化为 `HotspotItem`。

- 目标源：GitHub Trending、Hacker News、V2EX、少数派、36氪
- 使用 Jackson XML 或内置解析，避免引入新依赖

### 4.3 HotFilterCollector（生活热点）

复用现有知乎/微博热榜采集逻辑，增加领域关键词过滤：

- 采集知乎+微博热榜 → 关键词匹配过滤 → 排除词过滤 → 按匹配度排序取 Top N
- 生活热点关键词示例：家庭、育儿、美食、旅游、健康、情感
- 注意：当前项目已有知乎/微博采集器，直接复用其输出，在此基础上做过滤。后续可扩展抖音热榜源

### 4.4 降级策略

- 单源不可达 → 跳过该源，日志告警
- 某领域全部源失败 → 使用该领域历史热门选题作为兜底

---

## 5. 写作与优化层（领域感知）

### 5.1 ArticleWriter 改造

现有三步（大纲→正文→标题）保留，prompt 注入领域上下文：

- 人格由领域 `persona` 决定（外网热点=`国际新闻观察者`，IT=`资深程序员`，生活=`懂生活的过来人`）
- 标题策略按领域差异化：外网偏悬念+数据型，IT偏数字+实用型，生活偏情绪+反问型
- 风格偏好权重从 `stylePrefs` 注入 prompt

### 5.2 ContentOptimizer 改造

- 人格化改写参数从领域配置读取
- 敏感词检测：不同领域可覆盖全局敏感词列表
- CTA 策略按领域：外网=`comment`，IT=`private`+`follow`，生活=`share`+`comment`

### 5.3 字数范围

默认 100-200 字，各领域可在配置中覆盖。

---

## 6. 发布层

### 6.1 领域→平台映射

| 领域 | 平台 |
|------|------|
| 外网热点 | 微头条、脉脉 |
| IT热点 | 脉脉 |
| 生活热点 | 微头条、脉脉 |

每个领域写完文章后，遍历 `platforms` 列表逐平台发布。

### 6.2 发布文件增强

文件名加入领域标识：`{domain}_{platform}_{timestamp}_{uuid}.md`

Frontmatter 增加领域字段：

```yaml
---
title: "..."
platform: weitoutiao
domain: foreign-news
keywords: [...]
created: 2026-06-26T08:30:00
---
```

输出路径保持现有结构 `~/.ai-content-creator/output/{platform}/`。

---

## 7. 反馈闭环

### 7.1 数据采集

`FeedbackCollector` 接口，每个平台一个实现，拉取文章阅读量/点赞数/评论数/分享数。

初期实现通用方案：通过平台开放 API 拉取数据。无法对接的平台跳过，不影响主流程。

### 7.2 数据持久化

扩展 `Analytics`，`publish_history.json` 增加 metrics 字段：

```json
{
  "articleId": "uuid",
  "domain": "foreign-news",
  "platform": "weitoutiao",
  "topic": "...",
  "metrics": { "views": 1234, "likes": 56, "comments": 12, "shares": 3 },
  "fetchedAt": "2026-06-27T08:00:00"
}
```

### 7.3 AI 分析（FeedbackEngine）

每累计发布 30 篇文章或每周触发一次分析（取先到条件）：

- 输入：最近文章 metrics + 选题 + 领域 + 标题风格
- AI 分析：高/低互动文章的共性、各领域优化建议
- 输出：结构化建议写入 `~/.ai-content-creator/optimization_tips.json`

### 7.4 建议消费

TopicGenerator 和 ArticleWriter 从 `optimization_tips.json` 读取最新建议，注入 prompt 的优化方向部分，形成自优化循环。

---

## 8. 定时编排

```java
@Scheduled(cron = "${scheduler.daily-cron}")
public void scheduledDaily() {
    List<DomainDefinition> enabledDomains = domainRegistry.getEnabled();
    // 每个领域 @Async 并行执行
    enabledDomains.forEach(d -> domainWorkflowExecutor.execute(d));
}
```

- 每个 DomainWorkflow 独立线程，互不阻塞
- 整体耗时 = 最慢的单个领域
- 调度 cron 保持现有配置 `0 30 8 * * *`（每天 8:30）

---

## 9. 配置结构

```yaml
domain:
  foreign-news:
    enabled: true
    label: "外网热点"
    collector: ai_web_scrape
    persona: "国际新闻观察者，视角客观冷静"
    platforms: [weitoutiao, maimai]
    article-count: 3
    length: { min: 100, max: 200 }
    sources:
      - url: "https://www.bbc.com"
      - url: "https://www.zaobao.com"
      - url: "https://www.foxnews.com"
      - url: "https://edition.cnn.com"
      - url: "https://www.aljazeera.com"
  it-hot:
    enabled: true
    label: "IT热点"
    collector: rss_feed
    persona: "资深程序员，经验丰富，语言犀利"
    platforms: [maimai]
    article-count: 3
    length: { min: 100, max: 200 }
    sources:
      - url: "https://github.com/trending"
      - url: "https://news.ycombinator.com/rss"
      - url: "https://www.v2ex.com/feed/tab/tech.xml"
      - url: "https://www.36kr.com/feed"
      - url: "https://sspai.com/feed"
  lifestyle:
    enabled: true
    label: "生活热点"
    collector: hot_filter
    persona: "懂生活的过来人，温暖接地气"
    platforms: [weitoutiao, maimai]
    article-count: 3
    length: { min: 100, max: 200 }
    keywords: ["家庭", "育儿", "美食", "旅游", "健康", "情感"]
    excludeKeywords: ["政治", "军事", "灾难"]
```

---

## 10. 错误处理 & 降级

| 层级 | 失败场景 | 降级行为 |
|------|---------|---------|
| 单源采集 | 某 URL 不可达 | 跳过该源，其他源继续 |
| 单领域采集 | 所有源失败 | 使用历史热门选题兜底 |
| 单领域工作流 | AI 调用失败 | 跳过该领域本次执行，日志告警 |
| 反馈采集 | 平台 API 不可用 | 跳过反馈，不影响主流程 |
| 整体调度 | 全部领域失败 | 日志告警，下次定时重试 |

---

## 11. 新增/变更文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `config/DomainProperties.java` | 新增 | 多领域配置绑定 |
| `model/DomainDefinition.java` | 新增 | 领域定义模型 |
| `core/ContentCollector.java` | 新增 | 采集器接口 |
| `core/AiWebScrapeCollector.java` | 新增 | AI 辅助网页采集 |
| `core/RssCollector.java` | 新增 | RSS 订阅采集 |
| `core/HotFilterCollector.java` | 新增 | 热榜关键词过滤采集 |
| `core/DomainRegistry.java` | 新增 | 领域注册中心 |
| `service/DomainWorkflowService.java` | 新增 | 单领域工作流编排 |
| `service/DailyOrchestrator.java` | 新增 | 多领域并行编排（替代 DailyWorkflowService） |
| `core/FeedbackCollector.java` | 新增 | 反馈采集接口 |
| `core/FeedbackEngine.java` | 新增 | 反馈分析引擎 |
| `core/ArticleWriter.java` | 修改 | 注入领域上下文 |
| `core/ContentOptimizer.java` | 修改 | 领域感知优化 |
| `core/Publisher.java` | 修改 | 文件名加入领域标识 |
| `core/Analytics.java` | 修改 | metrics 字段扩展 |
| `core/TopicGenerator.java` | 修改 | 注入领域关键词和反馈建议 |
| `controller/AiCreatorRunner.java` | 修改 | CLI 适配新编排器 |
| `service/DailyWorkflowService.java` | 删除 | 由 DailyOrchestrator 替代 |
| `application.yml` | 修改 | 新增 domain 配置段 |

---

## 12. 测试策略

- **DomainRegistry 单元测试**：验证配置绑定、领域启用/禁用逻辑
- **AiWebScrapeCollector 单元测试**：mock HTML，验证 AI 提取 prompt 格式
- **RssCollector 单元测试**：mock RSS XML，验证解析正确性
- **HotFilterCollector 单元测试**：mock 热榜数据，验证关键词过滤
- **DomainWorkflowService 集成测试**：mock AiClient，验证全流程不抛异常
- **FeedbackEngine 单元测试**：mock 历史数据，验证分析建议输出格式
