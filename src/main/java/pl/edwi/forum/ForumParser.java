package pl.edwi.forum;


import org.jsoup.nodes.Document;

import java.util.List;

public interface ForumParser {

    List<String> startUrls();

    boolean isThatUrlForum(String url);

    boolean isThatUrlThread(String url);

    List<String> getAllParagraphs(Document document);
}
