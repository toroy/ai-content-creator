# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn clean package -DskipTests    # Build executable JAR
mvn spring-boot:run              # Run directly via Maven plugin
mvn test                         # Run tests (none exist yet)
java -jar target/ai-content-creator-1.0.0.jar daily                          # Full workflow
java -jar target/ai-content-creator-1.0.0.jar single "topic keyword"         # Single article
java -jar target/ai-content-creator-1.0.0.jar report                         # Analytics report
```

## Tech Stack

Java 17, Spring Boot 3.2.5, Maven, Spring WebFlux (WebClient for HTTP), Lombok, Jackson, Apache Commons Lang 3.

## Architecture

The app is a CLI-driven content creation pipeline (no web server — `CommandLineRunner` entry point). It calls OpenAI-compatible Chat Completions APIs (`AiClient` utility). The pipeline has 7 steps orchestrated by `DailyWorkflowService`:

1. **HotspotCollector** — fetches Zhihu/Weibo hot lists via WebClient, parses JSON
2. **TopicGenerator** — prompts AI to generate topics from hotspots
3. **ArticleWriter** — 3-step: outline → body → title (uses `Article.builder()`)
4. **ContentOptimizer** — de-AI-ize text, check sensitive words (string contains), assess originality (Jaccard similarity), append CTAs
5. **Publisher** — strategy pattern with `PlatformPublisher` interface; 3 implementations (Weitoutiao/Maimai/Zhihu) that are **all stubs** (log + return fake URL)
6. **Analytics** — persists to JSON file at `src/main/resources/history/publish_history.json`

`AiCreatorRunner` (in `controller/`) parses CLI args and delegates to `DailyWorkflowService`.

## Key Patterns

- **Constructor injection** via Lombok `@RequiredArgsConstructor` (fields are `final`)
- **Configuration binding** via `@ConfigurationProperties` with prefixes `ai`, `content`, `hotspot`
- **Defensive degradation** everywhere: external calls wrapped in try-catch, fall back to defaults on failure (never throws to caller)
- **AiClient** is a generic OpenAI-compatible wrapper — prompts as strings, responses parsed with Jackson `ObjectMapper`

## Configuration

- `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL` env vars (or set in `application.yml`)
- Sensitive words list in `application.yml` under `sensitive-words`
- Platform-specific article length ranges configured per platform under `content.article-length`

## Known Gaps

- **Publishers are all stubs** — no real platform integration
- **No scheduled task wired up** — `@EnableScheduling` and cron config exist in yml, but no `@Scheduled` method
- **No database** — analytics uses file-based JSON storage
- **No tests** — `src/test/` doesn't exist, though `spring-boot-starter-test` is in pom.xml
- **`templates/` directory is empty** — reserved for writing templates but unused
