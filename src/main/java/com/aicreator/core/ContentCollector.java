package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;

import java.util.List;

public interface ContentCollector {
    List<HotspotItem> collect(DomainProperties.DomainDefinition domain);
}
