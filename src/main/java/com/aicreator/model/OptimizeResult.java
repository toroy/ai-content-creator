package com.aicreator.model;

import lombok.Data;

import java.util.List;

@Data
public class OptimizeResult {
    private String original;
    private String humanized;
    private SensitiveCheck sensitiveCheck;
    private double originalityScore;
    private String finalContent;
    private List<String> suggestions;

    @Data
    public static class SensitiveCheck {
        private boolean passed;
        private List<String> hits;
    }
}
