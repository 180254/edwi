package pl.edwi.tool;

import org.eclipse.collections.impl.factory.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TomsParser implements ForumParser {

    private final Whitelist whitelist = Whitelist.none();
    private final Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);

    @Override
    public String startUrl() {
        return "http://www.tomshardware.co.uk/forum/";
    }

    @Override
    public boolean isThatUrlForum(String url) {
        return url.contains("tomshardware.co.uk/forum/");
    }

    @Override
    public boolean isThatUrlThread(String url) {
        return url.contains("/forum/id-");
    }

    public List<Element> selectDocPosts(Document document) {
        List<Element> result = Lists.mutable.empty();
        result.addAll(document.select(".thread__content"));
        result.addAll(document.select(".answer__content"));
        return result;
    }

    public List<String> selectPostParagraphs(Element element) {
        String html = element.html();
        String baseUri = "";
        String clean = Jsoup.clean(html, baseUri, whitelist, outputSettings);
        String[] paragraphs = clean.split("\n");

        return Stream.of(paragraphs)
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllParagraphs(Document document) {
        List<String> result = Lists.mutable.empty();

        for (Element post : selectDocPosts(document)) {
            List<String> postParagraphs = selectPostParagraphs(post);
            result.addAll(postParagraphs);
        }

        return result;
    }
}
