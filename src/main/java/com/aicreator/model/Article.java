package com.aicreator.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Article {
    private String topic;
    private String platform;
    private List<String> titles;
    private String hook;
    private String outline;
    private String content;
    private int wordCount;
    private List<String> keywords;
}
