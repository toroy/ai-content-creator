package com.aicreator.model;

import lombok.Data;

@Data
public class HotspotItem {
    private String source;
    private String title;
    private String url;
    private String heat;
    private String excerpt;
    private String category;
}
