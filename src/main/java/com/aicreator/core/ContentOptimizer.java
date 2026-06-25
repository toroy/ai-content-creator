package com.aicreator.core;

import com.aicreator.model.OptimizeResult;
import com.aicreator.util.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ContentOptimizer {

    private final AiClient ai;
    private final List<String> sensitiveWords;

    public OptimizeResult optimize(String content, String persona) {
        OptimizeResult result = new OptimizeResult();
        result.setOriginal(content);
        result.setSuggestions(new ArrayList<>());

        // 1. 人格化改写
        String humanized = ai.humanize(content, persona);
        result.setHumanized(humanized);

        // 2. 敏感词检测
        OptimizeResult.SensitiveCheck check = new OptimizeResult.SensitiveCheck();
        List<String> hits = new ArrayList<>();
        for (String word : sensitiveWords) {
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

        // 3. 原创度评估（简化：与原文对比字符重合度）
        double similarity = calculateSimilarity(content, humanized);
        result.setOriginalityScore(1.0 - similarity);
        if (similarity > 0.9) {
            result.getSuggestions().add("改写程度不够，建议增加更多个人表达");
        }

        // 4. 格式化
        result.setFinalContent(format(humanized));
        return result;
    }

    private String format(String text) {
        return text.replaceAll("\s{3,}", "\n\n").trim();
    }

    private double calculateSimilarity(String a, String b) {
        // 简化：字符集合 Jaccard 相似度
        java.util.Set<Character> setA = new java.util.HashSet<>();
        for (char c : a.toCharArray()) setA.add(c);
        java.util.Set<Character> setB = new java.util.HashSet<>();
        for (char c : b.toCharArray()) setB.add(c);

        java.util.Set<Character> intersection = new java.util.HashSet<>(setA);
        intersection.retainAll(setB);
        java.util.Set<Character> union = new java.util.HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    public String addCta(String content, String ctaType) {
        String cta = switch (ctaType) {
            case "comment" -> "\n\n你怎么看？欢迎在评论区聊聊。";
            case "follow" -> "\n\n觉得有用就点个关注，持续分享干货。";
            case "share" -> "\n\n如果对你有帮助，欢迎转发给需要的朋友。";
            case "private" -> "\n\n整理了资料，需要的朋友私信\"资料\"领取。";
            default -> "";
        };
        return content + cta;
    }

    public List<String> extractKeywords(String content) {
        String prompt = String.format("""
            从以下文章中提取 5-8 个关键词/标签，用于社交媒体发布：
            %s
            只输出标签列表，用逗号分隔。
            """, content.substring(0, Math.min(content.length(), 500)));
        String result = ai.chat(prompt, 0.3, 500);
        return List.of(result.split("，|,"));
    }
}
