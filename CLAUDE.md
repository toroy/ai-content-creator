# CLAUDE.md

本文件为 Claude Code 提供项目上下文指引。

## 构建与运行

```bash
mvn clean package -DskipTests    # 构建可执行 JAR
mvn spring-boot:run              # 通过 Maven 插件直接运行
mvn test                         # 运行测试（暂无测试用例）
java -jar target/ai-content-creator-1.0.0.jar daily                          # 全流程工作流
java -jar target/ai-content-creator-1.0.0.jar single "topic keyword"         # 单篇文章
java -jar target/ai-content-creator-1.0.0.jar report                         # 数据分析报告
```

## 技术栈

Java 17, Spring Boot 3.2.5, Maven, Spring WebFlux (WebClient 用于 HTTP), Lombok, Jackson, Apache Commons Lang 3.

## 架构

应用是一个 CLI 驱动的 AI 内容创作流水线，同时提供 REST 管理接口。调用 OpenAI 兼容的 Chat Completions API（通过 `AiClient` 工具类）。流水线共 7 步，由 `DailyWorkflowService` 编排：

1. **HotspotCollector** — 通过 WebClient 抓取知乎/微博热榜，解析 JSON
2. **TopicGenerator** — 让 AI 根据热点生成选题
3. **ArticleWriter** — 三步写作：大纲 → 正文 → 标题（使用 `Article.builder()`）
4. **ContentOptimizer** — 去 AI 味改写、敏感词检测（字符串包含匹配）、原创度评估（Jaccard 相似度）、追加 CTA
5. **Publisher** — 策略模式，`PlatformPublisher` 接口，3 个实现（Weitoutiao/Maimai/Zhihu），写 artifact 文件到 `~/.ai-content-creator/output/` 供 RPA 消费
6. **Analytics** — 持久化到 `~/.ai-content-creator/publish_history.json`

`AiCreatorRunner`（在 `controller/` 中）解析 CLI 参数并分发到 `DailyWorkflowService`。
`ManagementController` 提供 REST 接口：`/api/health`、`/api/workflow/daily`、`/api/workflow/single`、`/api/workflow/report`。

## 关键模式

- **构造器注入**：通过 Lombok `@RequiredArgsConstructor`（字段为 `final`）
- **配置绑定**：通过 `@ConfigurationProperties`，前缀为 `ai`、`content`、`hotspot`、`sensitive-words`
- **防御性降级**：所有外部调用包裹在 try-catch 中，失败时回退到默认值（永不向调用方抛异常）
- **AiClient**：通用的 OpenAI 兼容封装 — prompt 用字符串，响应通过 Jackson `ObjectMapper` 解析

## 配置

- `AI_API_KEY`、`AI_BASE_URL`、`AI_MODEL` 环境变量（或在 `application.yml` 中设置）
- 敏感词列表在 `application.yml` 的 `sensitive-words` 下
- 各平台文章长度范围在 `content.article-length` 下按平台配置
- `scheduler.enabled` 控制定时任务开关，`scheduler.daily-cron` 控制执行时间
- `app.data-dir` 数据目录，`app.publishing.output-dir` 发布文件输出目录

## 已知不足

- **Publisher 为 RPA 文件输出** — 写文件供外部 RPA 工具扫描发布，非直接平台集成
- **无数据库** — 分析统计使用 JSON 文件存储
- **无测试** — `src/test/` 不存在，但 `spring-boot-starter-test` 已在 pom.xml 中
- **`templates/` 目录为空** — 预留给写作模板，尚未使用
