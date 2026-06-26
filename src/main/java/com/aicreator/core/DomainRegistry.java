package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainRegistry {

    private final DomainProperties domainProps;

    public List<DomainProperties.DomainDefinition> getEnabled() {
        if (domainProps.getDomains() == null || domainProps.getDomains().isEmpty()) {
            log.warn("⚠️ 未配置任何领域");
            return List.of();
        }
        return domainProps.getDomains().values().stream()
                .filter(DomainProperties.DomainDefinition::isEnabled)
                .toList();
    }

    public Optional<DomainProperties.DomainDefinition> getByName(String name) {
        if (domainProps.getDomains() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(domainProps.getDomains().get(name));
    }

    public List<String> getDomainNames() {
        if (domainProps.getDomains() == null) {
            return List.of();
        }
        return List.copyOf(domainProps.getDomains().keySet());
    }
}
