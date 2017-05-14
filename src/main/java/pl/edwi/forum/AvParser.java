package pl.edwi.forum;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AvParser implements ForumParser {

    private static final Pattern NBSP = Pattern.compile("&nbsp;", Pattern.LITERAL);

    private final Whitelist whitelist = Whitelist.none();
    private final Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);

    @Override
    public List<String> startUrls() {
        MutableList<String> ret = Lists.mutable.empty();

        ret.add("https://www.avforums.com/forums/mobile-phones-forum.106/");
        for (int i = 0; i < 750; i++) {
            ret.add("https://www.avforums.com/forums/mobile-phones-forum.106/page-" + i);
        }

        return ret;
    }

    @Override
    public boolean isThatUrlForum(String url) {
        return isThatUrlThread(url);
    }

    @Override
    public boolean isThatUrlThread(String url) {
        return url.contains("/threads/");
    }

    @Override
    public List<String> getAllParagraphs(Document document) {
        Document doc = document.clone();
        doc.select(".bbCodeQuote").remove();
        Elements posts = doc.select(".messageText");

        String html = posts.html();
        String baseUri = "";
        String clean = Jsoup.clean(html, baseUri, whitelist, outputSettings);
        String[] paragraphs = clean.split("\n");

        return Stream.of(paragraphs)
                .map(p -> NBSP.matcher(p).replaceAll(""))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());
    }
}
