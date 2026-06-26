package com.aicreator.core;

import com.aicreator.config.ContentProperties;
import com.aicreator.config.DomainProperties;
import com.aicreator.model.Article;
import com.aicreator.util.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleWriter {

    private final AiClient ai;
    private final ContentProperties contentProps;
    private final FeedbackEngine feedbackEngine;

    public Article write(String topic, String platform, String style) {
        ContentProperties.LengthRange length = contentProps.getArticleLength().getOrDefault(platform, new ContentProperties.LengthRange());
        length.setMin(length.getMin() > 0 ? length.getMin() : 300);
        length.setMax(length.getMax() > 0 ? length.getMax() : 800);

        // 生成大纲
        String outline = generateOutline(topic, platform);
        // 生成正文
        String content = generateContent(topic, outline, length, platform);
        // 生成标题
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

    private String generateOutline(String topic, String platform) {
        String prompt = String.format("""
            请为"%s"生成一个%s风格的文章大纲。
            要求：
            - 结构清晰，有起承转合
            - 适合%s平台的阅读习惯
            - 每个部分标注预计字数
            输出格式：
            一、XXX（约XX字）
            二、XXX（约XX字）
            ...
            """, topic, platform, platform);
        return ai.chat(prompt, 0.5, 1000);
    }

    private String generateContent(String topic, String outline, ContentProperties.LengthRange length, String platform) {
        String prompt = String.format("""
            请根据以下大纲，撰写一篇完整的文章。
            主题：%s
            大纲：%s
            平台：%s
            字数要求：%d-%d字
            写作要求：
            1. 开头必须有钩子（故事/冲突/数据/疑问）
            2. 中间有细节和案例，不要泛泛而谈
            3. 结尾有金句总结 + 引导评论
            4. 语气像一位40岁互联网老兵在聊天
            5. 适当使用 emoji 和网络用语
            6. 段落要短，适合手机阅读
            请直接输出正文，不要包含大纲。
            """, topic, outline, platform, length.getMin(), length.getMax());
        return ai.chat(prompt, 0.7, 2500);
    }

    public Article write(String topic, DomainProperties.DomainDefinition domain) {
        String platform = domain.getPlatforms().isEmpty() ? "weitoutiao" : domain.getPlatforms().get(0);
        ContentProperties.LengthRange length;
        if (domain.getLength() != null) {
            length = new ContentProperties.LengthRange();
            length.setMin(domain.getLength().getMin());
            length.setMax(domain.getLength().getMax());
        } else {
            length = contentProps.getArticleLength().getOrDefault(platform, new ContentProperties.LengthRange());
        }

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
}
