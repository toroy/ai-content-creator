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
