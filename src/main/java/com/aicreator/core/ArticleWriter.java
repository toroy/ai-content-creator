package com.aicreator.core;

import com.aicreator.config.ContentProperties;
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
}
