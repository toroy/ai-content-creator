# 🤖 AI Content Creator (Java / Spring Boot)

> 基于 Spring Boot 3 + 知识图谱 + AI 的自动化自媒体内容生产系统，支持微头条、脉脉、知乎等多平台。

---

## 📁 项目结构

```
ai-content-creator/
├── pom.xml
├── src/main/java/com/aicreator/
│   ├── AiContentCreatorApplication.java    # 主入口
│   ├── config/                              # 配置层
│   │   ├── AiProperties.java                # AI 配置
│   │   ├── ContentProperties.java           # 内容配置
│   │   ├── HotspotProperties.java           # 热点源配置
│   │   └── AppConfig.java                   # Bean 装配
│   ├── controller/
│   │   └── AiCreatorRunner.java             # 命令行运行器
│   ├── core/                                # 核心业务层
│   │   ├── HotspotCollector.java            # 热点采集
│   │   ├── TopicGenerator.java             # 选题生成
│   │   ├── ArticleWriter.java               # AI 写作
│   │   ├── ContentOptimizer.java            # 内容优化（去AI味、敏感词）
│   │   ├── Publisher.java                   # 多平台发布框架
│   │   └── Analytics.java                   # 数据监控与复盘
│   ├── model/                               # 模型层
│   │   ├── Article.java
│   │   ├── HotspotItem.java
│   │   ├── Topic.java
│   │   └── OptimizeResult.java
│   ├── service/
│   │   └── DailyWorkflowService.java        # 主工作流编排
│   └── util/
│       └── AiClient.java                    # AI API 统一封装
├── src/main/resources/
│   ├── application.yml                       # 全局配置
│   ├── templates/                            # 写作模板
│   └── history/                              # 发布历史
└── README.md
```

---

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- AI API Key（OpenAI / Claude / Kimi / DeepSeek）

### 2. 克隆 & 构建

```bash
git clone https://github.com/yourname/ai-content-creator-java.git
cd ai-content-creator-java
mvn clean package -DskipTests
```

### 3. 配置环境变量

```bash
export AI_API_KEY=your_api_key_here
export AI_BASE_URL=https://api.openai.com/v1
export AI_MODEL=gpt-4o-mini
```

或修改 `src/main/resources/application.yml`：

```yaml
ai:
  api-key: your_api_key_here
  base-url: https://api.openai.com/v1
  model: gpt-4o-mini
```

### 4. 运行

```bash
# 每日全自动工作流
java -jar target/ai-content-creator-1.0.0.jar daily

# 单篇文章生成（交互式）
java -jar target/ai-content-creator-1.0.0.jar single "35岁程序员的出路"

# 数据复盘报告
java -jar target/ai-content-creator-1.0.0.jar report
```

---

## ⚙️ 配置说明

### application.yml 关键配置

```yaml
ai:
  provider: openai          # openai / claude / kimi / deepseek
  api-key: ${AI_API_KEY}
  base-url: ${AI_BASE_URL:https://api.openai.com/v1}
  model: ${AI_MODEL:gpt-4o-mini}
  temperature: 0.7
  max-tokens: 2000

content:
  platform: weitoutiao    # weitoutiao / maimai / zhihu
  target-audience: "30-50岁男性，互联网从业者"
  daily-topic-count: 5
  article-length:
    weitoutiao:
      min: 300
      max: 800
    maimai:
      min: 800
      max: 2000

hotspot:
  sources:
    zhihu:
      url: "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total"
      enabled: true
    weibo:
      url: "https://weibo.com/ajax/side/hotSearch"
      enabled: true
```

---

## 🔄 工作流详解

```
┌─────────────────────────────────────────────┐
│  1. 热点采集（知乎/微博/自定义源）            │
│     ↓                                       │
│  2. AI 选题（筛选爆款潜力话题）               │
│     ↓                                       │
│  3. AI 写作（生成大纲 + 正文）               │
│     ↓                                       │
│  4. 内容优化（人格化改写 + 敏感词检测）       │
│     ↓                                       │
│  5. 标题生成（10个候选，A/B测试）            │
│     ↓                                       │
│  6. 多平台发布（微头条/脉脉/知乎）           │
│     ↓                                       │
│  7. 数据复盘（阅读量/互动率/爆款分析）       │
└─────────────────────────────────────────────┘
```

---

## 🛠️ 核心功能

### 1. 热点雷达（HotspotCollector）

- 自动抓取知乎热榜、微博热搜
- 支持关键词过滤
- 可扩展自定义数据源

### 2. AI 选题引擎（TopicGenerator）

- 基于热点 + 用户画像生成选题
- 评估选题的爆款潜力
- JSON 解析失败自动降级

### 3. 智能写作（ArticleWriter）

- 内置微头条、脉脉写作模板
- 自动生成大纲 + 正文
- 支持系列文章生成

### 4. 人格化改写（ContentOptimizer）

**最关键的功能**：

- 加入个人经历（"我前公司..."）
- 加入口头禅（"说实话"、"说白了"）
- 加入情绪词（"离谱"、"扎心"）
- 敏感词自动检测
- 原创度评估

### 5. 多平台发布（Publisher）

- 微头条 / 脉脉 / 知乎 统一接口
- 内容自动适配各平台风格
- 预留 RPA 接入点

### 6. 数据复盘（Analytics）

- 记录每日发布数据
- 生成周报/月报
- 基于历史数据推荐新选题方向

---

## 📌 使用建议

### 第一阶段：测试期（1-2周）

```bash
# 每天手动运行，人工审核每篇文章
java -jar target/ai-content-creator-1.0.0.jar single
```

### 第二阶段：半自动期（1个月）

```bash
# 使用 daily 模式，人工只做标题选择和最终审核
java -jar target/ai-content-creator-1.0.0.jar daily
```

### 第三阶段：全自动期（长期）

- 接入 Linux crontab / Windows 任务计划程序
- 定时每日 8:30 自动运行
- 定期复盘数据，调整选题策略

```bash
# Linux crontab 示例
crontab -e
# 添加：30 8 * * * /usr/bin/java -jar /path/to/ai-content-creator-1.0.0.jar daily >> /var/log/ai-creator.log 2>&1
```

---

## 🔗 相关工具推荐

| 环节 | 工具 | 说明 |
|------|------|------|
| **RPA 发布** | 影刀 / UiBot | 模拟人工发布到微头条 |
| **配图生成** | Midjourney / 可灵 | AI 生成封面图 |
| **数据分析** | 新榜 / 西瓜数据 | 监控竞品和热点趋势 |
| **多平台分发** | 蚁小二 / 易撰 | 一键多发 |

---

## ⚠️ 注意事项

1. **不要完全依赖 AI**：AI 生成后必须人工审核，尤其是事实性内容
2. **去 AI 味是关键**：直接发布 AI 内容容易被平台限流
3. **遵守平台规则**：避免敏感话题，不要批量刷量
4. **保护 API Key**：不要将 `application.yml` 中的 key 提交到 GitHub

---

## 📄 License

MIT License

---

## 💬 交流

有问题欢迎提 Issue，或联系作者交流自媒体 + AI 的玩法！
