package com.aicreator.core;

import com.aicreator.config.DomainProperties;
import com.aicreator.model.HotspotItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RssCollector implements ContentCollector {

    @Override
    public List<HotspotItem> collect(DomainProperties.DomainDefinition domain) {
        List<HotspotItem> allItems = new ArrayList<>();
        List<DomainProperties.SourceConfig> sources = domain.getSources();
        if (sources == null || sources.isEmpty()) {
            log.warn("[{}] 未配置 RSS 源", domain.getLabel());
            return allItems;
        }

        for (DomainProperties.SourceConfig source : sources) {
            try {
                List<HotspotItem> items = fetchRss(source);
                allItems.addAll(items);
                log.info("[{}] RSS {} 采集到 {} 条", domain.getLabel(), source.getUrl(), items.size());
            } catch (Exception e) {
                log.error("[{}] RSS 采集失败 {}: {}", domain.getLabel(), source.getUrl(), e.getMessage());
            }
        }
        log.info("[{}] RSS 采集共获取 {} 条", domain.getLabel(), allItems.size());
        return allItems;
    }

    private List<HotspotItem> fetchRss(DomainProperties.SourceConfig source) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source.getUrl()))
                .header("User-Agent", "Mozilla/5.0 (compatible; AiContentCreator/1.0)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(response.body());

        List<HotspotItem> items = new ArrayList<>();

        // RSS 2.0: <item> elements
        NodeList itemNodes = doc.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element elem = (Element) itemNodes.item(i);
            HotspotItem hi = new HotspotItem();
            hi.setTitle(getText(elem, "title"));
            hi.setUrl(getText(elem, "link"));
            hi.setExcerpt(truncate(getText(elem, "description"), 100));
            hi.setSource("rss");
            items.add(hi);
        }

        // Atom: <entry> elements
        if (items.isEmpty()) {
            NodeList entryNodes = doc.getElementsByTagName("entry");
            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element elem = (Element) entryNodes.item(i);
                HotspotItem hi = new HotspotItem();
                hi.setTitle(getText(elem, "title"));
                hi.setUrl(getLinkHref(elem));
                hi.setExcerpt(truncate(getText(elem, "summary"), 100));
                hi.setSource("rss");
                items.add(hi);
            }
        }

        return items;
    }

    private String getText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }

    private String getLinkHref(Element parent) {
        NodeList links = parent.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String href = link.getAttribute("href");
            if (href != null && !href.isBlank()) return href;
            String rel = link.getAttribute("rel");
            if (rel == null || rel.isBlank() || "alternate".equals(rel)) {
                return link.getTextContent().trim();
            }
        }
        return "";
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.isBlank()) return "";
        String cleaned = text.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
    }
}
