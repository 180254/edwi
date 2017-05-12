package pl.edwi.forum;


import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.stream.Collectors;

public class LttParser implements ForumParser {

    @Override
    public String startUrl() {
        return "https://linustechtips.com/main/";
    }

    @Override
    public boolean isThatUrlForum(String url) {
        return url.contains("/forum/");
    }

    @Override
    public boolean isThatUrlThread(String url) {
        return url.contains("/topic/");
    }

    @Override
    public List<String> getAllParagraphs(Document document) {
        return document.select("article .cPost_contentWrap > div:first-child p")
                .stream()
                .map(Element::text)
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());
    }
}
